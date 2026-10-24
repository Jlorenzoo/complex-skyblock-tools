package com.skyblocktoolkit;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public final class StartupConnection {
    private StartupConnection(){}

    public static void init(){
        var startup=new StartupJoin(ToolkitConfig.get().startupServer);
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            var screen=mc.gui.screen();
            String address=startup.poll(mc.gui.overlay()!=null,screen instanceof TitleScreen,
                mc.level!=null||mc.getConnection()!=null||screen instanceof ConnectScreen||screen instanceof DisconnectedScreen);
            if(address==null)return;
            try{
                ConnectScreen.startConnecting(screen,mc,ServerAddress.parseString(address),
                    new ServerData(address,address,ServerData.Type.OTHER),false,null);
            }catch(Exception e){
                System.err.println("[SkyblockToolkit] Startup server could not be opened: "+e.getMessage());
            }
        });
    }
}
