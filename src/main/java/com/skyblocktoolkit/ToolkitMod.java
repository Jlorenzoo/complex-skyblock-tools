package com.skyblocktoolkit;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
public final class ToolkitMod implements ClientModInitializer {
    public static final com.fishingpings.FishingPingsMod FISHING=new com.fishingpings.FishingPingsMod();
    public static final com.fishingpings.FishingEarnings FISHING_EARNINGS=new com.fishingpings.FishingEarnings();
    private static Object previousLevel;
    public static KeyMapping menuKey;
    public static String fishingLabel(){
        var cfg=FISHING.settings();long now=System.currentTimeMillis();String window=cfg.earningsWindow==300?"5m":"60s";
        if(cfg.averageFishPrice()<=0)return "Fishing: set fish prices";
        return "Fishing est. ("+window+"): "+com.moneybooster.BoosterStatsManager.formatMoney(FISHING_EARNINGS.hourly(now,cfg.earningsWindow,cfg.averageFishPrice()))+"/h"+(FISHING_EARNINGS.warming(now,cfg.earningsWindow)?" *":"");
    }
    public static boolean showFishing(){
        var cfg=ToolkitConfig.get();var mc=net.minecraft.client.Minecraft.getInstance();
        return cfg.earningsMode==2||(cfg.earningsMode==0&&(FISHING_EARNINGS.recent(System.currentTimeMillis(),FISHING.settings().earningsWindow)||(mc!=null&&mc.player!=null&&mc.player.fishing!=null)));
    }
    private static boolean requested;
    @Override public void onInitializeClient(){
        ToolkitConfig.get();
        new com.moneybooster.MoneyBoosterMod().onInitializeClient();FISHING.onInitializeClient();
        menuKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.skyblocktoolkit.menu",GLFW.GLFW_KEY_RIGHT_SHIFT,KeyMapping.Category.MISC));
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            var mc=net.minecraft.client.Minecraft.getInstance();if(!overlay&&mc.level!=null&&mc.player!=null){
                if(mc.level!=previousLevel){FISHING_EARNINGS.reset();previousLevel=mc.level;}
                FISHING_EARNINGS.accept(message.getString(),System.currentTimeMillis());
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(mc.level!=previousLevel){FISHING_EARNINGS.reset();previousLevel=mc.level;}
            if(mc!=null&&mc.player!=null&&mc.player.fishing!=null)FISHING_EARNINGS.observe(System.currentTimeMillis());
            boolean clicked=false;while(menuKey.consumeClick())clicked=true;
            if(requested||clicked){requested=false;if(mc.gui.screen() instanceof ToolkitScreen)mc.gui.screen().onClose();else mc.setScreenAndShow(new ToolkitScreen(mc.gui.screen(),0));}
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->dispatcher.register(ClientCommands.literal("toolkit").executes(ctx->{requested=true;return 1;})));
        ScreenEvents.AFTER_INIT.register((mc,screen,width,height)->{
            if(!(screen instanceof OptionsScreen)&&!(screen instanceof PauseScreen p&&p.showsPauseMenu()))return;
            var widgets=Screens.getWidgets(screen);int bw=Math.min(112,width-16),bx=width-bw-8,by=8;
            for(var widget:widgets)if(widget.visible&&bx<widget.getRight()&&bx+bw>widget.getX()&&by<widget.getBottom()&&by+20>widget.getY()){bx=8;break;}
            widgets.add(com.moneybooster.FlatButton.flat(Component.literal("Skyblock Toolkit"),b->mc.setScreenAndShow(new ToolkitScreen(screen,0))).bounds(bx,by,bw,20).build());
        });
    }
}
