package com.moneybooster;

/** The sidebar confirms server state; chat time is a fallback estimate. */
public final class BoosterClock {
    private long started = -1, end, total, lastPresent = -1, lastSample = -1, sidebarRemaining = -1;
    private boolean present;
    private long lastDuration=-1;
    public void start(long seconds, long now) {
        started = now; total = seconds * 1000; end = now + total;
        lastPresent = lastSample = -1; sidebarRemaining = -1; lastDuration=-1; present = false;
    }
    public void observe(boolean found,long remainingMs,long now){observe(found,remainingMs,true,now);}
    public void observe(boolean found, long remainingMs, boolean exactSeconds, long now) {
        present = found; lastSample = now;
        if (!found) return;
        lastPresent = now;
        if (started < 0) started = now;
        sidebarRemaining = remainingMs;
        if (remainingMs >= 0) {
            if(exactSeconds) end=now+remainingMs;
            else if(remainingMs!=lastDuration){
                long current=Math.max(0,end-now);
                // A minute-only sidebar is a bucket, not an exact countdown.
                // Preserve the chat deadline when it fits; otherwise seed a local estimate once.
                if(current<remainingMs || current>=remainingMs+60000)end=now+remainingMs+59000;
            }
            lastDuration=remainingMs;
            total=Math.max(total,end-started);
        }
    }
    private boolean recent(long now) { return lastPresent >= 0 && now - lastPresent <= 3000; }
    public boolean active(long now) {
        if (recent(now)) return true;
        // Once the sidebar has shown the effect, a confirmed removal ends it even if chat predicted longer.
        if (lastPresent >= 0 && lastSample > lastPresent && !present) return false;
        return started >= 0 && now < end;
    }
    public boolean hasTimer(long now) {
        return started >= 0 && now < end;
    }
    public long remaining(long now) {
        return Math.max(0, end - now);
    }
    public float progress(long now) {
        if (!active(now)) return 0;
        if (!hasTimer(now) || total <= 0) return 1;
        return Math.max(0, Math.min(1, (float) remaining(now) / total));
    }
    public void reset() { started = lastPresent = lastSample = -1; end = total = 0; sidebarRemaining = -1; lastDuration=-1; present = false; }
}
