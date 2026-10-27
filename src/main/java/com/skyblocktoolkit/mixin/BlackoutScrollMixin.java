package com.skyblocktoolkit.mixin;

import com.skyblocktoolkit.BlackoutManager;
import com.skyblocktoolkit.ToolkitMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class BlackoutScrollMixin {
    @Inject(method="onScroll", at=@At("HEAD"), cancellable=true)
    private void toolkit$blackoutScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (window == mc.getWindow().handle() && mc.gui.screen() == null
                && ToolkitMod.blackoutKey != null && ToolkitMod.blackoutKey.isDown()
                && BlackoutManager.adjustOpacity(vertical)) {
            ci.cancel(); // Keep the held-key gesture from changing the hotbar slot.
        }
    }
}
