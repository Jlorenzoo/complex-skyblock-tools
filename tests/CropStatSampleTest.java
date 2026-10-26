package com.skyblocktoolkit;

import java.util.HashMap;
import java.util.Map;

public final class CropStatSampleTest {
    public static void main(String[] args) {
        var sample = new CropStatSample();
        var initial = new HashMap<>(Map.of("wheat", 100, "nether wart", 50));
        sample.start(initial, 0);
        initial.put("wheat", 900); // The baseline must be an independent snapshot.
        var result = sample.finish(Map.of("wheat", 120, "nether wart", 60), 60_000_000_000L);
        check(result.valid() && result.total() == 30 && result.hourly() == 1800, "delta across crop types and hourly rate");
        check(!sample.active(), "stop consumes sample");
        sample.start(Map.of("wheat", 50), 0);
        check(!sample.finish(Map.of("wheat", 1), 10).valid(), "server reset invalidates result");
        sample.start(Map.of("wheat", 50), 0);
        check(!sample.finish(Map.of(), 10).valid(), "missing counter invalidates result");
        sample.start(Map.of("wheat", 50), 10);
        check(!sample.finish(Map.of("wheat", 70), 10).valid(), "no zero-duration rate");
        sample.start(Map.of("wheat", 50), 0);
        var zero = sample.finish(Map.of("wheat", 50), 10);
        check(zero.valid() && zero.total() == 0 && zero.hourly() == 0, "unchanged counters remain zero");
        sample.start(Map.of("a", 0, "b", 0), 0);
        check(sample.finish(Map.of("a", Integer.MAX_VALUE, "b", Integer.MAX_VALUE), 10).total() == 4294967294L, "total does not overflow int");
        sample.start(Map.of("wheat", 5), 0); sample.reset();
        check(!sample.active(), "disconnect/cancel discards baseline");
        System.out.println("PASS: independent crop-stat snapshots, multi-crop rates, resets, incomplete replies, zero gains and overflow");
    }
    static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
}
