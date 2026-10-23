package com.moneybooster;

public final class CoconutAlertTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        var timers = new BoosterTimers();
        var money = timers.onChat("✔ You have gained the effect of a 61% Money Booster for 60 seconds!", 0);
        check(CoconutAlert.qualifies(money, "Coconut", true, 61), "inclusive threshold");
        check(CoconutAlert.qualifies(money, "Coconut", true, 0), "zero alerts for every coconut money effect");
        check(!CoconutAlert.qualifies(money, "Coconut", true, 62), "below threshold");
        check(!CoconutAlert.qualifies(money, "Coconut", false, 0), "disabled alert");
        for (String source : new String[]{"Unknown source", "Money booster item", "Server booster"})
            check(!CoconutAlert.qualifies(money, source, true, 0), "non-coconut source");
        var xp = timers.onChat("✔ You have gained the effect of a 99% Experience Booster for 60 seconds!", 1);
        check(!CoconutAlert.qualifies(xp, "Coconut", true, 0), "XP excluded");
        check(!CoconutAlert.qualifies(new BoosterTimers.Activation(BoosterTimers.Type.MONEY, 100, 60, true), "Coconut", true, 0), "server excluded");
        check(!CoconutAlert.qualifies(null, "Coconut", true, 0), "unrelated message excluded");
        System.out.println("PASS: coconut money alert threshold, enabled preference, source and XP/server isolation");
    }
}
