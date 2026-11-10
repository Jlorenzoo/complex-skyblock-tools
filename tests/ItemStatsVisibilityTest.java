package com.skyblocktoolkit;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.util.*;

public final class ItemStatsVisibilityTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static ItemStatsAutomation.Observation item(String where,String slot,String id){return new ItemStatsAutomation.Observation(where,slot,"fingerprint-"+id,id,id,"{\"id\":\"minecraft:"+id+"\"}");}
    public static void main(String[] args)throws Exception{
        Path directory=Files.createTempDirectory("earning-item-visibility");
        try{
            Path file=directory.resolve("history.json");var h=new ItemStatsHistory(file);var auto=new ItemStatsAutomation(h);Object world=new Object();
            var hoe=item("0","HAND","netherite_hoe");var spear=item("1",null,"wooden_spear");
            auto.observe("s/p",world,ItemStatsLocation.REALM,List.of(hoe,spear),true,1000);
            String hoeId=auto.currentHand();
            auto.observe("s/p",world,ItemStatsLocation.REALM,List.of(hoe,spear),true,5000);h.save(5000);
            check(h.visibleProfiles("s/p",null).isEmpty()&&h.earningSessions()==0,"using/carrying tools without rewards does not add rows or claim recording income");
            check(!Files.exists(file)||JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonArray("profiles").isEmpty(),"empty candidates are not registered on disk");
            h.money(0,0,1000,5000);h.money(Double.NaN,0,1000,5000);h.shards(0,3000);h.fish(0,0,3000);
            check(h.visibleProfiles("s/p",null).isEmpty(),"invalid or zero gains never register an item");
            h.shards(1,4000);
            check(h.visibleProfiles("s/p",null).size()==1&&h.visibleProfiles("s/p",ItemStatsLocation.REALM).getFirst().id.equals(hoeId),"one real shard qualifies the actual hoe even without a measurable cash change");
            check(h.visibleProfiles("s/p",ItemStatsLocation.OVERWORLD).isEmpty(),"area filters do not show zero-data rows from another area");
            check(h.earningSessions()==1&&h.totals(hoeId).elapsed()==4000,"first result retains pre-reward measurement time rather than inflating the rate");
            h.save(5001);var loaded=new ItemStatsHistory(file);
            check(loaded.profiles("s/p").size()==1&&loaded.visibleProfiles("s/p",null).getFirst().id.equals(hoeId),"restart persists earned hoe and omits unused spear");

            var cash=loaded.register("s/p","cash-fingerprint","Pickaxe","HAND","{\"id\":\"minecraft:diamond_pickaxe\"}");
            loaded.start(cash,6000);loaded.tick(8000);loaded.money(50,0,6000,8000);loaded.stop(8000,"Stopped");
            var rod=loaded.register("s/p","fish-fingerprint","Void Rod","HAND","{\"id\":\"minecraft:fishing_rod\"}");
            loaded.start(rod,9000);loaded.tick(10000);loaded.fish(2,0,9500);loaded.stop(10000,"Stopped");
            check(loaded.visibleProfiles("s/p",null).size()==3,"positive cash and confirmed catches also qualify tools, including unpriced fish");
            loaded.start(loaded.profile(hoeId),10500,ItemStatsLocation.REALM);loaded.tick(10800);
            loaded.shards(1,10600);loaded.stop(10800,"Stopped");
            check(loaded.visibleProfiles("s/p",null).stream().map(p->p.id).toList().equals(List.of(hoeId,rod.id,cash.id)),
                "items sort by latest earning session, even when the oldest profile earns again");
            check(loaded.visibleProfiles("s/p",ItemStatsLocation.UNKNOWN).stream().map(p->p.id).toList().equals(List.of(rod.id,cash.id)),
                "area filters use only matching earning sessions");
            // Legacy broad-scan history must disappear from the list, even if old code copied rewards to armor.
            var armor=loaded.register("s/p","armor-fingerprint","Bone Leggings","LEGS","{\"id\":\"minecraft:netherite_leggings\"}");
            loaded.start(armor,11000);loaded.tick(12000);loaded.shards(1,11500);loaded.stop(12000,"Stopped");
            var box=loaded.register("s/p","box-fingerprint","Yellow Shulker Box","Inventory","{\"id\":\"minecraft:yellow_shulker_box\"}");
            loaded.start(box,13000);loaded.tick(14000);loaded.money(100,0,13000,14000);loaded.stop(14000,"Stopped");
            loaded.save(14000);var cleaned=new ItemStatsHistory(file);
            check(cleaned.visibleProfiles("s/p",null).size()==3,"legacy copied rewards cannot keep armor or storage in Stats");
            check(cleaned.profile(armor.id)!=null&&cleaned.profile(box.id)!=null,"legacy records are preserved, not destructively deleted");
            var movement=cleaned.register("s/p","movement-spear-fp","Movement spear","HAND","{\"id\":\"minecraft:wooden_spear\"}");
            cleaned.start(movement,15000);cleaned.tick(17000);
            cleaned.shards(1,16000,ItemStatsEligibility::farming);cleaned.fish(1,50,16000,ItemStatsEligibility::fishing);
            check(!cleaned.hasData(movement.id,null)&&cleaned.visibleProfiles("s/p",null).size()==3,"delayed farming/fishing rewards cannot qualify an unrelated movement weapon");
            for(String id:List.of("minecraft:yellow_shulker_box","minecraft:leather_boots","minecraft:netherite_leggings","minecraft:stick","minecraft:carrot_on_a_stick"))check(!ItemStatsEligibility.tool(id),"unrelated gear/storage/transport is not an earning tool: "+id);
            for(String id:List.of("minecraft:netherite_hoe","minecraft:fishing_rod","minecraft:diamond_pickaxe","minecraft:wooden_spear"))check(ItemStatsEligibility.tool(id),"tools may qualify after actual data: "+id);
            System.out.println("PASS: reward-gated Stats, no empty registrations, correct initial duration, cash/shards/catches, area filtering, held-tool attribution and non-destructive legacy cleanup");
        }finally{try(var paths=Files.walk(directory)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
    }
}
