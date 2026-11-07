package com.moneybooster;

/** Time-based HUD smoothing only; never changes earned totals or test receipts. */
public final class SmoothedRate {
    private long updated = -1;
    private double value;
    public void reset() { updated=-1;value=0; }
    public double update(double target, long now) {
        if (!Double.isFinite(target) || target < 0) return value;
        if (updated < 0) value=target;
        else if (now>updated) value+=(target-value)*(-Math.expm1(-(now-updated)/20_000.0));
        updated=Math.max(updated,now);
        return value;
    }
}
