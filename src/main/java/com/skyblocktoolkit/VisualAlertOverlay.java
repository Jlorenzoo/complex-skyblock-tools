package com.skyblocktoolkit;

import com.moneybooster.BoosterManager;
import com.moneybooster.BoosterTimers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

/**
 * Prominent on-screen title banner and screen flash/vignette notifications for
 * qualifying coconut hits and expiring booster timers.
 */
public final class VisualAlertOverlay {
    private static String title = "";
    private static String subtitle = "";
    private static int accentColor = 0;
    private static long startTime = 0;
    public static final long DURATION_MS = 2000;
    public static final long FADE_DURATION_MS = 600;

    private static boolean alertedMoney = false;
    private static boolean alertedXp = false;

    private VisualAlertOverlay() {}

    public static void triggerCoconut(int percentage, int threshold) {
        title = "§a§l+" + percentage + "% MONEY BOOSTER!";
        subtitle = "§f§lRELEASE RIGHT-CLICK §7· Threshold met (≥" + threshold + "%)";
        accentColor = 0x2ECC71; // Emerald green
        startTime = System.currentTimeMillis();
    }

    public static void triggerExpiring(BoosterTimers.Type type, int secondsRemaining) {
        String name = type == BoosterTimers.Type.MONEY ? "MONEY" : "XP";
        title = "§c§l" + name + " BOOSTER EXPIRING!";
        subtitle = "§e" + secondsRemaining + "s remaining §7· Prepare another booster!";
        accentColor = 0xE5254A; // Brand Crimson Red
        startTime = System.currentTimeMillis();
    }

    public static void previewCoconutAlert() {
        int thresh = 0;
        try {
            thresh = ToolkitConfig.get().coconutSoundThreshold;
        } catch (Throwable ignored) {}
        triggerCoconut(Math.max(thresh, 50), thresh);
    }

    public static void previewExpiringAlert() {
        triggerExpiring(BoosterTimers.Type.MONEY, 5);
    }

    public static boolean isActive() {
        return isActive(System.currentTimeMillis());
    }

    public static boolean isActive(long now) {
        return startTime > 0 && now - startTime < DURATION_MS;
    }

    public static void reset() {
        startTime = 0;
        alertedMoney = false;
        alertedXp = false;
    }

    public static void tick(long now) {
        boolean enabled = true;
        try {
            enabled = ToolkitConfig.get().boosterExpiringAlert;
        } catch (Throwable ignored) {}
        tick(now, enabled);
    }

    public static void tick(long now, boolean expiringAlertEnabled) {
        if (!expiringAlertEnabled) {
            alertedMoney = false;
            alertedXp = false;
            return;
        }
        checkTimerExpiring(BoosterTimers.Type.MONEY, now);
        checkTimerExpiring(BoosterTimers.Type.XP, now);
    }

    private static void checkTimerExpiring(BoosterTimers.Type type, long now) {
        var clock = BoosterManager.timers.clock(type);
        boolean active = clock.active(now) && clock.hasTimer(now);
        long remaining = clock.remaining(now);
        boolean inWarningWindow = active && remaining <= 5000 && remaining > 0;
        boolean alreadyAlerted = (type == BoosterTimers.Type.MONEY) ? alertedMoney : alertedXp;

        if (inWarningWindow && !alreadyAlerted) {
            if (type == BoosterTimers.Type.MONEY) alertedMoney = true;
            else alertedXp = true;
            int seconds = (int) Math.ceil(remaining / 1000.0);
            triggerExpiring(type, seconds);
        } else if (!active || remaining > 5500) {
            if (type == BoosterTimers.Type.MONEY) alertedMoney = false;
            else alertedXp = false;
        }
    }

    public static void render(GuiGraphicsExtractor g, int width, int height) {
        long now = System.currentTimeMillis();
        if (!isActive(now)) return;
        long elapsed = now - startTime;
        if (elapsed >= DURATION_MS) return;

        float alpha;
        if (elapsed < 100) {
            alpha = elapsed / 100.0f;
        } else if (elapsed < (DURATION_MS - FADE_DURATION_MS)) {
            alpha = 1.0f;
        } else {
            alpha = 1.0f - (float) (elapsed - (DURATION_MS - FADE_DURATION_MS)) / FADE_DURATION_MS;
        }
        alpha = Math.clamp(alpha, 0.0f, 1.0f);

        int color = accentColor & 0xFFFFFF;

        // 1. Full-screen soft tint wash
        int tintAlpha = (int) (alpha * 36);
        if (tintAlpha > 0) {
            g.fill(0, 0, width, height, (tintAlpha << 24) | color);
        }

        // 2. Screen border vignette glow
        drawVignette(g, width, height, color, alpha);

        // 3. Center-top prominent banner card
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;

        float titleScale = 1.4f;
        float titleW = mc.font.width(title) * titleScale;
        float subW = mc.font.width(subtitle);
        int cardW = (int) Math.max(titleW, subW) + 32;
        int cardH = 42;
        int cardX = (width - cardW) / 2;
        int cardY = Math.max(16, height / 4 - 20);

        int bgAlpha = (int) (alpha * 220);
        int bgColor = (bgAlpha << 24) | 0x111317;
        ToolkitTheme.rounded(g, cardX, cardY, cardW, cardH, 5, bgColor);

        int borderAlpha = (int) (alpha * 200);
        int borderCol = (borderAlpha << 24) | color;
        g.outline(cardX, cardY, cardW, cardH, borderCol);
        g.fill(cardX + 4, cardY + 1, cardX + cardW - 4, cardY + 3, borderCol);

        // Title text
        float tx = cardX + (cardW - titleW) / 2.0f;
        float ty = cardY + 7;
        g.pose().pushMatrix();
        g.pose().translate(tx, ty);
        g.pose().scale(titleScale, titleScale);
        g.text(mc.font, title, 0, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();

        // Subtitle text
        float sx = cardX + (cardW - subW) / 2.0f;
        float sy = cardY + 25;
        g.text(mc.font, subtitle, (int) sx, (int) sy, 0xFFFFFFFF, true);
    }

    private static void drawVignette(GuiGraphicsExtractor g, int width, int height, int color, float alpha) {
        int[] depths = {4, 10, 18, 28};
        float[] opacities = {0.45f, 0.30f, 0.16f, 0.07f};
        int lastInset = 0;
        for (int i = 0; i < depths.length; i++) {
            int inset = depths[i];
            int bandAlpha = (int) (alpha * opacities[i] * 255);
            if (bandAlpha <= 0) continue;
            int col = (bandAlpha << 24) | color;
            g.fill(lastInset, lastInset, width - lastInset, inset, col);
            g.fill(lastInset, height - inset, width - lastInset, height - lastInset, col);
            g.fill(lastInset, inset, inset, height - inset, col);
            g.fill(width - inset, inset, width - lastInset, height - inset, col);
            lastInset = inset;
        }
    }

    // Accessors for testing
    public static String getTitle() { return title; }
    public static String getSubtitle() { return subtitle; }
    public static int getAccentColor() { return accentColor; }
    public static boolean isAlertedMoney() { return alertedMoney; }
    public static boolean isAlertedXp() { return alertedXp; }
}
