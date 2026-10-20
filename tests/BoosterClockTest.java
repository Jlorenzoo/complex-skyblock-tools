package com.moneybooster;
import java.util.List;
public class BoosterClockTest {
 static void check(boolean value,String msg){if(!value)throw new AssertionError(msg);}
 public static void main(String[] args){
  var clock=new BoosterClock(); clock.start(36,0);
  clock.observe(true,-1,35900); check(clock.active(36100),"Local expiry defeated sidebar");
  clock.observe(true,-1,70000); check(clock.active(70000),"Below-minute booster ended early");
  check(!clock.hasTimer(70000),"Invented exact seconds");
  clock.observe(false,-1,70250); check(clock.active(72500),"Temporary removal caused flicker");
  clock.observe(false,-1,73500); check(!clock.active(73500),"Effect survived confirmed removal");
  clock.reset(); clock.start(180,0); clock.observe(true,60000,1000);
  check(clock.remaining(1000)==60000,"Sidebar time not used");
  clock.observe(true,120000,2000); check(clock.remaining(2000)==120000 && clock.progress(2000)<=1,"Extension not reconciled");
  clock.observe(false,-1,2500);clock.observe(false,-1,5500);check(!clock.active(5500),"Chat timer overrode server removal");
  clock.reset();clock.start(10,0);check(clock.active(9000)&&!clock.active(11000),"Chat-only fallback");
  clock.reset();clock.observe(true,-1,0);check(clock.active(0),"Joining with active sidebar booster");clock.reset();check(!clock.active(0),"Disconnect baseline");
  var state=BoosterSidebar.parse(List.of("+39% Money Booster:"));
  check(state.active()&&state.percentage()==39&&state.remainingMs()==-1,"Actual below-minute format");
  check(BoosterSidebar.parse(List.of("+39$ Money Booster:")).percentage()==39,"Reported dollar-symbol variant");
  check(BoosterSidebar.parse(List.of("+39% Money Booster: 1m 20s")).remainingMs()==80000,"Units timer");
  check(BoosterSidebar.parse(List.of("Money Booster: +39% (01:20)")).remainingMs()==80000,"Colon timer");
  check(BoosterSidebar.parse(List.of("+39% Money Booster:","[1 minute]")).remainingMs()==60000,"Separate timer");
  check(BoosterSidebar.parse(List.of("+39% Money Booster:","Flight: 10m")).remainingMs()==-1,"Unrelated timer borrowed");
  check(BoosterSidebar.parse(List.of("Balance: $1M", "Flight: 10m")).active()==false,"Unrelated sidebar activated booster");
  check(BoosterSidebar.parse(List.of("§a+39% §fMoney Booster: 1m")).remainingMs()==60000,"Formatting codes");
  clock.reset();clock.start(138,0);clock.observe(true,120000,false,1000);
  check(clock.remaining(1000)==137000,"Coarse sidebar replaced precise chat time");
  clock.observe(true,120000,false,6000);check(clock.remaining(6000)==132000,"Minute-only sidebar froze seconds");
  clock.observe(true,60000,false,26000);check(clock.remaining(27000)==111000,"Minute transition reset seconds");
  clock.observe(true,-1,false,79000);check(clock.remaining(79000)==59000,"Hidden seconds lost local countdown");
  check(!BoosterSidebar.parse(List.of("+39% Money Booster: 2m")).exactSeconds(),"Coarse parsing");
  check(BoosterSidebar.parse(List.of("+39% Money Booster: 1m 20s")).exactSeconds(),"Precise parsing");
  clock.reset();clock.start(36,0);
  clock.observe(true,36000,true,250);clock.observe(true,36000,true,500);clock.observe(true,36000,true,900);
  check(clock.remaining(1000)==35000,"Repeated scoreboard seconds extended precise chat deadline");
  clock.observe(true,60000,false,1250);
  check(clock.remaining(2000)==34000,"Rounded-up minute sidebar replaced coconut chat duration");
  clock.reset();clock.observe(true,10000,true,0);clock.observe(true,10000,true,750);
  check(clock.remaining(1000)==9000,"Stale exact sample froze countdown on joining");
  System.out.println("PASS: server-active after local expiry, hidden seconds, removal grace, server extensions, chat fallback and sidebar formats");
 }
}
