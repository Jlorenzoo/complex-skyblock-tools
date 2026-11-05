package com.skyblocktoolkit;

import java.nio.file.*;
import java.util.*;

public final class ItemStatsLocationTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        check(ItemStatsLocation.sidebar(List.of("§6✦ Pharaoh's Realm", "Balance: $1B"))==ItemStatsLocation.REALM,"formatted realm marker");
        check(ItemStatsLocation.sidebar(List.of("Location: PHARAOH’S REALM"))==ItemStatsLocation.REALM,"curly apostrophe and case");
        check(ItemStatsLocation.sidebar(List.of("Complex Skyblock","Balance: $1B"))==ItemStatsLocation.OVERWORLD,"received sidebar without marker is Overworld");
        check(ItemStatsLocation.sidebar(null)==ItemStatsLocation.UNKNOWN&&ItemStatsLocation.sidebar(List.of())==ItemStatsLocation.UNKNOWN,"missing sidebar is unknown, not guessed Overworld");
        check(ItemStatsLocation.sidebar(List.of("Realm shards: 123","Pharaoh's Tomb"))==ItemStatsLocation.OVERWORLD,"unrelated realm/tomb wording");
        Path directory=Files.createTempDirectory("item-stats-locations");Path file=directory.resolve("stats.json");
        try{
            var h=new ItemStatsHistory(file);var auto=new ItemStatsAutomation(h);Object connection=new Object();
            var hoe=new ItemStatsAutomation.Observation("0","HAND","same-config-0001","unique-hoe-0001","Seasonal Hoe",null);
            auto.observe("s/p",connection,ItemStatsLocation.OVERWORLD,List.of(hoe),true,1000);
            String profile=auto.currentHand();auto.observe("s/p",connection,ItemStatsLocation.OVERWORLD,List.of(hoe),true,2000);
            h.money(100,0,1000,2000);h.shards(2,1500);
            auto.observe("s/p",connection,ItemStatsLocation.REALM,List.of(hoe),true,3000);
            check(auto.currentHand().equals(profile)&&h.sessions(profile).size()==2,"realm entry splits session without splitting item identity");
            auto.observe("s/p",connection,ItemStatsLocation.REALM,List.of(hoe),true,5000);
            h.money(600,50,3000,5000);h.shards(12,4000);h.shards(3,1600);
            check(h.totals(profile,ItemStatsLocation.OVERWORLD).shards()==5,"delayed reward retains original Overworld session");
            auto.observe("s/p",connection,ItemStatsLocation.OVERWORLD,List.of(hoe),true,6000);
            h.money(999,0,4990,6100);
            check(h.totals(profile,ItemStatsLocation.REALM).cash()==600&&h.totals(profile,ItemStatsLocation.OVERWORLD).cash()==100,"realm/Overworld cash stays separate; boundary-spanning samples excluded");
            check(h.totals(profile,ItemStatsLocation.REALM).baseCash()==400&&h.totals(profile,ItemStatsLocation.REALM).shards()==12,"realm base/shards");
            check(h.sessions(profile,ItemStatsLocation.REALM).size()==1&&h.sessions(profile,ItemStatsLocation.OVERWORLD).size()==2,"area history filters");
            auto.observe("s/p",connection,ItemStatsLocation.UNKNOWN,List.of(hoe),true,7000);h.save(7000);
            var loaded=new ItemStatsHistory(file);
            check(loaded.totals(profile,ItemStatsLocation.REALM).cash()==600&&loaded.totals(profile,ItemStatsLocation.OVERWORLD).cash()==100,"area labels and totals persist across restart");
            check(loaded.sessions(profile).getFirst().location==ItemStatsLocation.UNKNOWN,"unknown area retained honestly");
            System.out.println("PASS: received sidebar realm detection, Overworld/unknown handling, automatic area splits, delayed receipts, independent totals/filters and restart persistence");
        }finally{try(var paths=Files.walk(directory)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
    }
}
