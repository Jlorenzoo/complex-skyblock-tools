package com.skyblocktoolkit;

import com.moneybooster.BoosterManager;
import com.moneybooster.BoosterTimers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Prominent on-screen title banner and subtle border flash notifications for
 * qualifying coconut hits, general boosters, and expiring booster timers.
 */
public final class VisualAlertOverlay {
    public enum AlertKind { COCONUT, MONEY, XP, EXPIRING }

    private static AlertKind currentKind = AlertKind.COCONUT;
    private static String title = "";
    private static String subtitle = "";
    private static int accentColor = 0;
    private static long startTime = 0;
    private static boolean isPreview = false;
    public static final long DURATION_MS = 1800;
    public static final long FADE_DURATION_MS = 500;

    private static boolean alertedMoney = false;
    private static boolean alertedXp = false;

    private VisualAlertOverlay() {}

    public static void triggerCoconut(int percentage, int threshold) {
        currentKind = AlertKind.COCONUT;
        isPreview = false;
        title = "§a§l+" + percentage + "% MONEY BOOSTER!";
        subtitle = "§f§lRELEASE RIGHT-CLICK §7· Threshold met (≥" + threshold + "%)";
        accentColor = 0x2ECC71; // Emerald green
        startTime = System.currentTimeMillis();
    }

    public static void triggerMoneyBooster(int percentage, String sourceName, long seconds) {
        currentKind = AlertKind.MONEY;
        isPreview = false;
        title = "§a§l+" + percentage + "% MONEY BOOSTER!";
        subtitle = "§f" + sourceName + " §7· " + (seconds > 0 ? (seconds + "s duration") : "Active");
        accentColor = 0x2ECC71; // Emerald green
        startTime = System.currentTimeMillis();
    }

    public static void triggerXpBooster(int percentage, String sourceName, long seconds) {
        currentKind = AlertKind.XP;
        isPreview = false;
        title = "§b§l+" + percentage + "% XP BOOSTER!";
        subtitle = "§f" + sourceName + " §7· " + (seconds > 0 ? (seconds + "s duration") : "Active");
        accentColor = 0x9D9AEB; // Amethyst purple
        startTime = System.currentTimeMillis();
    }

    private static BoosterTimers.Type currentExpiringType = BoosterTimers.Type.MONEY;

    public static void triggerExpiring(BoosterTimers.Type type, int secondsRemaining) {
        currentKind = AlertKind.EXPIRING;
        currentExpiringType = type;
        isPreview = false;
        String name = type == BoosterTimers.Type.MONEY ? "MONEY" : "XP";
        title = "§c§l" + name + " BOOSTER ENDING!";
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
        isPreview = true;
    }

    public static void previewExpiringAlert() {
        triggerExpiring(BoosterTimers.Type.MONEY, 5);
        isPreview = true;
    }

    public static void previewXpAlert() {
        triggerXpBooster(100, "Experience booster", 300);
        isPreview = true;
    }

    public static boolean isActive() {
        return isActive(System.currentTimeMillis());
    }

    public static boolean isActive(long now) {
        return startTime > 0 && now - startTime < DURATION_MS;
    }

    public static void reset() {
        startTime = 0;
        isPreview = false;
        alertedMoney = false;
        alertedXp = false;
    }

    public static void tick(long now) {
        boolean expiringAlertEnabled = false;
        boolean moneyEnabled = true;
        boolean xpEnabled = false;
        try {
            var cfg = ToolkitConfig.get();
            expiringAlertEnabled = cfg.boosterExpiringAlert || cfg.expiringFlash;
            moneyEnabled = cfg.boosterHud || cfg.moneyVisualAlert;
            xpEnabled = cfg.xpVisualAlert && cfg.xpBoosterHud;
        } catch (Throwable ignored) {}
        tick(now, expiringAlertEnabled, moneyEnabled, xpEnabled);
    }

    public static void tick(long now, boolean expiringAlertEnabled) {
        tick(now, expiringAlertEnabled, true, true);
    }

    public static void tick(long now, boolean expiringAlertEnabled, boolean xpEnabled) {
        tick(now, expiringAlertEnabled, true, xpEnabled);
    }

    public static void tick(long now, boolean expiringAlertEnabled, boolean moneyEnabled, boolean xpEnabled) {
        if (!expiringAlertEnabled) {
            alertedMoney = false;
            alertedXp = false;
            return;
        }
        if (moneyEnabled) checkTimerExpiring(BoosterTimers.Type.MONEY, now);
        else alertedMoney = false;

        if (xpEnabled) checkTimerExpiring(BoosterTimers.Type.XP, now);
        else alertedXp = false;
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

        boolean showBanner = true;
        boolean showFlash = false;
        try {
            var cfg = ToolkitConfig.get();
            if (isPreview) {
                showBanner = true;
                showFlash = (currentKind == AlertKind.COCONUT) ? cfg.coconutFlash : (currentKind == AlertKind.EXPIRING ? cfg.expiringFlash : true);
            } else if (currentKind == AlertKind.COCONUT) {
                showBanner = cfg.coconutVisualAlert;
                showFlash = cfg.coconutFlash;
            } else if (currentKind == AlertKind.EXPIRING) {
                if (currentExpiringType == BoosterTimers.Type.XP && (!cfg.xpVisualAlert || !cfg.xpBoosterHud)) {
                    showBanner = false;
                    showFlash = false;
                } else {
                    showBanner = cfg.boosterExpiringAlert;
                    showFlash = cfg.expiringFlash;
                }
            } else if (currentKind == AlertKind.MONEY) {
                showBanner = cfg.moneyVisualAlert;
                showFlash = false;
            } else if (currentKind == AlertKind.XP) {
                showBanner = cfg.xpVisualAlert;
                showFlash = false;
            }
        } catch (Throwable ignored) {}

        if (!showBanner && !showFlash) return;

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

        // Subtle, toned-down border vignette glow (NO full-screen wash)
        if (showFlash) {
            drawVignette(g, width, height, color, alpha);
        }

        // Center-top prominent banner card
        if (showBanner) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.font == null) return;

            float titleScale = 1.3f;
            float titleW = mc.font.width(title) * titleScale;
            float subW = mc.font.width(subtitle);
            int cardW = (int) Math.max(titleW, subW) + 24;
            int cardH = 38;
            int cardX = (width - cardW) / 2;
            int cardY = Math.max(14, height / 4 - 18);

            int bgAlpha = (int) (alpha * 200);
            int bgColor = (bgAlpha << 24) | 0x111317;
            ToolkitTheme.rounded(g, cardX, cardY, cardW, cardH, 4, bgColor);

            int borderAlpha = (int) (alpha * 170);
            int borderCol = (borderAlpha << 24) | color;
            g.outline(cardX, cardY, cardW, cardH, borderCol);
            g.fill(cardX + 4, cardY + 1, cardX + cardW - 4, cardY + 2, borderCol);

            // Title text
            float tx = cardX + (cardW - titleW) / 2.0f;
            float ty = cardY + 6;
            g.pose().pushMatrix();
            g.pose().translate(tx, ty);
            g.pose().scale(titleScale, titleScale);
            g.text(mc.font, title, 0, 0, 0xFFFFFFFF, true);
            g.pose().popMatrix();

            // Subtitle text
            float sx = cardX + (cardW - subW) / 2.0f;
            float sy = cardY + 23;
            g.text(mc.font, subtitle, (int) sx, (int) sy, 0xFFFFFFFF, true);
        }
    }

    private static void drawVignette(GuiGraphicsExtractor g, int width, int height, int color, float alpha) {
        int c = color & 0xFFFFFF;
        // Subtle, crisp edge frame: 2px soft outline along the viewport perimeter
        int outerAlpha = (int) (alpha * 70); // max ~27% opacity
        int innerAlpha = (int) (alpha * 30); // max ~12% opacity
        if (outerAlpha > 0) {
            g.outline(0, 0, width, height, (outerAlpha << 24) | c);
            if (innerAlpha > 0 && width > 4 && height > 4) {
                g.outline(1, 1, width - 2, height - 2, (innerAlpha << 24) | c);
            }
        }
    }

    // Accessors for testing
    public static AlertKind getCurrentKind() { return currentKind; }
    public static String getTitle() { return title; }
    public static String getSubtitle() { return subtitle; }
    public static int getAccentColor() { return accentColor; }
    public static boolean isAlertedMoney() { return alertedMoney; }
    public static boolean isAlertedXp() { return alertedXp; }
    public static BoosterTimers.Type getCurrentExpiringType() { return currentExpiringType; }
}
