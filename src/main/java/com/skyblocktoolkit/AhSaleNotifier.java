package com.skyblocktoolkit;

import net.minecraft.client.Minecraft;

import java.util.regex.Pattern;

public final class AhSaleNotifier {
    private static final Pattern[] AH_PATTERNS = new Pattern[]{
        Pattern.compile("(?i)^\\(/ah\\)\\s+.+?\\s+purchased your listing of\\s+.+?\\s+for:\\s*.+"),
        Pattern.compile("(?i)^\\[Auction\\]\\s+(?:.+?\\s+bought|Your\\s+.+?\\s+sold to|You sold)\\s+.+"),
        Pattern.compile("(?i)^Auction\\s*>\\s*.+?\\s+purchased your listing of\\s+.+"),
        Pattern.compile("(?i)^You sold\\s+.+?\\s+(?:on|in) the (?:Auction House|auction)\\b")
    };

    private AhSaleNotifier() {}

    public static boolean isAhSaleMessage(String text) {
        if (text == null || text.isEmpty()) return false;
        String clean = text.replaceAll("§.", "").strip();
        for (Pattern p : AH_PATTERNS) {
            if (p.matcher(clean).find()) {
                return true;
            }
        }
        return false;
    }

    public static void onChatMessage(String text) {
        if (!ToolkitConfig.get().ahSoundEnabled) return;
        if (isAhSaleMessage(text)) {
            playSound();
        }
    }

    public static void playPreviewSound() {
        playSound();
    }

    private static void playSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;
        var cfg = ToolkitConfig.get();
        ToolkitAudio.playSelected(ToolkitAudio.Cue.AUCTION, mc, (float) cfg.ahSoundVolume, cfg.ahSoundGain);
    }
}
