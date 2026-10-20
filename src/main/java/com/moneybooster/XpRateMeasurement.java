package com.moneybooster;

/** A timed XP sample with fixed boundaries; display changes never alter the sample. */
public final class XpRateMeasurement {
    private long start, limit, stoppedAt;
    private boolean running, completed;
    private double total, base;

    public void reset() { running = completed = false; total = base = 0; }
    public void start(long now, int seconds) {
        reset(); start = now; limit = seconds > 0 ? now + seconds * 1000L : Long.MAX_VALUE; running = true;
    }
    public void stop(long now) { if (running) { stoppedAt = Math.min(now, limit); running = false; completed = true; } }
    public void add(XpEarnings.Gain gain) {
        if (!running || gain.to() <= gain.from() || !Double.isFinite(gain.total()) || !Double.isFinite(gain.base())
                || gain.total() <= 0 || gain.base() < 0) return;
        long overlap = Math.max(0, Math.min(gain.to(), limit) - Math.max(gain.from(), start));
        double fraction = (double) overlap / (gain.to() - gain.from());
        total += gain.total() * fraction; base += gain.base() * fraction;
    }
    public double earned(boolean boosted) { return boosted ? total : base; }
    public double perHour(long now, boolean boosted) {
        if (!running && !completed) return 0;
        double seconds = ((running ? Math.min(now, limit) : stoppedAt) - start) / 1000.0;
        return seconds > 0 && (completed || seconds >= 2) ? earned(boosted) * 3600 / seconds : 0;
    }
}
