package com.skyblocktoolkit;

import com.moneybooster.BoosterManager;
import com.moneybooster.BoosterTimers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Compact translucent notifications and subtle border flash notifications for
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
        title = "Money booster +" + percentage + "%";
        subtitle = "Release right-click · ≥" + threshold + "% reached";
        accentColor = ToolkitTheme.MONEY_TIMER & 0xFFFFFF;
        startTime = System.currentTimeMillis();
    }

    public static void triggerMoneyBooster(int percentage, String sourceName, long seconds) {
        currentKind = AlertKind.MONEY;
        isPreview = false;
        title = "Money booster +" + percentage + "%";
        subtitle = sourceName + " · " + (seconds > 0 ? (seconds + "s duration") : "Active");
        accentColor = ToolkitTheme.MONEY_TIMER & 0xFFFFFF;
        startTime = System.currentTimeMillis();
    }

    public static void triggerXpBooster(int percentage, String sourceName, long seconds) {
        currentKind = AlertKind.XP;
        isPreview = false;
        title = "XP booster +" + percentage + "%";
        subtitle = sourceName + " · " + (seconds > 0 ? (seconds + "s duration") : "Active");
        accentColor = ToolkitTheme.XP_TIMER & 0xFFFFFF;
        startTime = System.currentTimeMillis();
    }

    private static BoosterTimers.Type currentExpiringType = BoosterTimers.Type.MONEY;

    public static void triggerExpiring(BoosterTimers.Type type, int secondsRemaining) {
        currentKind = AlertKind.EXPIRING;
        currentExpiringType = type;
        isPreview = false;
        String name = type == BoosterTimers.Type.MONEY ? "Money" : "XP";
        title = name + " booster ending";
        subtitle = secondsRemaining + "s remaining";
        accentColor = (type == BoosterTimers.Type.MONEY ? ToolkitTheme.MONEY_TIMER : ToolkitTheme.XP_TIMER) & 0xFFFFFF;
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

        // A small top-center toast leaves the crosshair and the world unobstructed.
        if (showBanner && alpha > 0.02f) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.font == null) return;

            int maxTextWidth = Math.max(0, Math.min(280, width - 48));
            String heading = fit(mc, title, maxTextWidth);
            String detail = fit(mc, subtitle, maxTextWidth);
            int cardW = Math.max(mc.font.width(heading), mc.font.width(detail)) + 28;
            int cardH = 30;
            int cardX = (width - cardW) / 2;
            int cardY = Math.min(22, Math.max(4, height / 12));

            ToolkitTheme.rounded(g, cardX, cardY, cardW, cardH, 5,
                    faded(ToolkitTheme.ALERT_SURFACE, alpha));
            // Color belongs to the tiny status mark, rather than a frame around the card.
            ToolkitTheme.rounded(g, cardX + 8, cardY + 8, 3, 3, 1,
                    faded(0xFF000000 | color, alpha));
            g.text(mc.font, heading, cardX + 16, cardY + 5,
                    faded(ToolkitTheme.ALERT_TEXT, alpha), true);
            g.text(mc.font, detail, cardX + 16, cardY + 17,
                    faded(ToolkitTheme.ALERT_MUTED, alpha), true);
        }
    }

    private static String fit(Minecraft mc, String text, int width) {
        if (mc.font.width(text) <= width) return text;
        String ellipsis = "…";
        return mc.font.substrByWidth(net.minecraft.network.chat.Component.literal(text),
                Math.max(0, width - mc.font.width(ellipsis))).getString() + ellipsis;
    }

    private static int faded(int color, float alpha) {
        return ((int) ((color >>> 24) * alpha) << 24) | (color & 0xFFFFFF);
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
