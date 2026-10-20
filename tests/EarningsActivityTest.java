package com.moneybooster;

import static com.moneybooster.EarningsActivity.Tracker.*;

public class EarningsActivityTest {
    private static void expect(EarningsActivity.Tracker got, EarningsActivity.Tracker wanted) {
        if (got != wanted) throw new AssertionError(got + " expected " + wanted);
    }
    public static void main(String[] args) {
        var activity = new EarningsActivity();
        expect(activity.select(1000, true, true, false, false, false), NONE);
        activity.cropIncome(1000);
        expect(activity.select(1000, true, true, false, false, false), CROPS);
        expect(activity.select(5999, true, true, false, false, false), CROPS);
        expect(activity.select(6000, true, true, false, false, false), NONE);
        expect(activity.select(2000, true, true, true, true, false), FISHING);
        expect(activity.select(2000, true, true, true, false, false), FISHING);
        expect(activity.select(2000, true, true, false, true, true), FISHING);
        expect(activity.select(2000, true, true, false, true, false), FISHING);
        expect(activity.select(2000, true, false, true, true, false), NONE);
        expect(activity.select(2000, false, true, false, false, false), NONE);
        expect(activity.select(2000, false, false, true, true, true), NONE);
        // Delayed older receipts cannot replace newer activity, and reset hides it.
        activity.cropIncome(4000); activity.cropIncome(1000);
        expect(activity.select(7000, true, true, false, false, false), CROPS);
        activity.reset();
        expect(activity.select(7000, true, true, false, false, true), NONE);
        expect(activity.select(7000, true, true, false, true, false), NONE);
        // Waiting for a bite stays visible even after several minutes.
        expect(activity.select(10000, true, true, true, true, false), FISHING);
        expect(activity.select(190000, true, true, true, true, false), FISHING);
        // The gap after reeling is measured from the cast, not the previous catch.
        expect(activity.select(210000, true, true, false, true, false), FISHING);
        expect(activity.select(250000, true, true, false, true, false), FISHING);
        expect(activity.select(309999, true, true, false, true, false), FISHING);
        expect(activity.select(310000, true, true, false, true, false), NONE);
        expect(activity.select(320000, true, true, false, true, true), FISHING);
        expect(activity.select(320001, true, true, false, false, false), NONE);
        activity.reset();
        expect(activity.select(320002, true, true, false, true, false), NONE);
        System.out.println("PASS: idle expiry, exclusive switching, toggles, long casts, 2min recast grace and reset");
    }
}
