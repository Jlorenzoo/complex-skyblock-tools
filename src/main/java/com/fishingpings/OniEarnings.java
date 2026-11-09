package com.fishingpings;

import java.util.ArrayDeque;
import java.util.function.ToDoubleFunction;

/** Retains quantities so changing a price also updates earlier catches in the sample. */
final class OniEarnings {
    static final long HOUR = 3_600_000;
    static final long WARMUP = 300_000;
    enum Reward { COD, SALMON, PUFFERFISH, TROPICAL_FISH, GOLD_SHARD, VOTE_KEY }
    private record Gain(long time, Reward reward, long count) {}

    private final ArrayDeque<Gain> gains = new ArrayDeque<>();
    private long started = -1;

    void start(long now) {
        if (started < 0) started = now;
    }

    void add(long now, Reward reward, long count) {
        if (count <= 0) return;
        start(now);
        prune(now);
        gains.addLast(new Gain(now, reward, count));
    }

    private void prune(long now) {
        while (!gains.isEmpty() && gains.getFirst().time() <= now - HOUR) gains.removeFirst();
    }

    long sampleTime(long now) {
        return started < 0 ? 0 : Math.clamp(now - started, 0, HOUR);
    }

    double hourly(long now, ToDoubleFunction<Reward> price) {
        prune(now);
        double value = 0;
        for (var gain : gains) value += gain.count() * price.applyAsDouble(gain.reward());
        return value * HOUR / Math.max(WARMUP, sampleTime(now));
    }

    void reset() {
        started = -1;
        gains.clear();
    }
}
