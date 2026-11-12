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
  buckets.addEstimated(10_000_000,271000,346000);
  expect(buckets.estimatedPerHour(348000),320_000_000d); // Weighted 225s, not latest 75s = $480M/h.
  var smooth=new SmoothedRate();
  expect(smooth.update(100,0),100);
  double eased=smooth.update(200,1000);
  if(eased<=100||eased>=105)throw new AssertionError("One-second rate jump: "+eased);
  expect(smooth.update(900,1000),eased); // Rendering twice cannot accelerate smoothing.
  var frequent=new SmoothedRate();frequent.update(100,0);
  for(int second=1;second<=20;second++)frequent.update(200,second*1000);
  smooth.reset();smooth.update(100,0);
  expect(smooth.update(200,20000),frequent.update(200,20000));
  smooth.reset();expect(smooth.update(50,30000),50);
  expect(smooth.update(Double.NaN,31000),50);
  var resp = new RollingEarnings();
  resp.reset(0);
  resp.add(10000, 0, 10000);
  expect(resp.responsivePerHour(12000), 3600000);
  resp.add(15000, 10000, 15000);
  double boostedRate = resp.responsivePerHour(17000);
  if (boostedRate <= 5_000_000 || boostedRate >= 10_000_000)
      throw new AssertionError("Responsive proc rate expected between 5M and 10M, got: " + boostedRate);
  // Continue farming at base pace (1 $/ms) for 10 seconds after procs
  resp.add(10000, 15000, 25000);
  double postProcRate = resp.responsivePerHour(27000);
  if (postProcRate >= boostedRate || postProcRate <= 3_600_000)
      throw new AssertionError("Post-proc rate expected between 3.6M and boostedRate, got: " + postProcRate);
  var respBuckets = new RollingEarnings();
  respBuckets.reset(0);
  respBuckets.addEstimated(10_000_000, 0, 10000);
  respBuckets.addEstimated(10_000_000, 10000, 20000);
  respBuckets.addEstimated(10_000_000, 20000, 25000);
  double respEst = respBuckets.responsiveEstimatedPerHour(27000);
  if (respEst <= 4_000_000_000d || respEst >= 7_200_000_000d)
      throw new AssertionError("Responsive estimated rate expected between 4B and 7.2B, got: " + respEst);
  expect(respBuckets.responsiveEstimatedPerHour(30000), respEst);
  expect(respBuckets.responsiveEstimatedPerHour(85000), 0);
  var responsiveSmooth = new SmoothedRate(SmoothedRate.RESPONSIVE_TAU_MS);
  expect(responsiveSmooth.update(100, 0), 100);
  double respEased = responsiveSmooth.update(200, 1000);
  if (respEased <= 125 || respEased >= 140)
      throw new AssertionError("Responsive 1s smoothing step expected ~133, got: " + respEased);
  System.out.println("PASS: rolling expiry, overlap, warmup, idle, reset, finite values, trade exclusion and stable rounded estimates");
 }
}
