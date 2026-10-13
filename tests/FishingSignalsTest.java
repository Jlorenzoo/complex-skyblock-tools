package com.fishingpings;
import java.util.UUID;
public final class FishingSignalsTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(FishingSignals.depth("֎ What Lurks Below IX - Depth 1.5 blocks")==1.5,"Screenshot format");
        check(FishingSignals.depth("§5What Lurks Below IX §f- Depth 30 blocks")==30,"Formatting");
        check(FishingSignals.depth("Depth: 30/30")==30,"Fraction fallback");
        check(Double.isNaN(FishingSignals.depth("Money booster 30/30")),"Unrelated numbers");
        check(Double.isNaN(FishingSignals.depth("Depth NaN blocks")),"Invalid numeric value");
        var s=new FishingSignals();UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        check(!s.ready(first,30,30,0),"Stale initial bossbar");
        check(!s.ready(first,30,30,1000),"Stale value with no rising depth");
        check(!s.ready(first,1.5,30,1200),"Shallow cast");
        check(!s.ready(first,29.9,30,1300),"Early threshold");
        check(s.ready(first,30,30,1400),"Target not alerted");
        check(!s.ready(first,30,30,1500),"Repeated bossbar spam");
        check(!s.ready(first,29,30,1600)&&!s.ready(first,30,30,1700),"Jitter rearmed same cast");
        check(!s.ready(second,30,30,2000),"New cast inherited old full depth");
        check(!s.ready(second,2,30,2100)&&s.ready(second,31,30,2800),"Next cast or overshoot");
        s.cast(null,2900);check(!s.ready(null,30,30,3000),"Alert without hook");
        String low="You sense that there might not be many fish left in this area. Try fishing at least 3 blocks away.";
        String empty="This area is suffering from overfishing, cast your rod in a different spot for more fish. At least 3 blocks away.";
        check(s.warning(low,0),"Low-fish warning");
        check(!s.warning(low,500)&&!s.warning(empty,6000),"Duplicate warning cooldown");
        check(s.warning(empty,15000),"Overfishing after cooldown");
        check(!FishingSignals.depleted("[Member] Someone: "+low),"Player-chat spoof");
        check(!FishingSignals.depleted("Caught 17 items!"),"Unrelated chat");
        check(FishingSignals.depleted("§7"+low),"Formatted warning");
        s.reset();check(s.warning(low,15001),"Reconnect warning state");
        String snap="֎ What Lurks Below IX ┃ ✘ The fishing line snapped.";
        check(FishingSignals.snappedText(snap),"Actual snapped-line format");
        check(!FishingSignals.snappedText("[Pharaoh] Someone: "+snap),"Snap player spoof");
        check(s.snapped(snap,0)&&!s.snapped(snap,100)&&s.snapped(snap,2000),"Snap cooldown");
        check(!FishingSignals.snappedText("֎ What Lurks Below IX ┃ ✔ Caught 10 items!"),"Catch treated as snapped line");
        s.reset();check(s.snapped(snap,2001),"Reset snap state");
        s.reset();s.cast(first,10000);
        check(!s.snapped(null,11499),"Missing bar grace period");
        check(s.snapped(null,11500),"Missing bar early cue");
        check(!s.snapped(null,12000),"Early cue once per cast");
        s.cast(null,12001);check(!s.snapped(snap,20000),"Delayed confirmation duplicated early cue");
        s.cast(second,21000);check(s.snapped(snap,21001),"Next cast confirmation suppressed");
        s.reset();s.cast(first,10000);s.ready(first,1.5,30,11000);
        check(!s.snapped(null,20000),"Seen bar subsequently absent");
        check(s.ready(first,30,30,21000),"Early detection broke ready cue");
        s.reset();s.cast(first,10000);s.ready(first,30,30,10001);
        check(!s.snapped(null,12000),"Visible full-depth bar treated as missing");
        s.reset();s.cast(first,10000);s.cast(null,11000);
        check(!s.snapped(null,12000),"Cancelled cast warned");
        s.cast(second,12000);check(!s.snapped(null,13000)&&s.snapped(null,13500),"Recast timer inherited");
        System.out.println("PASS: depth formats, exact threshold, stale bossbars, one cue per cast, overshoot, no-hook guard, both warnings, cooldown, spoof rejection and reconnect");
    }
}
