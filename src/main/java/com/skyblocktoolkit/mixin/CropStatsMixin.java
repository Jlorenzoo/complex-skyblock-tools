package com.skyblocktoolkit.mixin;

import com.skyblocktoolkit.CropStatsProbe;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class CropStatsMixin {
    @Inject(method = "handleAwardStats", at = @At("TAIL"))
    private void toolkit$cropStats(ClientboundAwardStatsPacket packet, CallbackInfo ci) {
        CropStatsProbe.onStatsReceived();
    }
}
