package com.skyblocktoolkit.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Offline smoke-jar-only probe: prevent network access and record the real vanilla entry point. */
@Mixin(ConnectScreen.class)
public abstract class StartupConnectProbe {
    @Inject(method="startConnecting",at=@At("HEAD"),cancellable=true)
    private static void toolkit$probe(Screen parent,Minecraft mc,ServerAddress address,ServerData server,
            boolean quickPlay,TransferState transfer,CallbackInfo ci){
        if(!Boolean.getBoolean("toolkit.testStartupConnect"))return;
        System.setProperty("toolkit.startupProbeAddress",address.getHost()+":"+address.getPort());
        int count=Integer.parseInt(System.getProperty("toolkit.startupProbeCount","0"));
        System.setProperty("toolkit.startupProbeCount",Integer.toString(count+1));
        ci.cancel();
    }
}
