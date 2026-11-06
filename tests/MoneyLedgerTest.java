package com.moneybooster;
import java.util.*;
public class MoneyLedgerTest {
 static List<MoneyLedger.Receipt> results = new ArrayList<>();
 static MoneyLedger ledger;
 static void setup() { results.clear(); ledger = new MoneyLedger(results::add); ledger.observe(1000000,0,20,1); }
 static MoneyLedger.Shop tx(double delta, double before) { return new MoneyLedger.Shop(delta,before,before+delta); }
 static void expect(double amount) { double got=results.stream().mapToDouble(MoneyLedger.Receipt::earned).sum(); if(Math.abs(got-amount)>.001) throw new AssertionError(got+" expected "+amount); }
 static void settle(double balance) { ledger.observe(balance,3000,20,1); ledger.tick(3000); }
 public static void main(String[] args) {
  String sale="(CS) [Noble] VortexSaber bought ♦ VOTE KEY ♦ for +$270,000 ($61,937,725.2 → $62,207,725.2)";
  if(MoneyLedger.shopDelta(sale)!=270000) throw new AssertionError("sale parser");
  if(MoneyLedger.shopDelta("(CS) [Member] TraderJae sold 28x ♦ VOTE KEY ♦ for -$6,720,000 ($68,147,725.2 → $61,427,725.2)")!=-6720000)throw new AssertionError("purchase parser");
  if(Double.isFinite(MoneyLedger.shopDelta("[Member] Someone: "+sale)))throw new AssertionError("spoof");
  if(Double.isFinite(MoneyLedger.shopDelta(sale.replace("+$270,000","+$270,001"))))throw new AssertionError("mismatch");
  setup(); ledger.shop(tx(270000,1000000),100,20,1); ledger.observe(1270000,200,20,1); settle(1270000); expect(0);
  setup(); ledger.observe(1270100,100,20,1); ledger.shop(tx(270000,1000000),400,20,1); settle(1270100); expect(100);
  setup(); ledger.shop(tx(-240000,1000000),100,20,1); ledger.observe(760100,200,20,1); settle(760100); expect(100);
  setup(); ledger.shop(tx(270000,1000000),100,20,1); ledger.shop(tx(-240000,1270000),110,20,1); ledger.observe(1030150,200,20,1); settle(1030150); expect(150);
  setup(); ledger.shop(tx(-100,1000000),100,20,1); settle(1000000); expect(100); // farming completely cancels shop debit
  setup(); ledger.observe(1000100,100,20,1); ledger.reset(); ledger.observe(2000000,200,20,1); ledger.tick(4000); expect(0);
  setup(); ledger.observe(1270100,100,20,1); ledger.observe(1270100,200,0,-1); ledger.shop(tx(270000,1000000),300,0,-1); ledger.observe(1270150,400,0,-1); ledger.observe(1270150,3000,0,-1); ledger.tick(3000); expect(150);
  if(results.size()!=2 || results.get(0).testId()!=1 || results.get(0).percentage()!=20 || results.get(1).testId()!=-1)throw new AssertionError("boundary attribution");
  setup(); ledger.observe(Double.NaN,100,20,1); ledger.observe(Double.POSITIVE_INFINITY,200,20,1); settle(1000000); expect(0);
  setup(); ledger.observe(1000100,100,20,1); ledger.tick(200); expect(0); settle(1000100); expect(100); ledger.tick(6000); expect(100);
  setup(); ledger.shop(tx(-240000,1000000),100,20,1); ledger.observe(1000000,150,0,-1); ledger.observe(760100,300,0,-1); ledger.observe(760100,3000,0,-1); ledger.tick(3000); expect(100);
  if(MoneyLedger.shopDelta("(/ah)  [Pharaoh➕] 1o74 purchased your listing of ♦ VOTE KEY ♦ x64 for: $34,000,000.")!=34000000)throw new AssertionError("Auction sale");
  if(Double.isFinite(MoneyLedger.shopDelta("(/ah)  Player purchased your listing of Key x64 for: 6 GC.")))throw new AssertionError("GC treated as dollars");
  if(MoneyLedger.shopDelta("(/pay) ✔ [Mummy] eloise_13 has sent you: $16,000,000 ($27,817,725 ➜ $43,817,725)")!=16000000)throw new AssertionError("Incoming pay");
  if(MoneyLedger.shopDelta("(/pay) ✔ You paid [Pharaoh] Alexeitor1: $18 ($22,721,776 ➜ $22,721,758) for 'money'.")!=-18)throw new AssertionError("Outgoing pay");
  if(MoneyLedger.shopDelta("You paid a listing fee of $19,000 (2%)")!=-19000)throw new AssertionError("Listing fee");
  setup();ledger.shop(MoneyLedger.parseTransaction("(/ah)  [Pharaoh➕] 1o74 purchased your listing of ♦ VOTE KEY ♦ x64 for: $34,000,000."),100,20,1);ledger.observe(35001000,200,20,1);settle(35001000);expect(1000);
  setup();ledger.observe(35001000,100,20,1);ledger.shop(MoneyLedger.parseTransaction("(/ah)  [Pharaoh➕] 1o74 purchased your listing of ♦ VOTE KEY ♦ x64 for: $34,000,000."),200,20,1);ledger.observe(35001000,300,0,-1);ledger.shop(tx(-475000,35001000),400,0,-1);ledger.observe(34526000,500,0,-1);ledger.observe(34526000,3000,0,-1);ledger.tick(3000);expect(1000);
  if(results.size()!=1||results.get(0).testId()!=1)throw new AssertionError("Post-booster purchases credited to old activation");
  // An unchanged billion-scale bucket must not compress a 10s gain into 250ms.
  results.clear();ledger=new MoneyLedger(results::add);
  ledger.observe(2_340_000_000d,0,0,1,-1,true);
  for(long t=250;t<10000;t+=250)ledger.observe(2_340_000_000d,t,0,1,-1,true);
  ledger.observe(2_350_000_000d,10000,0,1,-1,true);
  ledger.observe(2_350_000_000d,12000,0,1,-1,true);ledger.tick(12000);
  expect(10_000_000);
  var rounded=results.get(0);
  if(!rounded.estimated()||rounded.fromMs()!=0||rounded.toMs()!=10000)throw new AssertionError("Rounded gain interval: "+rounded);
  for(long t=12250;t<20000;t+=250)ledger.observe(2_350_000_000d,t,0,1,-1,true);
  ledger.observe(2_360_000_000d,20000,0,1,-1,true);
  ledger.observe(2_360_000_000d,22000,0,1,-1,true);ledger.tick(22000);
  if(results.get(1).fromMs()!=10000||results.get(1).toMs()!=20000)throw new AssertionError("Settlement polling shortened second interval");
  var roundedAverage=new RollingEarnings();roundedAverage.reset(0);
  for(var receipt:results)roundedAverage.add(receipt.earned(),receipt.fromMs(),receipt.toMs());
  if(Math.abs(roundedAverage.perHour(22000)-3_600_000_000d)>.01)throw new AssertionError("Rounded rate spikes");
  ledger.resumeFarming(50000);
  ledger.observe(2_370_000_000d,60000,0,1,-1,true);
  ledger.observe(2_370_000_000d,62000,0,1,-1,true);ledger.tick(62000);
  if(results.get(2).fromMs()!=50000)throw new AssertionError("Idle included in resumed farming interval");
  results.clear();ledger=new MoneyLedger(results::add);
  ledger.observe(2_340_000_000d,0,0,1,-1,true);
  ledger.shop(tx(-100,2_340_000_000d),100,0,1);
  ledger.observe(2_340_000_000d,3000,0,1,-1,true);ledger.tick(3000);
  expect(0); // A purchase hidden inside one rounded bucket is not a measured gain.
  results.clear();ledger=new MoneyLedger(results::add);
  ledger.observe(1_000_000_000d,0,0,-1,-1,true);
  ledger.observe(1_000_000_000d,149000,50,-1,1,true);
  ledger.observe(1_010_000_000d,150000,50,-1,1,true);
  ledger.observe(1_010_000_000d,152000,50,-1,1,true);ledger.tick(152000);
  if(results.size()!=1||results.get(0).fromMs()!=0||results.get(0).toMs()!=150000)
      throw new AssertionError("Coconut context compressed rounded interval: "+results);
  ledger.observe(1_010_000_000d,299000,20,-1,2,true);
  ledger.observe(1_020_000_000d,300000,20,-1,2,true);
  ledger.observe(1_020_000_000d,302000,20,-1,2,true);ledger.tick(302000);
  if(results.get(1).fromMs()!=150000)throw new AssertionError("Context lost consecutive bucket boundary");
  System.out.println("PASS: transactions, boundaries, packet order, rounded context timing and no double counting");
 }
}
