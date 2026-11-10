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
import com.skyblocktoolkit.ToolkitAudio;
import com.skyblocktoolkit.ToolkitAudio.Cue;
import com.moneybooster.BoosterStatsManager;
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
    private static final SoundEvent PEBBLE_SPLASH=ToolkitAudio.event("pebble_splash");
    private static final SoundEvent RIPPLE_TICK=ToolkitAudio.event("ripple_tick");
    private static final SoundEvent CORK_POP=ToolkitAudio.event("cork_pop");
    private static final SoundEvent MUTED_SPLASH=ToolkitAudio.event("muted_splash");
    private static final SoundEvent HOLLOW_PLOP=ToolkitAudio.event("hollow_plop");
    private static final SoundEvent WOODEN_TAP=ToolkitAudio.event("wooden_tap");
    private static final SoundEvent DOUBLE_KNOCK=ToolkitAudio.event("double_knock");
    private static final SoundEvent SHORT_SPLASH=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("skyblocktoolkit","short_splash"));
    private static final SoundEvent WATER_DROP=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("skyblocktoolkit","water_drop"));
    private static final SoundEvent ONI_FULL_CHIME=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("skyblocktoolkit","oni_full_chime"));
    private static final SoundEvent ONI_APPROACH_CHIME=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("skyblocktoolkit","oni_approach_chime"));
    private final FishingSignals signals=new FishingSignals();
    private final OniEarnings oniEarnings=new OniEarnings();
    private List<net.minecraft.world.item.Item> oniFish;
    private static final OniEarnings.Reward[] ONI_FISH_REWARDS={
        OniEarnings.Reward.COD,OniEarnings.Reward.SALMON,
        OniEarnings.Reward.PUFFERFISH,OniEarnings.Reward.TROPICAL_FISH
    };
    private int[] previousFish;
    private boolean oniCast;
    private long oniReelAt=-1;
    private long lastOniActivity=-1;
    private String observedOniRod;
    private Object oniLevel;
    private FishingConfig config;
    private Object lastLevel;
    private long secondWarningAt=-1;
    private boolean warnedAboutBossAccess, openMenu,warningPreview;
    public FishingConfig settings(){return config;}
    public boolean recentOniActivity(long now){
        return lastOniActivity>=0&&now-lastOniActivity<5_000;
    }
    public String oniLabel(long now){
        if(!config.hasOniPrices())return "";
        long minutes=oniEarnings.sampleTime(now)/60_000;
        String sample=minutes<5?"warming up":minutes==60?"1h":minutes+"m";
        return "Oni est. ("+sample+"): "+BoosterStatsManager.formatMoney(oniEarnings.hourly(now,config::oniPrice))+"/h";
    }
    public void previewBite(){
        Minecraft.getInstance().getSoundManager().play(biteSound(SoundSource.NEUTRAL));
    }
    public void previewDepth(){readySound(Minecraft.getInstance());}
    public void previewSnap(){snapSound(Minecraft.getInstance());}
    public void previewOniCue(FishingSignals.OniCue cue){playOniCue(Minecraft.getInstance(),cue);}
    public void previewWarning(){warningSound(Minecraft.getInstance(),System.currentTimeMillis(),true);}
    public void warningChanged(){if(!config.warningEnabled&&!warningPreview)secondWarningAt=-1;}
    @Override public void onInitializeClient(){
        config=new FishingConfig();
        oniFish=List.of(Items.COD,Items.SALMON,Items.PUFFERFISH,Items.TROPICAL_FISH);
        ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
            String text=message.getString();long now=System.currentTimeMillis();
            if(!overlay){
                resetOniOnLevelChange(mc);
                if(FishingSignals.oniActivity(text))observeOniRod(mc,now);
                if(holdingOniRod(mc)){
                    long shards=FishingSignals.oniShardReward(text);
                    if(shards>0){
                        lastOniActivity=now;
                        oniEarnings.add(now,OniEarnings.Reward.GOLD_SHARD,shards);
                        ItemStatsTracker.oniShards(shards,now,heldRodName(mc));
                    }
                    oniEarnings.add(now,OniEarnings.Reward.VOTE_KEY,FishingSignals.oniVoteKeyReward(text));
                }
            }
            // Actionbar and system messages are supported; signed player-chat is not parsed.
            if(config.snapEnabled&&holdingVoidRod(mc)&&signals.snapped(text,now)){signals.cast(null,now);snapSound(mc);}
            if(config.warningEnabled&&signals.warning(text,now))warningSound(mc,now,false);
            if(config.depthEnabled||config.snapEnabled)checkDepth(mc,text,now);
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            long now=System.currentTimeMillis();
            resetOniOnLevelChange(mc);
            sampleOniFish(mc,now);
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
            if(mc.gui==null||mc.gui.hud==null)return;
            try{
                var overlay=mc.gui.hud.getBossOverlay();
                if(overlay==null)return;
                var events=((BossHealthOverlayAccessor)overlay).toolkit$getEvents();
                FishingSignals.Energy energy=null;
                for(var bar:events.values()){
                    String title=bar.getName().getString();
                    if(config.depthEnabled||config.snapEnabled)checkDepth(mc,title,now);
                    if(energy==null||!energy.full()){
                        var observed=FishingSignals.cursedEnergy(title);
                        if(observed!=null)energy=observed;
                    }
                }
                if(energy!=null&&(mc.player.fishing!=null||holdingOniRod(mc)))observeOniRod(mc,now);
                if(config.oniEnergyAlert)playOniCue(mc,signals.oniCue(energy,now));
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
    public boolean holdingOniRod(Minecraft mc){
        String name=heldRodName(mc);
        return name!=null&&(FishingSignals.oniRod(name)
            ||(mc.level==oniLevel&&name.equals(observedOniRod)));
    }
    private static String heldRodName(Minecraft mc){
        if(mc.player==null)return null;
        var rod=mc.player.getMainHandItem();
        if(!rod.is(Items.FISHING_ROD))rod=mc.player.getOffhandItem();
        return rod.is(Items.FISHING_ROD)?rod.getHoverName().getString():null;
    }
    private void observeOniRod(Minecraft mc,long now){
        String name=heldRodName(mc);
        if(name==null)return;
        observedOniRod=name;
        lastOniActivity=now;
        oniEarnings.start(now);
    }
    private void resetOniOnLevelChange(Minecraft mc){
        if(mc.level==oniLevel)return;
        oniLevel=mc.level;
        oniEarnings.reset();
        previousFish=null;
        oniCast=false;
        oniReelAt=-1;
        lastOniActivity=-1;
        observedOniRod=null;
    }
    private void sampleOniFish(Minecraft mc,long now){
        if(mc.level==null||!holdingOniRod(mc)||mc.gui.screen()!=null){
            previousFish=null;
            oniCast=false;
            oniReelAt=-1;
            return;
        }
        boolean cast=mc.player.fishing!=null;
        if(cast)oniEarnings.start(now);
        if(oniCast&&!cast)oniReelAt=now;
        oniCast=cast;
        boolean justReeled=oniReelAt>=0&&now-oniReelAt<=2_000;
        if(!cast&&!justReeled){
            previousFish=null;
            return;
        }
        var counts=new int[oniFish.size()];
        var inventory=mc.player.getInventory();
        for(int slot=0;slot<inventory.getContainerSize();slot++){
            var stack=inventory.getItem(slot);
            if(stack.isEmpty())continue;
            int fish=oniFish.indexOf(stack.getItem());
            if(fish>=0)counts[fish]+=stack.getCount();
        }
        if(justReeled&&previousFish!=null){
            long caught=0;
            double value=0;
            for(int i=0;i<counts.length;i++){
                int gained=counts[i]-previousFish[i];
                if(gained<=0)continue;
                var reward=ONI_FISH_REWARDS[i];
                oniEarnings.add(now,reward,gained);
                caught+=gained;
                value+=gained*config.oniPrice(reward);
            }
            if(caught>0)ItemStatsTracker.fish(caught,value,now);
        }
        previousFish=counts;
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
        SoundEvent event=switch(config.biteSound){
            case PEBBLE -> PEBBLE_SPLASH;
            case RIPPLE -> RIPPLE_TICK;
            case CORK -> CORK_POP;
            case MUTED_SPLASH -> MUTED_SPLASH;
            case HOLLOW_PLOP -> HOLLOW_PLOP;
            case WOODEN_TAP -> WOODEN_TAP;
            case DOUBLE_KNOCK -> DOUBLE_KNOCK;
            case SHORT_SPLASH -> SHORT_SPLASH;
            case VANILLA_SPLASH -> SoundEvents.FISHING_BOBBER_SPLASH;
            case WATER_DROP -> WATER_DROP;
        };
        float volume=config.biteSound==FishingConfig.BiteSound.VANILLA_SPLASH?0.25f:0.30f;
        return new AlertSound(event,source,1,volume,config.biteSoundVolume);
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
    private void playOniCue(Minecraft mc,FishingSignals.OniCue cue){
        switch(cue){
            case TWO_AWAY -> ToolkitAudio.play(Cue.ONI_TWO,mc,ONI_APPROACH_CHIME,1,0.55f,1.4);
            case ONE_AWAY -> ToolkitAudio.play(Cue.ONI_ONE,mc,ONI_APPROACH_CHIME,1.19f,0.65f,1.6);
            case FULL -> ToolkitAudio.play(Cue.ONI_FULL,mc,ONI_FULL_CHIME,1,0.8f,1.9);
            case NONE -> {}
        }
    }
    private void snapSound(Minecraft mc){ToolkitAudio.play(Cue.SNAP,mc,SoundEvents.ITEM_BREAK.value(),0.9f,Math.min(1f,config.volume*1.25f),Math.max(1f,config.volume)*ToolkitConfig.get().snapSoundGain);}
    private void readySound(Minecraft mc){ToolkitAudio.play(Cue.READY,mc,SoundEvents.NOTE_BLOCK_HARP.value(),1.6f,Math.min(1f,config.volume),Math.max(1f,config.volume)*ToolkitConfig.get().readySoundGain);}
    private void areaNote(Minecraft mc,float pitch){mc.getSoundManager().play(new AlertSound(SoundEvents.NOTE_BLOCK_BASS.value(),pitch,Math.min(1f,config.volume*3f),Math.max(1f,config.volume)*ToolkitConfig.get().areaSoundGain));}
    private void warningSound(Minecraft mc,long now,boolean preview){
        float volume=Math.min(1f,config.volume*3f);
        double gain=Math.max(1f,config.volume)*ToolkitConfig.get().areaSoundGain;
        if(ToolkitAudio.playSelected(Cue.DEPLETED,mc,volume,gain)){
            secondWarningAt=-1;
            warningPreview=false;
            return;
        }
        warningPreview=preview;
        areaNote(mc,1.3f);
        secondWarningAt=now+200;
    }
}
