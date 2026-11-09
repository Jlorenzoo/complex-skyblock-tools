package com.fishingpings;
import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.*;
/** Server text parser and per-cast/cooldown state, independent of Minecraft. */
public final class FishingSignals {
    private static final Pattern DEPTH=Pattern.compile("(?i)\\bDepth\\s*:?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:blocks?\\b|/\\s*\\d+(?:\\.\\d+)?)");
    private static final String LOW="You sense that there might not be many fish left in this area. Try fishing at least 3 blocks away.";
    private static final String EMPTY="This area is suffering from overfishing, cast your rod in a different spot for more fish. At least 3 blocks away.";
    private static final Pattern SNAP=Pattern.compile("(?i)^[^\\p{L}\\p{N}]*What Lurks Below\\s+[IVXLCDM]+\\s*[┃|]\\s*[✘✗×]?\\s*The fishing line snapped\\.?$");
    private static final Pattern ONI_ROD=Pattern.compile("(?i)^[^\\p{L}\\p{N}]*Oni(?:['’]s)? (?:Fishing )?Rod[^\\p{L}\\p{N}]*$");
    private static final Pattern CURSED_ENERGY=Pattern.compile("(?iU)\\bCursed\\s+Energy\\s*:\\s*([0-9,]+)\\s*/\\s*([0-9,]+)(?![0-9,])");
    private static final Pattern BAR_FORMATTING=Pattern.compile("§.|\\p{Cf}");
    private static final Pattern ONI_SHARDS=Pattern.compile(
        "(?i)^[^\\p{L}\\p{N}\\[]*(?:Cursed Spirits|Gold Fish) [IVXLCDM]+\\s*┃\\s*✔\\s*You found ([0-9,]+)x\\s*[^\\p{L}\\p{N}]*Gold Shards?[^\\p{L}\\p{N}]*$");
    private long lastSnap=Long.MIN_VALUE;
    private UUID cast;private boolean notified,sawBelowTarget;private long castSince,lastWarning=Long.MIN_VALUE;
    private Energy previousEnergy;
    private long chargeStep;
    private OniCue lastOniCue=OniCue.NONE;
    private long oniBarMissingSince=-1;
    public record Energy(long current,long maximum) {
        public boolean full(){return current==maximum;}
    }
    public enum OniCue { NONE, TWO_AWAY, ONE_AWAY, FULL }
    public static boolean oniRod(String name){
        return name!=null&&ONI_ROD.matcher(name.replaceAll("§.","")).matches();
    }
    public static long oniShardReward(String text){
        if(text==null)return 0;
        var match=ONI_SHARDS.matcher(text.replaceAll("§.","").strip());
        if(!match.matches())return 0;
        try{return Long.parseLong(match.group(1).replace(",",""));}
        catch(NumberFormatException ignored){return 0;}
    }
    public static Energy cursedEnergy(String text){
        if(text==null)return null;
        String clean=BAR_FORMATTING.matcher(text).replaceAll("");
        var match=CURSED_ENERGY.matcher(Normalizer.normalize(clean,Normalizer.Form.NFKC));
        if(!match.find())return null;
        try{
            long current=Long.parseLong(match.group(1).replace(",",""));
            long maximum=Long.parseLong(match.group(2).replace(",",""));
            if(maximum<=0||current>maximum)return null;
            return new Energy(current,maximum);
        }catch(NumberFormatException ignored){return null;}
    }
    public OniCue oniCue(Energy energy,long now){
        if(energy==null){
            if(oniBarMissingSince<0)oniBarMissingSince=now;
            return OniCue.NONE;
        }
        if((oniBarMissingSince>=0&&now-oniBarMissingSince>=1000)
            ||(previousEnergy!=null&&previousEnergy.maximum()!=energy.maximum()))resetOni();
        oniBarMissingSince=-1;
        Energy previous=previousEnergy;
        previousEnergy=energy;
        if(energy.equals(previous))return OniCue.NONE;
        if(energy.full()){
            lastOniCue=OniCue.FULL;
            return lastOniCue;
        }
        if(previous==null)return OniCue.NONE;
        long increase=energy.current()-previous.current();
        if(increase<0){
            lastOniCue=OniCue.NONE;
            return OniCue.NONE;
        }
        // Ignore the final, potentially capped gain. Smaller observed steps are
        // safer than a jump that may combine more than one bar update.
        chargeStep=chargeStep==0?increase:Math.min(chargeStep,increase);
        long catches=1+(energy.maximum()-energy.current()-1)/chargeStep;
        OniCue cue=catches==1?OniCue.ONE_AWAY:catches==2?OniCue.TWO_AWAY:OniCue.NONE;
        if(cue.ordinal()<=lastOniCue.ordinal())return OniCue.NONE;
        lastOniCue=cue;
        return cue;
    }
    private void resetOni(){
        previousEnergy=null;
        chargeStep=0;
        lastOniCue=OniCue.NONE;
        oniBarMissingSince=-1;
    }
    public static double depth(String text){
        if(text==null)return Double.NaN;
        Matcher m=DEPTH.matcher(text.replaceAll("§.",""));
        if(!m.find())return Double.NaN;
        try{double value=Double.parseDouble(m.group(1));return Double.isFinite(value)&&value>=0&&value<=10000?value:Double.NaN;}catch(NumberFormatException e){return Double.NaN;}
    }
    public static boolean depleted(String text){
        if(text==null)return false;String clean=text.replaceAll("§.","").strip().replaceAll("\\s+"," ");
        return LOW.equalsIgnoreCase(clean)||EMPTY.equalsIgnoreCase(clean);
    }
    public static boolean voidRod(String name){
        if(name==null)return false;
        String clean=name.replaceAll("§.","").strip();
        // The server's seasonal item is "֎ Void Fishing Rod ֎". Keep the
        // short legacy name too; both cues use this same held-item guard.
        return clean.matches("(?i)^[֎✦★\\[(]?\\s*Void (?:Fishing )?Rod\\s*[\\])★✦֎]?$");
    }
    public static boolean snappedText(String text){
        if(text==null)return false;String clean=text.replaceAll("§.","").strip();
        return clean.equalsIgnoreCase("The fishing line snapped.")||SNAP.matcher(clean).matches();
    }
    public boolean snapped(String text,long now){
        // Null is a successful boss-bar scan with no depth meter for this cast.
        // A negative castSince marks that a depth meter has been observed.
        // Negative lastSnap marks an inferred failure, retained until the next cast
        // so a delayed server confirmation cannot repeat its cue.
        if(text==null){
            if(cast==null||notified||castSince<0||now-castSince<1500)return false;
            notified=true;lastSnap=-(now+1);return true;
        }
        if(!snappedText(text)||(lastSnap!=Long.MIN_VALUE&&(lastSnap<0||now-lastSnap<2000)))return false;
        lastSnap=now;return true;
    }
    public void cast(UUID id,long now){if(id==null){cast=null;notified=false;sawBelowTarget=false;return;}if(!id.equals(cast)){cast=id;notified=false;sawBelowTarget=false;castSince=now;if(lastSnap<0&&lastSnap!=Long.MIN_VALUE)lastSnap=Long.MIN_VALUE;}}
    public boolean ready(UUID id,double depth,double target,long now){
        cast(id,now);
        if(cast!=null&&Double.isFinite(depth)){if(castSince>=0)castSince=-(castSince+1);if(depth<target)sawBelowTarget=true;}
        if(cast==null||notified||!sawBelowTarget||now-(castSince<0?-castSince-1:castSince)<750||!Double.isFinite(depth)||depth<target)return false;
        notified=true;return true;
    }
    public boolean warning(String text,long now){
        if(!depleted(text)||(lastWarning!=Long.MIN_VALUE&&now-lastWarning<15000))return false;
        lastWarning=now;return true;
    }
    public void reset(){cast=null;notified=false;sawBelowTarget=false;resetOni();lastWarning=lastSnap=Long.MIN_VALUE;}
}
