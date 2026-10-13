package com.moneybooster;
import java.util.ArrayDeque;
/** Uses the last ten seconds of settled earnings, delayed two seconds for shop reconciliation. */
public final class RollingEarnings {
    private record Sample(double amount,long start,long end) {}
    private final ArrayDeque<Sample> samples=new ArrayDeque<>();
    private long started;
    public void reset(long now) { samples.clear();started=now; }
    public void add(double amount,long start,long end) {
        if (!Double.isFinite(amount)||amount<=0||end<=start||end<=started)return;
        if(start<started){amount*=((double)(end-started)/(end-start));start=started;}
        samples.add(new Sample(amount,start,end));
    }
    public double perHour(long now) {
        long end=now-2000, start=Math.max(started,end-10000);
        if(end<=start)return 0;
        samples.removeIf(s->s.end<=end-10000);
        double total=0;
        for(Sample s:samples){long overlap=Math.max(0,Math.min(end,s.end)-Math.max(start,s.start));total+=s.amount*overlap/(s.end-s.start);}
        return total*3600000.0/(end-start);
    }
    public boolean ready(long now){return now-started>=3000;}
}
