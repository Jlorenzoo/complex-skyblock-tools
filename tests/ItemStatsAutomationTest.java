package com.skyblocktoolkit;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.util.*;

public final class ItemStatsAutomationTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static ItemStatsAutomation.Observation item(String location,String slot,String fingerprint,String id){return new ItemStatsAutomation.Observation(location,slot,fingerprint,id,"Seasonal Hoe",null);}
    public static void main(String[] args)throws Exception{
        Path directory=Files.createTempDirectory("automatic-item-stats");
        try{
            Path file=directory.resolve("stats.json");var h=new ItemStatsHistory(file);var a=new ItemStatsAutomation(h);Object world=new Object();
            var hoe=item("0","HAND","same-config-0001",null);var duplicate=item("1",null,"same-config-0001",null);
            a.observe("server/player",world,List.of(hoe,duplicate),false,1000);
            check(h.profiles("server/player").size()==2&&h.active()==null,"automatically recognize separate identical inventory items without starting idle sessions");
            String first=a.currentHand();String second=h.profiles("server/player").stream().filter(p->!p.id.equals(first)).findFirst().orElseThrow().id;
            a.observe("server/player",world,List.of(hoe,duplicate),true,2000);
            a.observe("server/player",world,List.of(hoe,duplicate),true,5000);h.money(100,0,2000,5000);h.shards(2,4000);
            a.observe("server/player",world,List.of(item("0",null,"same-config-0001",null),item("1","HAND","same-config-0001",null)),true,6000);
            check(a.currentHand().equals(second)&&h.recording(second)&&!h.recording(first),"hotbar switching automatically changes physical-item history");
            a.observe("server/player",world,List.of(item("8","HAND","same-config-0001",null),duplicate),true,7000);
            check(a.currentHand().equals(first),"follow a moved item after reserving an unchanged identical duplicate");
            a.observe("server/player",world,List.of(item("8","HAND","same-config-0001",null),duplicate),false,38000);
            check(h.active()==null&&h.sessions(first).getFirst().status.equals("Paused while idle"),"automatically pause after inactivity");
            h.save(38000);h=new ItemStatsHistory(file);a=new ItemStatsAutomation(h);world=new Object();
            a.observe("server/player",world,List.of(item("8","HAND","same-config-0001",null),duplicate),true,40000);
            check(a.currentHand().equals(first)&&h.profiles("server/player").size()==2&&h.totals(first).cash()==100,"restart recognizes same physical occurrence and preserves earnings");
            a.observe("server/player",world,List.of(item("8","HAND","enchanted-config-2",null),duplicate),true,41000);
            String upgrade=a.currentHand();check(!upgrade.equals(first)&&h.totals(upgrade).cash()==0,"new enchant/tier creates a separate automatic history");
            a.observe("server/player",null,List.of(),false,42000);check(h.active()==null,"disconnect closes all automatic sessions");
            a.observe("another/player",new Object(),List.of(hoe),true,43000);check(!a.currentHand().equals(first),"account/server scopes stay separate");

            var ids=new ItemStatsHistory(directory.resolve("ids.json"));var b=new ItemStatsAutomation(ids);Object connection=new Object();
            b.observe("s/p",connection,List.of(item("0","HAND","same-config-0001","server-id-one"),item("1",null,"same-config-0001","server-id-two")),true,1000);
            String unique=b.currentHand();
            b.observe("s/p",connection,List.of(item("1","HAND","same-config-0001","server-id-one"),item("0",null,"same-config-0001","server-id-two"),item("37","LEGS","bone-leggings-01","server-id-legs")),true,2000);
            check(b.currentHand().equals(unique)&&ids.activeSessions().size()==2,"server IDs survive identical-item swaps and equipped seasonal gear records concurrently");
            b.observe("s/p",connection,List.of(item("1","HAND","same-config-0001","server-id-one"),item("37","LEGS","bone-leggings-01","server-id-legs")),true,3000);
            ids.money(50,0,2000,3000);ids.shards(3,3000);
            for(var session:ids.activeSessions())check(session.cash==50&&session.shards==3,"equipped item histories each describe the same observed activity, without summing across items");

            String raw="{\"id\":\"minecraft:diamond_hoe\",\"components\":{\"minecraft:custom_data\":{\"PublicBukkitValues\":{\"plugin:item_uuid\":\"01234567-uuid-one\",\"plugin:crops_harvested\":123},\"owner\":{\"uuid\":\"owner-12345678\"}},\"minecraft:lore\":[\"Holy Cultivation IV\",\"Crops harvested: 123\"]}}";
            var encoded=JsonParser.parseString(raw);String stable=ItemStatsIdentity.automaticFingerprint(encoded);
            check(stable.equals(ItemStatsIdentity.automaticFingerprint(JsonParser.parseString(raw.replace(":123",":999").replace("harvested: 123","harvested: 999")))),"counter custom data and counter lore do not fragment item history");
            check(!stable.equals(ItemStatsIdentity.automaticFingerprint(JsonParser.parseString(raw.replace("Cultivation IV","Cultivation V")))),"enchants remain configuration identity");
            check(ItemStatsIdentity.automaticFingerprint(JsonParser.parseString("{\"id\":\"minecraft:diamond_hoe\"}"))
                .equals(ItemStatsIdentity.automaticFingerprint(JsonParser.parseString("{\"id\":\"minecraft:diamond_hoe\",\"components\":{\"minecraft:damage\":1}}"))),"first durability component does not split a plain tool");
            check(ItemStatsIdentity.physicalId(encoded).contains("01234567-uuid-one")&&!ItemStatsIdentity.physicalId(encoded).contains("owner"),"nested server item UUID recognized without confusing owner UUID");
            var legacy=new ItemStatsHistory(directory.resolve("legacy.json"));var old=legacy.register("s/p",ItemStatsIdentity.fingerprint(encoded),"My Hoe","HAND",raw);
            var migrate=new ItemStatsAutomation(legacy);migrate.observe("s/p",new Object(),List.of(new ItemStatsAutomation.Observation("0","HAND",stable,ItemStatsIdentity.physicalId(encoded),"Hoe",raw)),true,1000);
            check(migrate.currentHand().equals(old.id)&&old.automatic,"existing manual history migrates automatically");
            migrate.observe("s/p",null,List.of(),false,2000);migrate.observe("s/p",new Object(),List.of(new ItemStatsAutomation.Observation("5","HAND",stable,ItemStatsIdentity.physicalId(encoded),"Hoe",raw)),true,3000);
            check(migrate.currentHand().equals(old.id),"migrated history stays recognizable after reconnect");
            System.out.println("PASS: zero-setup recognition, identical inventory instances, switching, moves, restart, upgrades, idle/disconnect, scope isolation, server IDs, equipped seasonals, counters and manual-history migration");
        }finally{try(var paths=Files.walk(directory)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
    }
}
