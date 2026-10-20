package com.skyblocktoolkit.mixin;

import com.moneybooster.XpTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ExperienceSyncMixin {
    @Inject(method="handleSetExperience",at=@At("TAIL"))
    private void toolkit$experienceSync(ClientboundSetExperiencePacket packet, CallbackInfo ci) {
        XpTracker.onExperienceSync();
    }
}
