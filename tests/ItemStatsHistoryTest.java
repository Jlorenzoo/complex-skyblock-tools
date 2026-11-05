package com.skyblocktoolkit;

import com.google.gson.JsonParser;
import java.nio.file.*;

public final class ItemStatsHistoryTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void close(double expected,double actual,String message){check(Math.abs(expected-actual)<0.001,message+": "+actual);}
    public static void main(String[] args)throws Exception {
        Path directory=Files.createTempDirectory("item-stats-test");
        Path file=directory.resolve("stats.json");
        try {
            var h=new ItemStatsHistory(file);
            var first=h.register("server/player","tier1-enchant1","Seasonal Hoe","HAND","{}");
            var duplicate=h.register("server/player","tier1-enchant1","Seasonal Hoe","HAND","{}");
            var upgraded=h.register("server/player","tier2-enchant2","Seasonal Hoe","HAND","{}");
            h.register("other/player","tier1-enchant1","Other","HAND","{}");
            check(!first.id.equals(duplicate.id),"identical-looking physical items never merged");
            check(h.profiles("server/player").size()==3,"server/account isolation");
            h.rename(first,"My farming hoe");
            var one=h.start(first,1000);h.tick(61000);
            h.money(300,50,1001,60000);h.shards(12,10000);h.fish(4,80,20000);
            h.money(Double.NaN,0,1001,60000);h.money(-1,0,1001,60000);
            h.money(999,0,999,2000); // spanning start is ambiguous
            h.stop(61000,"Stopped");
            h.money(150,50,60000,61000);h.shards(3,60999); // late delivery uses observation time
            var two=h.start(duplicate,62000);h.tick(122000);
            h.money(999,0,60000,63000); // spanning items cannot be assigned
            h.money(50,0,62001,100000);h.shards(2,63000);
            h.stop(122000,"Switched item");
            h.start(first,130000);h.tick(310000);h.money(900,50,130001,309999);h.stop(310000,"Stopped");
            var totals=h.totals(first.id);
            check(totals.sessions()==2&&totals.elapsed()==240000,"duration includes zero-reward/idle time");
            close(1350,totals.cash(),"cash totals");close(900,totals.baseCash(),"per-receipt booster normalization");
            close(20250,totals.hourly(totals.cash()),"duration-weighted average, not mean of session rates");
            check(totals.shards()==15&&totals.fish()==4,"delayed shards and fishing counts");close(80,totals.fishValue(),"fish estimate separate from cash");
            close(50,h.totals(duplicate.id).cash(),"separate identical-item cash");
            close(0,h.totals(upgraded.id).cash(),"upgrades have separate history");
            h.start(upgraded,400000);h.tick(410000);h.shards(1,409999);h.save(410000);
            var reloaded=new ItemStatsHistory(file);
            check(reloaded.active()==null,"never resume a session on restart");
            var interrupted=reloaded.sessions(upgraded.id).getFirst();
            check(interrupted.ended==410000&&interrupted.status.equals("Interrupted"),"restart closes at last observation, not now");
            check(reloaded.profile(first.id).label.equals("My farming hoe"),"labels survive restart");
            close(1350,reloaded.totals(first.id).cash(),"cash survives restart");
            Files.writeString(file,"broken original data");
            var corrupt=new ItemStatsHistory(file);corrupt.tick(1000000);corrupt.save(1000000);
            check(!corrupt.error().isEmpty()&&Files.readString(file).equals("broken original data"),"preserve unreadable file");
            String base="{\"id\":\"minecraft:diamond_hoe\",\"count\":1,\"components\":{\"minecraft:damage\":1,\"minecraft:custom_name\":\"Tier 1\",\"minecraft:enchantments\":{\"a\":1,\"b\":2},\"minecraft:custom_data\":{\"uuid\":\"one\"}}}";
            String fingerprint=ItemStatsIdentity.fingerprint(JsonParser.parseString(base));
            check(fingerprint.equals(ItemStatsIdentity.fingerprint(JsonParser.parseString(base.replace("\"count\":1","\"count\":2").replace("\"damage\":1","\"damage\":5").replace("\"minecraft:damage\":1","\"minecraft:damage\":5").replace("\"a\":1,\"b\":2","\"b\":2,\"a\":1")))),"durability/count/order do not change configuration");
            for(String change:new String[]{base.replace("Tier 1","Tier 2"),base.replace("\"a\":1","\"a\":2"),base.replace("one","two")})
                check(!fingerprint.equals(ItemStatsIdentity.fingerprint(JsonParser.parseString(change))),"tiers, enchants and server item IDs separate");
            System.out.println("PASS: individual profiles, upgrade fingerprints, delayed/boundary receipts, weighted rates, booster normalization, shards/fish, scope isolation, restart and safe persistence");
        } finally {try(var paths=Files.walk(directory)){for(var p:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(p);}}
    }
}
