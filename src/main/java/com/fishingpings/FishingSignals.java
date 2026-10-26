package com.fishingpings;
import java.util.UUID;
import java.util.regex.*;
/** Server text parser and per-cast/cooldown state, independent of Minecraft. */
public final class FishingSignals {
    private static final Pattern DEPTH=Pattern.compile("(?i)\\bDepth\\s*:?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:blocks?\\b|/\\s*\\d+(?:\\.\\d+)?)");
    private static final String LOW="You sense that there might not be many fish left in this area. Try fishing at least 3 blocks away.";
    private static final String EMPTY="This area is suffering from overfishing, cast your rod in a different spot for more fish. At least 3 blocks away.";
    private static final Pattern SNAP=Pattern.compile("(?i)^[^\\p{L}\\p{N}]*What Lurks Below\\s+[IVXLCDM]+\\s*[┃|]\\s*[✘✗×]?\\s*The fishing line snapped\\.?$");
    private long lastSnap=Long.MIN_VALUE;
    private UUID cast;private boolean notified,sawBelowTarget;private long castSince,lastWarning=Long.MIN_VALUE;
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
        return name!=null && name.replaceAll("§.","").strip().equalsIgnoreCase("Void Rod");
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
    public void reset(){cast=null;notified=false;sawBelowTarget=false;lastWarning=lastSnap=Long.MIN_VALUE;}
}
