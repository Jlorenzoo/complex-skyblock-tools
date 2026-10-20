package com.moneybooster;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoosterStatsManager {
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
            "(?i)Balance:\\s*\\$([0-9.,]+)\\s*([KMBkmb])?"
    );
    private static final Pattern CHAT_EFFECT_PATTERN = Pattern.compile(
            "(?i)^(?:[✔✓]\\s*)?You have gained (?:the effect of )?(?:a )?([^!]+) for (\\d+) second"
    );

    private static final RollingEarnings average = new RollingEarnings(), baseAverage = new RollingEarnings();
    public static final EarningsActivity earningsActivity = new EarningsActivity();
    private static long averageUpdated;
    private static double averageRate, baseAverageRate;
    public static void resetAverage() { average.reset(System.currentTimeMillis());baseAverage.reset(System.currentTimeMillis()); earningsActivity.reset();averageUpdated=0;averageRate=baseAverageRate=0; }
    public static String averageLabel() { return (ModConfig.getInstance().includesBoostersInAverage()?"Crops (10s): ":"Crops base (10s): ") + (average.ready(System.currentTimeMillis()) ? formatMoney(ModConfig.getInstance().includesBoostersInAverage()?averageRate:baseAverageRate) : "--") + "/h"; }
    private static double lastKnownBalance = -1;
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
        if (line == null) return;
        Matcher m = BALANCE_PATTERN.matcher(line);
        if (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1).replace(",", ""));
                String unit = m.group(2);
                if (unit != null) {
                    char u = Character.toUpperCase(unit.charAt(0));
                    if (u == 'K') val *= 1_000.0;
                    else if (u == 'M') val *= 1_000_000.0;
                    else if (u == 'B') val *= 1_000_000_000.0;
                }
                onBalanceUpdated(val);
            } catch (Exception ignored) {
            }
        }
    }

    public static int getExcludedTransactions() { return excludedTransactions; }
    public static double getExcludedVolume() { return excludedVolume; }
    public static boolean isSettlingTest(int id) { return ledger.pendingForTest(id); }
    public static void markContext() {
        if (lastKnownBalance >= 0) ledger.observe(lastKnownBalance, System.currentTimeMillis(), BoosterManager.isActive() ? BoosterManager.getPercentage() : 0, RateTracker.getActiveTestId(), history.activeId());
    }
    public static void tick() {
        long now = System.currentTimeMillis();
        if (!BoosterManager.isActive() && history.activeId() >= 0) {
            history.finish(now, "Finished"); markContext();
        } else if (BoosterManager.isActive() && history.activeId() < 0 && BoosterManager.getPercentage() > 0) {
            ensureActiveHistory(BoosterManager.getPercentage(), now);
        }
        ledger.tick(now); history.tick(now);
        if (now-averageUpdated>=1000) { averageRate=average.perHour(now);baseAverageRate=baseAverage.perHour(now);averageUpdated=now; }
    }
    public static void resetConnection() {
        history.finish(System.currentTimeMillis(), "Interrupted"); history.save(System.currentTimeMillis());
        resetAverage(); BoosterSources.reset(); ledger.reset(); lastKnownBalance = -1; wasBoosterActive = false;
    }
    public static void onBalanceUpdated(double newBal) {
        if (!Double.isFinite(newBal) || newBal < 0) return;
        boolean active = BoosterManager.isActive();
        if (active && history.activeId() < 0) {
            ensureActiveHistory(BoosterManager.getPercentage(), System.currentTimeMillis());
        }
        wasBoosterActive = active;
        ledger.observe(newBal, System.currentTimeMillis(), active ? BoosterManager.getPercentage() : 0, RateTracker.getActiveTestId(), history.activeId());
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
        earningsActivity.cropIncome(receipt.toMs());
        average.add(receipt.earned(),receipt.fromMs(),receipt.toMs());baseAverage.add(receipt.earned()/(1+receipt.percentage()/100.0),receipt.fromMs(),receipt.toMs());
        RateTracker.onSettledDelta(receipt.earned(), receipt.percentage(), receipt.testId());
        if (receipt.percentage() > 0) {
            sessionTotalEarned += receipt.earned();
            double bonus = receipt.earned() * receipt.percentage() / (100.0 + receipt.percentage());
            sessionBonusEarned += bonus;
            history.addBonus(receipt.boosterId(), bonus);
            save();
        }
    }

    public static String formatMoney(double amount) {
        if (amount >= 1_000_000_000) {
            return String.format(Locale.ROOT, "$%.2fB", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000) {
            return String.format(Locale.ROOT, "$%.2fM", amount / 1_000_000.0);
        } else if (amount >= 1_000) {
            return String.format(Locale.ROOT, "$%.1fK", amount / 1_000.0);
        } else {
            return String.format(Locale.ROOT, "$%.0f", amount);
        }
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
