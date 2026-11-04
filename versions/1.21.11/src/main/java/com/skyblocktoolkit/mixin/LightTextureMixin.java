package com.skyblocktoolkit.mixin;

import com.skyblocktoolkit.ToolkitConfig;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    // ponytail: Fullbright clamps night vision intensity to 1.0f on the client lightmap without applying any server-side status effects or HUD icons.
    @ModifyArg(
        method = "updateLightTexture",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
            ordinal = 3
        )
    )
    private float toolkit$fullbrightNightVision(float original) {
        return ToolkitConfig.get().fullBright ? 1.0f : original;
    }
}
