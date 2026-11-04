package com.skyblocktoolkit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Locale;

/** Tracks Bone Leggings / Burning Rush status and renders an item HUD overlay. */
public final class BoneLeggingsTracker {
    public static final int STYLE_ICON_NAME = 0;
    public static final int STYLE_ICON_STATUS = 1;
    public static final int STYLE_ICON_SYMBOL = 2;
    public static final int STYLE_NAME_ONLY = 3;

    private static boolean lastSpeed6 = false;
    private static ItemStack lastServerStack = null;

    public static String styleName(int style) {
        return switch (style) {
            case STYLE_ICON_STATUS -> "Icon & Status";
            case STYLE_ICON_SYMBOL -> "Icon & Symbol";
            case STYLE_NAME_ONLY -> "Name only";
            default -> "Icon & Name";
        };
    }

    public static boolean hasIcon(int style) {
        return style != STYLE_NAME_ONLY;
    }

    public static String formatLabel(int style, boolean active) {
        return switch (style) {
            case STYLE_ICON_STATUS -> active ? "§a✔ On" : "§c✖ Off";
            case STYLE_ICON_SYMBOL -> active ? "§a✔" : "§c✖";
            case STYLE_NAME_ONLY -> active ? "Bone Leggings: §aOn" : "Bone Leggings: §cOff";
            default -> active ? "Bone Leggings: §a✔ On" : "Bone Leggings: §c✖ Off";
        };
    }

    public static String previewLabel() {
        return formatLabel(ToolkitConfig.get().boneLeggingsHudStyle, true);
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

    public static boolean isActive(Minecraft mc) {
        return hasSpeed6(mc);
    }

    public static boolean shouldShow(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        var cfg = ToolkitConfig.get();
        if (cfg.boneLeggingsAlwaysVisible) return true;
        return hasSpeed6(mc) || isEquippedOrHeld(mc);
    }

    public static String currentLabel(Minecraft mc) {
        var cfg = ToolkitConfig.get();
        boolean active = mc != null && isActive(mc);
        return formatLabel(cfg.boneLeggingsHudStyle, active);
    }

    public static ItemStack getItemStack(Minecraft mc) {
        if (mc != null && mc.player != null) {
            try {
                ItemStack legs = mc.player.getItemBySlot(EquipmentSlot.LEGS);
                if (isBoneLeggings(legs)) {
                    lastServerStack = legs.copy();
                    return legs;
                }
                ItemStack main = mc.player.getMainHandItem();
                if (isBoneLeggings(main)) {
                    lastServerStack = main.copy();
                    return main;
                }
                ItemStack off = mc.player.getOffhandItem();
                if (isBoneLeggings(off)) {
                    lastServerStack = off.copy();
                    return off;
                }
                for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = mc.player.getInventory().getItem(i);
                    if (isBoneLeggings(stack)) {
                        lastServerStack = stack.copy();
                        return stack;
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (lastServerStack != null) {
            return lastServerStack;
        }
        try {
            var seasonal = SeasonalRegistry.find("bone_pants");
            if (seasonal != null) return seasonal.icon();
            return new ItemStack(Items.CHAINMAIL_LEGGINGS);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static int getWidth(Minecraft mc) {
        var cfg = ToolkitConfig.get();
        String label = currentLabel(mc);
        int textW = mc != null && mc.font != null ? mc.font.width(label) : label.length() * 6;
        int width = textW;
        if (hasIcon(cfg.boneLeggingsHudStyle)) {
            width += 19;
        }
        if (cfg.boneLeggingsBackground) {
            width += 6;
        }
        return width;
    }

    public static int getHeight() {
        var cfg = ToolkitConfig.get();
        if (hasIcon(cfg.boneLeggingsHudStyle)) {
            return cfg.boneLeggingsBackground ? 18 : 16;
        }
        return cfg.boneLeggingsBackground ? 14 : 10;
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
        int x = com.moneybooster.BoosterHudOverlay.boneLeggingsX(mc, "");
        int y = com.moneybooster.BoosterHudOverlay.boneLeggingsY(mc);
        renderWidget(extractor, mc, x, y, false);
    }

    public static void renderPill(GuiGraphicsExtractor extractor, Minecraft mc, String label, int x, int y) {
        renderWidget(extractor, mc, x, y, true);
    }

    public static void renderWidget(GuiGraphicsExtractor extractor, Minecraft mc, int x, int y, boolean isPreview) {
        var cfg = ToolkitConfig.get();
        int style = cfg.boneLeggingsHudStyle;
        boolean active = isPreview || (mc != null && isActive(mc));
        String label = formatLabel(style, active);
        ItemStack icon = hasIcon(style) ? getItemStack(mc) : null;
        renderItemStatus(extractor, mc != null ? mc.font : null, icon, label, x, y, hasIcon(style), cfg.boneLeggingsBackground);
    }

    /**
     * Reusable renderer for item HUD overlays (Bone Leggings, Void Rod, and future seasonals).
     */
    public static void renderItemStatus(GuiGraphicsExtractor extractor, Font font, ItemStack stack,
                                        String label, int x, int y, boolean showIcon, boolean background) {
        if (extractor == null || font == null) return;
        boolean hasSprite = showIcon && stack != null && !stack.isEmpty();
        int textW = font.width(label);
        int contentW = hasSprite ? (19 + textW) : textW;
        int contentH = hasSprite ? 16 : 9;
        int totalW = contentW + (background ? 6 : 0);
        int totalH = contentH + (background ? 4 : 0);

        if (background) {
            // Subtle, non-intrusive dark glass background with no harsh border
            extractor.fill(x, y, x + totalW, y + totalH, 0x40000000);
        }

        int drawX = x + (background ? 3 : 0);
        int drawY = y + (background ? 2 : 0);

        if (hasSprite) {
            extractor.item(stack, drawX, drawY);
            extractor.text(font, label, drawX + 19, drawY + 3, 0xFFFFFFFF, true);
        } else {
            extractor.text(font, label, drawX, drawY, 0xFFFFFFFF, true);
        }
    }

    private BoneLeggingsTracker() {}
}
