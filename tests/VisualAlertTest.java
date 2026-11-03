package com.skyblocktoolkit;

import com.moneybooster.BoosterManager;
import com.moneybooster.BoosterTimers;

public final class VisualAlertTest {
    private static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }

    public static void main(String[] args) {
        VisualAlertOverlay.reset();
        check(!VisualAlertOverlay.isActive(1000), "initially inactive");

        // 1. Coconut trigger
        VisualAlertOverlay.triggerCoconut(75, 50);
        check(VisualAlertOverlay.isActive(System.currentTimeMillis()), "active after coconut trigger");
        check(VisualAlertOverlay.getTitle().contains("+75%"), "title has percentage: " + VisualAlertOverlay.getTitle());
        check(VisualAlertOverlay.getTitle().contains("MONEY BOOSTER"), "title mentions money booster");
        check(VisualAlertOverlay.getSubtitle().contains("RELEASE RIGHT-CLICK"), "subtitle has release instruction: " + VisualAlertOverlay.getSubtitle());
        check(VisualAlertOverlay.getSubtitle().contains("50%"), "subtitle has threshold");
        check(VisualAlertOverlay.getAccentColor() == 0x2ECC71, "emerald green accent color");
        check(!VisualAlertOverlay.isActive(System.currentTimeMillis() + VisualAlertOverlay.DURATION_MS + 100), "inactive after duration");

        // 2. Expiring trigger
        VisualAlertOverlay.reset();
        VisualAlertOverlay.triggerExpiring(BoosterTimers.Type.MONEY, 5);
        check(VisualAlertOverlay.isActive(System.currentTimeMillis()), "active after expiring trigger");
        check(VisualAlertOverlay.getTitle().contains("MONEY BOOSTER EXPIRING"), "title mentions expiring money booster");
        check(VisualAlertOverlay.getSubtitle().contains("5s remaining"), "subtitle has countdown: " + VisualAlertOverlay.getSubtitle());
        check(VisualAlertOverlay.getAccentColor() == 0xE5254A, "crimson red accent color");

        // 3. Tick and single-trigger logic for expiring timers
        VisualAlertOverlay.reset();
        var timers = BoosterManager.timers;
        timers.reset();

        // Start a 60-second money booster at t=100000
        long start = 100000;
        timers.onChat("✔ You have gained the effect of a 50% Money Booster for 60 seconds!", start);

        // At t=110000 (50s remaining): not in warning window
        VisualAlertOverlay.tick(110000, true);
        check(!VisualAlertOverlay.isAlertedMoney(), "no alert at 50s remaining");
        check(!VisualAlertOverlay.isActive(110000), "overlay not active at 50s");

        // At t=156000 (4s remaining <= 5s): warning window entered!
        VisualAlertOverlay.tick(156000, true);
        check(VisualAlertOverlay.isAlertedMoney(), "alerted flag set when entering window");
        check(VisualAlertOverlay.isActive(156000), "overlay active at 4s remaining");
        check(VisualAlertOverlay.getTitle().contains("MONEY BOOSTER EXPIRING"), "triggered money expiring alert");

        // Tick again at t=157000 (3s remaining): should NOT re-trigger / restart overlay
        VisualAlertOverlay.tick(157000, true);
        check(VisualAlertOverlay.isAlertedMoney(), "alerted flag remains set");

        // Re-start / renew booster (e.g. eaten new coconut, 60s remaining)
        timers.onChat("✔ You have gained the effect of a 60% Money Booster for 60 seconds!", 158000);
        VisualAlertOverlay.tick(158000, true);
        check(!VisualAlertOverlay.isAlertedMoney(), "alerted flag resets when booster renewed");

        // 4. Disabled preference ignores expiring window
        VisualAlertOverlay.reset();
        // Move to 3s before end of renewed booster (158000 + 60000 = 218000; t=215000 is 3s left)
        VisualAlertOverlay.tick(215000, false);
        check(!VisualAlertOverlay.isAlertedMoney(), "disabled setting does not alert");
        check(!VisualAlertOverlay.isActive(215000), "overlay remains inactive when setting disabled");

        // 5. General Money and XP booster activations
        VisualAlertOverlay.reset();
        VisualAlertOverlay.triggerMoneyBooster(50, "Money booster item", 300);
        check(VisualAlertOverlay.isActive(System.currentTimeMillis()), "active after money booster trigger");
        check(VisualAlertOverlay.getTitle().contains("+50% MONEY BOOSTER"), "title has money percentage");
        check(VisualAlertOverlay.getSubtitle().contains("Money booster item"), "subtitle mentions item source");
        check(VisualAlertOverlay.getAccentColor() == 0x2ECC71, "emerald green accent");

        VisualAlertOverlay.reset();
        VisualAlertOverlay.triggerXpBooster(150, "Experience booster item", 600);
        check(VisualAlertOverlay.isActive(System.currentTimeMillis()), "active after xp booster trigger");
        check(VisualAlertOverlay.getTitle().contains("+150% XP BOOSTER"), "title has xp percentage");
        check(VisualAlertOverlay.getSubtitle().contains("Experience booster item"), "subtitle mentions xp item source");
        check(VisualAlertOverlay.getAccentColor() == 0x9D9AEB, "amethyst purple accent color");

        // 6. Preview methods
        VisualAlertOverlay.reset();
        VisualAlertOverlay.previewCoconutAlert();
        check(VisualAlertOverlay.isActive(System.currentTimeMillis()), "preview coconut active");
        VisualAlertOverlay.previewExpiringAlert();
        check(VisualAlertOverlay.isActive(System.currentTimeMillis()), "preview expiring active");
        VisualAlertOverlay.previewXpAlert();
        check(VisualAlertOverlay.isActive(System.currentTimeMillis()), "preview xp active");
        check(VisualAlertOverlay.getAccentColor() == 0x9D9AEB, "preview xp has amethyst color");

        VisualAlertOverlay.reset();
        System.out.println("PASS: VisualAlertOverlay coconut hit, money & XP activation, expiring countdown, single-trigger and preview logic");
    }
}
