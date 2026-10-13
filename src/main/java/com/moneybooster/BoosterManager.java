package com.moneybooster;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoosterManager {
    private static final Pattern CHAT_OTHER_EFFECT = Pattern.compile("(?i)^(?:[✔✓]\\s*)?You have gained (?:the effect of )?(?:a )?([^!]+) for (\\d+) second");
    private static final Pattern CHAT_GAIN_PATTERN = Pattern.compile(
            "(?i)^(?:[✔✓]\\s*)?You have gained the effect of a (\\d+)% Money Booster for (\\d+) second"
    );
    private static final Pattern CHAT_SERVER_PCT_PATTERN = Pattern.compile(
            "(?i)receive \\+(\\d+)% Shop Money"
    );
    private static final Pattern CHAT_SERVER_DURATION_PATTERN = Pattern.compile(
            "(?i)Your Booster will end in:\\s*(\\d+)\\s*minute"
    );
    private static final BoosterClock clock = new BoosterClock();
    private static int percentage = 0;
    private static int pendingServerBoosterPct = 0;

    private static int tickCounter = 0;

    public static boolean isActive() { return clock.active(System.currentTimeMillis()); }
    public static boolean hasTimer() { return clock.hasTimer(System.currentTimeMillis()); }
    public static int getPercentage() { return percentage; }
    public static long getRemainingMs(long now) { return clock.remaining(now); }
    public static float getProgress(long now) { return clock.progress(now); }

    public static String formatTime(long remainingMs) {
        if (!hasTimer()) return "Active";
        if (remainingMs <= 0) return "0.0s";
        long totalSecs = remainingMs / 1000;
        if (totalSecs < 10) {
            return String.format(Locale.ROOT, "%.1fs", remainingMs / 1000.0);
        } else if (totalSecs < 60) {
            return totalSecs + "s";
        } else {
            long mins = totalSecs / 60;
            long secs = totalSecs % 60;
            return mins + "m " + (secs < 10 ? "0" : "") + secs + "s";
        }
    }

    public static void setTestBooster(int pct, int seconds) {
        percentage = pct; clock.start(seconds, System.currentTimeMillis());
        BoosterStatsManager.recordEffect(pct, seconds, "Money booster", new BoosterSources.Source("Test", "Test booster", null, false));
    }
    public static void reset() {
        percentage = 0; clock.reset(); pendingServerBoosterPct = 0; tickCounter = 0;
    }

    public static void onChatMessage(String text) {
        if (text == null || text.isEmpty()) return;
        text = text.replaceAll("§.", "");

        BoosterStatsManager.onChatMessage(text);

        // 1. Check exact gain pattern: "You have gained the effect of a 34% Money Booster for 16 seconds!"
        Matcher gainMatcher = CHAT_GAIN_PATTERN.matcher(text);
        if (gainMatcher.find()) {
            int pct = safeInt(gainMatcher.group(1));
            int seconds = safeInt(gainMatcher.group(2));
            if (pct <= 0 || pct > 100000 || seconds <= 0 || seconds > 86400) return;
            percentage = pct;
            clock.start(seconds, System.currentTimeMillis());
            BoosterStatsManager.recordEffect(pct, seconds, "Money booster", BoosterSources.recent());
            return;
        }

        Matcher otherEffect = CHAT_OTHER_EFFECT.matcher(text);
        if (otherEffect.find()) {
            int seconds = safeInt(otherEffect.group(2));
            if (seconds > 0 && seconds <= 86400) BoosterStatsManager.recordEffect(0, seconds, otherEffect.group(1), BoosterSources.recent());
            return;
        }

        // 2. Check server-wide / shop money booster notifications
        Matcher shopPctMatcher = CHAT_SERVER_PCT_PATTERN.matcher(text);
        if (shopPctMatcher.find()) {
            pendingServerBoosterPct = safeInt(shopPctMatcher.group(1));
        }

        Matcher serverDurMatcher = CHAT_SERVER_DURATION_PATTERN.matcher(text);
        if (serverDurMatcher.find()) {
            int minutes = safeInt(serverDurMatcher.group(1));
            if (minutes <= 0 || minutes > 1440) return;
            int pct = pendingServerBoosterPct > 0 ? pendingServerBoosterPct : (percentage > 0 ? percentage : 20);
            percentage = pct;
            clock.start(minutes * 60L, System.currentTimeMillis());
            pendingServerBoosterPct = 0;
            BoosterStatsManager.recordEffect(pct, minutes * 60L, "Money booster", new BoosterSources.Source("Server booster", "Server booster", null, false));
        }
    }

    private static int safeInt(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return 0; }
    }

    public static void onClientTick(Minecraft client) {
        tickCounter++;
        // Check scoreboard every 5 ticks (0.25 seconds)
        if (tickCounter % 5 != 0) {
            return;
        }

        if (client.level == null) {
            return;
        }

        Scoreboard scoreboard = client.level.getScoreboard();
        if (scoreboard == null) {
            return;
        }

        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            return;
        }

        Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(objective);
        java.util.List<String> lines = scores.stream().filter(entry -> !entry.isHidden())
            .sorted(java.util.Comparator.comparingInt(PlayerScoreEntry::value).reversed())
            .map(entry -> {
                PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
                return PlayerTeam.formatNameForTeam(team, entry.ownerName()).getString();
            }).toList();
        BoosterSidebar.State state = BoosterSidebar.parse(lines);
        clock.observe(state.active(), state.remainingMs(), state.exactSeconds(), System.currentTimeMillis());
        if (state.percentage() > 0) percentage = state.percentage();
        for (String line : lines) BoosterStatsManager.checkScoreboardLine(line);
    }
}
