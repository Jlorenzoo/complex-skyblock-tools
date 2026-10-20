package com.skyblocktoolkit.mixin;

import com.mojang.blaze3d.audio.Channel;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class ChannelGainMixin {
    @Shadow @Final private int source;

    @Inject(method = "setVolume", at = @At("HEAD"))
    private void toolkit$gainCeiling(float volume, CallbackInfo ci) {
        if (AL.getCapabilities().AL_SOFT_gain_clamp_ex) {
            // Pooled channels return to the normal ceiling for ordinary sounds.
            AL10.alSourcef(source, AL10.AL_MAX_GAIN, Math.max(1f, volume));
        }
    }
}
