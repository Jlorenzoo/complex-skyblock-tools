package com.moneybooster;

import com.google.gson.GsonBuilder;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;

/** Captures only already-received money text during farming; sends no packets or commands. */
public final class PassiveIncomeCapture {
    private static final IncomeObservations observations = new IncomeObservations();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("skyblocktoolkit-income-observations.json");
    private static long until = -1, started, saved, inventoryAt;
    private static boolean dirty;
    private PassiveIncomeCapture() {}
    public static void reset() {
        observations.reset(); until = -1; started = System.currentTimeMillis(); saved = inventoryAt = 0; dirty = false;
    }
    public static void observe(String source, Component component) {
        long now = System.currentTimeMillis();
        if (now > until) return;
        for (String text : BalanceComponents.texts(component)) dirty |= observations.add(source, text, now);
    }
    public static void tick(Minecraft mc, boolean farming) {
        long now = System.currentTimeMillis();
        if (farming || RateTracker.isRunning() || RateTracker.isPreparing()) {
            if (until < 0) { started = now; dirty = true; }
            until = now + 10_000;
        }
        if (mc.player != null && now <= until && now - inventoryAt >= 1_000) {
            inventoryAt = now;
            var stack = mc.player.getMainHandItem();
            observe("held-item-name", stack.getHoverName());
            var lore = stack.get(DataComponents.LORE);
            if (lore != null) for (Component line : lore.lines()) observe("held-item-lore", line);
        }
        if (dirty && now - saved >= 5_000) {
            saved = now;
            try {
                var data = new com.google.gson.JsonObject();
                data.addProperty("schema", 1);
                data.addProperty("startedAt", started);
                data.addProperty("updatedAt", now);
                data.addProperty("mode", "passive: received money text only; no commands or requests");
                data.add("observations", new com.google.gson.Gson().toJsonTree(observations.entries()));
                SafeFiles.write(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(data));
                dirty = false;
            } catch (java.io.IOException ignored) { /* Diagnostics never interrupt gameplay. */ }
        }
    }
}
