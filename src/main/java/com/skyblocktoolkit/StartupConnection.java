package com.skyblocktoolkit;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public final class StartupConnection {
    private StartupConnection(){}

    public static void init(){
        var config=ToolkitConfig.get();
        var startup=new StartupJoin(config.startupServer);
        // Use the same ServerData instance to distinguish our startup attempt from
        // manual joins, including manual connections to the same host.
        var attempt=new ServerData("Startup server",config.startupServer,ServerData.Type.OTHER);
        if(startup.isComplexServer()&&config.startupServerPacks)
            attempt.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);

        ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            var mc=Minecraft.getInstance();
            if(!overlay&&mc.getCurrentServer()==attempt)
                startup.onSystemMessage(message.getString());
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            var screen=mc.gui.screen();
            if(startup.takeRedirectRetry(mc.getCurrentServer()==attempt
                    &&mc.level!=null&&screen==null&&mc.gui.overlay()==null)){
                var title=new TitleScreen();
                mc.disconnect(title,false);
                mc.setScreenAndShow(title);
                return;
            }
            String address=startup.poll(mc.gui.overlay()!=null,screen instanceof TitleScreen,
                mc.level!=null||mc.getConnection()!=null||screen instanceof ConnectScreen||screen instanceof DisconnectedScreen);
            if(address==null)return;
            try{
                ConnectScreen.startConnecting(screen,mc,ServerAddress.parseString(address),attempt,false,null);
            }catch(Exception e){
                System.err.println("[SkyblockToolkit] Startup server could not be opened: "+e.getMessage());
            }
        });
    }
}
