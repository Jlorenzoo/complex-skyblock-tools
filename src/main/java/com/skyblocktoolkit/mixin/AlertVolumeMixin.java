package com.skyblocktoolkit.mixin;

import com.skyblocktoolkit.AlertSound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class AlertVolumeMixin {
    @Shadow private float calculateVolume(float volume, SoundSource source) { throw new AssertionError(); }

    // Initial playback otherwise uses the float/source overload, bypassing the
    // instance-aware gain hook below. Reuse it for both playback and updates.
    @Redirect(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float toolkit$initialAlertVolume(SoundEngine engine, float volume, SoundSource source, SoundInstance sound) {
        float baseVolume = calculateVolume(volume, source);
        return sound instanceof AlertSound alert ? baseVolume * alert.gain : baseVolume;
    }

    // Apply after vanilla's 100% ceiling, retaining Minecraft's UI/master controls.
    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
        at = @At("RETURN"), cancellable = true)
    private void toolkit$alertGain(SoundInstance sound, CallbackInfoReturnable<Float> result) {
        if (sound instanceof AlertSound alert) result.setReturnValue(result.getReturnValueF() * alert.gain);
    }
}
