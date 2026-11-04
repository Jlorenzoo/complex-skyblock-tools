package com.skyblocktoolkit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

/** Tracks Bone Leggings / Burning Rush status and renders a subtle top-right HUD pill. */
public final class BoneLeggingsTracker {
    public static final String PREVIEW_LABEL = "§b⚡ Speed VI";
    private static boolean lastSpeed6 = false;

    public static String previewLabel() {
        return PREVIEW_LABEL;
    }

    public static boolean isBoneLeggings(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (name.contains("bone") && (name.contains("legging") || name.contains("pant") || name.contains("rush") || name.contains("armor"))) return true;
        if (name.contains("burning rush")) return true;
        String full = stack.toString().toLowerCase(Locale.ROOT);
        if (full.contains("burning rush")) return true;
        if (full.contains("bone") && (full.contains("legging") || full.contains("pant"))) return true;
        return false;
    }

    public static boolean isWearingBoneLeggings(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        ItemStack legs = mc.player.getItemBySlot(EquipmentSlot.LEGS);
        return isBoneLeggings(legs);
    }

    public static boolean isEquippedOrHeld(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        if (isWearingBoneLeggings(mc)) return true;
        return isBoneLeggings(mc.player.getMainHandItem()) || isBoneLeggings(mc.player.getOffhandItem());
    }

    public static boolean hasSpeed6(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        var effect = mc.player.getEffect(MobEffects.SPEED);
        return effect != null && effect.getAmplifier() >= 5;
    }

    public static boolean hasRegen(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        return mc.player.hasEffect(MobEffects.REGENERATION);
    }

    public static boolean shouldShow(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        var cfg = ToolkitConfig.get();
        if (cfg.boneLeggingsAlwaysVisible) return true;
        return hasSpeed6(mc) || isEquippedOrHeld(mc);
    }

    public static String currentLabel(Minecraft mc) {
        if (hasSpeed6(mc)) {
            return "§b⚡ Speed VI";
        }
        if (isWearingBoneLeggings(mc) || isEquippedOrHeld(mc)) {
            if (hasRegen(mc)) {
                return "§a✦ Regen";
            }
            return "§7⚡ Speed VI §8(Off)";
        }
        return "§b⚡ Speed VI";
    }

    public static void tick(Minecraft mc) {
        if (mc == null || mc.player == null) return;
        boolean speed6 = hasSpeed6(mc);
        if (speed6 && !lastSpeed6) {
            var cfg = ToolkitConfig.get();
            if (cfg.boneLeggingsSpeedAlert && (isEquippedOrHeld(mc) || cfg.boneLeggingsAlwaysVisible)) {
                mc.getSoundManager().play(new AlertSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.8f, 0.7f, 1.0));
            }
        }
        lastSpeed6 = speed6;
    }

    public static void render(GuiGraphicsExtractor extractor, Minecraft mc) {
        if (!shouldShow(mc)) return;
        String label = currentLabel(mc);
        int x = com.moneybooster.BoosterHudOverlay.boneLeggingsX(mc, label);
        int y = com.moneybooster.BoosterHudOverlay.boneLeggingsY(mc);
        renderPill(extractor, mc, label, x, y);
    }

    public static void renderPill(GuiGraphicsExtractor extractor, Minecraft mc, String label, int x, int y) {
        Font font = mc.font;
        int textW = font.width(label);
        int pillW = textW + 8;
        int pillH = 12;

        // Dark translucent background with subtle outline matching Complexify telemetry style
        extractor.fill(x, y, x + pillW, y + pillH, 0x90000000);
        boolean active = hasSpeed6(mc);
        int border = active ? 0x6000E5FF : 0x30FFFFFF;
        extractor.outline(x, y, pillW, pillH, border);

        extractor.text(font, label, x + 4, y + 2, 0xFFFFFFFF, true);
    }

    private BoneLeggingsTracker() {}
}
