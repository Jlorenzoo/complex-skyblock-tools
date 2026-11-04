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
    public static final int STYLE_EFFECT_ICONS = 0;
    public static final int STYLE_ICON_STATUS = 1;
    public static final int STYLE_ICON_SIMPLE = 2;
    public static final int STYLE_STATUS_ONLY = 3;

    private static boolean lastSpeed6 = false;
    private static ItemStack lastServerStack = null;

    public static String styleName(int style) {
        return switch (style) {
            case STYLE_ICON_STATUS -> "Icon & Effects";
            case STYLE_ICON_SIMPLE -> "Icon & On/Off";
            case STYLE_STATUS_ONLY -> "Effects only";
            default -> "Effect icons";
        };
    }

    public static boolean hasIcon(int style) {
        return style != STYLE_STATUS_ONLY;
    }

    public static String effectLabel(boolean speed, boolean regen) {
        if (speed && regen) return "§bSpeed §dRegen";
        if (speed) return "§bSpeed";
        if (regen) return "§dRegen";
        return "§7Idle";
    }

    public static String formatLabel(int style, boolean active) {
        return labelForEffects(style, active, false);
    }

    public static String labelForEffects(int style, boolean speed, boolean regen) {
        return switch (style) {
            case STYLE_ICON_STATUS, STYLE_STATUS_ONLY -> effectLabel(speed, regen);
            case STYLE_ICON_SIMPLE -> speed || regen ? "§aOn" : "§7Off";
            default -> "";
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
        return hasSpeed6(mc) || hasRegen(mc);
    }

    public static boolean shouldShow(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        var cfg = ToolkitConfig.get();
        if (cfg.boneLeggingsAlwaysVisible) return true;
        return hasSpeed6(mc) || isEquippedOrHeld(mc);
    }

    public static String currentLabel(Minecraft mc) {
        var cfg = ToolkitConfig.get();
        return labelForEffects(cfg.boneLeggingsHudStyle, hasSpeed6(mc), hasRegen(mc));
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
        int padding = cfg.boneLeggingsBackground ? 6 : 0;
        if (cfg.boneLeggingsHudStyle == STYLE_EFFECT_ICONS) return 46 + padding;
        // Reserve the widest state so right alignment and HUD editor bounds never jump.
        String label = cfg.boneLeggingsHudStyle == STYLE_ICON_SIMPLE ? "Off" : "Speed Regen";
        int textW = mc != null && mc.font != null ? mc.font.width(label) : label.length() * 6;
        return textW + (hasIcon(cfg.boneLeggingsHudStyle) ? 19 : 0) + padding;
    }

    public static int getHeight() {
        var cfg = ToolkitConfig.get();
        int height = cfg.boneLeggingsHudStyle == STYLE_EFFECT_ICONS ? 20
                : hasIcon(cfg.boneLeggingsHudStyle) ? 16 : 9;
        return height + (cfg.boneLeggingsBackground ? 4 : 0);
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
        boolean speed = isPreview || hasSpeed6(mc);
        boolean regen = !isPreview && hasRegen(mc);
        if (style == STYLE_EFFECT_ICONS) {
            renderEffectIcons(extractor, getItemStack(mc), x, y, speed, regen, cfg.boneLeggingsBackground);
            return;
        }
        String label = labelForEffects(style, speed, regen);
        ItemStack icon = hasIcon(style) ? getItemStack(mc) : null;
        renderItemStatus(extractor, mc != null ? mc.font : null, icon, label, x, y, hasIcon(style), cfg.boneLeggingsBackground);
    }

    /** Two sprite variants: speed trails and a regeneration heart. No title or status symbols. */
    public static void renderEffectIcons(GuiGraphicsExtractor g, ItemStack stack, int x, int y,
                                         boolean speed, boolean regen, boolean background) {
        if (g == null || stack == null || stack.isEmpty()) return;
        if (background) g.fill(x, y, x + 52, y + 24, 0x40000000);
        int dx = x + (background ? 3 : 0), dy = y + (background ? 2 : 0);
        renderEffectIcon(g, stack, dx, dy, speed, false);
        renderEffectIcon(g, stack, dx + 24, dy, regen, true);
    }

    private static void renderEffectIcon(GuiGraphicsExtractor g, ItemStack stack, int x, int y,
                                         boolean active, boolean heart) {
        g.item(stack, x + 2, y);
        int color = active ? (heart ? 0xFFF29AAF : 0xFF8CD5E7) : 0xFF858991;
        String[] pixels = heart
                ? new String[]{".##.##.", "#######", "#######", ".#####.", "..###..", "...#..."}
                : new String[]{"...####", "...####", ".####..", ".####..", "####...", "####..."};
        int bx = x + 13, by = y + 10;
        // One-pixel dark silhouette keeps the small badge readable over any server sprite.
        for (int row = 0; row < pixels.length; row++) for (int col = 0; col < pixels[row].length(); col++) {
            if (pixels[row].charAt(col) == '#') g.fill(bx + col - 1, by + row - 1, bx + col + 2, by + row + 2, 0xE0181A20);
        }
        for (int row = 0; row < pixels.length; row++) for (int col = 0; col < pixels[row].length(); col++) {
            if (pixels[row].charAt(col) == '#') g.fill(bx + col, by + row, bx + col + 1, by + row + 1, color);
        }
        g.fill(x + 3, y + 18, x + 17, y + 19, active ? color : 0x667C818A);
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
