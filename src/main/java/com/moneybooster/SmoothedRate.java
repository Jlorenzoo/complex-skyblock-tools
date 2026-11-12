package com.moneybooster;

/** Time-based HUD smoothing only; never changes earned totals or test receipts. */
public final class SmoothedRate {
    public static final double DEFAULT_TAU_MS = 20_000.0;
    public static final double RESPONSIVE_TAU_MS = 2_500.0;

    private final double timeConstantMs;
    private long updated = -1;
    private double value;

    public SmoothedRate() {
        this(DEFAULT_TAU_MS);
    }

    public SmoothedRate(double timeConstantMs) {
        this.timeConstantMs = timeConstantMs > 0 ? timeConstantMs : DEFAULT_TAU_MS;
    }

    public void reset() { updated = -1; value = 0; }

    public double update(double target, long now) {
        if (!Double.isFinite(target) || target < 0) return value;
        if (updated < 0) value = target;
        else if (now > updated) value += (target - value) * (-Math.expm1(-(now - updated) / timeConstantMs));
        updated = Math.max(updated, now);
        return value;
    }
}
