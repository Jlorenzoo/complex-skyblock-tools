package com.moneybooster;

import java.util.List;
import static com.moneybooster.BoosterTimers.Type.*;

public final class BoosterTimersTest {
    private static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); }
    public static void main(String[] args) {
        var timers = new BoosterTimers();
        timers.onChat("✔ You have gained the effect of a 39% Money Booster for 42 seconds!", 0);
        var xp = timers.onChat("✔ You have gained the effect of a 59% Experience Booster for 113 seconds!", 1000);
        check(xp.type() == XP && xp.percentage() == 59, "actual coconut XP message");
        check(timers.clock(MONEY).remaining(2000) == 40000, "XP must not replace money deadline");
        check(timers.percentage(MONEY) == 39 && timers.percentage(XP) == 59, "percentages stay independent");
        check(timers.visible(2000, true, true).equals(List.of(MONEY, XP)), "both stack money then XP");
        check(timers.visible(2000, false, true).equals(List.of(XP)), "hidden money leaves XP first without a gap");
        check(timers.visible(2000, true, false).equals(List.of(MONEY)), "hidden XP leaves money");
        check(timers.visible(2000, false, false).isEmpty(), "both toggles off");
        check(timers.visible(43000, true, true).equals(List.of(XP)), "expired money releases first row");
        check(timers.visible(115000, true, true).isEmpty(), "no inactive placeholders");
        check(!timers.reminding(MONEY, 36999, true), "no early reminder");
        check(timers.reminding(MONEY, 37000, true), "reminder begins at five seconds");
        check(!timers.reminding(MONEY, 37000, false), "reminder can be disabled");
        check(!timers.reminding(XP, 37000, true), "reminder does not leak to other timer");
        check(!timers.reminding(MONEY, 42000, true), "expired timer has no reminder");
        timers.onChat("  EXPERIENCE BOOSTER ACTIVATED!  ", 200000);
        timers.onChat("  ⚡ You will now receive +150% EXP Gain.", 200010);
        var serverXp = timers.onChat(" ⚡ Your Booster will end in: 5 minutes.", 200020);
        check(serverXp.type() == XP && serverXp.server(), "actual server XP activation");
        check(!timers.clock(MONEY).active(200020) && timers.percentage(MONEY) == 39, "XP duration cannot create money boost");
        timers.onChat("MONEY BOOSTER ACTIVATED!", 201000);
        timers.onChat("⚡ You will now receive +20% Shop Money.", 201010);
        timers.onChat("⚡ Your Booster will end in: 5 minutes.", 201020);
        check(timers.clock(XP).remaining(201020) == 299000, "money activation cannot reset XP");
        check(timers.percentage(MONEY) == 20 && timers.percentage(XP) == 150, "server percentages independent");
        long moneyRemaining = timers.clock(MONEY).remaining(202000);
        check(timers.onChat("⚡ Your Booster will end in: 10 minutes.", 202000) == null, "orphan duration rejected");
        check(timers.clock(MONEY).remaining(202000) == moneyRemaining, "orphan duration leaves timer intact");
        timers.onChat("⚡ You will now receive +200% EXP Gain.", 203000);
        check(timers.onChat("⚡ Your Booster will end in: 5 minutes.", 214000) == null, "stale pending message rejected");
        for (String message : List.of(
            "[Member] Player: ✔ You have gained the effect of a 999% Money Booster for 42 seconds!",
            "Player: ⚡ You will now receive +999% EXP Gain.",
            "(/bidwars) ✔ Bid War for +162% Experience Booster (21m) started at $1,000,000.",
            "✔ You have gained the effect of a 99999999999999999999% XP Booster for 42 seconds!",
            "✔ You have gained the effect of a 50% Money Booster for 99999999999999999999 seconds!")) {
            check(timers.onChat(message, 215000) == null, "spoof, listing or invalid activation rejected");
        }
        var lines = List.of("§a+39% Money Booster: 1m 20s", "+150% Experience Booster:", "[2 minutes]");
        check(BoosterSidebar.parse(lines, MONEY).remainingMs() == 80000, "money sidebar timer");
        var xpSidebar = BoosterSidebar.parse(lines, XP);
        check(xpSidebar.percentage() == 150 && xpSidebar.remainingMs() == 120000 && !xpSidebar.exactSeconds(), "XP adjacent timer belongs to XP");
        check(!BoosterSidebar.parse(List.of("+150% XP Booster: 30s"), MONEY).active(), "XP sidebar cannot activate money");
        check(BoosterSidebar.parse(List.of("EXP Booster: +90% 00:25"), XP).remainingMs() == 25000, "EXP alias and exact time");
        timers.reset();
        timers.observe(XP, BoosterSidebar.parse(List.of("XP Booster: +150%"), XP), 500000);
        check(timers.visible(500000, true, true).equals(List.of(XP)), "joining with XP only shows one row");
        check(!timers.reminding(XP, 500000, true), "no invented warning for unknown duration");
        timers.reset();
        timers.onChat("You have gained the effect of a 61% Money Booster for 60 seconds!", 0);
        timers.observe(MONEY, BoosterSidebar.parse(List.of("+20% Money Booster: 5m")), 1000);
        check(timers.percentage(MONEY)==61 && timers.clock(MONEY).remaining(1000)==59000, "stale sidebar cannot replace coconut strength or duration");
        timers.observe(MONEY, BoosterSidebar.parse(List.of("+20% Money Booster:")), 60000);
        check(!timers.clock(MONEY).active(60000), "stale label cannot extend coconut accounting");
        timers.reset();
        check(timers.visible(500000, true, true).isEmpty() && timers.percentage(XP) == 0, "disconnect clears both clocks");
        check(BoosterTimers.formatTime(42000, true).equals("42s") && BoosterTimers.formatTime(4000, true).equals("4.0s"), "countdown formatting");
        check(BoosterTimers.formatTime(0, false).equals("Active"), "server active with unknown timer");
        System.out.println("PASS: independent XP/money clocks, actual server messages, sidebar reconciliation, visibility packing, five-second reminders, spoof/stale rejection and disconnect reset");
    }
}
