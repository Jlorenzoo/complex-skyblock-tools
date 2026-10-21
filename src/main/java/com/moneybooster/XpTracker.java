package com.moneybooster;

import com.skyblocktoolkit.ToolkitConfig;
import net.minecraft.client.Minecraft;
import java.util.Locale;

/** Observes the player's XP even when both XP HUDs are hidden. */
public final class XpTracker {
    private static final XpEarnings earnings = new XpEarnings();
    private static final RollingEarnings total = new RollingEarnings(60000, 0), base = new RollingEarnings(60000, 0);
    private static Object player;
    private static boolean synced;
    private static long averageUpdated;
    private static double averageRate, baseAverageRate;

    public static void reset() {
        earnings.reset(); player = null; synced = false;
        long now = System.currentTimeMillis(); total.reset(now); base.reset(now);
        averageUpdated = 0; averageRate = baseAverageRate = 0;
    }

    public static void onExperienceSync() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (player != mc.player) { reset(); player = mc.player; }
        // A joining player's default empty bar is not an XP observation.
        if (!synced) {
            earnings.observe(mc.player.experienceLevel, mc.player.experienceProgress, 0, System.currentTimeMillis());
            synced = true;
        }
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) { if (player != null) reset(); return; }
        if (player != mc.player) { reset(); player = mc.player; }
        if (!synced) return;
        if (mc.player.isDeadOrDying()) { earnings.reset(); return; }
        long now = System.currentTimeMillis();
        int pct = BoosterManager.timers.clock(BoosterTimers.Type.XP).active(now)
                ? BoosterManager.timers.percentage(BoosterTimers.Type.XP) : 0;
        var gain = earnings.observe(mc.player.experienceLevel, mc.player.experienceProgress, pct, now);
        if (gain != null) {
            total.add(gain.total(), gain.from(), gain.to()); base.add(gain.base(), gain.from(), gain.to());
            RateTracker.onXpGain(gain);
        }
        if (now - averageUpdated >= 1000) {
            averageRate = total.perHour(now);
            baseAverageRate = base.perHour(now);
            averageUpdated = now;
        }
    }

    public static String averageLabel() {
        long now = System.currentTimeMillis();
        if (now - averageUpdated >= 1000) {
            averageRate = total.perHour(now);
            baseAverageRate = base.perHour(now);
            averageUpdated = now;
        }
        boolean boosted = ToolkitConfig.get().xpIncludesBoosters;
        double rate = boosted ? averageRate : baseAverageRate;
        return (boosted ? "XP (60s): " : "XP base (60s): ")
                + (total.ready(now) ? format(rate) : "-- XP") + "/h";
    }

    public static String format(double value) {
        if (value >= 1_000_000_000) return String.format(Locale.ROOT, "%.2fB XP", value / 1_000_000_000);
        if (value >= 1_000_000) return String.format(Locale.ROOT, "%.2fM XP", value / 1_000_000);
        if (value >= 1_000) return String.format(Locale.ROOT, "%.1fK XP", value / 1_000);
        return String.format(Locale.ROOT, "%.0f XP", value);
    }
}
