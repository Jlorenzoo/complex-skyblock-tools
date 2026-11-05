package com.skyblocktoolkit;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.item.*;
import java.util.*;

/** Read-only inventory sensing. Histories resume automatically across switches and restarts. */
public final class ItemStatsTracker {
    private static final ItemStatsHistory HISTORY=new ItemStatsHistory(FabricLoader.getInstance().getConfigDir().resolve("skyblocktoolkit-item-stats.json"));
    private static final ItemStatsAutomation AUTOMATION=new ItemStatsAutomation(HISTORY);
    private static long checkedAt;
    private static Object level;
    private ItemStatsTracker(){}
    public static ItemStatsHistory history(){return HISTORY;}
    public static String currentItem(){return AUTOMATION.currentHand();}
    public static ItemStatsLocation location(Minecraft mc){
        if(mc.level==null)return ItemStatsLocation.UNKNOWN;
        var scoreboard=mc.level.getScoreboard();var objective=scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if(objective==null)return ItemStatsLocation.UNKNOWN;
        var lines=new ArrayList<String>();
        for(var score:scoreboard.listPlayerScores(objective))if(!score.isHidden())
            lines.add(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(score.owner()),score.ownerName()).getString());
        if(!lines.isEmpty())lines.add(objective.getDisplayName().getString());
        return ItemStatsLocation.sidebar(lines);
    }
    public static String message(){return HISTORY.error().isEmpty()?(HISTORY.activeSessions().isEmpty()?"Automatic tracking · waiting for tool use":"Automatically recording "+HISTORY.activeSessions().size()+" equipped item(s)"):HISTORY.error();}
    public static String scope(){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return "";
        String server=mc.getCurrentServer()!=null?mc.getCurrentServer().ip:mc.getSingleplayerServer()!=null?mc.getSingleplayerServer().getWorldData().getLevelName():"offline";
        return server.toLowerCase(Locale.ROOT)+"/"+mc.player.getUUID();
    }
    public static String encode(ItemStack stack){
        var mc=Minecraft.getInstance();if(mc.level==null||stack.isEmpty())return null;
        return ItemStack.CODEC.encodeStart(mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE),stack.copyWithCount(1)).result().map(Object::toString).orElse(null);
    }
    public static ItemStack icon(ItemStatsHistory.Profile p){
        try{var mc=Minecraft.getInstance();if(mc.level!=null&&p.icon!=null)return ItemStack.CODEC.parse(mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(p.icon)).result().orElse(ItemStack.EMPTY);}catch(Exception ignored){}
        return ItemStack.EMPTY;
    }
    public static boolean trackable(ItemStack stack){
        return !stack.isEmpty()&&(stack.is(ItemTags.HOES)||stack.is(ItemTags.PICKAXES)||stack.is(ItemTags.AXES)||stack.is(ItemTags.SHOVELS)||stack.is(ItemTags.SWORDS)||stack.getItem() instanceof FishingRodItem
            ||stack.getMaxStackSize()==1&&(stack.has(DataComponents.CUSTOM_NAME)||stack.has(DataComponents.CUSTOM_DATA)||stack.has(DataComponents.LORE)));
    }
    public static void tick(Minecraft mc){sample(mc,false,false);}
    private static void sample(Minecraft mc,boolean force,boolean reward){
        long now=System.currentTimeMillis();
        if(!force&&mc.level==level&&now-checkedAt<250){HISTORY.tick(now);return;}
        checkedAt=now;level=mc.level;
        var observations=new ArrayList<ItemStatsAutomation.Observation>();boolean activity=false;
        if(mc.player!=null&&mc.level!=null){
            var inventory=mc.player.getInventory();int selected=inventory.getSelectedSlot();
            for(int i=0;i<Math.min(36,inventory.getContainerSize());i++){
                var stack=inventory.getItem(i);if(!trackable(stack))continue;
                observeItem(observations,stack,"inventory:"+i,i==selected?"HAND":null);
            }
            for(var slot:new EquipmentSlot[]{EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})
                observeItem(observations,mc.player.getItemBySlot(slot),"equipment:"+slot.name(),slot.name());
            var hand=mc.player.getMainHandItem();
            activity=mc.gui.screen()==null&&(mc.options.keyAttack.isDown()||mc.options.keyUse.isDown()
                ||hand.getItem() instanceof FishingRodItem&&mc.player.fishing!=null
                ||hand.getItem() instanceof HoeItem&&CropHarvestTracker.isActive(now));
            activity|=reward;
        }
        try{AUTOMATION.observe(scope(),mc.level,location(mc),observations,activity,now);}catch(Exception e){System.err.println("[Complexify] Item recognition: "+e.getMessage());}
    }
    private static void observeItem(List<ItemStatsAutomation.Observation> observations,ItemStack stack,String location,String slot){
        if(!trackable(stack))return;
        String encoded=encode(stack);if(encoded==null)return;var json=JsonParser.parseString(encoded);
        observations.add(new ItemStatsAutomation.Observation(location,slot,ItemStatsIdentity.automaticFingerprint(json),ItemStatsIdentity.physicalId(json),stack.getHoverName().getString(),encoded));
    }
    public static void money(double cash,int percentage,long from,long to){sample(Minecraft.getInstance(),true,false);HISTORY.money(cash,percentage,from,to);}
    public static void shards(long count,long at){sample(Minecraft.getInstance(),true,true);HISTORY.shards(count,at);}
    public static void fish(long count,double value,long at){sample(Minecraft.getInstance(),true,true);HISTORY.fish(count,value,at);}
    public static void shutdown(){AUTOMATION.end("Game closed");HISTORY.save(System.currentTimeMillis());}
}
