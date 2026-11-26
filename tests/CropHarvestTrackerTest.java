package com.skyblocktoolkit;

public final class CropHarvestTrackerTest {
    public static void main(String[] args) {
        testScoreboardParsing();
        testBaselineAndIslandBlockRejection();
        testActiveHarvestingAndRate();
        testIdleTimeout();
        testRateThresholdFilter();
        testResetOnDecrease();
        testFormatting();
        testRealmCropCalculation();
        System.out.println("PASS: sidebar scoreboard parsing, 60s rolling rate, 1k/h threshold, idle timeout, island rejection and realm crop income");
    }

    private static void testRealmCropCalculation() {
        CropHarvestTracker.reset();
        var cfg = ToolkitConfig.get();
        cfg.realmCropIncome = false;
        cfg.realmMoneyPerMillionCrops = 100_000.0; // $100K per 1M crops

        check(!CropHarvestTracker.isInRealm(), "starts not in realm");
        check(CropHarvestTracker.realmCropHourlyValue() == 0, "no realm value when toggle disabled");

        // Scoreboard lines detection
        CropHarvestTracker.checkScoreboardLines(java.util.List.of("☽ PHARAOH'S REALM ☾", "Crops Harvested: 1,000"));
        check(CropHarvestTracker.isInRealm(), "in realm after scoreboard sweep");

        // Toggle disabled even though in realm
        check(CropHarvestTracker.realmCropHourlyValue() == 0, "no realm value when toggle is off");

        // Enable toggle
        cfg.realmCropIncome = true;

        long t0 = 500_000;
        CropHarvestTracker.observe(1000, t0); // baseline
        long t = t0;
        long count = 1000;
        for (int i = 0; i < 16; i++) {
            t += 250;
            count += 10;
            CropHarvestTracker.observe(count, t);
        }
        // Actively harvesting 160 crops in 4s (~144,000 crops/hr)
        double rate = CropHarvestTracker.getRatePerHour(t);
        check(rate > 100_000, "rate > 100k/h");
        double hourlyValue = CropHarvestTracker.realmCropHourlyValue(t);
        double expected = (rate / 1_000_000.0) * cfg.realmMoneyPerMillionCrops;
        check(Math.abs(hourlyValue - expected) < 1e-4, "hourly value matches (rate / 1M) * price: " + hourlyValue + " vs " + expected);
        check(hourlyValue > 0, "positive hourly value");

        // Test cropValue helper
        double value500k = CropHarvestTracker.cropValue(500_000);
        check(Math.abs(value500k - 50_000.0) < 1e-4, "500k crops at 100k/M = 50k");

        // Leave realm -> Overworld sidebar
        CropHarvestTracker.checkScoreboardLines(java.util.List.of("Complex Skyblock"));
        check(!CropHarvestTracker.isInRealm(), "no longer in realm after overworld sidebar");
        check(CropHarvestTracker.realmCropHourlyValue(t) == 0, "zero value outside realm even when actively harvesting");
        check(CropHarvestTracker.cropValue(500_000) == 0, "zero crop value outside realm");

        // Re-enter realm
        CropHarvestTracker.checkScoreboardLines(java.util.List.of("☽ PHARAOH'S REALM ☾"));
        check(CropHarvestTracker.isInRealm(), "in realm after scoreboard sweep");
        check(CropHarvestTracker.realmCropHourlyValue(t) > 0, "realm value restored");

        // Turn toggle off
        cfg.realmCropIncome = false;
        check(CropHarvestTracker.realmCropHourlyValue(t) == 0, "zero value when toggle turned off");

        // Reset
        CropHarvestTracker.reset();
        check(!CropHarvestTracker.isInRealm(), "not in realm after reset");
    }

    private static void testScoreboardParsing() {
        CropHarvestTracker.reset();
        check(CropHarvestTracker.getLastKnownCount() == -1, "initial count is -1");

        // Various scoreboard formats from MC-Complex
        CropHarvestTracker.checkScoreboardLine("§6🌾 §eCrops Harvested: §b1,217,085");
        check(CropHarvestTracker.getLastKnownCount() == 1217085, "parsed exact count with icon and color codes");

        CropHarvestTracker.checkScoreboardLine("Crops Harvested: 1,217,090");
        check(CropHarvestTracker.getLastKnownCount() == 1217090, "parsed plain count");

        CropHarvestTracker.checkScoreboardLine("\uE024 Crop Harvested: 500");
        // Count decreased from 1.2M -> triggers rebase to 500
        check(CropHarvestTracker.getLastKnownCount() == 500, "parsed custom unicode and singular 'Crop'");

        CropHarvestTracker.checkScoreboardLine("Crops Harvest: 1.5M");
        check(CropHarvestTracker.getLastKnownCount() == 1500000, "parsed suffix M");

        // Non-matching lines must be safely ignored
        CropHarvestTracker.checkScoreboardLine("Balance: $12,345.67");
        CropHarvestTracker.checkScoreboardLine("☽ PHARAOH'S REALM ☾");
        CropHarvestTracker.checkScoreboardLine("HUB.MC-COMPLEX.COM");
        CropHarvestTracker.checkScoreboardLine(null);
        CropHarvestTracker.checkScoreboardLine("");
        check(CropHarvestTracker.getLastKnownCount() == 1500000, "unrelated lines ignored");
    }

    private static void testBaselineAndIslandBlockRejection() {
        CropHarvestTracker.reset();

        long t0 = 100_000;
        // First observation establishes baseline, not a harvest
        CropHarvestTracker.observe(100, t0);
        check(!CropHarvestTracker.isActive(t0), "baseline observation is not active");
        check(!CropHarvestTracker.shouldShowHud(true, t0), "baseline observation does not show HUD");
        check(CropHarvestTracker.getTotalHarvestedInSession() == 0, "session harvest count is 0 at baseline");

        // Player breaks 1 single block on island
        CropHarvestTracker.observe(101, t0 + 500);
        check(CropHarvestTracker.isActive(t0 + 500), "active after 1 crop");
        check(CropHarvestTracker.getTotalHarvestedInSession() == 1, "session count is 1");
        // Should NOT show HUD because single-block breaking is rejected (requires >= 2 and warmup)
        check(!CropHarvestTracker.shouldShowHud(true, t0 + 500), "single block on island never triggers HUD");

        // Even after 3.5s elapsed, single block break must never show HUD
        check(!CropHarvestTracker.shouldShowHud(true, t0 + 4000), "single block still does not show HUD after warmup");

        // After 5s idle, session expires
        long tIdle = t0 + 500 + CropHarvestTracker.IDLE_MS + 100;
        check(!CropHarvestTracker.isActive(tIdle), "session expired after idle");

        // Breaking another isolated block later restarts session at 1, still never shows
        CropHarvestTracker.observe(102, tIdle);
        check(CropHarvestTracker.getTotalHarvestedInSession() == 1, "new isolated break has count 1");
        check(!CropHarvestTracker.shouldShowHud(true, tIdle), "second isolated block break still never triggers HUD");
    }

    private static void testActiveHarvestingAndRate() {
        CropHarvestTracker.reset();

        long t0 = 200_000;
        CropHarvestTracker.observe(1000, t0); // baseline

        // Active farming: player harvests 10 crops every 250ms for 4 seconds
        long t = t0;
        long current = 1000;
        for (int i = 0; i < 16; i++) {
            t += 250;
            current += 10;
            CropHarvestTracker.observe(current, t);
        }

        // At t = t0 + 4000ms: 160 crops harvested in 4 seconds
        // Rate should be approx 160 * 3600 / 4 = 144,000 crops/hr
        check(CropHarvestTracker.isActive(t), "active while farming");
        check(CropHarvestTracker.getTotalHarvestedInSession() == 160, "total session crops == 160");
        double rate = CropHarvestTracker.getRatePerHour(t);
        check(rate > 100_000, "rate > 100k/h during active farming (was " + rate + ")");
        check(CropHarvestTracker.shouldShowHud(true, t), "HUD shows during active farming");
        check(!CropHarvestTracker.shouldShowHud(false, t), "HUD respects disabled config");
        String label = CropHarvestTracker.averageLabel(t);
        check(label.startsWith("Harvest (60s): ") && label.endsWith("/h"), "correct HUD label format: " + label);
    }

    private static void testIdleTimeout() {
        CropHarvestTracker.reset();

        long t0 = 300_000;
        CropHarvestTracker.observe(5000, t0);
        CropHarvestTracker.observe(5050, t0 + 3000); // 50 crops
        check(CropHarvestTracker.isActive(t0 + 3000), "active immediately after harvest");

        // At 4.9 seconds after last harvest: still active
        check(CropHarvestTracker.isActive(t0 + 3000 + 4900), "active within 5s grace");

        // At 5.1 seconds after last harvest: idle timeout
        long tExpired = t0 + 3000 + 5100;
        check(!CropHarvestTracker.isActive(tExpired), "inactive after 5s idle");
        check(!CropHarvestTracker.shouldShowHud(true, tExpired), "HUD hides after 5s idle");
    }

    private static void testRateThresholdFilter() {
        CropHarvestTracker.reset();

        long t0 = 400_000;
        CropHarvestTracker.observe(10000, t0);

        // Player breaks 2 crops over 15 seconds (very slow):
        // 2 crops in 15 seconds = 2 * 3600 / 15 = 480 crops/hour (< 1000/h)
        CropHarvestTracker.observe(10001, t0 + 1000);
        CropHarvestTracker.observe(10002, t0 + 4000);

        long tCheck = t0 + 10000;
        // Total is 2, but rate is 2 * 3600 / 10 = 720/h (< 1000)
        double rate = CropHarvestTracker.getRatePerHour(tCheck);
        check(rate < 1000.0, "rate is below 1000/h (was " + rate + ")");
        check(!CropHarvestTracker.shouldShowHud(true, tCheck), "HUD does not show when rate is below 1000/h");
    }

    private static void testResetOnDecrease() {
        CropHarvestTracker.reset();
        CropHarvestTracker.observe(50000, 1000);
        check(CropHarvestTracker.getLastKnownCount() == 50000, "count observed");

        // Switch realm / profile: count drops to 200
        CropHarvestTracker.observe(200, 2000);
        check(CropHarvestTracker.getLastKnownCount() == 200, "count rebased on decrease");
        check(CropHarvestTracker.getTotalHarvestedInSession() == 0, "session reset on decrease");
        check(!CropHarvestTracker.shouldShowHud(true, 2000), "no HUD on decrease");
    }

    private static void testFormatting() {
        check(CropHarvestTracker.format(500).equals("500"), "format small");
        check(CropHarvestTracker.format(1500).equals("1.5K"), "format 1.5K");
        check(CropHarvestTracker.format(45200).equals("45.2K"), "format 45.2K");
        check(CropHarvestTracker.format(1200000).equals("1.20M"), "format 1.20M");
        check(CropHarvestTracker.format(1500000000).equals("1.50B"), "format 1.50B");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
