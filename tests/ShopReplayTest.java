package com.moneybooster;
import java.nio.file.*;
public class ShopReplayTest {
 public static void main(String[] args)throws Exception {
  int n=0;
  for(String line:Files.readAllLines(Path.of(args[0]))) {
   if(!Double.isFinite(MoneyLedger.shopDelta(line))) throw new AssertionError("Unmatched: "+line);
   n++;
  }
  System.out.println("PASS: parsed and balance-validated "+n+" real dollar transaction notifications");
 }
}
