package com.moneybooster;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.world.item.Items;
import com.skyblocktoolkit.ToolkitConfig;
import com.skyblocktoolkit.ToolkitTheme;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import com.skyblocktoolkit.mixin.BossHealthOverlayAccessor;

public class BoosterHudOverlay implements HudElement {
    public static final int BAR_WIDTH = 104;
    public static final int BAR_HEIGHT = 5;
    public static final float TEXT_SCALE = 0.75f;
    public static final int TIMER_SPACING = 24;

    public static int getBossBarCount(Minecraft mc) {
        if (mc.gui != null && mc.gui.hud != null) {
            BossHealthOverlay overlay = mc.gui.hud.getBossOverlay();
            if (overlay != null) return ((BossHealthOverlayAccessor) overlay).toolkit$getEvents().size();
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
        return getBarY(mc, 2);
    }
    public static int getBarY(Minecraft mc, int rows) {
        ModConfig config = ModConfig.getInstance();
        if (config.getPosition() == ModConfig.Position.TOP_LEFT) {
            return clampY(mc, 16 + config.getOffsetY(), rows);
        } else {
            int bossCount = getBossBarCount(mc);
            if (bossCount == 0) {
                return clampY(mc, 20 + config.getOffsetY(), rows);
            } else {
                int baseTextY = 12 + (bossCount - 1) * 19 + 15;
                return clampY(mc, baseTextY + 7 + config.getOffsetY(), rows);
            }
        }
    }

    private static int clampY(Minecraft mc, int y, int rows) {
        return Math.max(14, Math.min(mc.getWindow().getGuiScaledHeight() - 24 - Math.max(0, rows - 1) * TIMER_SPACING, y));
    }

    public static void renderBar(GuiGraphicsExtractor extractor, Minecraft mc, int barX, int barY, String label, float progress, boolean isLowTime, boolean flash) {
        renderTimerBar(extractor, mc, barX, barY, label, progress, BoosterTimers.Type.MONEY, isLowTime);
    }
    public static void renderTimerBar(GuiGraphicsExtractor extractor, Minecraft mc, int barX, int barY, String label, float progress, BoosterTimers.Type type, boolean reminder) {
        Font font = mc.font;
        boolean money = type == BoosterTimers.Type.MONEY;
        int color = money ? ToolkitTheme.MONEY_TIMER : ToolkitTheme.XP_TIMER;
        float scaledTextWidth = font.width(label) * TEXT_SCALE;
        float textX = barX + (BAR_WIDTH - scaledTextWidth) / 2.0f;
        float textY = barY - 8.0f;

        // 1. Text with MC font
        extractor.pose().pushMatrix();
        extractor.pose().translate(textX, textY);
        extractor.pose().scale(TEXT_SCALE, TEXT_SCALE);
        extractor.text(font, label, 0, 0, color, true);
        extractor.pose().popMatrix();

        // 2. Boss-bar styled progress bar (5px high)
        // Outer dark border (1px black outline)
        extractor.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xFF000000);

        // Empty track (deep dark green background)
        int emptyTrackColor = money ? ToolkitTheme.MONEY_TRACK : ToolkitTheme.XP_TRACK;
        extractor.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, emptyTrackColor);

        // Filled portion
        int fillWidth = Math.max(0, Math.min(BAR_WIDTH, (int) (BAR_WIDTH * progress)));
        if (fillWidth > 0) {
            int mainColor = color;
            int highlightColor = money ? ToolkitTheme.MONEY_HIGHLIGHT : ToolkitTheme.XP_HIGHLIGHT;
            int shadowColor = money ? ToolkitTheme.MONEY_SHADOW : ToolkitTheme.XP_SHADOW;

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
        if (reminder) {
            // A slow, low-opacity outline; no sound, full-screen flash or chat spam.
            int alpha = 28 + (int)(16 * (1 + Math.sin(System.currentTimeMillis() / 500.0)));
            extractor.outline(barX - 2, barY - 2, BAR_WIDTH + 4, BAR_HEIGHT + 4, (alpha << 24) | (color & 0xFFFFFF));
        }
    }

    public static void renderRateTestHud(GuiGraphicsExtractor extractor, Minecraft mc, int anchorX, int y) {
        String label;
        if (RateTracker.isPreparing()) {
            int count = RateTracker.getPrepCountdownSeconds();
            label = "§eStarting in §c§l" + count + "s...";
        } else if (RateTracker.isRunning()) {
            String rateStr = " §7(" + RateTracker.formatDisplayedRate() + "/h)";

            if (RateTracker.getSelectedDuration().seconds > 0) {
                long rem = RateTracker.getRemainingSeconds();
                String timeStr = rem >= 60 ? String.format("%02d:%02d", rem / 60, rem % 60) : (rem + "s");
                label = (RateTracker.isXpSelected() ? "§eXP Test: §f" : "§eRate Test: §f") + timeStr + rateStr;
            } else {
                long el = RateTracker.getElapsedSeconds();
                String timeStr = String.format("%02d:%02d", el / 60, el % 60);
                label = (RateTracker.isXpSelected() ? "§eXP Test: §f" : "§eRate Test: §f") + timeStr + rateStr;
            }
        } else if (RateTracker.isCompleted()) {
            label = (RateTracker.isSettling() && !RateTracker.isXpSelected() ? "§eSettling: " : "§aTest Done: §a§l") + RateTracker.formatDisplayedRate() + "/h";
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

    private static final Identifier SHARD_ICON = Identifier.withDefaultNamespace("textures/item/gold_nugget.png");
    private static final Identifier KEY_ICON = Identifier.withDefaultNamespace("textures/block/tripwire_hook.png");
    private static final int EARNINGS_ICON_STEP = 10;

    public static int averageWidth(Minecraft mc, String label) {
        var config = ToolkitConfig.get();
        return mc.font.width(label) + (config.includeGoldShards ? EARNINGS_ICON_STEP : 0)
                + (config.includeVoteKeys ? EARNINGS_ICON_STEP : 0);
    }

    public static int averageX(Minecraft mc,String label) { return Math.max(2,Math.min(mc.getWindow().getGuiScaledWidth()-averageWidth(mc,label)-2,ModConfig.getInstance().averageX())); }
    public static int averageY(Minecraft mc) { return Math.max(2,Math.min(mc.getWindow().getGuiScaledHeight()-11,ModConfig.getInstance().averageY())); }
    public static int fishX(Minecraft mc,String label){return Math.max(2,Math.min(mc.getWindow().getGuiScaledWidth()-mc.font.width(label)-2,com.skyblocktoolkit.ToolkitConfig.get().fishHudX));}
    public static int fishY(Minecraft mc){return Math.max(2,Math.min(mc.getWindow().getGuiScaledHeight()-11,com.skyblocktoolkit.ToolkitConfig.get().fishHudY));}
    public static int xpX(Minecraft mc,String label){return Math.max(2,Math.min(mc.getWindow().getGuiScaledWidth()-mc.font.width(label)-2,com.skyblocktoolkit.ToolkitConfig.get().xpHudX));}
    public static int xpY(Minecraft mc){return Math.max(2,Math.min(mc.getWindow().getGuiScaledHeight()-11,com.skyblocktoolkit.ToolkitConfig.get().xpHudY));}
    public static int cropHarvestX(Minecraft mc,String label){return Math.max(2,Math.min(mc.getWindow().getGuiScaledWidth()-mc.font.width(label)-2,com.skyblocktoolkit.ToolkitConfig.get().cropHarvestHudX));}
    public static int cropHarvestY(Minecraft mc){return Math.max(2,Math.min(mc.getWindow().getGuiScaledHeight()-11,com.skyblocktoolkit.ToolkitConfig.get().cropHarvestHudY));}
    public static int boneLeggingsX(Minecraft mc,String label){
        int w=mc.getWindow().getGuiScaledWidth();int configured=com.skyblocktoolkit.ToolkitConfig.get().boneLeggingsHudX;
        int pillW=com.skyblocktoolkit.BoneLeggingsTracker.getWidth(mc);
        if(configured<0)return Math.max(2,w-pillW-8);
        return Math.max(2,Math.min(w-pillW-2,configured));
    }
    public static int boneLeggingsY(Minecraft mc){return Math.max(2,Math.min(mc.getWindow().getGuiScaledHeight()-com.skyblocktoolkit.BoneLeggingsTracker.getHeight()-2,com.skyblocktoolkit.ToolkitConfig.get().boneLeggingsHudY));}
    public static void renderAverage(GuiGraphicsExtractor g,Minecraft mc) {
        String label=BoosterStatsManager.averageLabel();
        renderAverage(g, mc, label, averageX(mc, label), averageY(mc));
    }

    public static void renderAverage(GuiGraphicsExtractor g, Minecraft mc, String label, int x, int y) {
        g.text(mc.font, label, x, y, 0xFFFFFFFF, true);
        var config = ToolkitConfig.get();
        int iconX = x + mc.font.width(label) + 2;
        if (config.includeGoldShards) {
            renderEarningsIcon(g, SHARD_ICON, iconX, y);
            iconX += EARNINGS_ICON_STEP;
        }
        if (config.includeVoteKeys) renderEarningsIcon(g, KEY_ICON, iconX, y);
    }

    private static void renderEarningsIcon(GuiGraphicsExtractor g, Identifier texture, int x, int y) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.5f, 0.5f);
        g.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0f, 0f, 16, 16, 16, 16, 16, 16, 0xA0D6DAE2);
        g.pose().popMatrix();
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        var toolkit=com.skyblocktoolkit.ToolkitConfig.get();
        long now = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.gui != null && mc.gui.hud != null && mc.gui.hud.isHidden()) {
            return;
        }

        var timerRows = BoosterManager.timers.visible(now, toolkit.boosterHud, toolkit.xpBoosterHud);
        boolean rateActive = toolkit.rateTestHud && RateTracker.shouldShowHud();

        var fishing = com.skyblocktoolkit.ToolkitMod.FISHING;
        boolean oni = fishing.holdingOniRod(mc);
        var fishSettings = fishing.settings();
        boolean fishActive = toolkit.fishEarningsHud && (oni ? fishSettings.hasOniPrices() : fishSettings.hasPrices());
        boolean boneActive = toolkit.boneLeggingsHud && com.skyblocktoolkit.BoneLeggingsTracker.shouldShow(mc);
        if (timerRows.isEmpty() && !rateActive && !toolkit.cropEarningsHud && !fishActive && !toolkit.xpEarningsHud && !toolkit.cropHarvestHud && !boneActive) {
            return;
        }
        if (boneActive) com.skyblocktoolkit.BoneLeggingsTracker.render(extractor, mc);
        if (toolkit.xpEarningsHud && XpTracker.shouldShowHud()) { String label=XpTracker.averageLabel();extractor.text(mc.font,label,xpX(mc,label),xpY(mc),0xFFFFFFFF,true); }
        if (toolkit.cropHarvestHud && com.skyblocktoolkit.CropHarvestTracker.shouldShowHud()) { String label=com.skyblocktoolkit.CropHarvestTracker.averageLabel();extractor.text(mc.font,label,cropHarvestX(mc,label),cropHarvestY(mc),0xFFFFFFFF,true); }

        boolean holdingRod = mc.player.getMainHandItem().is(Items.FISHING_ROD)
                || mc.player.getOffhandItem().is(Items.FISHING_ROD);
        var earnings = BoosterStatsManager.earningsActivity.select(now,
                toolkit.cropEarningsHud, fishActive,
                mc.player.fishing != null, holdingRod,
                oni ? fishing.recentOniActivity(now) : com.skyblocktoolkit.ToolkitMod.FISHING_EARNINGS.recent(now, 5));
        if(earnings == EarningsActivity.Tracker.CROPS)renderAverage(extractor,mc);
        if(earnings == EarningsActivity.Tracker.FISHING && fishActive){
            String label=oni?fishing.oniLabel(now):com.skyblocktoolkit.ToolkitMod.fishingLabel();
            if(!label.isEmpty())extractor.text(mc.font,label,fishX(mc,label),fishY(mc),0xFFFFFFFF,true);
        }
        int barX = getBarX(extractor.guiWidth());
        int barY = getBarY(mc, timerRows.size());

        // Pack active, enabled timers into consecutive rows: money first, then XP.
        for (int row = 0; row < timerRows.size(); row++) {
            var type = timerRows.get(row);
            var clock = BoosterManager.timers.clock(type);
            boolean reminder = BoosterManager.timers.reminding(type, now, toolkit.boosterReminder);
            String label = type.label + " +" + BoosterManager.timers.percentage(type) + "%: §f"
                + BoosterTimers.formatTime(clock.remaining(now), clock.hasTimer(now)) + (reminder ? " §7· ending" : "");
            renderTimerBar(extractor, mc, barX, barY + row * TIMER_SPACING, label, clock.progress(now), type, reminder);
        }

        // 2. Render Rate Test countdown indicator if running, preparing, or recently finished
        if (rateActive) {
            int rateY = timerRows.isEmpty() ? barY : barY + (timerRows.size() - 1) * TIMER_SPACING + BAR_HEIGHT + 4;
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
