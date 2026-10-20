package com.skyblocktoolkit;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in Clear Harvest resource pack manager:
 * Dim and desaturate immature crops so fully grown crops are immediately obvious.
 * Packaged inside the mod jar for automatic downloads and zero-friction updates.
 */
public final class ClearHarvest {
    public static final String PACK_ID = "skyblocktoolkit:clear_harvest";
    public static final String LEGACY_ZIP_ID = "file/Clear-Harvest-90pct-26.2.zip";

    private ClearHarvest() {}

    public static void init() {
        FabricLoader.getInstance().getModContainer("skyblocktoolkit").ifPresent(container -> {
            ResourceManagerHelper.registerBuiltinResourcePack(
                Identifier.fromNamespaceAndPath("skyblocktoolkit", "clear_harvest"),
                container,
                Component.literal("Clear Harvest"),
                ResourcePackActivationType.DEFAULT_ENABLED
            );
        });
    }

    public static boolean isEnabled() {
        var mc = Minecraft.getInstance();
        if (mc != null && mc.getResourcePackRepository() != null) {
            var selected = mc.getResourcePackRepository().getSelectedIds();
            return selected.contains(PACK_ID) || selected.contains(LEGACY_ZIP_ID);
        }
        return ToolkitConfig.get().clearHarvest;
    }

    public static void toggle(Minecraft mc) {
        setEnabled(mc, !isEnabled());
    }

    public static void setEnabled(Minecraft mc, boolean enable) {
        ToolkitConfig.get().clearHarvest = enable;
        ToolkitConfig.get().save();
        if (mc == null) return;
        PackRepository repo = mc.getResourcePackRepository();
        if (repo == null) return;

        List<String> selected = new ArrayList<>(repo.getSelectedIds());
        boolean currentlyActive = selected.contains(PACK_ID) || selected.contains(LEGACY_ZIP_ID);

        if (enable && !currentlyActive) {
            if (repo.isAvailable(PACK_ID)) {
                selected.add(PACK_ID);
            } else if (repo.isAvailable(LEGACY_ZIP_ID)) {
                selected.add(LEGACY_ZIP_ID);
            } else {
                selected.add(PACK_ID);
            }
            repo.setSelected(selected);
            mc.options.updateResourcePacks(repo);
        } else if (!enable && currentlyActive) {
            selected.remove(PACK_ID);
            selected.remove(LEGACY_ZIP_ID);
            repo.setSelected(selected);
            mc.options.updateResourcePacks(repo);
        }
    }
}
