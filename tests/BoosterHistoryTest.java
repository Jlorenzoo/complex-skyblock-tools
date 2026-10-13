package com.moneybooster;
import java.nio.file.*;
import java.util.*;
public class BoosterHistoryTest {
 public static void main(String[] args) throws Exception {
  Path path=Path.of(args[0]); Files.deleteIfExists(path);
  var history=new BoosterHistory(path);
  var coconut=history.start("Coconut","Coconut","{\"id\":\"minecraft:player_head\"}",true,20,60,"Money booster",1000);
  var ledger=new MoneyLedger(r -> history.addBonus(r.boosterId(),r.earned()*r.percentage()/(100.0+r.percentage())));
  ledger.observe(1000000,0,20,-1,coconut.id);
  ledger.observe(1000120,100,20,-1,coconut.id);
  var potion=history.start("Money booster item","Money Booster Potion",null,true,20,120,"Money booster",1200);
  ledger.observe(1000120,200,20,-1,potion.id);
  ledger.observe(1000360,300,20,-1,potion.id);
  ledger.observe(1000360,3000,20,-1,potion.id); ledger.tick(3000);
  if(Math.abs(coconut.bonus-20)>.001 || Math.abs(potion.bonus-40)>.001)throw new AssertionError("Same-percent activations mixed");
  if(!coconut.status.equals("Replaced") || !potion.status.equals("Active"))throw new AssertionError("Status");
  var failed=history.start("Coconut","Coconut",null,true,0,15,"Speed",1300);
  if(history.activeId()!=potion.id || failed.bonus!=0 || !failed.status.equals("No money boost"))throw new AssertionError("Non-money effect");
  if(history.newestFirst(true).size()!=2 || history.newestFirst(false).size()!=3 || history.newestFirst(true).stream().anyMatch(e->e.percentage<=0))throw new AssertionError("Money-only filter");
  if(history.latestMoneyBooster().id!=potion.id)throw new AssertionError("Latest booster selected non-money effect");
  failed.excluded=true;if(history.newestFirst(false).size()!=2)throw new AssertionError("Excluded entry visible");failed.excluded=false;
  history.finish(2000,"Finished"); history.save(2000);
  var loaded=new BoosterHistory(path);
  if(loaded.newestFirst().size()!=3 || loaded.find(coconut.id).bonus!=20 || loaded.find(potion.id).bonus!=40)throw new AssertionError("Persistence");
  if(!loaded.find(coconut.id).iconJson.equals(coconut.iconJson))throw new AssertionError("Sprite data lost");
  var unknown=loaded.start("Unknown source","Detected on scoreboard",null,false,40,-1,"Money booster",3000); loaded.save(4000);
  var reopened=new BoosterHistory(path);
  if(reopened.activeId()!=-1 || !reopened.find(unknown.id).status.equals("Interrupted"))throw new AssertionError("Stale active after restart");
  long id=reopened.start("Server booster","Server booster",null,false,50,600,"Money booster",5000).id;
  if(id<=unknown.id)throw new AssertionError("IDs reused");
  reopened.clear(6000); if(!new BoosterHistory(path).newestFirst().isEmpty())throw new AssertionError("Clear persisted");
  System.out.println("PASS: per-item attribution, equal-percent replacements, non-money effects, custom sprite persistence, restart, IDs and reset");
 }
}
