package com.moneybooster;
public class RollingEarningsTest {
 static void expect(double got,double wanted){if(Math.abs(got-wanted)>.01)throw new AssertionError(got+" expected "+wanted);}
 public static void main(String[] args){
  var avg=new RollingEarnings();avg.reset(0);avg.add(10000,0,10000);expect(avg.perHour(12000),3600000);expect(avg.perHour(22000),1800000);expect(avg.perHour(32000),1200000);expect(avg.perHour(62000),600000);expect(avg.perHour(72000),0);
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
  var buckets=new RollingEarnings();buckets.reset(0);
  buckets.addEstimated(10_000_000,0,250); // Joined just before a rounding boundary.
  if(buckets.estimatedReady(2250))throw new AssertionError("Partial first bucket produced a rate");
  buckets.addEstimated(10_000_000,250,10250);
  expect(buckets.estimatedPerHour(12250),3_600_000_000d);
  expect(buckets.estimatedPerHour(19250),3_600_000_000d); // No sawtooth between updates.
  buckets.addEstimated(10_000_000,10250,20250);
  expect(buckets.estimatedPerHour(22250),3_600_000_000d);
  if(buckets.estimatedReady(80250))throw new AssertionError("Stale estimate remained live");
  buckets.addEstimated(10_000_000,90000,100000); // Resume/context change must warm up again.
  if(buckets.estimatedReady(102000))throw new AssertionError("Gap reused old measured rate");
  buckets.addEstimated(10_000_000,100000,110000);
  expect(buckets.estimatedPerHour(112000),3_600_000_000d);
  buckets.reset(120000);if(buckets.estimatedReady(120001))throw new AssertionError("Reset kept estimate");
  buckets.addEstimated(10_000_000,120000,121000);
  buckets.addEstimated(10_000_000,121000,271000); // Real sidebar may move only once per 2.5 minutes.
  expect(buckets.estimatedPerHour(273000),240_000_000d);
  expect(buckets.estimatedPerHour(400000),240_000_000d);
  if(buckets.estimatedReady(571000))throw new AssertionError("Slow bucket estimate never expires");
  System.out.println("PASS: rolling expiry, overlap, warmup, idle, reset, finite values, trade exclusion and stable rounded estimates");
 }
}
