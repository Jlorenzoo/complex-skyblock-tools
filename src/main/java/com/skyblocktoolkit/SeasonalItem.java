package com.skyblocktoolkit;

import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

/** Declarative definition of an in-game seasonal item with its icon and settings builder. */
public final class SeasonalItem {
    @FunctionalInterface
    public interface SettingsBuilder {
        int build(ToolkitScreen screen, int top);
    }
    private final String id;
    private final String name;
    private final String subtitle;
    private final Supplier<ItemStack> icon;
    private final SettingsBuilder settings;

    public SeasonalItem(String id, String name, String subtitle, Supplier<ItemStack> icon, SettingsBuilder settings) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle;
        this.icon = icon;
        this.settings = settings;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String subtitle() { return subtitle; }
    public ItemStack icon() { return icon != null ? icon.get() : ItemStack.EMPTY; }
    public int buildSettings(ToolkitScreen screen, int top) {
        return settings != null ? settings.build(screen, top) : top;
    }
}
