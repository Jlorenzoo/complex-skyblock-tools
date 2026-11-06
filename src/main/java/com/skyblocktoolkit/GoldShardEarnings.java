package com.skyblocktoolkit;

import com.moneybooster.RollingEarnings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Reconciles reward chat with inventory receipts, including silent realm drops. */
public final class GoldShardEarnings {
    private static final long MATCH_MS = 3000;
    private static final Pattern REWARD = Pattern.compile(
        "(?i)^[^\\p{L}\\p{N}\\[]*(?:Holy Cultivation [IVXLCDM]+|Withering Plague [IVXLCDM]+)\\s*┃\\s*✔\\s*You(?:'ve)? (?:found|got) ([0-9,]+)x\\s*[^\\p{L}\\p{N}]*Gold Shards?[^\\p{L}\\p{N}]*(?:with your Holy Cultivation!)?[!\\s]*$");
    public record Gain(long count, long at, int testId) {}
    private static final class Receipt {
        long count; final long at; final int testId; boolean counted;
        Receipt(long count,long at,int testId,boolean counted){this.count=count;this.at=at;this.testId=testId;this.counted=counted;}
    }
    private final ArrayDeque<Receipt> chats=new ArrayDeque<>(), inventory=new ArrayDeque<>();
    private final List<Gain> gains=new ArrayList<>();
    private final RollingEarnings rolling=new RollingEarnings();
    private long baseline=-1, total;
    public static long parseReward(String text) {
        if(text==null)return 0;
        var m=REWARD.matcher(text.replaceAll("§.","").strip());
        if(!m.matches())return 0;
        try{return Long.parseLong(m.group(1).replace(",",""));}catch(NumberFormatException e){return 0;}
    }
    public void reset(long now){chats.clear();inventory.clear();gains.clear();rolling.reset(now);baseline=-1;total=0;}
    private void credit(long count,long at,int id){if(count>0){total+=count;rolling.add(count,at-1,at);gains.add(new Gain(count,at,id));}}
    public void chat(long count,long now,int id){
        if(count<=0)return;
        flush(now);
        long remaining=count;
        for(var r:inventory){
            if(now-r.at>MATCH_MS||remaining==0)continue;
            long matched=Math.min(remaining,r.count);
            // Uncounted inventory is confirmed by chat now; already credited drops stay single.
            if(!r.counted)credit(matched,r.at,r.testId);
            r.count-=matched;remaining-=matched;
        }
        if(remaining>0){credit(remaining,now,id);chats.add(new Receipt(remaining,now,id,true));}
    }
    public void observe(long count,boolean farming,long now,int id){
        flush(now);
        if(count<0)return;
        long delta=baseline<0?0:count-baseline;baseline=count;
        if(!farming||delta<=0)return;
        for(var r:chats){
            if(now-r.at>MATCH_MS||delta==0)continue;
            long matched=Math.min(delta,r.count);r.count-=matched;delta-=matched;
        }
        if(delta>0)inventory.add(new Receipt(delta,now,id,false));
    }
    public void flush(long now){
        for(var r:inventory)if(!r.counted&&now-r.at>=1500){credit(r.count,r.at,r.testId);r.counted=true;}
        inventory.removeIf(r->r.count==0||now-r.at>MATCH_MS);
        chats.removeIf(r->r.count==0||now-r.at>MATCH_MS);
    }
    public List<Gain> drain(){var result=List.copyOf(gains);gains.clear();return result;}
    public long total(){return total;}
    public double perHour(long now){return rolling.perHourWindow(now);}
    public boolean ready(long now){return rolling.ready(now);}
}
