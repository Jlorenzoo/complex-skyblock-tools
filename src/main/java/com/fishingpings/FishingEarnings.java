package com.fishingpings;
import java.util.*;
import java.util.regex.*;
/** Actual caught-item counts, valued using an expected fish mix; never bank income. */
public final class FishingEarnings {
    private static final Pattern CATCH=Pattern.compile("(?i)^[^\\p{L}\\p{N}]*What Lurks Below\\s+[IVXLCDM]+\\s*[┃|]\\s*[✔✓]?\\s*Caught\\s+(\\d{1,6})\\s+items?[!.]?$");
    private record Catch(long time,int count){}
    private final ArrayDeque<Catch> catches=new ArrayDeque<>();
    private long observedSince=-1,lastCatch=-1,lastMessage=-1;
    private int lastCount;
    private long sessionFish;
    public static int caught(String text){
        if(text==null)return 0;var m=CATCH.matcher(text.replaceAll("§.","").strip());
        if(!m.matches())return 0;
        try{int n=Integer.parseInt(m.group(1));return n>0&&n<=100000?n:0;}catch(NumberFormatException e){return 0;}
    }
    public static double weightedPrice(double cod,double salmon,double puffer,double tropical){
        for(double v:new double[]{cod,salmon,puffer,tropical})if(!Double.isFinite(v)||v<0)throw new IllegalArgumentException("Fish prices must be finite and non-negative");
        return cod*.60+salmon*.25+puffer*.13+tropical*.02;
    }
    /** Dollar fields accept plain numbers, grouped commas and k/m/b suffixes. */
    public static double price(String text){
        if(text==null)return Double.NaN;String s=text.strip().replace("$","").replace(",","");
        if(!s.matches("(?i)(?:\\d+(?:\\.\\d*)?|\\.\\d+)\\s*[kmb]?"))return Double.NaN;
        double multiplier=1;char end=s.charAt(s.length()-1);
        if(Character.isLetter(end)){multiplier=switch(Character.toLowerCase(end)){case 'k'->1e3;case 'm'->1e6;case 'b'->1e9;default->1;};s=s.substring(0,s.length()-1).strip();}
        try{double v=Double.parseDouble(s)*multiplier;return Double.isFinite(v)&&v>=0&&v<=1e12?v:Double.NaN;}catch(NumberFormatException e){return Double.NaN;}
    }
    public void observe(long now){if(observedSince<0)observedSince=now;}
    public boolean accept(String text,long now){
        int n=caught(text);if(n==0)return false;
        if(lastMessage>=0&&now-lastMessage<500&&lastCount==n)return false;
        observe(now);lastMessage=lastCatch=now;lastCount=n;sessionFish+=n;catches.addLast(new Catch(now,n));prune(now);return true;
    }
    private void prune(long now){while(!catches.isEmpty()&&catches.peekFirst().time()<=now-300000)catches.removeFirst();}
    public long count(long now,int seconds){prune(now);long total=0;for(var c:catches)if(c.time()>now-seconds*1000L&&c.time()<=now)total+=c.count();return total;}
    public double value(long now,int seconds,double averagePrice){return count(now,seconds)*averagePrice;}
    public double hourly(long now,int seconds,double averagePrice){if(seconds!=60&&seconds!=300)throw new IllegalArgumentException("Window must be 60s or 300s");return value(now,seconds,averagePrice)*3600/seconds;}
    public boolean recent(long now,int seconds){return lastCatch>=0&&now-lastCatch<seconds*1000L;}
    public boolean warming(long now,int seconds){return observedSince<0||now-observedSince<seconds*1000L;}
    public long sessionFish(){return sessionFish;}
    public void reset(){catches.clear();observedSince=lastCatch=lastMessage=-1;lastCount=0;sessionFish=0;}
}
