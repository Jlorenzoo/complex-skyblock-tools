package com.skyblocktoolkit;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CompactMoney {
    private static final String[] SUFFIXES = {"", "K", "M", "B"};

    private CompactMoney() {}

    public static String price(double amount) {
        return format(amount, false);
    }

    public static String display(double amount) {
        return format(amount, true);
    }

    private static String format(double amount, boolean round) {
        if (!Double.isFinite(amount)) return Double.toString(amount);

        BigDecimal value = BigDecimal.valueOf(amount);
        int unit = 0;
        while (unit < SUFFIXES.length - 1 && value.abs().compareTo(BigDecimal.valueOf(1000)) >= 0) {
            value = value.movePointLeft(3);
            unit++;
        }
        if (round) {
            value = value.setScale(2, RoundingMode.HALF_UP);
            if (unit < SUFFIXES.length - 1 && value.abs().compareTo(BigDecimal.valueOf(1000)) >= 0) {
                value = value.movePointLeft(3);
                unit++;
            }
        }
        return value.stripTrailingZeros().toPlainString() + SUFFIXES[unit];
    }
}
