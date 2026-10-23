package com.skyblocktoolkit;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

public final class CoconutNotifier {
    private CoconutNotifier() {}
    public static void preview() {
        var mc = Minecraft.getInstance();
        if (mc != null && mc.getSoundManager() != null)
            mc.getSoundManager().play(new AlertSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.6f, 1f, 1));
    }
}
