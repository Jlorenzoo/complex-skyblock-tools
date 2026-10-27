package com.skyblocktoolkit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BlackoutManager {
    private static boolean active = false;
    private static boolean pendingPress;
    private static boolean adjusted;

    /** Resolve taps on release so an opacity gesture never toggles the overlay off. */
    public static void updateKey(boolean down, boolean clicked, boolean gameplay) {
        if (!gameplay) {
            finishGesture();
            return;
        }
        if (clicked || down) pendingPress = true;
        if (!down && pendingPress) {
            if (!adjusted) toggle();
            finishGesture();
        }
    }

    public static boolean adjustOpacity(double scroll) {
        if (!Double.isFinite(scroll) || scroll == 0) return false;
        pendingPress = true;
        adjusted = true;
        active = true;
        var cfg = ToolkitConfig.get();
        cfg.blackoutOpacity = Math.clamp(Math.round((cfg.blackoutOpacity + scroll * .05) * 1000) / 1000.0, 0.0, 1.0);
        return true;
    }

    private static void finishGesture() {
        if (adjusted) ToolkitConfig.get().save();
        pendingPress = false;
        adjusted = false;
    }

    private BlackoutManager() {}

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean state) {
        active = state;
    }

    public static void toggle() {
        setActive(!active);
    }

    public static void render(GuiGraphicsExtractor g, int width, int height) {
        int alpha = (int) Math.round(ToolkitConfig.get().blackoutOpacity * 255);
        if (alpha > 0) g.fill(0, 0, width, height, alpha << 24);
        if (ToolkitConfig.get().blackoutHint) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.font != null) {
                String keyName = ToolkitMod.blackoutKey != null ? ToolkitMod.blackoutKey.getTranslatedKeyMessage().getString() : "F6";
                String msg = "Screen blacked out · Press " + keyName + " to close";
                int textW = mc.font.width(msg);
                g.text(mc.font, msg, (width - textW) / 2, height - 24, 0x50FFFFFF, false);
            }
        }
    }
}
