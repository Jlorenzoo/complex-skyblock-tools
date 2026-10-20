package com.skyblocktoolkit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BlackoutManager {
    private static boolean active = false;

    private BlackoutManager() {}

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean state) {
        if(active==state)return;
        active = state;
        Minecraft mc=Minecraft.getInstance();
        if(state)mc.setScreenAndShow(new BlackoutScreen(mc.gui.screen()));
        else if(mc.gui.screen() instanceof BlackoutScreen screen)mc.setScreenAndShow(screen.previous);
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
                String msg = "Screen blacked out · Press " + keyName + " to wake";
                int textW = mc.font.width(msg);
                g.text(mc.font, msg, (width - textW) / 2, height - 24, 0x50FFFFFF, false);
            }
        }
    }
}
