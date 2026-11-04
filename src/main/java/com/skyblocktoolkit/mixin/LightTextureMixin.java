package com.skyblocktoolkit.mixin;

import com.skyblocktoolkit.ToolkitConfig;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightTextureMixin {
    // ponytail: Fullbright clamps night vision intensity to 1.0f on the client lightmap without applying any server-side status effects or HUD icons.
    @Inject(method = "extract", at = @At("RETURN"))
    private void toolkit$fullbright(LightmapRenderState state, float f, CallbackInfo ci) {
        if (ToolkitConfig.get().fullBright) {
            state.nightVisionEffectIntensity = 1.0f;
        }
    }
}
