package com.moneybooster;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Independent server-backed clocks; only money effects participate in earnings accounting. */
public final class BoosterTimers {
    public enum Type {
        MONEY("Money"), XP("XP");
        public final String label;
        Type(String label) { this.label = label; }
    }
    public record Activation(Type type, int percentage, long seconds, boolean server) {}
    private static final Pattern GAIN = Pattern.compile(
        "(?i)^(?:[✔✓]\\s*)?You have gained (?:the effect of )?(?:a )?(\\d+)% (Money|Experience|XP|EXP) Booster for (\\d+) seconds?[!.]?$");
    private static final Pattern SERVER_GAIN = Pattern.compile(
        "(?i)^(?:[⚡✔✓]\\s*)?You will now receive \\+(\\d+)% (Shop Money|EXP Gain|XP Gain|Experience Gain)[.!]?$");
    private static final Pattern SERVER_HEADER = Pattern.compile("(?i)^(MONEY|EXPERIENCE|XP|EXP) BOOSTER ACTIVATED!$");
    private static final Pattern SERVER_DURATION = Pattern.compile(
        "(?i)^(?:[⚡✔✓]\\s*)?Your Booster will end in:\\s*(\\d+)\\s*minutes?[.!]?$");
    private final EnumMap<Type, BoosterClock> clocks = new EnumMap<>(Type.class);
    private final EnumMap<Type, Integer> percentages = new EnumMap<>(Type.class);
    private Type pendingType;
    private int pendingPercentage;
    private long pendingAt;

    public BoosterTimers() { for (Type type : Type.values()) { clocks.put(type, new BoosterClock()); percentages.put(type, 0); } }
    public BoosterClock clock(Type type) { return clocks.get(type); }
    public int percentage(Type type) { return percentages.get(type); }
    public void start(Type type, int percentage, long seconds, long now) {
        percentages.put(type, percentage);
        clock(type).start(seconds, now);
    }
    public void observe(Type type, BoosterSidebar.State state, long now) {
        clock(type).observe(state.active(), state.remainingMs(), state.exactSeconds(), now);
        if (state.percentage() > 0 && !clock(type).chatTimed()) percentages.put(type, state.percentage());
    }
    public Activation onChat(String raw, long now) {
        if (raw == null) return null;
        String text = raw.replaceAll("§.", "").strip();
        if (pendingType != null && (now < pendingAt || now - pendingAt > 10000)) clearPending();
        var gain = GAIN.matcher(text);
        if (gain.matches()) {
            int percentage = number(gain.group(1)), seconds = number(gain.group(3));
            if (!valid(percentage, seconds)) return null;
            Type type = type(gain.group(2));
            start(type, percentage, seconds, now);
            return new Activation(type, percentage, seconds, false);
        }
        var header = SERVER_HEADER.matcher(text);
        if (header.matches()) {
            pendingType = type(header.group(1)); pendingPercentage = 0; pendingAt = now;
            return null;
        }
        var serverGain = SERVER_GAIN.matcher(text);
        if (serverGain.matches()) {
            pendingType = type(serverGain.group(2)); pendingPercentage = number(serverGain.group(1)); pendingAt = now;
            return null;
        }
        var duration = SERVER_DURATION.matcher(text);
        if (duration.matches()) {
            long seconds = number(duration.group(1)) * 60L;
            if (pendingType == null || !valid(pendingPercentage, seconds)) { clearPending(); return null; }
            var activation = new Activation(pendingType, pendingPercentage, seconds, true);
            clearPending();
            start(activation.type(), activation.percentage(), activation.seconds(), now);
            return activation;
        }
        return null;
    }
    public List<Type> visible(long now, boolean money, boolean xp) {
        List<Type> result = new ArrayList<>(2);
        if (money && clock(Type.MONEY).active(now)) result.add(Type.MONEY);
        if (xp && clock(Type.XP).active(now)) result.add(Type.XP);
        return result;
    }
    public boolean reminding(Type type, long now, boolean enabled) {
        var clock = clock(type);
        return enabled && clock.active(now) && clock.hasTimer(now) && clock.remaining(now) <= 5000;
    }
    public static String formatTime(long remainingMs, boolean hasTimer) {
        if (!hasTimer) return "Active";
        if (remainingMs <= 0) return "0.0s";
        long seconds = remainingMs / 1000;
        if (seconds < 10) return String.format(Locale.ROOT, "%.1fs", remainingMs / 1000.0);
        if (seconds < 60) return seconds + "s";
        return seconds / 60 + "m " + String.format(Locale.ROOT, "%02d", seconds % 60) + "s";
    }
    public void reset() { clocks.values().forEach(BoosterClock::reset); percentages.replaceAll((type, pct) -> 0); clearPending(); }
    private void clearPending() { pendingType = null; pendingPercentage = 0; pendingAt = 0; }
    private static Type type(String name) { return name.toLowerCase(Locale.ROOT).contains("money") ? Type.MONEY : Type.XP; }
    private static boolean valid(int percentage, long seconds) { return percentage > 0 && percentage <= 100000 && seconds > 0 && seconds <= 86400; }
    private static int number(String value) { try { return Integer.parseInt(value); } catch (NumberFormatException e) { return 0; } }
}
