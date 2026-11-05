package com.moneybooster;

import java.util.regex.Pattern;

/** The displayed balance and the smallest change its printed digits can represent. */
public record BalanceReading(double amount, double resolution) {
    private static final Pattern LINE = Pattern.compile(
            "(?i)Balance:\\s*\\$\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*([KMB])?(?![\\p{L}\\p{N}.])");

    public boolean rounded() { return resolution > 0.01 + 1e-9; }

    public static BalanceReading parse(String line) {
        if (line == null) return null;
        var match = LINE.matcher(line.replaceAll("§.", ""));
        if (!match.find()) return null;
        String digits = match.group(1).replace(",", "");
        String suffix = match.group(2);
        double multiplier = suffix == null ? 1 : switch (suffix.toUpperCase(java.util.Locale.ROOT)) {
            case "K" -> 1_000; case "M" -> 1_000_000; default -> 1_000_000_000;
        };
        try {
            double amount = Double.parseDouble(digits) * multiplier;
            int dot = digits.indexOf('.');
            // A full dollar amount is a cash observation, rather than an abbreviated bucket.
            double resolution = suffix == null ? 0.01 : multiplier * Math.pow(10, dot < 0 ? 0 : dot + 1 - digits.length());
            return Double.isFinite(amount) && amount >= 0 ? new BalanceReading(amount, resolution) : null;
        } catch (NumberFormatException ignored) { return null; }
    }
}
