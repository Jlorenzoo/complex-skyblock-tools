package com.skyblocktoolkit;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Marks toolkit alerts so their extra gain never changes ordinary game sounds. */
public final class AlertSound extends SimpleSoundInstance {
    public final float gain;

    public AlertSound(SoundEvent sound, float pitch, float volume, double gain) {
        super(sound.location(), SoundSource.UI, volume, pitch, SoundInstance.createUnseededRandom(),
            false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        this.gain = (float) (Double.isFinite(gain) ? Math.clamp(gain, 0, 9) : 1.9);
    }
}
