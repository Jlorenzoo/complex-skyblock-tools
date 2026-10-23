package com.moneybooster;

/** Alert only on a confirmed personal money activation from a recently used coconut. */
public final class CoconutAlert {
    private CoconutAlert() {}
    public static boolean qualifies(BoosterTimers.Activation activation, String source, boolean enabled, int threshold) {
        return enabled && activation != null && !activation.server()
            && activation.type() == BoosterTimers.Type.MONEY && "Coconut".equals(source)
            && activation.percentage() >= Math.max(0, threshold);
    }
}
