package com.skyblocktoolkit;

import java.util.LinkedHashMap;
import java.util.Map;

/** Differences server-reported block statistics; never estimates from money or rune messages. */
public final class CropStatSample {
    private Map<String, Integer> baseline;
    private long started;

    public void start(Map<String, Integer> counts, long now) {
        baseline = Map.copyOf(counts);
        started = now;
    }

    public boolean active() { return baseline != null; }
    public void reset() { baseline = null; }

    public Result finish(Map<String, Integer> counts, long now) {
        if (!active()) throw new IllegalStateException("No crop-stat sample running");
        var changes = new LinkedHashMap<String, Long>();
        boolean valid = now > started;
        for (var entry : baseline.entrySet()) {
            int before = entry.getValue();
            Integer after = counts.get(entry.getKey());
            if (before < 0 || after == null || after < before) valid = false;
            else changes.put(entry.getKey(), (long) after - before);
        }
        long elapsed = now - started;
        reset();
        return new Result(valid, elapsed, Map.copyOf(changes));
    }

    public record Result(boolean valid, long elapsedNanos, Map<String, Long> changes) {
        public long total() { return changes.values().stream().mapToLong(Long::longValue).sum(); }
        public double hourly() { return valid ? total() * 3_600_000_000_000.0 / elapsedNanos : 0; }
    }
}
