package com.skyblocktoolkit;

import com.fishingpings.FishingSignals;
import com.moneybooster.RollingEarnings;

/** Tracks confirmed personal Vote Key reward messages for the earnings HUD. */
public final class VoteKeyTracker {
    private static final RollingEarnings earnings = new RollingEarnings();

    private VoteKeyTracker() {}

    public static void reset() {
        earnings.reset(System.currentTimeMillis());
    }

    public static void chat(String text) {
        long count = FishingSignals.oniVoteKeyReward(text);
        if (count > 0) {
            long now = System.currentTimeMillis();
            earnings.add(count, now - 1, now);
        }
    }

    public static double hourlyValue(long now) {
        double rate = earnings.perHourWindow(now);
        return earnings.ready(now) ? rate * ToolkitConfig.get().voteKeyPrice : 0;
    }
}
