package com.moneybooster;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.*;
import java.util.Locale;

public final class BoosterSources {
    public record Source(String type, String name, String icon, boolean inferred) {}
    private static ItemStack lastUsed = ItemStack.EMPTY;
    private static long usedAt;
    public static void capture(ItemStack stack) {
        if (stack.isEmpty()) return;
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (!name.contains("coconut") && !(name.contains("money") && name.contains("boost"))) return;
        lastUsed = stack.copyWithCount(1); usedAt = System.currentTimeMillis();
    }
    public static Source recent() {
        if (lastUsed.isEmpty() || System.currentTimeMillis() - usedAt > 5000) return new Source("Unknown source", "Money booster", null, false);
        ItemStack stack = lastUsed; lastUsed = ItemStack.EMPTY;
        String name = stack.getHoverName().getString();
        String type = name.toLowerCase(Locale.ROOT).contains("coconut") ? "Coconut" : "Money booster item";
        return new Source(type, name, encode(stack), true);
    }
    public static void reset() { lastUsed = ItemStack.EMPTY; usedAt = 0; }
    private static String encode(ItemStack stack) {
        try {
            var mc = Minecraft.getInstance();
            if (mc.level != null) return ItemStack.CODEC.encodeStart(mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE), stack).result().map(Object::toString).orElse(null);
        } catch (Exception ignored) {}
        return null;
    }
    public static ItemStack icon(BoosterHistory.Entry entry) {
        if (entry.iconJson != null) try {
            var mc = Minecraft.getInstance();
            if (mc.level != null) return ItemStack.CODEC.parse(mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(entry.iconJson)).result().orElse(new ItemStack(Items.CLOCK));
        } catch (Exception ignored) {}
        return new ItemStack(entry.source.equals("Money booster item") ? Items.POTION : Items.CLOCK);
    }
}
