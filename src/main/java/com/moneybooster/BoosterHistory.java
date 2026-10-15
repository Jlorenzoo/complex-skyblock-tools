package com.moneybooster;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Durable per-activation accounting; the aggregate from older versions stays separate. */
public final class BoosterHistory {
    public static final class Entry {
        public long id, started, ended, lastSeen;
        public String source = "Unknown source", itemName = "Money booster", iconJson, effect = "Money booster", status = "Active";
        public int percentage;
        public long durationSeconds = -1;
        public double bonus;
        public boolean inferredSource, excluded;
        public double excludedBonus;
        public String exclusionReason;
        public long elapsedSeconds(long now) { return Math.max(0, ((ended > 0 ? ended : now) - started) / 1000); }
    }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private final ArrayList<Entry> entries = new ArrayList<>();
    private long nextId = 1, activeId = -1, savedAt;
    private boolean dirty;
    public BoosterHistory(Path path) {
        this.path = path;
        if (!Files.exists(path)) return;
        try {
            Entry[] loaded = GSON.fromJson(Files.readString(path), Entry[].class);
            if (loaded != null) for (Entry e : loaded) {
                if (e == null || e.id < 1 || e.started < 0 || !Double.isFinite(e.bonus) || e.bonus < 0) continue;
                if (e.ended == 0) { e.ended = Math.max(e.started, e.lastSeen); e.status = "Interrupted"; }
                entries.add(e); nextId = Math.max(nextId, e.id + 1);
            }
        } catch (Exception e) { System.err.println("[MoneyBoosterHUD] Cannot load history: " + e.getMessage()); }
    }
    public List<Entry> newestFirst() {
        ArrayList<Entry> result = new ArrayList<>(entries); result.removeIf(e->e.excluded); Collections.reverse(result); return result;
    }
    public List<Entry> newestFirst(boolean moneyOnly) {
        List<Entry> result = newestFirst();
        if(moneyOnly) result.removeIf(entry -> entry.percentage <= 0);
        return result;
    }
    public Entry latestMoneyBooster(){for(Entry e:newestFirst())if(e.percentage>0)return e;return null;}
    public boolean tryResume(int pct, long now, long maxGapMs) {
        Entry latest = latestMoneyBooster();
        if (latest != null && latest.percentage == pct && (now - latest.lastSeen <= maxGapMs)) {
            activeId = latest.id;
            latest.status = "Active";
            latest.ended = 0;
            latest.lastSeen = now;
            dirty = true;
            return true;
        }
        return false;
    }
    public long activeId() { return activeId; }
    public Entry find(long id) { for (Entry e : entries) if (e.id == id) return e; return null; }
    public Entry start(String source, String name, String icon, boolean inferred, int pct, long duration, String effect, long now) {
        if (pct > 0) finish(now, "Replaced");
        Entry e = new Entry(); e.id = nextId++; e.source = source; e.itemName = name; e.iconJson = icon;
        e.inferredSource = inferred; e.percentage = pct; e.durationSeconds = duration; e.effect = effect; e.started = e.lastSeen = now;
        if (pct <= 0) { e.ended = now; e.status = "No money boost"; } else activeId = e.id;
        entries.add(e); dirty = true; save(now); return e;
    }
    public void finish(long now, String status) {
        Entry e = find(activeId);
        if (e != null) { e.ended = Math.max(e.started, now); e.lastSeen = e.ended; e.status = status; dirty = true; }
        activeId = -1;
    }
    public void addBonus(long id, double bonus) {
        Entry e = find(id);
        if (e == null || e.excluded || !Double.isFinite(bonus) || bonus < 0) return;
        e.bonus += bonus; dirty = true;
    }
    public void tick(long now) {
        Entry e = find(activeId);
        if (e != null) { e.lastSeen = now; dirty = true; }
        if (dirty && now - savedAt >= 2000) save(now);
    }
    public void clear(long now) { entries.clear(); activeId = -1; dirty = true; save(now); }
    public void save(long now) {
        try { SafeFiles.write(path, GSON.toJson(entries)); dirty = false; savedAt = now; }
        catch (Exception e) { System.err.println("[MoneyBoosterHUD] Cannot save history: " + e.getMessage()); }
    }
}
