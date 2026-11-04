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
            "bone_pants",
            "Bone Leggings",
            "Seasonal Armor Piece · Burning Rush Rune (Speed VI)",
            () -> new ItemStack(Items.CHAINMAIL_LEGGINGS),
            (screen, top) -> screen.buildBoneLeggingsSettings(top)
        )
    );

    public static SeasonalItem find(String id) {
        for (var item : ITEMS) {
            if (item.id().equals(id) || (id.equals("bone_leggings") && item.id().equals("bone_pants")))
                return item;
        }
        return null;
    }

    private SeasonalRegistry() {}
}
