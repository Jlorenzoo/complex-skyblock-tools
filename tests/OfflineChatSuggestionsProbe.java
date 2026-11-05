package com.skyblocktoolkit.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Smoke jar only: chat can render offline without a server suggestion provider. */
@Mixin(CommandSuggestions.class)
public abstract class OfflineChatSuggestionsProbe {
    @Inject(method="updateCommandInfo",at=@At("HEAD"),cancellable=true)
    private void toolkit$offlineSuggestions(CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) ci.cancel();
    }
}
