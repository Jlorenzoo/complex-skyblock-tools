package com.moneybooster;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class RateTracker {
    public enum Duration {
        ONE_MIN(60, "1 Min"),
        FIVE_MIN(300, "5 Min"),
        FREE_RUN(0, "Free Run");

        public final int seconds;
        public final String label;

        Duration(int seconds, String label) {
            this.seconds = seconds;
            this.label = label;
        }
    }

    private static int testId = 0;
    private static int excludedTransactions;
    private static double excludedVolume;
    public static int getActiveTestId() { return running ? testId : -1; }
    public static boolean isSettling() { return completed && BoosterStatsManager.isSettlingTest(testId); }
    public static int getExcludedTransactions() { return excludedTransactions; }
    public static double getExcludedVolume() { return excludedVolume; }
    public static void onShopExcluded(double delta) {
        if (running) { excludedTransactions++; excludedVolume += Math.abs(delta); }
    }
    public static void disconnect() {
        preparing = false; running = false; completed = false; testId++;
    }

    private static Duration selectedDuration = Duration.ONE_MIN;
    private static boolean preparing = false;
    private static long prepStartTimeMs = 0;
    private static boolean running = false;
    private static boolean completed = false;
    private static long startTimeMs = 0;
    private static long stopTimeMs = 0;
    private static double baseEarned = 0;
    private static double totalEarned = 0;
    private static double finalRatePerHour = 0;
    private static long showFinishedUntilMs = 0;

    public static Duration getSelectedDuration() {
        return selectedDuration;
    }

    public static void setSelectedDuration(Duration duration) {
        if (!running && !preparing) {
            selectedDuration = duration;
        }
    }

    public static boolean isPreparing() {
        return preparing;
    }

    public static int getPrepCountdownSeconds() {
        if (!preparing) return 0;
        long elapsed = System.currentTimeMillis() - prepStartTimeMs;
        long remaining = Math.max(0, 3000L - elapsed);
        return (int) Math.ceil(remaining / 1000.0);
    }

    public static boolean isRunning() {
        return running;
    }

    public static boolean isCompleted() {
        return completed;
    }

    public static boolean shouldShowHud() {
        if (preparing || running) return true;
        return completed && System.currentTimeMillis() < showFinishedUntilMs;
    }

    public static void startCountdown() {
        if (running || preparing || isSettling() || Minecraft.getInstance().level == null) return;
        testId++; excludedTransactions = 0; excludedVolume = 0;
        preparing = true;
        running = false;
        completed = false;
        prepStartTimeMs = System.currentTimeMillis();
        startTimeMs = 0;
        stopTimeMs = 0;
        baseEarned = 0;
        totalEarned = 0;
        finalRatePerHour = 0;
        showFinishedUntilMs = 0;
    }

    private static void actuallyStart() {
        preparing = false;
        running = true;
        completed = false;
        startTimeMs = System.currentTimeMillis();
        BoosterStatsManager.markContext();
        stopTimeMs = 0;
        baseEarned = 0;
        totalEarned = 0;
        finalRatePerHour = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendOverlayMessage(
                    Component.literal("§a§lGO! §fRate Test Started (" + selectedDuration.label + ")")
            );
        }
    }

    public static void stop() {
        preparing = false;
        if (!running) return;
        running = false;
        BoosterStatsManager.markContext();
        completed = true;
        stopTimeMs = System.currentTimeMillis();
        showFinishedUntilMs = System.currentTimeMillis() + 6000L;

        long elapsedSec = getElapsedSeconds();
        double elapsed = (stopTimeMs - startTimeMs) / 1000.0;
        if (elapsed > 0) {
            finalRatePerHour = (baseEarned / elapsed) * 3600.0;
        } else {
            finalRatePerHour = 0;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendOverlayMessage(
                    Component.literal("§a[Test stopped; results settling] §fBase Rate: §a§l" + BoosterStatsManager.formatMoney(finalRatePerHour) + " / hr")
            );
        }
    }

    public static long getElapsedSeconds() {
        if (!running && !completed) return 0;
        long end = running ? System.currentTimeMillis() : stopTimeMs;
        return Math.max(0, (end - startTimeMs) / 1000);
    }

    public static long getRemainingSeconds() {
        if (!running) return 0;
        if (selectedDuration.seconds <= 0) return getElapsedSeconds();
        return Math.max(0, selectedDuration.seconds - getElapsedSeconds());
    }

    public static double getBaseEarned() {
        return baseEarned;
    }

    public static double getTotalEarned() {
        return totalEarned;
    }

    public static double getFinalRatePerHour() {
        return finalRatePerHour;
    }

    public static double getCurrentRatePerHour() {
        if (completed) {
            return finalRatePerHour;
        }
        if (!running) {
            return 0;
        }
        long sec = getElapsedSeconds();
        if (sec < 2) return 0;
        return baseEarned / ((System.currentTimeMillis() - startTimeMs) / 1000.0) * 3600.0;
    }

    public static void onSettledDelta(double diff, int percentage, int id) {
        if (id != testId || (!running && !completed) || !Double.isFinite(diff) || diff <= 0) return;
        baseEarned += diff / (1.0 + Math.max(0, percentage) / 100.0);
        totalEarned += diff;
        if (completed) {
            double seconds = (stopTimeMs - startTimeMs) / 1000.0;
            finalRatePerHour = seconds > 0 ? baseEarned / seconds * 3600.0 : 0;
        }
    }

    public static void onClientTick() {
        if (preparing) {
            long elapsed = System.currentTimeMillis() - prepStartTimeMs;
            if (elapsed >= 3000L) {
                actuallyStart();
            }
            return;
        }

        if (!running) return;

        if (selectedDuration.seconds > 0) {
            long elapsed = (System.currentTimeMillis() - startTimeMs) / 1000;
            if (elapsed >= selectedDuration.seconds) {
                stop();
            }
        }
    }
}
