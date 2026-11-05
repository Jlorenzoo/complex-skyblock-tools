package com.skyblocktoolkit;

import java.util.*;
import java.util.regex.*;

/** Passive server observations; unknown sources never become confirmed chest opens. */
public final class TreasureChestState {
    public static final long PERIOD=2*60*60*1000L;
    public enum Rarity {
        RARE("Rare",64,192), EPIC("Epic",96,256), LEGENDARY("Legendary",128,320), MYTHIC("Mythic",192,384);
        public final String label; public final int min,max;
        Rarity(String label,int min,int max){this.label=label;this.min=min;this.max=max;}
    }
    public static final class Chest {
        public final Rarity rarity; public final String location; public final long at;
        public int confirmed,possible;
        Chest(Rarity rarity,String location,long at){this.rarity=rarity;this.location=location;this.at=at;}
    }
    private static final Pattern SPAWN=Pattern.compile("^A[n]? (Rare|Epic|Legendary|Mythic) Treasure Chest was discovered in (.+)$",Pattern.CASE_INSENSITIVE);
    private static final Pattern OPEN=Pattern.compile("^([A-Za-z0-9_.]{1,32}) opened the (?:(Rare|Epic|Legendary|Mythic) )?Treasure Chest(?: in (PHARAOH'S (?:TOMB|REALM)))? and received: (.+)$",Pattern.CASE_INSENSITIVE);
    private static final Pattern SHARDS=Pattern.compile("Gold Shards?\\s+(\\d+)x",Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME=Pattern.compile("Time:\\s*(?:(\\d+)h\\s*)?(?:(\\d+)m\\s*)?(?:(\\d+)s)?",Pattern.CASE_INSENSITIVE);
    private final List<Chest> chests=new ArrayList<>();
    private long deadline;
    private boolean estimated;
    private String reward="";
    private long rewardAt;
    public void reset(){chests.clear();deadline=0;reward="";rewardAt=0;estimated=false;}
    public static String clean(String s){return s.replaceAll("§.","").replaceAll("[^\\p{L}\\p{N}_.:' /×-]","").strip().replaceAll("\\s+"," ");}
    public static long menuSeconds(String text){
        var m=TIME.matcher(clean(text));if(!m.find()||(m.group(1)==null&&m.group(2)==null&&m.group(3)==null))return -1;
        long seconds=0;for(int i=1;i<=3;i++)if(m.group(i)!=null){try{seconds+=Long.parseLong(m.group(i))*(i==1?3600:i==2?60:1);}catch(NumberFormatException e){return -1;}}
        return seconds<=7200?seconds:-1;
    }
    public void sync(long seconds,long now){if(seconds<0||seconds>7200)return;deadline=now+seconds*1000;estimated=false;}
    public String timer(long now){
        if(deadline==0)return "Treasure · open treasure menu to sync";
        if(now>=deadline){long cycles=(now-deadline)/PERIOD+1;deadline+=cycles*PERIOD;estimated=true;}
        long s=Math.max(0,(deadline-now+999)/1000);
        return "Treasure · "+(estimated?"~":"")+String.format(Locale.ROOT,"%d:%02d:%02d",s/3600,s/60%60,s%60)+(estimated?" estimated":"");
    }
    public void accept(String raw,long now){
        String text=clean(raw);var spawn=SPAWN.matcher(text);
        chests.removeIf(c->now-c.at>=PERIOD);
        if(spawn.matches()){
            Rarity rarity=Rarity.valueOf(spawn.group(1).toUpperCase(Locale.ROOT));String place=spawn.group(2).toUpperCase(Locale.ROOT);
            if(!place.equals("PHARAOH'S TOMB")&&!place.equals("PHARAOH'S REALM"))return;
            String location=place.endsWith("REALM")?"Realm":"Tomb";
            // A discovery is not proof of the global spawn clock. Only menu lore syncs it.
            if(chests.stream().anyMatch(c->c.rarity==rarity&&c.location.equals(location)&&now-c.at<3000))return;
            if(chests.size()==12)chests.removeFirst();chests.add(new Chest(rarity,location,now));return;
        }
        var open=OPEN.matcher(text);if(!open.matches())return;
        EnumSet<Rarity> pools=EnumSet.allOf(Rarity.class);var shards=SHARDS.matcher(open.group(4));
        String detail="Item reward";
        if(shards.find()){
            int amount;try{amount=Integer.parseInt(shards.group(1));}catch(NumberFormatException e){return;}
            pools.removeIf(r->amount<r.min||amount>r.max);detail=amount+" shards";
        }
        if(open.group(2)!=null)pools.retainAll(EnumSet.of(Rarity.valueOf(open.group(2).toUpperCase(Locale.ROOT))));
        String location=open.group(3)==null?null:open.group(3).toUpperCase(Locale.ROOT).endsWith("REALM")?"Realm":"Tomb";
        var candidates=chests.stream().filter(c->c.confirmed<2&&pools.contains(c.rarity)&&(location==null||c.location.equals(location))).toList();
        // All observed chests are not necessarily all server chests. Require an explicit
        // location AND an unambiguous rarity before asserting an individual open.
        boolean certain=location!=null&&pools.size()==1&&candidates.size()==1;
        if(certain){var c=candidates.getFirst();c.confirmed++;c.possible=Math.min(2,Math.max(c.confirmed,c.possible+1));}
        else for(var c:candidates)c.possible=Math.min(2,c.possible+1);
        String names=String.join(" / ",pools.stream().map(r->r.label).toList());
        reward=detail+" · "+(names.isEmpty()?"unknown pool":names)+(certain?"":" · unassigned");rewardAt=now;
    }
    public List<String> lines(long now,int seconds,boolean showChests,boolean infer){
        return lines(now,seconds,showChests,infer,false);
    }
    public List<String> lines(long now,int seconds,boolean showChests,boolean infer,boolean lastThirtySeconds){
        // Advance estimated cycles even while the countdown is hidden.
        String countdown=timer(now);
        List<String> result=new ArrayList<>();
        if(!lastThirtySeconds||(deadline!=0&&deadline-now<=30_000))result.add(countdown);
        int timerRows=result.size();
        if(showChests){
            for(var c:chests)if(now-c.at<seconds*1000L){
                String count=infer&&c.possible>c.confirmed?c.confirmed+"–"+Math.max(c.confirmed,c.possible):Integer.toString(c.confirmed);
                result.add(c.rarity.label+" · "+c.location+" · "+count+"/2 observed");
            }
            if(infer&&!reward.isEmpty()&&now-rewardAt<seconds*1000L&&result.size()>timerRows){
                String[] parts=reward.split(" · ",3);result.add(parts[0]+(reward.endsWith("unassigned")?" · unassigned":""));
                if(parts.length>1)result.add("Pool: "+parts[1]);
            }
        }
        return result;
    }
}
