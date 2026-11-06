package com.skyblocktoolkit;

public final class GoldShardEarningsTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static GoldShardEarnings fresh(){var e=new GoldShardEarnings();e.reset(1000);return e;}
    public static void main(String[] args){
        check(GoldShardEarnings.parseReward("◆ Holy Cultivation XVI ┃ ✔ You found 1x  Gold Shard !")==1,"real enchant message");
        check(GoldShardEarnings.parseReward("§6☠ Withering Plague VIII ┃ ✔ You've found 1x  Gold Shard  with your Holy Cultivation!")==1,"real seasonal variant");
        check(GoldShardEarnings.parseReward("◆ Holy Cultivation XIII ┃ ✔ You found 1,024x  Gold Shard !")==1024,"quantity");
        for(String text:new String[]{null,"", "[Member] Player: ◆ Holy Cultivation XVI ┃ ✔ You found 1x  Gold Shard !", "◆ Gold Fish XX ┃ ✔ You found 1x  Gold Shard !", "◆ Scavenged Shards IX ┃ ✔ You found 1x  Gold Shard !", "You got a  Gold Shard  from the present! Ooo what riches!", "Player received: Gold Shard 171x"})check(GoldShardEarnings.parseReward(text)==0,"unrelated message rejected: "+text);
        var e=fresh();e.observe(100,true,2000,4);check(e.total()==0,"baseline isn't income");
        e.chat(1,3000,4);e.observe(101,true,3100,4);e.flush(7000);check(e.total()==1,"chat then inventory once");
        e=fresh();e.observe(0,true,2000,4);e.observe(5,true,3000,4);e.chat(5,3100,4);e.flush(7000);check(e.total()==5,"inventory then chat once");
        e=fresh();e.observe(0,true,2000,4);e.observe(5,true,3000,4);e.flush(4600);check(e.total()==5,"silent realm reward");e.chat(5,4800,4);check(e.total()==5,"delayed chat reconciles credited inventory");
        check(e.drain().getFirst().testId()==4,"delayed receipt retains sample id");
        e=fresh();e.observe(0,true,2000,4);e.chat(1,3000,4);e.chat(1,3000,4);e.observe(2,true,3100,4);e.flush(7000);check(e.total()==2,"identical real rewards are separate");
        e=fresh();e.observe(100,false,2000,4);e.observe(200,false,3000,4);e.observe(100,true,4000,4);e.observe(100,true,5000,4);e.flush(9000);check(e.total()==0,"transfers, spends and unchanged totals ignored");
        e=fresh();e.observe(0,true,2000,4);e.chat(1,3000,4);e.observe(4,true,3100,4);e.flush(5000);check(e.total()==4,"mixed chat and silent reward batch");
        check(e.perHour(6000)>0&&e.ready(6000),"rolling shard rate");e.reset(7000);check(e.total()==0&&e.perHour(8000)==0,"reconnect clears baseline and earnings");
        e=fresh();e.chat(1,100_000,4); // First drop after a long idle must not become 1,200 shards/h.
        check(Math.abs(e.perHour(105_000)-60)<.001,"one shard in the 60s window is 60/h");
        check(Math.abs(e.perHour(120_000)-60)<.001,"sparse drops do not decay during warmup");
        e.chat(1,130_000,4);check(Math.abs(e.perHour(135_000)-120)<.001,"two shards in the window are 120/h");
        check(Math.abs(e.perHour(165_000)-60)<.001,"old reward expires independently");
        System.out.println("PASS: gold shard parsing, reconciliation, attribution and full-window sparse-drop rates");
    }
}
