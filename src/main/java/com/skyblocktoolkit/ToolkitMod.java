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
    public static KeyMapping.Category CATEGORY;
    public static KeyMapping menuKey;
    public static KeyMapping blackoutKey;
    public static KeyMapping sellAllKey;
    public static String fishingLabel(){
        var cfg=FISHING.settings();long now=System.currentTimeMillis();String window=cfg.earningsWindow==300?"5m":"60s";
        if(cfg.averageFishPrice()<=0)return "Fishing: set fish prices";
        return "Fishing est. ("+window+"): "+com.moneybooster.BoosterStatsManager.formatMoney(FISHING_EARNINGS.hourly(now,cfg.earningsWindow,cfg.averageFishPrice()))+"/h"+(FISHING_EARNINGS.warming(now,cfg.earningsWindow)?" *":"");
    }
    private static boolean requested;
    private static int requestedPage;
    @Override public void onInitializeClient(){
        ToolkitConfig.get();
        StartupConnection.init();
        ClearHarvest.init();
        UpdateManager.cleanOldBackups();
        UpdateManager.checkForUpdatesAsync();
        new com.moneybooster.MoneyBoosterMod().onInitializeClient();FISHING.onInitializeClient();
        CATEGORY=KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("skyblocktoolkit","main"));
        menuKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.skyblocktoolkit.menu",GLFW.GLFW_KEY_RIGHT_SHIFT,CATEGORY));
        blackoutKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.skyblocktoolkit.blackout",GLFW.GLFW_KEY_F6,CATEGORY));
        sellAllKey=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.skyblocktoolkit.sellall",GLFW.GLFW_KEY_UNKNOWN,CATEGORY));
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            var mc=net.minecraft.client.Minecraft.getInstance();if(!overlay&&mc.level!=null&&mc.player!=null){
                if(mc.level!=previousLevel){FISHING_EARNINGS.reset();previousLevel=mc.level;}
                String text=message.getString();
                FISHING_EARNINGS.accept(text,System.currentTimeMillis());
                AhSaleNotifier.onChatMessage(text);
            }
        });
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("skyblocktoolkit","blackout"),
            (extractor,deltaTracker)->{
                // Non-intrusive HUD overlay: renders over gameplay while maintaining full player interaction
                if(BlackoutManager.isActive()&&net.minecraft.client.Minecraft.getInstance().gui.screen()==null)
                    BlackoutManager.render(extractor,extractor.guiWidth(),extractor.guiHeight());
            }
        );
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(mc.level!=previousLevel){FISHING_EARNINGS.reset();previousLevel=mc.level;}
            if(mc!=null&&mc.player!=null){
                UpdateManager.checkAndNotifyChat(mc.player);
                if(mc.player.fishing!=null)FISHING_EARNINGS.observe(System.currentTimeMillis());
            }
            boolean clicked=false;while(menuKey.consumeClick())clicked=true;
            boolean blackoutClicked=false;while(blackoutKey.consumeClick())blackoutClicked=true;
            boolean sellClicked=false;while(sellAllKey.consumeClick())sellClicked=true;
            if(sellClicked&&mc.player!=null&&mc.getConnection()!=null&&mc.gui.screen()==null)
                mc.getConnection().sendCommand("sellall");
            if(blackoutClicked)BlackoutManager.toggle();
            if(requested||clicked){
                if(clicked&&!requested)requestedPage=0;
                requested=false;
                if(BlackoutManager.isActive())return;
                if(mc.gui.screen() instanceof ToolkitScreen)mc.gui.screen().onClose();
                else mc.setScreenAndShow(new ToolkitScreen(mc.gui.screen(),requestedPage));
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->{
            var command=ClientCommands.literal("toolkit").executes(ctx->{requestedPage=0;requested=true;return 1;});
            command.then(ClientCommands.literal("update").executes(ctx->{UpdateManager.triggerUpdateFromCommand();return 1;}));
            command.then(ClientCommands.literal("checkupdate").executes(ctx->{UpdateManager.triggerUpdateFromCommand();return 1;}));
            String[] modules={"boosters","fishing","chat","auction","hud","blackout","farming","farm"};
            int[] pages={1,2,3,4,5,7,9,9};
            for(int i=0;i<modules.length;i++){
                final int target=pages[i];
                command.then(ClientCommands.literal(modules[i]).executes(ctx->{requestedPage=target;requested=true;return 1;}));
            }
            dispatcher.register(command);
        });
        ScreenEvents.AFTER_INIT.register((mc,screen,width,height)->{
            if(!(screen instanceof OptionsScreen)&&!(screen instanceof PauseScreen p&&p.showsPauseMenu()))return;
            var widgets=Screens.getWidgets(screen);int bw=Math.min(112,width-16),bx=width-bw-8,by=8;
            for(var widget:widgets)if(widget.visible&&bx<widget.getRight()&&bx+bw>widget.getX()&&by<widget.getBottom()&&by+20>widget.getY()){bx=8;break;}
            widgets.add(com.moneybooster.FlatButton.flat(Component.literal("Skyblock Toolkit"),b->mc.setScreenAndShow(new ToolkitScreen(screen,0))).bounds(bx,by,bw,20).build());
        });
    }
}
