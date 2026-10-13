package com.moneybooster;
public class RollingEarningsTest {
 static void expect(double got,double wanted){if(Math.abs(got-wanted)>.01)throw new AssertionError(got+" expected "+wanted);}
 public static void main(String[] args){
  var avg=new RollingEarnings();avg.reset(0);avg.add(10000,0,10000);expect(avg.perHour(12000),3600000);expect(avg.perHour(17000),1800000);expect(avg.perHour(22000),0);
  avg.reset(0);avg.add(5000,5000,10000);expect(avg.perHour(12000),1800000);
  avg.reset(0);avg.add(1000,0,1000);expect(avg.perHour(3000),3600000);expect(avg.perHour(1000),0);
  avg.reset(0);avg.add(Double.NaN,0,1000);avg.add(100,1,1);expect(avg.perHour(12000),0);
  avg.reset(5000);avg.add(10000,0,10000);expect(avg.perHour(12000),3600000);avg.reset(12000);expect(avg.perHour(20000),0);
  var reconciled=new RollingEarnings();reconciled.reset(0);
  var ledger=new MoneyLedger(r->reconciled.add(r.earned(),r.fromMs(),r.toMs()));
  ledger.observe(1000000,0,0,-1);ledger.shop(new MoneyLedger.Shop(270000,1000000,1270000),100,0,-1);ledger.observe(1271000,1000,0,-1);ledger.observe(1271000,3000,0,-1);ledger.tick(3000);
  expect(reconciled.perHour(5000),1200000); // $1000 farming / 3s, not $271000 shop+farming
  var gross=new RollingEarnings();var base=new RollingEarnings();gross.reset(0);base.reset(0);
  var boosted=new MoneyLedger(r->{gross.add(r.earned(),r.fromMs(),r.toMs());base.add(r.earned()/(1+r.percentage()/100.0),r.fromMs(),r.toMs());});
  boosted.observe(1000000,0,68,-1);boosted.observe(1001680,1000,68,-1);boosted.observe(1001680,3000,68,-1);boosted.tick(3000);expect(gross.perHour(5000),2016000);expect(base.perHour(5000),1200000);
  System.out.println("PASS: rolling expiry, overlap, warmup, idle, reset, finite values and exclusion of shop sales");
 }
}
