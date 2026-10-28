package com.skyblocktoolkit.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererResizeMixin {
    @Shadow private RenderTarget entityOutlineTarget;
    @Shadow private CloudRenderer cloudRenderer;
    @Shadow private SectionOcclusionGraph sectionOcclusionGraph;

    @Unique private int toolkit$lastWidth = -1;
    @Unique private int toolkit$lastHeight = -1;

    /**
     * Decision check for window resize events.
     * Rejects zero/negative dimensions (minimized/unmapped during Alt-Tab)
     * and identical dimensions (redundant X11/Xwayland focus events).
     */
    public static boolean shouldSkipResize(int width, int height, int lastWidth, int lastHeight) {
        if (width <= 0 || height <= 0) return true;
        return width == lastWidth && height == lastHeight;
    }

    /**
     * Prevents destructive SectionOcclusionGraph invalidations on window resize / focus switches.
     * Vanilla unconditionally calls needsUpdate() -> sectionOcclusionGraph.invalidate(), wiping all
     * visible chunk sections and causing the world to disappear into void when Alt-Tabbing.
     */
    @Inject(method = "resize", at = @At("HEAD"), cancellable = true)
    private void toolkit$preserveWorldOnResize(int width, int height, CallbackInfo ci) {
        if (shouldSkipResize(width, height, this.toolkit$lastWidth, this.toolkit$lastHeight)) {
            ci.cancel();
            return;
        }
        this.toolkit$lastWidth = width;
        this.toolkit$lastHeight = height;

        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.resize(width, height);
        }
        if (this.cloudRenderer != null) {
            this.cloudRenderer.markForRebuild();
        }
        // Request a non-destructive frustum recalculation without wiping active visible sections
        if (this.sectionOcclusionGraph != null) {
            ((SectionOcclusionGraphAccessor) this.sectionOcclusionGraph).toolkit$getNeedsFrustumUpdate().set(true);
        }
        ci.cancel();
    }
}
