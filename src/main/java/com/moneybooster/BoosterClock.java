package com.moneybooster;

/** Activation durations are authoritative; the sidebar fills in state when joining mid-effect. */
public final class BoosterClock {
    private long started = -1, end, total, lastPresent = -1, lastSample = -1, sidebarRemaining = -1;
    private boolean present;
    private long lastDuration=-1;
    private boolean precise, chatTimed;
    public void start(long seconds, long now) {
        started = now; total = seconds * 1000; end = now + total; precise = true; chatTimed = true;
        lastPresent = lastSample = -1; sidebarRemaining = -1; lastDuration=-1; present = false;
    }
    public void observe(boolean found,long remainingMs,long now){observe(found,remainingMs,true,now);}
    public void observe(boolean found, long remainingMs, boolean exactSeconds, long now) {
        present = found; lastSample = now;
        if (!found) return;
        lastPresent = now;
        if (started < 0) started = now;
        sidebarRemaining = remainingMs;
        if (remainingMs >= 0 && !chatTimed) {
            if(exactSeconds){
                // Repeated scoreboard frames are the same sample, not fresh server time.
                // Only reconcile sidebar-only clocks within the display's one-second precision.
                if(remainingMs!=lastDuration && (!precise || Math.abs(end-(now+remainingMs))>1000))
                    end=now+remainingMs;
                precise=true;
            }
            else if(!precise && remainingMs!=lastDuration){
                long current=Math.max(0,end-now);
                // A minute-only sidebar is a bucket, not an exact countdown.
                // Keep the local estimate when it fits; otherwise seed from the new bucket.
                if(current<remainingMs || current>=remainingMs+60000)end=now+remainingMs+59000;
            }
            lastDuration=remainingMs;
            total=Math.max(total,end-started);
        }
    }
    private boolean recent(long now) { return lastPresent >= 0 && now - lastPresent <= 3000; }
    public boolean active(long now) {
        // A persistent/lagging sidebar label must never outlive a known deadline.
        if ((chatTimed || total > 0) && now >= end) return false;
        if (recent(now)) return true;
        // Once the sidebar has shown the effect, a confirmed removal ends it even if chat predicted longer.
        if (lastPresent >= 0 && lastSample > lastPresent && !present) return false;
        return started >= 0 && now < end;
    }
    public boolean hasTimer(long now) {
        return started >= 0 && now < end;
    }
    public boolean chatTimed() { return chatTimed; }
    public long remaining(long now) {
        return Math.max(0, end - now);
    }
    public float progress(long now) {
        if (!active(now)) return 0;
        if (!hasTimer(now) || total <= 0) return 1;
        return Math.max(0, Math.min(1, (float) remaining(now) / total));
    }
    public void reset() { started = lastPresent = lastSample = -1; end = total = 0; sidebarRemaining = -1; lastDuration=-1; present = false; precise=chatTimed=false; }
}
