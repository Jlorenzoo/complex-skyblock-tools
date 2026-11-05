package com.skyblocktoolkit;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Minecraft adapter. Selecting a profile is explicit because some servers expose no individual item ID. */
public final class ItemStatsTracker {
    public enum Slot {
        HAND("Main hand",EquipmentSlot.MAINHAND), OFFHAND("Off hand",EquipmentSlot.OFFHAND),
        HEAD("Head",EquipmentSlot.HEAD), CHEST("Chest",EquipmentSlot.CHEST), LEGS("Legs",EquipmentSlot.LEGS), FEET("Feet",EquipmentSlot.FEET);
        public final String label;final EquipmentSlot equipment;
        Slot(String label,EquipmentSlot equipment){this.label=label;this.equipment=equipment;}
    }
    private static final ItemStatsHistory HISTORY=new ItemStatsHistory(FabricLoader.getInstance().getConfigDir().resolve("skyblocktoolkit-item-stats.json"));
    private static Object level;
    private static int handSlot;
    private static String message="";
    private static long checkedAt;
    private ItemStatsTracker(){}
    public static ItemStatsHistory history(){return HISTORY;}
    public static String message(){return HISTORY.error().isEmpty()?message:HISTORY.error();}
    public static String scope(){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return "";
        String server=mc.getCurrentServer()!=null?mc.getCurrentServer().ip:mc.getSingleplayerServer()!=null?mc.getSingleplayerServer().getWorldData().getLevelName():"offline";
        return server.toLowerCase(java.util.Locale.ROOT)+"/"+mc.player.getUUID();
    }
    public static ItemStack stack(Slot slot){var mc=Minecraft.getInstance();return mc.player==null?ItemStack.EMPTY:mc.player.getItemBySlot(slot.equipment);}
    public static String encode(ItemStack stack){
        var mc=Minecraft.getInstance();
        if(mc.level==null||stack.isEmpty())return null;
        return ItemStack.CODEC.encodeStart(mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE),stack.copyWithCount(1)).result().map(Object::toString).orElse(null);
    }
    public static ItemStack icon(ItemStatsHistory.Profile p){
        try{var mc=Minecraft.getInstance();if(mc.level!=null&&p.icon!=null)return ItemStack.CODEC.parse(mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(p.icon)).result().orElse(ItemStack.EMPTY);}catch(Exception ignored){}
        return ItemStack.EMPTY;
    }
    public static ItemStatsHistory.Profile register(Slot slot){
        try{
            String encoded=encode(stack(slot));
            if(encoded==null){message="Join a world and equip the item first.";return null;}
            var p=HISTORY.register(scope(),ItemStatsIdentity.fingerprint(JsonParser.parseString(encoded)),stack(slot).getHoverName().getString(),slot.name(),encoded);
            HISTORY.save(System.currentTimeMillis());message="Registered separately. Give this item a recognizable label.";return p;
        }catch(Exception e){message="Could not register item: "+e.getMessage();return null;}
    }
    private static boolean matches(ItemStatsHistory.Profile p){
        try{String encoded=encode(stack(Slot.valueOf(p.slot)));return p.scope.equals(scope())&&encoded!=null&&p.fingerprint.equals(ItemStatsIdentity.fingerprint(JsonParser.parseString(encoded)));}
        catch(Exception e){return false;}
    }
    public static void start(ItemStatsHistory.Profile p){
        tick(Minecraft.getInstance());
        if(HISTORY.active()!=null){message="Stop the current session first.";return;}
        if(!matches(p)){message="Equip the registered configuration, or register its upgrade separately.";return;}
        long now=System.currentTimeMillis();
        HISTORY.start(p,now);level=Minecraft.getInstance().level;handSlot=Minecraft.getInstance().player.getInventory().getSelectedSlot();
        com.moneybooster.BoosterStatsManager.markContext();HISTORY.save(now);message="Recording. Stop when finished; idle time counts.";
    }
    public static void stop(){
        long now=System.currentTimeMillis();HISTORY.stop(now,"Stopped");HISTORY.save(now);message="Session saved. Recent cash and shards may take a few seconds to settle.";
    }
    public static void tick(Minecraft mc){tick(mc,false);}
    private static void tick(Minecraft mc,boolean force){
        long now=System.currentTimeMillis();var p=HISTORY.activeProfile();
        if(p!=null){
            String reason=null;
            if(mc.level!=level||mc.player==null)reason="Disconnected / changed world";
            else if(p.slot.equals("HAND")&&mc.player.getInventory().getSelectedSlot()!=handSlot)reason="Switched item";
            else if(force||now-checkedAt>=250){checkedAt=now;if(!matches(p))reason="Item removed or changed";}
            if(reason!=null){HISTORY.stop(HISTORY.active().lastSeen,reason);HISTORY.save(now);message=reason+"; session saved.";}
        }
        HISTORY.tick(now);
    }
    public static void money(double cash,int percentage,long from,long to){tick(Minecraft.getInstance(),true);HISTORY.money(cash,percentage,from,to);}
    public static void shards(long count,long at){tick(Minecraft.getInstance(),true);HISTORY.shards(count,at);}
    public static void fish(long count,double value,long at){tick(Minecraft.getInstance(),true);HISTORY.fish(count,value,at);}
    public static void shutdown(){long now=System.currentTimeMillis();if(HISTORY.active()!=null)HISTORY.stop(HISTORY.active().lastSeen,"Game closed");HISTORY.save(now);}
}
