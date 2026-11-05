package com.skyblocktoolkit;

public final class TreasureChestStateTest {
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static void spawn(TreasureChestState s,String rarity,long t){s.accept("  A "+rarity+" Treasure Chest was discovered in PHARAOH'S TOMB ",t);}
    private static void reward(TreasureChestState s,int n,long t){s.accept(" Nciodsnf opened the Treasure Chest and received:  Gold Shard  "+n+"x ",t);}
    private static String rows(TreasureChestState s,long t){return String.join("\n",s.lines(t,120,true,true));}
    public static void main(String[] args){
        var s=new TreasureChestState();
        check(TreasureChestState.menuSeconds("§b✦ Time: 55m 37s")==3337,"screenshot timer");
        check(TreasureChestState.menuSeconds("Time: 1h 59m 59s")==7199,"hours");
        check(TreasureChestState.menuSeconds("Time: 0s")==0,"zero deadline");
        check(TreasureChestState.menuSeconds("Time: ")==-1&&TreasureChestState.menuSeconds("Time: 3h")==-1,"invalid lore");
        spawn(s,"Rare",1000);check(rows(s,2000).contains("open treasure menu"),"discoveries never invent clock");
        check(rows(s,2000).contains("Rare · Tomb · 0/2 observed"),"spawn list");
        s.accept("[Pharaoh] Player: A Mythic Treasure Chest was discovered in PHARAOH'S TOMB",2000);
        s.accept("[Pharaoh] Player: Nciodsnf opened the Treasure Chest and received: Gold Shard 66x",2000);
        check(!rows(s,2000).contains("Mythic")&&!rows(s,2000).contains("unassigned"),"player chat excluded");
        for(String rarity:new String[]{"Epic","Legendary","Mythic"})spawn(s,rarity,3000);
        reward(s,203,4000);
        check(rows(s,4000).contains("Epic · Tomb · 0–1/2")&&rows(s,4000).contains("Legendary · Tomb · 0–1/2")&&rows(s,4000).contains("Mythic · Tomb · 0–1/2"),"overlapping pools preserve ambiguity");
        check(rows(s,4000).contains("Rare · Tomb · 0/2"),"203 excludes rare");
        reward(s,380,5000);check(rows(s,5000).contains("Mythic · Tomb · 0–2/2"),"unique rarity still may be unseen chest");
        reward(s,380,6000);check(!rows(s,6000).contains("3/2"),"two open cap");
        s.accept("Pilot opened the Rare Treasure Chest in PHARAOH'S TOMB and received: Gold Shard 64x",7000);
        check(rows(s,7000).contains("Rare · Tomb · 1/2"),"explicit source confirms open");
        s.accept("Pilot opened the Rare Treasure Chest in PHARAOH'S TOMB and received: Gold Shard 64x",8000);
        check(rows(s,8000).contains("Rare · Tomb · 2/2"),"identical rewards can be two real opens");
        check(s.lines(9000,120,true,false).stream().noneMatch(l->l.contains("–")||l.contains("Pool:")),"disable inference");
        check(s.lines(9000,120,false,true).size()==1,"hide details independently");
        check(s.lines(123000,120,true,true).size()==1,"details expire from discovery, not latest reward");
        s.sync(3337,10000);check(s.timer(10000).contains("0:55:37"),"authoritative sync");
        check(s.timer(3347000).contains("~2:00:00"),"subsequent period explicitly estimated");
        s.reset();check(rows(s,999999).contains("open treasure menu")&&s.lines(999999,120,true,true).size()==1,"disconnect clears stale clock and chest state");
        check(s.lines(1000,120,false,true,true).isEmpty(),"unsynced countdown hidden in final-30 mode");
        s.sync(31,1000);
        check(s.lines(1999,120,false,true,true).isEmpty(),"hidden immediately before 30 seconds");
        check(s.lines(2000,120,false,true,true).getFirst().contains("0:00:30"),"visible at exactly 30 seconds");
        check(s.lines(31999,120,false,true,true).getFirst().contains("0:00:01"),"visible until spawn");
        check(s.lines(32000,120,false,true,true).isEmpty(),"hidden again at cycle rollover");
        check(s.lines(7202000,120,false,true,true).getFirst().contains("~0:00:30"),"estimated cycles also respect threshold");
        s.reset();s.sync(300,1000);spawn(s,"Rare",1000);reward(s,64,1500);
        var details=s.lines(2000,120,true,true,true);
        check(details.size()==3&&details.getFirst().startsWith("Rare")&&details.get(1).contains("unassigned"),"discovery and reward details remain independent of hidden timer");
        check(s.lines(2000,120,true,true,false).getFirst().startsWith("Treasure"),"disabling preference restores countdown");
        System.out.println("PASS: treasure menu synchronization, observed broadcasts, spoof rejection, reward ambiguity, two-open cap, expiry and disconnect");
    }
}
