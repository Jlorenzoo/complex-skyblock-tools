package com.moneybooster;

/** Chooses one live earnings label without changing tracking or HUD preferences. */
public final class EarningsActivity {
    public enum Tracker { NONE, CROPS, FISHING }
    public static final long IDLE_MS = 5_000;
    public static final long FISHING_IDLE_MS = 120_000;
    private long lastCropIncome = -1;
    private long lastFishingActivity = -1;

    public void cropIncome(long at) { cropActivity(at); }
    /** Harvesting stays visible while rounded balance digits have not changed yet. */
    public boolean cropActivity(long at) {
        boolean resumed = lastCropIncome < 0 || at - lastCropIncome >= IDLE_MS;
        lastCropIncome = Math.max(lastCropIncome, at);
        return resumed;
    }
    public void reset() { lastCropIncome = lastFishingActivity = -1; }

    public Tracker select(long now, boolean cropsEnabled, boolean fishEnabled,
                          boolean cast, boolean holdingRod, boolean recentCatch) {
        // Keep the fishing context even when its label is disabled: an old crop
        // sample must not reappear while the player is fishing or between casts.
        if (cast || holdingRod) {
            if (cast || recentCatch) lastFishingActivity = now;
            boolean fishing = lastFishingActivity >= 0 && now >= lastFishingActivity
                    && now - lastFishingActivity < FISHING_IDLE_MS;
            return fishEnabled && fishing ? Tracker.FISHING : Tracker.NONE;
        }
        return cropsEnabled && lastCropIncome >= 0 && now >= lastCropIncome
                && now - lastCropIncome < IDLE_MS ? Tracker.CROPS : Tracker.NONE;
    }
}
