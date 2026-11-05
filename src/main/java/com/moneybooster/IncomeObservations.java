package com.moneybooster;

import java.util.LinkedHashMap;
import java.util.List;

/** Bounded local evidence, never an income ledger or an outgoing request. */
public final class IncomeObservations {
    public record Entry(String source, String text, long firstSeen, long lastSeen, long observations) {}
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
    public boolean add(String source, String text, long now) {
        if (text == null) return false;
        text = text.replaceAll("§.", "").strip();
        if (!text.matches("(?is).*(?:\\$|\\bmoney\\b|\\bbalance\\b|\\bearned\\b|\\bsold\\b).*")) return false;
        if (text.length() > 512) text = text.substring(0, 512);
        String key = source + "\n" + text;
        Entry previous = entries.remove(key);
        if (previous == null) {
            // Preserve room for action-bar and item observations even with changing balances.
            while (entries.values().stream().filter(e -> e.source().equals(source)).count() >= 16) {
                String oldest = entries.entrySet().stream().filter(e -> e.getValue().source().equals(source)).findFirst().orElseThrow().getKey();
                entries.remove(oldest);
            }
        }
        entries.put(key, new Entry(source, text, previous == null ? now : previous.firstSeen(), now,
                previous == null ? 1 : previous.observations() + 1));
        while (entries.size() > 64) entries.remove(entries.keySet().iterator().next());
        return true;
    }
    public List<Entry> entries() { return List.copyOf(entries.values()); }
    public void reset() { entries.clear(); }
}
