package com.moneybooster;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoosterManager {
    private static final Pattern CHAT_OTHER_EFFECT = Pattern.compile("(?i)^(?:[✔✓]\\s*)?You have gained (?:the effect of )?(?:a )?([^!]+) for (\\d+) second");
    public static final BoosterTimers timers = new BoosterTimers();

    private static int tickCounter = 0;

    // The existing accounting API always refers to MONEY, regardless of XP state.
    public static boolean isActive() { return timers.clock(BoosterTimers.Type.MONEY).active(System.currentTimeMillis()); }
    public static boolean hasTimer() { return timers.clock(BoosterTimers.Type.MONEY).hasTimer(System.currentTimeMillis()); }
    public static int getPercentage() { return timers.percentage(BoosterTimers.Type.MONEY); }
    public static long getRemainingMs(long now) { return timers.clock(BoosterTimers.Type.MONEY).remaining(now); }
    public static float getProgress(long now) { return timers.clock(BoosterTimers.Type.MONEY).progress(now); }

    public static String formatTime(long remainingMs) {
        return BoosterTimers.formatTime(remainingMs, hasTimer());
    }

    public static void setTestBooster(int pct, int seconds) {
        timers.start(BoosterTimers.Type.MONEY, pct, seconds, System.currentTimeMillis());
        BoosterStatsManager.recordEffect(pct, seconds, "Money booster", new BoosterSources.Source("Test", "Test booster", null, false));
    }
    public static void reset() {
        timers.reset(); tickCounter = 0;
    }

    public static void onChatMessage(String text) {
        if (text == null || text.isEmpty()) return;
        text = text.replaceAll("§.", "").strip();

        BoosterStatsManager.onChatMessage(text);

        var activation = timers.onChat(text, System.currentTimeMillis());
        if (activation != null) {
            boolean money = activation.type() == BoosterTimers.Type.MONEY;
            var source = activation.server()
                ? new BoosterSources.Source("Server booster", "Server booster", null, false) : BoosterSources.recent();
            // XP can be recorded as a non-money effect without replacing the active money ledger.
            BoosterStatsManager.recordEffect(money ? activation.percentage() : 0, activation.seconds(),
                money ? "Money booster" : activation.percentage() + "% Experience booster", source);
            return;
        }

        Matcher otherEffect = CHAT_OTHER_EFFECT.matcher(text);
        if (otherEffect.find()) {
            int seconds = safeInt(otherEffect.group(2));
            if (seconds > 0 && seconds <= 86400) BoosterStatsManager.recordEffect(0, seconds, otherEffect.group(1), BoosterSources.recent());
            return;
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
        long now = System.currentTimeMillis();
        for (var type : BoosterTimers.Type.values()) timers.observe(type, BoosterSidebar.parse(lines, type), now);
        for (String line : lines) BoosterStatsManager.checkScoreboardLine(line);
    }
}
