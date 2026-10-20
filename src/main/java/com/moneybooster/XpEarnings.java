package com.moneybooster;

/** XP points represented by the vanilla level/bar, independent of money accounting. */
public final class XpEarnings {
    public record Gain(double total, double base, long from, long to) {}
    private long previous = -1, observedAt;

    public static long points(int level, float progress) {
        if (level < 0 || !Float.isFinite(progress) || progress < 0 || progress > 1) return -1;
        double l = level;
        double atLevel = level <= 16 ? l * l + 6 * l
                : level <= 31 ? 2.5 * l * l - 40.5 * l + 360
                : 4.5 * l * l - 162.5 * l + 2220;
        double needed = level >= 30 ? 112 + (l - 30) * 9 : level >= 15 ? 37 + (l - 15) * 5 : 7 + l * 2;
        return Math.round(atLevel + progress * needed);
    }

    public Gain observe(int level, float progress, int percentage, long now) {
        long value = points(level, progress);
        if (value < 0) return null;
        long old = previous, from = observedAt;
        previous = value; observedAt = now;
        // First observation, spending XP, death and resets only establish a baseline.
        if (old < 0 || value <= old || now <= from) return null;
        double total = value - old;
        return new Gain(total, total / (1 + Math.max(0, percentage) / 100.0), from, now);
    }

    public void reset() { previous = -1; observedAt = 0; }
}
