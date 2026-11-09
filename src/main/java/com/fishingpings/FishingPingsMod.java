package com.fishingpings;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import com.skyblocktoolkit.AlertSound;
import com.skyblocktoolkit.ToolkitConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;
import com.skyblocktoolkit.mixin.BossHealthOverlayAccessor;
import java.util.*;

public final class FishingPingsMod implements ClientModInitializer {
    private final FishingSignals signals=new FishingSignals();
    private FishingConfig config;
    private Object lastLevel;
    private long secondWarningAt=-1;
    private boolean warnedAboutBossAccess, openMenu,warningPreview;
    private boolean oniMeterVisible;
    public FishingConfig settings(){return config;}
    public void previewDepth(){readySound(Minecraft.getInstance());}
    public void previewSnap(){snapSound(Minecraft.getInstance());}
    public void previewWarning(){warningSound(Minecraft.getInstance(),System.currentTimeMillis());warningPreview=true;}
    public void warningChanged(){if(!config.warningEnabled&&!warningPreview)secondWarningAt=-1;}
    @Override public void onInitializeClient(){
        config=new FishingConfig();
        ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
            String text=message.getString();long now=System.currentTimeMillis();
            // Actionbar and system messages are supported; signed player-chat is not parsed.
            if(config.snapEnabled&&holdingVoidRod(mc)&&signals.snapped(text,now)){signals.cast(null,now);snapSound(mc);}
            if(config.warningEnabled&&signals.warning(text,now))warningSound(mc,now);
            if(config.depthEnabled||config.snapEnabled)checkDepth(mc,text,now);
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            long now=System.currentTimeMillis();
            oniMeterVisible=false;
            if(mc.level!=lastLevel){signals.reset();secondWarningAt=-1;warningPreview=false;lastLevel=mc.level;}
            if(openMenu){openMenu=false;mc.setScreenAndShow(new com.skyblocktoolkit.ToolkitScreen(null,2));}
            // Positive deadline = middle note; negative deadline = final low note.
            if(secondWarningAt!=-1&&now>=Math.abs(secondWarningAt)){
                boolean middle=secondWarningAt>=0;secondWarningAt=middle?-(now+200):-1;
                if(config.warningEnabled||warningPreview)areaNote(mc,middle?0.9f:0.6f);
                if(!middle)warningPreview=false;
            }
            if(mc.player==null||mc.level==null)return;
            signals.cast(mc.player.fishing==null?null:mc.player.fishing.getUUID(),now);
            boolean oni=holdingOniRod(mc);
            if((!config.depthEnabled&&!config.snapEnabled&&!oni)||mc.gui==null||mc.gui.hud==null)return;
            try{
                var overlay=mc.gui.hud.getBossOverlay();
                if(overlay==null)return;
                var events=((BossHealthOverlayAccessor)overlay).toolkit$getEvents();
                var energy=FishingSignals.Energy.MISSING;
                for(var bar:events.values()){
                    String title=bar.getName().getString();
                    if(config.depthEnabled||config.snapEnabled)checkDepth(mc,title,now);
                    if(oni&&energy!=FishingSignals.Energy.FULL){
                        var observed=FishingSignals.cursedEnergy(title);
                        if(observed!=FishingSignals.Energy.MISSING)energy=observed;
                    }
                }
                oniMeterVisible=energy!=FishingSignals.Energy.MISSING;
                if(signals.oniReady(energy)&&config.oniEnergyAlert){
                    mc.getSoundManager().play(new AlertSound(SoundEvents.NOTE_BLOCK_BELL.value(),1.2f,0.8f,1.9));
                }
                if(config.snapEnabled&&mc.player.fishing!=null
                    &&holdingVoidRod(mc)
                    &&signals.snapped(null,now))snapSound(mc);
            }catch(Exception e){if(!warnedAboutBossAccess){warnedAboutBossAccess=true;System.err.println("[FishingPings] Cannot read boss bars: "+e.getMessage());}}
        });

    }
    private static boolean overlaps(int x,int y,int w,int h,net.minecraft.client.gui.components.AbstractWidget widget){return widget.visible&&x<widget.getRight()&&x+w>widget.getX()&&y<widget.getBottom()&&y+h>widget.getY();}
    private void checkDepth(Minecraft mc,String text,long now){
        if(mc.player==null||mc.player.fishing==null||!holdingVoidRod(mc))return;
        double depth=FishingSignals.depth(text);
        if(signals.ready(mc.player.fishing.getUUID(),depth,config.target,now)&&config.depthEnabled)readySound(mc);
    }
    private static boolean holdingVoidRod(Minecraft mc){
        if(mc.player==null)return false;
        var main=mc.player.getMainHandItem();var off=mc.player.getOffhandItem();
        return (main.is(Items.FISHING_ROD)&&FishingSignals.voidRod(main.getHoverName().getString()))
            ||(off.is(Items.FISHING_ROD)&&FishingSignals.voidRod(off.getHoverName().getString()));
    }
    private static boolean holdingOniRod(Minecraft mc){
        if(mc.player==null)return false;
        var rod=mc.player.getMainHandItem();
        return rod.is(Items.FISHING_ROD)&&FishingSignals.oniRod(rod.getHoverName().getString());
    }
    public boolean trackingOniShards(Minecraft mc){
        return mc.level!=null&&mc.gui.screen()==null&&holdingOniRod(mc)
            &&(mc.player.fishing!=null||mc.level==lastLevel&&oniMeterVisible);
    }
    public float biteGain(SoundInstance sound){
        if(config==null||config.biteSoundVolume==1||sound.isRelative()
            ||!sound.getIdentifier().equals(SoundEvents.FISHING_BOBBER_SPLASH.location()))return 1;
        var mc=Minecraft.getInstance();
        if(mc==null||mc.level==null||mc.player==null||mc.player.fishing==null)return 1;
        var rod=mc.player.getMainHandItem();
        if(!rod.is(Items.FISHING_ROD))rod=mc.player.getOffhandItem();
        if(!rod.is(Items.FISHING_ROD)||FishingSignals.voidRod(rod.getHoverName().getString()))return 1;
        double x=sound.getX(),y=sound.getY(),z=sound.getZ();
        var hook=mc.player.fishing;
        double distance=hook.distanceToSqr(x,y,z);
        // Position packets are quantized and the local hook can be interpolating.
        if(distance>1)return 1;
        var area=new AABB(x-1,y-1,z-1,x+1,y+1,z+1);
        for(var other:mc.level.getEntitiesOfClass(FishingHook.class,area)){
            if(other!=hook&&other.distanceToSqr(x,y,z)<=distance)return 1;
        }
        return config.biteSoundVolume;
    }
    private void snapSound(Minecraft mc){mc.getSoundManager().play(new AlertSound(SoundEvents.ITEM_BREAK.value(),0.9f,Math.min(1f,config.volume*1.25f),Math.max(1f,config.volume)*ToolkitConfig.get().snapSoundGain));}
    private void readySound(Minecraft mc){mc.getSoundManager().play(new AlertSound(SoundEvents.NOTE_BLOCK_HARP.value(),1.6f,Math.min(1f,config.volume),Math.max(1f,config.volume)*ToolkitConfig.get().readySoundGain));}
    private void areaNote(Minecraft mc,float pitch){mc.getSoundManager().play(new AlertSound(SoundEvents.NOTE_BLOCK_BASS.value(),pitch,Math.min(1f,config.volume*3f),Math.max(1f,config.volume)*ToolkitConfig.get().areaSoundGain));}
    private void warningSound(Minecraft mc,long now){warningPreview=false;areaNote(mc,1.3f);secondWarningAt=now+200;}
}
