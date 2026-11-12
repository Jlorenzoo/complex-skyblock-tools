package com.moneybooster;

import com.skyblocktoolkit.CompactMoney;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoosterStatsManager {
    private static final Pattern CHAT_EFFECT_PATTERN = Pattern.compile(
            "(?i)^(?:[✔✓]\\s*)?You have gained (?:the effect of )?(?:a )?([^!]+) for (\\d+) second"
    );

    private static final RollingEarnings baseAverage = new RollingEarnings();
    public static final EarningsActivity earningsActivity = new EarningsActivity();
    private static long averageUpdated;
    private static double averageRate, baseAverageRate;
    private static final SmoothedRate cashDisplay = new SmoothedRate(SmoothedRate.RESPONSIVE_TAU_MS);
    private static final SmoothedRate baseDisplay = new SmoothedRate(SmoothedRate.RESPONSIVE_TAU_MS);
    private static final SmoothedRate shardDisplay = new SmoothedRate();
    public static void resetAverage() { baseAverage.reset(System.currentTimeMillis()); earningsActivity.reset();averageUpdated=0;averageRate=baseAverageRate=0;cashDisplay.reset();baseDisplay.reset();shardDisplay.reset(); }
    public static String averageLabel() {
        var config = com.skyblocktoolkit.ToolkitConfig.get();
        long now = System.currentTimeMillis();
        boolean rounded = isBalanceRounded();
        boolean cashReady = rounded ? baseAverage.estimatedReady(now) : baseAverage.ready(now);
        boolean boosted = ModConfig.getInstance().includesBoostersInAverage();
        boolean shardsReady = config.includeGoldShards && com.skyblocktoolkit.GoldShardTracker.ready();
        double keyRate = config.includeVoteKeys ? com.skyblocktoolkit.VoteKeyTracker.hourlyValue(now) : 0;
        boolean keysReady = keyRate > 0;
        if (!shardsReady) shardDisplay.reset();
        if (!cashReady && !shardsReady && !keysReady) return "Earning: " + (rounded ? "waiting" : "--/h");

        double rate = cashReady ? (boosted ? averageRate : baseAverageRate) : 0;
        if (shardsReady) rate += shardDisplay.update(com.skyblocktoolkit.GoldShardTracker.hourlyValue(), now);
        rate += keyRate;
        String estimate = rounded || boosted || shardsReady || keysReady ? "~" : "";
        return "Earning: " + estimate + formatMoney(rate) + "/h" + (cashReady ? "" : " (cash --)");
    }
    private static double lastKnownBalance = -1;
    private static double balanceResolution = 0.01;
    public static boolean isBalanceRounded() { return balanceResolution > 0.01 + 1e-9; }
    private static final MoneyLedger ledger = new MoneyLedger(BoosterStatsManager::onSettled);
    private static int excludedTransactions;
    private static double excludedVolume;
    private static boolean wasBoosterActive = false;

    // Current Minecraft process only; reconnecting does not start a new session.
    private static double sessionTotalEarned = 0;
    private static double sessionBonusEarned = 0;
    private static int sessionBoosterCount = 0;
    private static long sessionBoosterSeconds = 0;
    // Persistent coconut totals, independent of the current session.
    private static int coconutTotalEffects = 0;
    private static int coconutBoosterProcs = 0;

    private static final Path STATS_PATH = FabricLoader.getInstance().getConfigDir().resolve("moneyboosterstats.json");

    private static final BoosterHistory history = new BoosterHistory(STATS_PATH.resolveSibling("moneyboosterhistory.json"));
    public static BoosterHistory history() { return history; }

    static {
        load();
    }

    public static double getSessionTotalEarned() {
        return sessionTotalEarned;
    }

    public static double getSessionBaseEarned() {
        return Math.max(0, sessionTotalEarned - sessionBonusEarned);
    }

    public static double getSessionBonusEarned() {
        return sessionBonusEarned;
    }

    public static int getSessionBoosterCount() {
        return sessionBoosterCount;
    }

    public static long getSessionBoosterSeconds() {
        return sessionBoosterSeconds;
    }

    public static int getCoconutTotalEffects() {
        return coconutTotalEffects;
    }

    public static int getCoconutBoosterProcs() {
        return coconutBoosterProcs;
    }

    public static double getLastKnownBalance() {
        return lastKnownBalance;
    }

    public static void resetStats() {
        history.clear(System.currentTimeMillis());
        sessionTotalEarned = 0;
        sessionBonusEarned = 0;
        sessionBoosterCount = 0;
        sessionBoosterSeconds = 0;
        coconutTotalEffects = 0;
        coconutBoosterProcs = 0;
        ledger.reset();
        lastKnownBalance = -1;
        balanceResolution = 0.01;
        wasBoosterActive = false;
        excludedTransactions = 0;
        excludedVolume = 0;
        save();
    }

    public static void recordBoosterDuration(long seconds) {
        sessionBoosterSeconds += seconds;
        save();
    }

    public static void onChatMessage(String text) {
        if (text == null || text.isEmpty()) return;

        double shopDelta = MoneyLedger.shopDelta(text);
        if (Double.isFinite(shopDelta)) {
            ledger.shop(MoneyLedger.parseTransaction(text), System.currentTimeMillis(), BoosterManager.isActive() ? BoosterManager.getPercentage() : 0, RateTracker.getActiveTestId(), history.activeId());
            excludedTransactions++;
            excludedVolume += Math.abs(shopDelta);
            RateTracker.onShopExcluded(shopDelta);
            return;
        }
    }

    public static void recordEffect(int pct, long seconds, String effect, BoosterSources.Source source) {
        history.start(source.type(), source.name(), source.icon(), source.inferred(), pct, seconds, effect, System.currentTimeMillis());
        if (source.type().equals("Coconut")) { coconutTotalEffects++; if (pct > 0) coconutBoosterProcs++; }
        if (pct > 0) { sessionBoosterCount++; sessionBoosterSeconds += Math.max(0, seconds); }
        save();
        markContext();
    }

    public static void checkScoreboardLine(String line) {
        var reading = BalanceReading.parse(line);
        if (reading != null) onBalanceUpdated(reading.amount(), reading.resolution());
    }
    public static void checkScoreboardComponent(net.minecraft.network.chat.Component component) {
        var reading = BalanceComponents.read(component);
        if (reading != null) onBalanceUpdated(reading.amount(), reading.resolution());
    }

    public static int getExcludedTransactions() { return excludedTransactions; }
    public static double getExcludedVolume() { return excludedVolume; }
    public static boolean isSettlingTest(int id) { return ledger.pendingForTest(id); }
    public static void farmingActivity(long now) {
        if (earningsActivity.cropActivity(now)) ledger.resumeFarming(now);
    }
    public static void markContext() {
        if (lastKnownBalance >= 0) ledger.observe(lastKnownBalance, System.currentTimeMillis(), BoosterManager.isActive() ? BoosterManager.getPercentage() : 0, RateTracker.getActiveTestId(), history.activeId(), isBalanceRounded());
    }
    public static void tick() {
        long now = System.currentTimeMillis();
        if (!BoosterManager.isActive() && history.activeId() >= 0) {
            history.finish(now, "Finished"); markContext();
        } else if (BoosterManager.isActive() && history.activeId() < 0 && BoosterManager.getPercentage() > 0) {
            ensureActiveHistory(BoosterManager.getPercentage(), now);
        }
        ledger.tick(now); history.tick(now);
        if (now-averageUpdated>=1000) {
            boolean ready=isBalanceRounded()?baseAverage.estimatedReady(now):baseAverage.ready(now);
            if(!ready){cashDisplay.reset();baseDisplay.reset();averageRate=baseAverageRate=0;}
            else {
                double baseTarget = isBalanceRounded() ? baseAverage.responsiveEstimatedPerHour(now) : baseAverage.responsivePerHour(now);
                // Project the observed base pace at the current money boost. Historical
                // receipts still carry the multiplier at the time they were earned.
                double currentMultiplier = 1.0 + (BoosterManager.isActive() ? BoosterManager.getPercentage() / 100.0 : 0.0);
                double boostedTarget = baseTarget * currentMultiplier;
                baseAverageRate = baseDisplay.update(baseTarget, now);
                averageRate = cashDisplay.update(boostedTarget, now);
            }
            averageUpdated=now;
        }
    }
    public static void resetConnection() {
        history.finish(System.currentTimeMillis(), "Interrupted"); history.save(System.currentTimeMillis());
        resetAverage(); BoosterSources.reset(); ledger.reset(); lastKnownBalance = -1; wasBoosterActive = false;
        balanceResolution = 0.01;
    }
    public static void onBalanceUpdated(double newBal) {
        onBalanceUpdated(newBal, 0.01);
    }
    private static void onBalanceUpdated(double newBal, double resolution) {
        if (!Double.isFinite(newBal) || newBal < 0) return;
        // Abbreviated balances omit trailing zeroes (79.19M -> 79.2M -> 79.21M).
        // Changing printed decimal places must not discard consecutive cash buckets.
        // Switching between full cash and abbreviations still needs a new baseline.
        boolean rounded=resolution>0.01+1e-9;
        if (lastKnownBalance >= 0 && isBalanceRounded()!=rounded) { ledger.reset(); resetAverage(); }
        balanceResolution = resolution;
        boolean active = BoosterManager.isActive();
        if (active && history.activeId() < 0) {
            ensureActiveHistory(BoosterManager.getPercentage(), System.currentTimeMillis());
        }
        wasBoosterActive = active;
        ledger.observe(newBal, System.currentTimeMillis(), active ? BoosterManager.getPercentage() : 0, RateTracker.getActiveTestId(), history.activeId(), isBalanceRounded());
        lastKnownBalance = newBal;
    }
    public static void ensureActiveHistory(int pct, long now) {
        if (pct <= 0 || history.activeId() >= 0) return;
        if (history.tryResume(pct, now, 45_000)) {
            markContext();
            return;
        }
        BoosterSources.Source src = resolveScoreboardSource(pct, now);
        recordEffect(pct, -1, "Money booster", src);
    }
    private static BoosterSources.Source resolveScoreboardSource(int pct, long now) {
        BoosterHistory.Entry latest = history.latestMoneyBooster();
        if (latest != null && latest.percentage == pct && "Coconut".equals(latest.source) && (now - latest.lastSeen <= 120_000)) {
            return new BoosterSources.Source("Coconut", latest.itemName != null ? latest.itemName : "Coconut", latest.iconJson, true);
        }
        return new BoosterSources.Source("Unknown source", "Detected on scoreboard", null, false);
    }
    private static void onSettled(MoneyLedger.Receipt receipt) {
        com.skyblocktoolkit.ItemStatsTracker.money(receipt.earned(),receipt.percentage(),receipt.fromMs(),receipt.toMs());
        earningsActivity.cropIncome(receipt.toMs());
        if(receipt.estimated()) {
            baseAverage.addEstimated(receipt.earned()/(1+receipt.percentage()/100.0),receipt.fromMs(),receipt.toMs());
        } else {
            baseAverage.add(receipt.earned()/(1+receipt.percentage()/100.0),receipt.fromMs(),receipt.toMs());
        }
        RateTracker.onSettledDelta(receipt.earned(), receipt.percentage(), receipt.testId(), receipt.estimated());
        if (receipt.percentage() > 0) {
            sessionTotalEarned += receipt.earned();
            double bonus = receipt.earned() * receipt.percentage() / (100.0 + receipt.percentage());
            sessionBonusEarned += bonus;
            history.addBonus(receipt.boosterId(), bonus);
            save();
        }
    }

    public static String formatMoney(double amount) {
        return "$" + CompactMoney.display(amount);
    }

    public static double parseCost(String input) {
        input = input.trim().replace("$", "").replace(",", "").toLowerCase();
        double multiplier = 1.0;
        if (input.endsWith("b")) {
            multiplier = 1_000_000_000.0;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("m")) {
            multiplier = 1_000_000.0;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("k")) {
            multiplier = 1_000.0;
            input = input.substring(0, input.length() - 1);
        }
        return Double.parseDouble(input) * multiplier;
    }

    public static void load() {
        if (!Files.exists(STATS_PATH)) return;
        try {
            String content = Files.readString(STATS_PATH);
            // Ignore session fields in older files: each Minecraft launch starts at zero.
            coconutTotalEffects = (int) parseDouble(content, "coconutTotalEffects", 0);
            coconutBoosterProcs = (int) parseDouble(content, "coconutBoosterProcs", 0);
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            String json = "{\n" +
                    "  \"coconutTotalEffects\": " + coconutTotalEffects + ",\n" +
                    "  \"coconutBoosterProcs\": " + coconutBoosterProcs + "\n" +
                    "}\n";
            SafeFiles.write(STATS_PATH, json);
        } catch (IOException e) {
            System.err.println("[MoneyBoosterHUD] Cannot save stats: " + e.getMessage());
        }
    }

    private static double parseDouble(String json, String key, double def) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*([0-9.eE+\\-]+)").matcher(json);
        if (m.find()) {
            double value = Double.parseDouble(m.group(1));
            return Double.isFinite(value) && value >= 0 ? value : def;
        }
        return def;
    }
}
