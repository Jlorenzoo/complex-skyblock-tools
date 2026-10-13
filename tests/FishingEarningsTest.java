import com.fishingpings.FishingEarnings;
public final class FishingEarningsTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void near(double a,double b,String why){check(Math.abs(a-b)<.00001,why+" "+a);}
 public static void main(String[] args){
  String message="֎ What Lurks Below IX ┃ ✔ Caught 13 items!";
  check(FishingEarnings.caught(message)==13,"Screenshot message");
  check(FishingEarnings.caught("§5֎ What Lurks Below VIII §7┃ §a✓ Caught 20 items!")==20,"Rod VIII and formatting");
  check(FishingEarnings.caught("[Pharaoh] Someone: "+message)==0,"Player spoof");
  check(FishingEarnings.caught("Caught 13 items!")==0,"Unrelated catch");
  check(FishingEarnings.caught("֎ What Lurks Below IX ┃ ✘ The fishing line snapped.")==0,"Snap counts as catch");
  check(FishingEarnings.caught("֎ What Lurks Below IX ┃ ✔ Caught 0 items!")==0,"Zero catch");
  near(FishingEarnings.weightedPrice(100,200,300,400),157,"Vanilla weight mapping");
  near(FishingEarnings.price("$1,250.50"),1250.5,"Grouped price");
  near(FishingEarnings.price("1.8m"),1800000,"Million price");
  near(FishingEarnings.price("2.5 k"),2500,"Thousand price");
  for(String bad:new String[]{"","NaN","Infinity","-1","1e9","foo","99999999999999999"})check(Double.isNaN(FishingEarnings.price(bad)),"Bad price "+bad);
  var s=new FishingEarnings();s.observe(0);
  check(s.accept(message,10000),"First catch");check(!s.accept(message,10200),"Duplicate system message");check(s.accept(message,20000),"Same count on next reel");
  check(s.count(20000,60)==26,"Catch sum");near(s.hourly(20000,60,157),244920,"60s rate");
  near(s.hourly(20000,300,157),48984,"5m rate");check(s.warming(20000,60),"Warmup");check(!s.warming(60000,60),"Warmup finished");
  check(s.count(70000,60)==13,"Exact window boundary excluded");check(s.count(80000,60)==0,"Idle decay");check(!s.recent(80000,60),"Auto fallback");
  check(s.count(80000,300)==26,"Long window preserved");check(s.count(320000,300)==0,"Old catches pruned");check(s.sessionFish()==26,"Session count survives window expiry");
  s.reset();check(s.count(400000,60)==0&&s.sessionFish()==0&&!s.recent(400000,60),"Reconnect reset");
  System.out.println("PASS: catch parser, spoof protection, duplicate suppression, vanilla weights, dollar inputs, 60s/5m rates, warmup, rolling expiry and reconnect reset");
 }
}
