package com.moneybooster;

public final class BalanceReadingTest {
    private static void reading(String line, double amount, double resolution, boolean rounded) {
        var result = BalanceReading.parse(line);
        if (result == null || Math.abs(result.amount() - amount) > .001
                || Math.abs(result.resolution() - resolution) > .001 || result.rounded() != rounded)
            throw new AssertionError(line + ": " + result);
    }
    public static void main(String[] args) {
        reading("§6Balance: §a$2.34B", 2_340_000_000d, 10_000_000, true);
        reading("Balance: $2.340b", 2_340_000_000d, 1_000_000, true);
        reading("Balance: $2,340,123,456.78", 2_340_123_456.78, .01, false);
        reading("Balance: $12.50M", 12_500_000, 10_000, true);
        reading("Balance: $ 12K", 12_000, 1_000, true);
        reading("Balance: $0", 0, .01, false);
        for (String invalid : new String[]{"Balance: $NaN", "Balance: $-4B", "Balance: $2.3Billion", "Balance: $2.3.4B", "XP: 2B"})
            if (BalanceReading.parse(invalid) != null) throw new AssertionError("Accepted " + invalid);
        if (BalanceReading.parse(null) != null) throw new AssertionError("null");
        System.out.println("PASS: billion balances, displayed resolution, full cash, suffixes and malformed input");
    }
}
