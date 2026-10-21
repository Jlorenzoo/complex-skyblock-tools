package com.moneybooster;
import java.util.ArrayDeque;
/** Rolling window of earnings (default 60s), adapting to elapsed active duration during warmup. */
public final class RollingEarnings {
    private record Sample(double amount,long start,long end) {}
    private final ArrayDeque<Sample> samples=new ArrayDeque<>();
    private final long windowMs;
    private final long delayMs;
    private long started;

    public RollingEarnings() { this(60000, 2000); }
    public RollingEarnings(long windowMs, long delayMs) {
        this.windowMs = windowMs;
        this.delayMs = delayMs;
    }

    public void reset(long now) { samples.clear();started=now; }
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
        long end=now-delayMs, start=Math.max(started,end-windowMs);
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

