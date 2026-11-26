package com.skyblocktoolkit;

import com.moneybooster.RollingEarnings;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks crops harvested per hour parsed from the sidebar scoreboard.
 * Displays only while actively harvesting and only when the rate is above 1,000 crops/hr.
 */
public final class CropHarvestTracker {
    public static final Pattern CROPS_HARVESTED_PATTERN = Pattern.compile(
            "(?i)crops?\\s*harvest(?:ed)?\\s*:\\s*([0-9.,]+)\\s*([KMBkmb])?"
    );
    public static final long IDLE_MS = 5_000;
    public static final double MIN_RATE = 1000.0;

    private static final RollingEarnings rolling = new RollingEarnings(60000, 0);
    private static ItemStatsLocation currentLocation = ItemStatsLocation.UNKNOWN;
    private static long lastKnownCount = -1;
    private static long lastObservationTime = 0;
    private static long lastHarvestTime = -1;
    private static long totalHarvestedInSession = 0;
    private static double averageRate = 0;
    private static long averageUpdated = 0;

    private CropHarvestTracker() {}

    public static void reset() {
        long now = System.currentTimeMillis();
        rolling.reset(now);
        currentLocation = ItemStatsLocation.UNKNOWN;
        lastKnownCount = -1;
        lastObservationTime = 0;
        lastHarvestTime = -1;
        totalHarvestedInSession = 0;
        averageRate = 0;
        averageUpdated = 0;
    }

    public static void checkScoreboardLines(java.util.List<String> lines) {
        if (lines == null || lines.isEmpty()) return;
        currentLocation = ItemStatsLocation.sidebar(lines);
        for (String line : lines) checkScoreboardLine(line);
    }

    public static void checkScoreboardLine(String line) {
        if (line == null) return;
        String cleaned = clean(line).replace('’', '\'').replace('ʼ', '\'').replace('‘', '\'');
        if (cleaned.toLowerCase(Locale.ROOT).matches(".*\\bpharaoh'?s\\s+realm\\b.*")) {
            currentLocation = ItemStatsLocation.REALM;
        }
        Matcher m = CROPS_HARVESTED_PATTERN.matcher(cleaned);
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
                long count = Math.round(val);
                observe(count, System.currentTimeMillis());
            } catch (Exception ignored) {}
        }
    }

    public static void observe(long count, long now) {
        if (count < 0) return;
        if (lastKnownCount < 0) {
            lastKnownCount = count;
            lastObservationTime = now;
            return;
        }
        if (count < lastKnownCount) {
            // Stats reset or switched realm/profile
            reset();
            lastKnownCount = count;
            lastObservationTime = now;
            return;
        }
        if (count == lastKnownCount) {
            lastObservationTime = now;
            return;
        }

        long delta = count - lastKnownCount;
        lastKnownCount = count;

        boolean wasIdle = (lastHarvestTime < 0 || now - lastHarvestTime >= IDLE_MS);
        long from = wasIdle ? now - 250 : Math.max(lastObservationTime, now - 2000);
        if (from >= now) from = now - 250;
        lastObservationTime = now;
        lastHarvestTime = now;

        if (wasIdle) {
            rolling.reset(from);
            totalHarvestedInSession = 0;
        }
        totalHarvestedInSession += delta;
        rolling.add(delta, from, now);
        com.moneybooster.RateTracker.onCropHarvest(delta, now, com.moneybooster.RateTracker.getActiveTestId());
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.player != null) ItemStatsTracker.crops(delta, now);
        } catch (Throwable ignored) {}
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (now - averageUpdated >= 1000) {
            averageRate = rolling.perHour(now);
            averageUpdated = now;
        }
    }

    public static boolean isActive(long now) {
        return lastHarvestTime >= 0 && now >= lastHarvestTime && (now - lastHarvestTime < IDLE_MS);
    }

    public static boolean isReady(long now) {
        return isActive(now) && rolling.ready(now) && totalHarvestedInSession >= 2;
    }

    public static void updateLocation(ItemStatsLocation loc) {
        if (loc != null) currentLocation = loc;
    }

    public static ItemStatsLocation getLocation() {
        return currentLocation;
    }

    public static boolean isInRealm() {
        if (currentLocation == ItemStatsLocation.REALM) return true;
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                return ItemStatsTracker.location(mc) == ItemStatsLocation.REALM;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static double realmCropHourlyValue() {
        return realmCropHourlyValue(System.currentTimeMillis());
    }

    public static double realmCropHourlyValue(long now) {
        var cfg = ToolkitConfig.get();
        if (!cfg.realmCropIncome || !isInRealm() || cfg.realmMoneyPerMillionCrops <= 0) return 0;
        if (!isReady(now)) return 0;
        return (getRatePerHour(now) / 1_000_000.0) * cfg.realmMoneyPerMillionCrops;
    }

    public static double cropValue(long count) {
        var cfg = ToolkitConfig.get();
        return (cfg.realmCropIncome && isInRealm()) ? (count / 1_000_000.0) * cfg.realmMoneyPerMillionCrops : 0;
    }

    public static String realmIncomeLabel() {
        var cfg = ToolkitConfig.get();
        if (!cfg.realmCropIncome) return "Realm crop income: disabled";
        boolean inRealm = isInRealm();
        double val = realmCropHourlyValue();
        return "Realm crops: " + (inRealm ? (val > 0 ? "+" + CompactMoney.display(val) + "/h" : "waiting for harvest") : "not in Realm");
    }

    public static boolean shouldShowHud() {
        return shouldShowHud(ToolkitConfig.get().cropHarvestHud, System.currentTimeMillis());
    }

    public static boolean shouldShowHud(boolean enabled, long now) {
        if (!enabled || !isActive(now)) return false;
        if (totalHarvestedInSession < 2 || !rolling.ready(now)) return false;
        return getRatePerHour(now) >= MIN_RATE;
    }

    public static double getRatePerHour(long now) {
        if (now - averageUpdated >= 1000) {
            averageRate = rolling.perHour(now);
            averageUpdated = now;
        }
        return averageRate;
    }

    public static String averageLabel() {
        return averageLabel(System.currentTimeMillis());
    }

    public static String averageLabel(long now) {
        double rate = getRatePerHour(now);
        boolean ready = rolling.ready(now) && totalHarvestedInSession >= 2;
        return "Harvest (60s): " + (ready ? format(rate) : "--") + "/h";
    }

    public static String format(double value) {
        if (value >= 1_000_000_000) return String.format(Locale.ROOT, "%.2fB", value / 1_000_000_000.0);
        if (value >= 1_000_000) return String.format(Locale.ROOT, "%.2fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format(Locale.ROOT, "%.1fK", value / 1_000.0);
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static String clean(String line) {
        return line == null ? "" : line.replaceAll("§.", "");
    }

    // Testing and inspection helpers
    public static long getLastKnownCount() { return lastKnownCount; }
    public static long getLastHarvestTime() { return lastHarvestTime; }
    public static long getTotalHarvestedInSession() { return totalHarvestedInSession; }
}
