package com.skyblocktoolkit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Locale;

/** Tracks Bone Leggings / Burning Rush status and renders an item HUD overlay. */
public final class BoneLeggingsTracker {
    public static final int STYLE_EFFECT_ICONS = 0;
    public static final int STYLE_ICON_STATUS = 1;
    public static final int STYLE_BADGE_ONLY = 2;
    public static final int STYLE_STATUS_ONLY = 3;

    private static final int MODE_BADGE_SIZE = 18;
    private static final int MODE_BADGE_X = 10, MODE_BADGE_Y = 8;
    private static final int EFFECT_ICON_SIZE = 16;

    private static boolean lastSpeed6 = false;
    private static ItemStack lastServerStack = null;

    public static String styleName(int style) {
        return switch (style) {
            case STYLE_ICON_STATUS -> "Icon & Mode";
            case STYLE_BADGE_ONLY -> "Effect badge only";
            case STYLE_STATUS_ONLY -> "Mode only";
            default -> "Leggings & effect badge";
        };
    }

    public static boolean hasIcon(int style) {
        return style != STYLE_STATUS_ONLY;
    }

    public static String effectLabel(boolean speed) {
        return speed ? "§bSpeed" : "§dRegen";
    }

    public static String formatLabel(int style, boolean speed) {
        return switch (style) {
            case STYLE_ICON_STATUS, STYLE_STATUS_ONLY -> effectLabel(speed);
            default -> "";
        };
    }

    public static String previewLabel() {
        return formatLabel(ToolkitConfig.get().boneLeggingsHudStyle, true);
    }

    public static boolean isBoneLeggings(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (matchesItemText(stack.getHoverName().getString())) return true;
        var lore = stack.get(DataComponents.LORE);
        if (lore != null) for (var line : lore.lines()) {
            if (matchesItemText(line.getString())) return true;
        }
        return false;
    }

    static boolean matchesItemText(String text) {
        String name = text.replaceAll("§.", "").toLowerCase(Locale.ROOT);
        return name.contains("burning rush") || (name.contains("bone") &&
                (name.contains("legging") || name.contains("pant") || name.contains("armor")));
    }

    static void rememberServerStack(ItemStack stack) {
        if (isBoneLeggings(stack) && (lastServerStack == null ||
                !ItemStack.isSameItemSameComponents(stack, lastServerStack))) {
            // Copy the complete component set, including model, dye and glint; never rebuild a generic item.
            lastServerStack = stack.copyWithCount(1);
        }
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

    public static boolean shouldShow(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        var cfg = ToolkitConfig.get();
        if (cfg.boneLeggingsAlwaysVisible) return true;
        return hasSpeed6(mc) || isEquippedOrHeld(mc);
    }

    public static String currentLabel(Minecraft mc) {
        var cfg = ToolkitConfig.get();
        return formatLabel(cfg.boneLeggingsHudStyle, hasSpeed6(mc));
    }

    public static ItemStack getItemStack(Minecraft mc) {
        if (mc != null && mc.player != null) {
            try {
                ItemStack legs = mc.player.getItemBySlot(EquipmentSlot.LEGS);
                if (isBoneLeggings(legs)) {
                    rememberServerStack(legs);
                    return legs;
                }
                ItemStack main = mc.player.getMainHandItem();
                if (isBoneLeggings(main)) {
                    rememberServerStack(main);
                    return main;
                }
                ItemStack off = mc.player.getOffhandItem();
                if (isBoneLeggings(off)) {
                    rememberServerStack(off);
                    return off;
                }
                for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = mc.player.getInventory().getItem(i);
                    if (isBoneLeggings(stack)) {
                        rememberServerStack(stack);
                        return stack;
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (lastServerStack != null) {
            return lastServerStack;
        }
        try {
            return new ItemStack(Items.CHAINMAIL_LEGGINGS);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static int getWidth(Minecraft mc) {
        var cfg = ToolkitConfig.get();
        int padding = cfg.boneLeggingsBackground ? 6 : 0;
        if (cfg.boneLeggingsHudStyle == STYLE_EFFECT_ICONS) return MODE_BADGE_X + MODE_BADGE_SIZE + padding;
        if (cfg.boneLeggingsHudStyle == STYLE_BADGE_ONLY) return MODE_BADGE_SIZE + padding;
        // Reserve the widest state so right alignment and HUD editor bounds never jump.
        String label = "Speed";
        int textW = mc != null && mc.font != null ? mc.font.width(label) : label.length() * 6;
        return textW + (hasIcon(cfg.boneLeggingsHudStyle) ? 19 : 0) + padding;
    }

    public static int getHeight() {
        var cfg = ToolkitConfig.get();
        int height = cfg.boneLeggingsHudStyle == STYLE_EFFECT_ICONS ? MODE_BADGE_Y + MODE_BADGE_SIZE
                : cfg.boneLeggingsHudStyle == STYLE_BADGE_ONLY ? MODE_BADGE_SIZE
                : hasIcon(cfg.boneLeggingsHudStyle) ? 16 : 9;
        return height + (cfg.boneLeggingsBackground ? 4 : 0);
    }

    public static void tick(Minecraft mc) {
        if (mc == null || mc.player == null) return;
        getItemStack(mc); // Capture the server appearance even while the HUD is hidden.
        boolean speed6 = hasSpeed6(mc);
        if (speed6 && !lastSpeed6) {
            var cfg = ToolkitConfig.get();
            if (cfg.boneLeggingsSpeedAlert && (isEquippedOrHeld(mc) || cfg.boneLeggingsAlwaysVisible)) {
                ToolkitAudio.playSpeed(mc);
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
        if (style == STYLE_EFFECT_ICONS) {
            renderEffectIcon(extractor, getItemStack(mc), x, y, speed, cfg.boneLeggingsBackground);
            return;
        }
        if (style == STYLE_BADGE_ONLY) {
            int padding = cfg.boneLeggingsBackground ? 3 : 0;
            if (cfg.boneLeggingsBackground) extractor.fill(x, y, x + MODE_BADGE_SIZE + 6, y + MODE_BADGE_SIZE + 4, 0x40000000);
            renderModeBadge(extractor, x + padding, y + (padding == 0 ? 0 : 2), speed);
            return;
        }
        String label = formatLabel(style, speed);
        ItemStack icon = hasIcon(style) ? getItemStack(mc) : null;
        renderItemStatus(extractor, mc != null ? mc.font : null, icon, label, x, y, hasIcon(style), cfg.boneLeggingsBackground);
    }

    /** One real leggings sprite with either the speed boot or regeneration heart. */
    public static void renderEffectIcon(GuiGraphicsExtractor g, ItemStack stack, int x, int y,
                                        boolean speed, boolean background) {
        if (g == null || stack == null || stack.isEmpty()) return;
        if (background) g.fill(x, y, x + MODE_BADGE_X + MODE_BADGE_SIZE + 6, y + MODE_BADGE_Y + MODE_BADGE_SIZE + 4, 0x40000000);
        int dx = x + (background ? 3 : 0), dy = y + (background ? 2 : 0);
        g.item(stack, dx, dy);
        renderModeBadge(g, dx + MODE_BADGE_X, dy + MODE_BADGE_Y, speed);
    }

    private static final Identifier SPEED_EFFECT_ICON = Identifier.withDefaultNamespace("textures/mob_effect/speed.png");
    private static final Identifier REGEN_EFFECT_ICON = Identifier.withDefaultNamespace("textures/mob_effect/regeneration.png");

    private static void renderModeBadge(GuiGraphicsExtractor g, int x, int y, boolean speed) {
        // Only the corner badge gets a contrast backing. The native sprite keeps its original colors.
        roundedBadge(g, x, y, MODE_BADGE_SIZE, speed ? 0xC385BDCD : 0xC3DEA3B4);
        roundedBadge(g, x + 1, y + 1, MODE_BADGE_SIZE - 2, 0xC529343F);
        g.blit(RenderPipelines.GUI_TEXTURED, speed ? SPEED_EFFECT_ICON : REGEN_EFFECT_ICON,
                x + 1, y + 1, 0f, 0f, EFFECT_ICON_SIZE, EFFECT_ICON_SIZE, 18, 18, 18, 18);
    }

    private static void roundedBadge(GuiGraphicsExtractor g, int x, int y, int size, int color) {
        // Non-overlapping rows preserve the intended translucency.
        g.fill(x + 3, y, x + size - 3, y + 1, color);
        g.fill(x + 1, y + 1, x + size - 1, y + 2, color);
        g.fill(x, y + 2, x + size, y + size - 2, color);
        g.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, color);
        g.fill(x + 3, y + size - 1, x + size - 3, y + size, color);
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
