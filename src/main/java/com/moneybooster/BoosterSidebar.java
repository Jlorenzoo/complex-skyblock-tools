package com.moneybooster;

import java.util.List;
import java.util.regex.*;

public final class BoosterSidebar {
    public record State(boolean active, int percentage, long remainingMs, boolean exactSeconds) {}
    private static final Pattern MONEY_LABEL = Pattern.compile("(?i)\\bmoney\\s*booster\\b");
    private static final Pattern XP_LABEL = Pattern.compile("(?i)\\b(?:experience|exp|xp)\\s*booster\\b");
    private static final Pattern PERCENT = Pattern.compile("\\+?(\\d+)\\s*[%$]");
    private static final Pattern UNITS = Pattern.compile("(?i)(\\d+)\\s*(hours?|hrs?|h|minutes?|mins?|m|seconds?|secs?|s)(?![a-z])");
    private static final Pattern COLON = Pattern.compile("(?<![0-9:])(\\d{1,2}):([0-5]\\d)(?::([0-5]\\d))?(?![0-9:])");
    private static final Pattern ONLY_TIME = Pattern.compile("(?i)^[\\s()\\[\\]]*(?:(?:\\d+\\s*(?:hours?|hrs?|h|minutes?|mins?|m|seconds?|secs?|s)\\s*)+|\\d{1,2}:[0-5]\\d(?::[0-5]\\d)?)[\\s()\\[\\]]*$");
    public static State parse(List<String> lines) {
        return parse(lines, BoosterTimers.Type.MONEY);
    }
    public static State parse(List<String> lines, BoosterTimers.Type type) {
        Pattern label = type == BoosterTimers.Type.MONEY ? MONEY_LABEL : XP_LABEL;
        for (int i = 0; i < lines.size(); i++) {
            String line = clean(lines.get(i));
            if (!label.matcher(line).find()) continue;
            Matcher pct = PERCENT.matcher(line); int percentage = -1;
            if (pct.find()) try { percentage = Integer.parseInt(pct.group(1)); } catch (NumberFormatException ignored) {}
            String timerText=PERCENT.matcher(line).replaceAll("");
            long time = duration(timerText);
            if (time < 0 && i + 1 < lines.size()) {
                String next = clean(lines.get(i + 1));
                if (ONLY_TIME.matcher(next).matches()){time = duration(next);timerText=next;}
            }
            return new State(true, percentage, time, COLON.matcher(timerText).find() || Pattern.compile("(?i)\\d+\\s*(?:seconds?|secs?|s)(?![a-z])").matcher(timerText).find());
        }
        return new State(false, -1, -1, false);
    }
    private static String clean(String line) { return line == null ? "" : line.replaceAll("§.", ""); }
    static long duration(String text) {
        try {
            Matcher colon = COLON.matcher(text);
            if (colon.find()) {
                long a = Long.parseLong(colon.group(1)), b = Long.parseLong(colon.group(2));
                long seconds = colon.group(3) == null ? a * 60 + b : a * 3600 + b * 60 + Long.parseLong(colon.group(3));
                return seconds <= 86400 ? seconds * 1000 : -1;
            }
            Matcher units = UNITS.matcher(text); long seconds = 0; boolean found = false;
            while (units.find()) {
                long value = Long.parseLong(units.group(1)); String unit = units.group(2).toLowerCase(java.util.Locale.ROOT);
                if (value > 86400) return -1;
                seconds += value * (unit.startsWith("h") ? 3600 : unit.startsWith("m") ? 60 : 1); found = true;
            }
            return found && seconds <= 86400 ? seconds * 1000 : -1;
        } catch (NumberFormatException e) { return -1; }
    }
}
