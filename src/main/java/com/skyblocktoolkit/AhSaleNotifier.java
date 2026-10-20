package com.skyblocktoolkit;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

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
        SoundEvent sound;
        float pitch = 1.0f;
        switch (cfg.ahSoundType) {
            case 1 -> { sound = SoundEvents.PLAYER_LEVELUP; pitch = 1.0f; }
            case 2 -> { sound = SoundEvents.EXPERIENCE_ORB_PICKUP; pitch = 1.2f; }
            case 3 -> { sound = SoundEvents.AMETHYST_BLOCK_CHIME; pitch = 1.4f; }
            case 4 -> { sound = SoundEvents.BELL_BLOCK; pitch = 1.0f; }
            default -> { sound = SoundEvents.NOTE_BLOCK_PLING.value(); pitch = 1.6f; }
        }
        mc.getSoundManager().play(new AlertSound(sound, pitch, (float) cfg.ahSoundVolume, cfg.ahSoundGain));
    }
}
