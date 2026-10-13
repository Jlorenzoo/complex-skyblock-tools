package com.moneybooster;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;

import java.lang.reflect.Field;
import java.util.Map;

public class BoosterHudOverlay implements HudElement {
    public static final int BAR_WIDTH = 104;
    public static final int BAR_HEIGHT = 5;
    public static final float TEXT_SCALE = 0.75f;

    private static Field eventsField;

    static {
        try {
            eventsField = BossHealthOverlay.class.getDeclaredField("events");
            eventsField.setAccessible(true);
        } catch (Exception ignored) {
        }
    }

    public static int getBossBarCount(Minecraft mc) {
        if (mc.gui != null && mc.gui.hud != null && eventsField != null) {
            try {
                BossHealthOverlay overlay = mc.gui.hud.getBossOverlay();
                if (overlay != null) {
                    Map<?, ?> map = (Map<?, ?>) eventsField.get(overlay);
                    if (map != null) return map.size();
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    public static int getBarX(int screenWidth) {
        ModConfig config = ModConfig.getInstance();
        if (config.getPosition() == ModConfig.Position.TOP_LEFT) {
            return Math.max(2, Math.min(screenWidth - BAR_WIDTH - 2, 10 + config.getOffsetX()));
        } else {
            return Math.max(2, Math.min(screenWidth - BAR_WIDTH - 2, (screenWidth - BAR_WIDTH) / 2 + config.getOffsetX()));
        }
    }

    public static int getBarY(Minecraft mc) {
        ModConfig config = ModConfig.getInstance();
        if (config.getPosition() == ModConfig.Position.TOP_LEFT) {
            return clampY(mc, 16 + config.getOffsetY());
        } else {
            int bossCount = getBossBarCount(mc);
            if (bossCount == 0) {
                return clampY(mc, 20 + config.getOffsetY());
            } else {
                int baseTextY = 12 + (bossCount - 1) * 19 + 15;
                return clampY(mc, baseTextY + 7 + config.getOffsetY());
            }
        }
    }

    private static int clampY(Minecraft mc, int y) {
        return Math.max(14, Math.min(mc.getWindow().getGuiScaledHeight() - 24, y));
    }

    public static void renderBar(GuiGraphicsExtractor extractor, Minecraft mc, int barX, int barY, String label, float progress, boolean isLowTime, boolean flash) {
        Font font = mc.font;
        float scaledTextWidth = font.width(label) * TEXT_SCALE;
        float textX = barX + (BAR_WIDTH - scaledTextWidth) / 2.0f;
        float textY = barY - 8.0f;

        // 1. Text with MC font
        extractor.pose().pushMatrix();
        extractor.pose().translate(textX, textY);
        extractor.pose().scale(TEXT_SCALE, TEXT_SCALE);
        extractor.text(font, label, 0, 0, 0xFFFFFFFF, true);
        extractor.pose().popMatrix();

        // 2. Boss-bar styled progress bar (5px high)
        // Outer dark border (1px black outline)
        extractor.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xFF000000);

        // Empty track (deep dark green background)
        int emptyTrackColor = isLowTime ? 0xFF2A0808 : 0xFF052205;
        extractor.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, emptyTrackColor);

        // Filled portion
        int fillWidth = Math.max(0, Math.min(BAR_WIDTH, (int) (BAR_WIDTH * progress)));
        if (fillWidth > 0) {
            int mainColor;
            int highlightColor;
            int shadowColor;

            if (isLowTime) {
                if (flash) {
                    mainColor = 0xFFFFBB00;
                    highlightColor = 0xFFFFEE55;
                    shadowColor = 0xFFCC8800;
                } else {
                    mainColor = 0xFFEE2222;
                    highlightColor = 0xFFFF6666;
                    shadowColor = 0xFFAA0000;
                }
            } else {
                mainColor = 0xFF00E600;
                highlightColor = 0xFF38FF38;
                shadowColor = 0xFF009900;
            }

            // Core fill
            extractor.fill(barX, barY, barX + fillWidth, barY + BAR_HEIGHT, mainColor);
            // Top highlight line
            extractor.fill(barX, barY, barX + fillWidth, barY + 1, highlightColor);
            // Bottom shadow line
            extractor.fill(barX, barY + BAR_HEIGHT - 1, barX + fillWidth, barY + BAR_HEIGHT, shadowColor);
        }

        // Segment notches (dividers at 1/4, 2/4, 3/4)
        for (int i = 1; i <= 3; i++) {
            int notchX = barX + (BAR_WIDTH * i) / 4;
            extractor.fill(notchX, barY, notchX + 1, barY + BAR_HEIGHT, 0x80000000);
        }
    }

    public static void renderRateTestHud(GuiGraphicsExtractor extractor, Minecraft mc, int anchorX, int y) {
        String label;
        if (RateTracker.isPreparing()) {
            int count = RateTracker.getPrepCountdownSeconds();
            label = "§eStarting in §c§l" + count + "s...";
        } else if (RateTracker.isRunning()) {
            double rate = RateTracker.getCurrentRatePerHour();
            String rateStr = rate > 0 ? (" §7(" + BoosterStatsManager.formatMoney(rate) + "/h)") : "";

            if (RateTracker.getSelectedDuration().seconds > 0) {
                long rem = RateTracker.getRemainingSeconds();
                String timeStr = rem >= 60 ? String.format("%02d:%02d", rem / 60, rem % 60) : (rem + "s");
                label = "§eRate Test: §f" + timeStr + rateStr;
            } else {
                long el = RateTracker.getElapsedSeconds();
                String timeStr = String.format("%02d:%02d", el / 60, el % 60);
                label = "§eRate Test: §f" + timeStr + rateStr;
            }
        } else if (RateTracker.isCompleted()) {
            double finalRate = RateTracker.getFinalRatePerHour();
            label = (RateTracker.isSettling() ? "§eSettling: " : "§aTest Done: §a§l") + BoosterStatsManager.formatMoney(finalRate) + "/h";
        } else {
            return;
        }

        Font font = mc.font;
        float scaledTextW = font.width(label) * TEXT_SCALE;
        int pillW = (int) scaledTextW + 8;
        int pillH = 10;
        int pillX = anchorX + (BAR_WIDTH - pillW) / 2;

        // Subtle dark translucent pill background
        extractor.fill(pillX, y, pillX + pillW, y + pillH, 0x90000000);
        extractor.outline(pillX, y, pillW, pillH, 0x30FFFFFF);

        // Scaled text inside pill
        extractor.pose().pushMatrix();
        extractor.pose().translate(pillX + 4, y + 1.5f);
        extractor.pose().scale(TEXT_SCALE, TEXT_SCALE);
        extractor.text(font, label, 0, 0, 0xFFFFFFFF, true);
        extractor.pose().popMatrix();
    }

    public static int averageX(Minecraft mc,String label) { return Math.max(2,Math.min(mc.getWindow().getGuiScaledWidth()-mc.font.width(label)-2,ModConfig.getInstance().averageX())); }
    public static int averageY(Minecraft mc) { return Math.max(2,Math.min(mc.getWindow().getGuiScaledHeight()-11,ModConfig.getInstance().averageY())); }
    public static void renderAverage(GuiGraphicsExtractor g,Minecraft mc) {
        String label=BoosterStatsManager.averageLabel();
        g.text(mc.font,label,averageX(mc,label),averageY(mc),0xFFFFFFFF,true);
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        boolean boosterActive = com.skyblocktoolkit.ToolkitConfig.get().boosterHud && BoosterManager.isActive();
        boolean rateActive = RateTracker.shouldShowHud();

        if (!boosterActive && !rateActive && !ModConfig.getInstance().isAverageEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null && mc.gui.hud != null && mc.gui.hud.isHidden()) {
            return;
        }

        if (ModConfig.getInstance().isAverageEnabled()) renderAverage(extractor, mc);
        int barX = getBarX(extractor.guiWidth());
        int barY = getBarY(mc);

        // 1. Render Booster bar if active
        if (boosterActive) {
            long now = System.currentTimeMillis();
            long remainingMs = BoosterManager.getRemainingMs(now);
            float progress = BoosterManager.getProgress(now);

            String timeStr = BoosterManager.formatTime(remainingMs);
            boolean isLowTime = BoosterManager.hasTimer() && remainingMs < 3000;
            boolean flash = isLowTime && (((now / 200) % 2) == 0);

            String label;
            if (isLowTime) {
                label = flash ? ("§c+" + BoosterManager.getPercentage() + "% Booster: §e" + timeStr)
                              : ("§e+" + BoosterManager.getPercentage() + "% Booster: §c" + timeStr);
            } else {
                label = "§a+" + BoosterManager.getPercentage() + "% Booster: §f" + timeStr;
            }

            renderBar(extractor, mc, barX, barY, label, progress, isLowTime, flash);
        }

        // 2. Render Rate Test countdown indicator if running, preparing, or recently finished
        if (rateActive) {
            int rateY = boosterActive ? (barY + BAR_HEIGHT + 4) : barY;
            renderRateTestHud(extractor, mc, barX, rateY);

            // Center-screen 3-2-1 Countdown Banner during prep
            if (RateTracker.isPreparing()) {
                int count = RateTracker.getPrepCountdownSeconds();
                if (count > 0) {
                    String centerMsg = "§eStarting in §c§l" + count + "...";
                    float scale = 1.6f;
                    float textW = mc.font.width(centerMsg) * scale;
                    float cx = (extractor.guiWidth() - textW) / 2.0f;
                    float cy = extractor.guiHeight() / 2.0f - 30.0f;

                    extractor.pose().pushMatrix();
                    extractor.pose().translate(cx, cy);
                    extractor.pose().scale(scale, scale);
                    extractor.text(mc.font, centerMsg, 0, 0, 0xFFFFFFFF, true);
                    extractor.pose().popMatrix();
                }
            }
        }
    }
}
