package com.moneybooster;
import java.util.ArrayDeque;
/** Rolling window of earnings (default 60s), adapting to elapsed active duration during warmup. */
public final class RollingEarnings {
    private record Sample(double amount,long start,long end) {}
    private final ArrayDeque<Sample> samples=new ArrayDeque<>();
    private final ArrayDeque<Sample> estimatedSamples=new ArrayDeque<>();
    private final long windowMs;
    private final long delayMs;
    private long started;
    private long estimatedBoundary = -1, estimatedEnd = -1, estimatedInterval;

    public RollingEarnings() { this(60000, 2000); }
    public RollingEarnings(long windowMs, long delayMs) {
        this.windowMs = windowMs;
        this.delayMs = delayMs;
    }

    public void reset(long now) { samples.clear();estimatedSamples.clear();started=now;estimatedBoundary=estimatedEnd=-1;estimatedInterval=0; }
    /** The first rounded change covers an unknown fraction of a bucket. Warm up
     * until two consecutive changes delimit a complete measured interval. */
    public void addEstimated(double amount, long start, long end) {
        if (!Double.isFinite(amount) || amount <= 0 || end <= start) return;
        if (estimatedBoundary != start) {
            estimatedSamples.clear(); estimatedBoundary=end; estimatedEnd=-1;
            return;
        }
        // Average complete bucket intervals. A 60s slice of a multi-minute bucket
        // collapses to the latest bucket's rate and jumps at every boundary.
        long horizon=Math.max(windowMs,3*(end-start));
        estimatedSamples.removeIf(s->s.end<=end-horizon);
        estimatedSamples.add(new Sample(amount,start,end));
        estimatedBoundary=estimatedEnd=end; estimatedInterval=end-start;
    }
    /** Unchanged rounded digits are missing information, not zero income.
     * Evaluate at the latest measured boundary and expire stale estimates. */
    public double estimatedPerHour(long now) {
        if(!estimatedReady(now))return 0;
        double total=0;long duration=0;
        for(Sample sample:estimatedSamples){total+=sample.amount;duration+=sample.end-sample.start;}
        return duration>0?total*3600000.0/duration:0;
    }
    public boolean estimatedReady(long now) {
        return estimatedEnd >= 0 && now >= estimatedEnd && now-estimatedEnd < Math.max(windowMs,2*estimatedInterval)
                && !estimatedSamples.isEmpty() && estimatedEnd-estimatedSamples.peekFirst().start>=3000;
    }
    public void add(double amount,long start,long end) {
        if (!Double.isFinite(amount)||amount<=0||end<=start)return;
        final long cutoff = start - windowMs;
        samples.removeIf(s -> s.end <= cutoff);
        if (samples.isEmpty() && start - started > windowMs) {
            started = start;
        }
        if (end <= started) return;
        if(start<started){amount*=((double)(end-started)/(end-start));start=started;}
        samples.add(new Sample(amount,start,end));
    }
    public double perHour(long now) {
        return rate(now, false);
    }
    /** Recency-weighted rate giving higher weight to recent samples (e.g. crop procs)
     * over the active window while smoothly decaying older samples without abrupt cliffs. */
    public double responsivePerHour(long now) {
        long end = now - delayMs;
        long start = Math.max(started, end - windowMs);
        if (end <= start) return 0;
        samples.removeIf(s -> s.end <= end - windowMs);
        if (samples.isEmpty()) return 0;
        double totalWeighted = 0;
        double tau = 15000.0;
        for (Sample s : samples) {
            long s0 = Math.max(start, s.start);
            long s1 = Math.min(end, s.end);
            if (s1 <= s0) continue;
            double age0 = Math.max(0, end - s1);
            double age1 = Math.max(0, end - s0);
            double weight = tau * (Math.exp(-age0 / tau) - Math.exp(-age1 / tau));
            double sampleRate = s.amount / (s.end - s.start);
            totalWeighted += sampleRate * weight;
        }
        double maxAge = end - start;
        double totalWeight = tau * (1.0 - Math.exp(-maxAge / tau));
        return totalWeight > 0 ? (totalWeighted / totalWeight) * 3600000.0 : 0;
    }
    /** Recency-weighted rate for rounded estimates giving higher weight to recent buckets. */
    public double responsiveEstimatedPerHour(long now) {
        if (!estimatedReady(now) || estimatedSamples.isEmpty()) return 0;
        double weightedAmount = 0;
        double weightedDuration = 0;
        long latestEnd = estimatedEnd;
        for (Sample s : estimatedSamples) {
            long duration = s.end - s.start;
            double age = Math.max(0, latestEnd - s.end);
            double weight = Math.exp(-age / 20000.0);
            weightedAmount += s.amount * weight;
            weightedDuration += duration * weight;
        }
        return weightedDuration > 0 ? weightedAmount * 3600000.0 / weightedDuration : 0;
    }
    /** Sparse random drops use a full window so a single early reward is not
     * extrapolated from milliseconds into an enormous hourly estimate. */
    public double perHourWindow(long now) { return rate(now, true); }
    private double rate(long now, boolean fullWindow) {
        long end=now-delayMs, start=fullWindow?end-windowMs:Math.max(started,end-windowMs);
        if(end<=start)return 0;
        samples.removeIf(s->s.end<=end-windowMs);
        if(samples.isEmpty())return 0;
        double total=0;
        for(Sample s:samples){long overlap=Math.max(0,Math.min(end,s.end)-Math.max(start,s.start));total+=s.amount*overlap/(s.end-s.start);}
        return total*3600000.0/(end-start);
    }
    public boolean ready(long now){
        long end=now-delayMs;
        return !samples.isEmpty() && (end-started>=3000);
    }
}

