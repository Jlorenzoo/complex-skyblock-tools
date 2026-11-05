package com.moneybooster;

import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/** Reads received text, including hover text, without requesting anything from the server. */
public final class BalanceComponents {
    private BalanceComponents() {}
    public static List<String> texts(Component component) {
        var texts = new LinkedHashSet<String>();
        collect(component, texts, 0);
        return List.copyOf(texts);
    }
    private static void collect(Component component, LinkedHashSet<String> texts, int depth) {
        if (component == null || depth > 8 || texts.size() >= 64) return;
        String text = component.getString(512).replaceAll("§.", "").strip();
        if (!text.isEmpty()) texts.add(text);
        if (component.getStyle().getHoverEvent() instanceof HoverEvent.ShowText hover)
            collect(hover.value(), texts, depth + 1);
        for (Component child : component.getSiblings()) collect(child, texts, depth + 1);
    }
    public static BalanceReading read(Component line) {
        if (line == null) return null;
        BalanceReading visible = BalanceReading.parse(line.getString());
        if (visible == null) return null;
        BalanceReading best = visible;
        for (String text : texts(line)) {
            BalanceReading candidate = BalanceReading.parse(text);
            // A balance line may carry only its unabridged dollar amount in the hover.
            if (candidate == null && text.matches("\\$[0-9,]+(?:\\.[0-9]+)?"))
                candidate = BalanceReading.parse("Balance: " + text);
            if (candidate != null && candidate.resolution() < best.resolution()
                    && Math.abs(candidate.amount() - visible.amount()) <= visible.resolution())
                best = candidate;
        }
        return best;
    }
}
