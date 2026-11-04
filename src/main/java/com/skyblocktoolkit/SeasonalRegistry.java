package com.skyblocktoolkit;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;

/** Registry of active and upcoming seasonal items. Add new seasonals here. */
public final class SeasonalRegistry {
    public static final List<SeasonalItem> ITEMS = List.of(
        new SeasonalItem(
            "void_rod",
            "Void Rod",
            "Seasonal Fishing Rod · Reel depth alert & line-snap cues",
            () -> new ItemStack(Items.FISHING_ROD),
            (screen, top) -> screen.buildVoidRodSettings(top)
        ),
        new SeasonalItem(
            "bone_leggings",
            "Bone Leggings",
            "Seasonal Armor Piece · Burning Rush Rune",
            () -> new ItemStack(Items.BONE),
            (screen, top) -> screen.buildBoneLeggingsSettings(top)
        )
    );

    public static SeasonalItem find(String id) {
        for (var item : ITEMS) if (item.id().equals(id)) return item;
        return null;
    }

    private SeasonalRegistry() {}
}
