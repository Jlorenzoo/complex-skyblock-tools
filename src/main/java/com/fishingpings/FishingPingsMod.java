package com.fishingpings;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import com.skyblocktoolkit.AlertSound;
import com.skyblocktoolkit.ItemStatsTracker;
import com.skyblocktoolkit.ToolkitConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;
import com.skyblocktoolkit.mixin.BossHealthOverlayAccessor;
import java.util.*;

public final class FishingPingsMod implements ClientModInitializer {
    private static final SoundEvent SHORT_SPLASH=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("skyblocktoolkit","short_splash"));
    private static final SoundEvent BITE_PING=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("skyblocktoolkit","bite_ping"));
    private static final SoundEvent ONI_FULL_CHIME=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("skyblocktoolkit","oni_full_chime"));
    private static final SoundEvent ONI_APPROACH_CHIME=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("skyblocktoolkit","oni_approach_chime"));
    private final FishingSignals signals=new FishingSignals();
    private FishingConfig config;
    private Object lastLevel;
    private long secondWarningAt=-1;
    private boolean warnedAboutBossAccess, openMenu,warningPreview;
    public FishingConfig settings(){return config;}
    public void previewBite(){
        Minecraft.getInstance().getSoundManager().play(biteSound(SoundSource.NEUTRAL));
    }
    public void previewDepth(){readySound(Minecraft.getInstance());}
    public void previewSnap(){snapSound(Minecraft.getInstance());}
    public void previewWarning(){warningSound(Minecraft.getInstance(),System.currentTimeMillis());warningPreview=true;}
    public void warningChanged(){if(!config.warningEnabled&&!warningPreview)secondWarningAt=-1;}
    @Override public void onInitializeClient(){
        config=new FishingConfig();
        ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
            String text=message.getString();long now=System.currentTimeMillis();
            if(!overlay&&holdingOniRod(mc)){
                long shards=FishingSignals.oniShardReward(text);
                if(shards>0)ItemStatsTracker.oniShards(shards,now);
            }
            // Actionbar and system messages are supported; signed player-chat is not parsed.
            if(config.snapEnabled&&holdingVoidRod(mc)&&signals.snapped(text,now)){signals.cast(null,now);snapSound(mc);}
            if(config.warningEnabled&&signals.warning(text,now))warningSound(mc,now);
            if(config.depthEnabled||config.snapEnabled)checkDepth(mc,text,now);
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            long now=System.currentTimeMillis();
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
            if((!config.depthEnabled&&!config.snapEnabled&&!config.oniEnergyAlert)||mc.gui==null||mc.gui.hud==null)return;
            try{
                var overlay=mc.gui.hud.getBossOverlay();
                if(overlay==null)return;
                var events=((BossHealthOverlayAccessor)overlay).toolkit$getEvents();
                FishingSignals.Energy energy=null;
                for(var bar:events.values()){
                    String title=bar.getName().getString();
                    if(config.depthEnabled||config.snapEnabled)checkDepth(mc,title,now);
                    if(config.oniEnergyAlert&&(energy==null||!energy.full())){
                        var observed=FishingSignals.cursedEnergy(title);
                        if(observed!=null)energy=observed;
                    }
                }
                switch(signals.oniCue(energy,now)){
                    case TWO_AWAY -> mc.getSoundManager().play(new AlertSound(ONI_APPROACH_CHIME,1,0.55f,1.4));
                    case ONE_AWAY -> mc.getSoundManager().play(new AlertSound(ONI_APPROACH_CHIME,1.19f,0.65f,1.6));
                    case FULL -> mc.getSoundManager().play(new AlertSound(ONI_FULL_CHIME,1,0.8f,1.9));
                    case NONE -> {}
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
    public float biteGain(SoundInstance sound){
        return config!=null&&config.biteSoundVolume!=1&&isOwnBite(sound)?config.biteSoundVolume:1;
    }
    public SoundInstance replaceBiteSound(SoundInstance sound){
        if(config!=null&&config.biteSound!=FishingConfig.BiteSound.VANILLA_SPLASH&&isOwnBite(sound)){
            return biteSound(sound.getSource());
        }
        return sound;
    }
    private AlertSound biteSound(SoundSource source){
        return switch(config.biteSound){
            case SHORT_SPLASH -> new AlertSound(SHORT_SPLASH,source,1,0.30f,config.biteSoundVolume);
            case VANILLA_SPLASH -> new AlertSound(SoundEvents.FISHING_BOBBER_SPLASH,source,1,0.25f,config.biteSoundVolume);
            case LOW_PING -> new AlertSound(BITE_PING,source,1,0.45f,config.biteSoundVolume);
        };
    }
    private static boolean isOwnBite(SoundInstance sound){
        if(sound.isRelative()||!sound.getIdentifier().equals(SoundEvents.FISHING_BOBBER_SPLASH.location()))return false;
        var mc=Minecraft.getInstance();
        if(mc==null||mc.level==null||mc.player==null||mc.player.fishing==null)return false;
        var rod=mc.player.getMainHandItem();
        if(!rod.is(Items.FISHING_ROD))rod=mc.player.getOffhandItem();
        if(!rod.is(Items.FISHING_ROD)||FishingSignals.voidRod(rod.getHoverName().getString()))return false;
        double x=sound.getX(),y=sound.getY(),z=sound.getZ();
        var hook=mc.player.fishing;
        double distance=hook.distanceToSqr(x,y,z);
        // Position packets are quantized and the local hook can be interpolating.
        if(distance>1)return false;
        var area=new AABB(x-1,y-1,z-1,x+1,y+1,z+1);
        for(var other:mc.level.getEntitiesOfClass(FishingHook.class,area)){
            if(other!=hook&&other.distanceToSqr(x,y,z)<=distance)return false;
        }
        return true;
    }
    private void snapSound(Minecraft mc){mc.getSoundManager().play(new AlertSound(SoundEvents.ITEM_BREAK.value(),0.9f,Math.min(1f,config.volume*1.25f),Math.max(1f,config.volume)*ToolkitConfig.get().snapSoundGain));}
    private void readySound(Minecraft mc){mc.getSoundManager().play(new AlertSound(SoundEvents.NOTE_BLOCK_HARP.value(),1.6f,Math.min(1f,config.volume),Math.max(1f,config.volume)*ToolkitConfig.get().readySoundGain));}
    private void areaNote(Minecraft mc,float pitch){mc.getSoundManager().play(new AlertSound(SoundEvents.NOTE_BLOCK_BASS.value(),pitch,Math.min(1f,config.volume*3f),Math.max(1f,config.volume)*ToolkitConfig.get().areaSoundGain));}
    private void warningSound(Minecraft mc,long now){warningPreview=false;areaNote(mc,1.3f);secondWarningAt=now+200;}
}
