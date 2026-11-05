package com.skyblocktoolkit.mixin;

import com.moneybooster.PassiveIncomeCapture;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the dedicated action-bar packet after vanilla's game-thread handoff. */
@Mixin(ClientPacketListener.class)
public abstract class IncomeActionBarMixin {
    @Inject(method = "setActionBarText", at = @At("TAIL"))
    private void toolkit$incomeActionBar(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
        PassiveIncomeCapture.observe("action-bar-packet", packet.text());
    }
}
