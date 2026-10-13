package com.fishingpings;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import java.lang.reflect.Field;
import java.util.*;

public final class FishingPingsMod implements ClientModInitializer {
    private final FishingSignals signals=new FishingSignals();
    private FishingConfig config;
    private Field bossEvents;
    private Object lastLevel;
    private long secondWarningAt=-1;
    private boolean warnedAboutBossAccess, openMenu;
    public FishingConfig settings(){return config;}
    public void previewDepth(){readySound(Minecraft.getInstance());}
    public void previewSnap(){snapSound(Minecraft.getInstance());}
    public void previewWarning(){warningSound(Minecraft.getInstance(),System.currentTimeMillis());}
    public void warningChanged(){if(!config.warningEnabled)secondWarningAt=-1;}
    @Override public void onInitializeClient(){
        config=new FishingConfig();
        try{bossEvents=BossHealthOverlay.class.getDeclaredField("events");bossEvents.setAccessible(true);}catch(Exception e){System.err.println("[FishingPings] Boss bar access unavailable; actionbar depth detection remains enabled.");}
        ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
            String text=message.getString();long now=System.currentTimeMillis();
            // Actionbar and system messages are supported; signed player-chat is not parsed.
            if(config.snapEnabled&&signals.snapped(text,now)){signals.cast(null,now);snapSound(mc);}
            if(config.warningEnabled&&signals.warning(text,now))warningSound(mc,now);
            if(config.depthEnabled||config.snapEnabled)checkDepth(mc,text,now);
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            long now=System.currentTimeMillis();
            if(mc.level!=lastLevel){signals.reset();secondWarningAt=-1;lastLevel=mc.level;}
            if(openMenu){openMenu=false;mc.setScreenAndShow(new com.skyblocktoolkit.ToolkitScreen(null,2));}
            // Positive deadline = middle note; negative deadline = final low note.
            if(secondWarningAt!=-1&&now>=Math.abs(secondWarningAt)){
                boolean middle=secondWarningAt>=0;secondWarningAt=middle?-(now+200):-1;
                if(config.warningEnabled)mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(),middle?0.9f:0.6f,Math.min(1f,config.volume*3f)));
            }
            if(mc.player==null||mc.level==null)return;
            signals.cast(mc.player.fishing==null?null:mc.player.fishing.getUUID(),now);
            if((!config.depthEnabled&&!config.snapEnabled)||bossEvents==null||mc.gui==null||mc.gui.hud==null)return;
            try{
                var overlay=mc.gui.hud.getBossOverlay();
                if(overlay==null)return;
                Object value=bossEvents.get(overlay);
                if(value instanceof Map<?,?> events){for(Object event:events.values()){
                    if(event instanceof net.minecraft.client.gui.components.LerpingBossEvent bar)checkDepth(mc,bar.getName().getString(),now);
                }
                    if(config.snapEnabled&&mc.player.fishing!=null
                        &&(mc.player.getMainHandItem().is(Items.FISHING_ROD)||mc.player.getOffhandItem().is(Items.FISHING_ROD))
                        &&signals.snapped(null,now))snapSound(mc);
                }
            }catch(Exception e){if(!warnedAboutBossAccess){warnedAboutBossAccess=true;System.err.println("[FishingPings] Cannot read boss bars: "+e.getMessage());}}
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->{
            dispatcher.register(ClientCommands.literal("fishping").executes(ctx->{openMenu=true;return 1;}));
        });
    }
    private static boolean overlaps(int x,int y,int w,int h,net.minecraft.client.gui.components.AbstractWidget widget){return widget.visible&&x<widget.getRight()&&x+w>widget.getX()&&y<widget.getBottom()&&y+h>widget.getY();}
    private void checkDepth(Minecraft mc,String text,long now){
        if(mc.player==null||mc.player.fishing==null)return;
        if(!mc.player.getMainHandItem().is(Items.FISHING_ROD)&&!mc.player.getOffhandItem().is(Items.FISHING_ROD))return;
        double depth=FishingSignals.depth(text);
        if(signals.ready(mc.player.fishing.getUUID(),depth,config.target,now)&&config.depthEnabled)readySound(mc);
    }
    private void snapSound(Minecraft mc){mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_BREAK.value(),0.9f,Math.min(1f,config.volume*1.25f)));}
    private void readySound(Minecraft mc){mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HARP.value(),1.6f,config.volume));}
    private void warningSound(Minecraft mc,long now){mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(),1.3f,Math.min(1f,config.volume*3f)));secondWarningAt=now+200;}
}
