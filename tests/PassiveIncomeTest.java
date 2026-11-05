package com.moneybooster;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public final class PassiveIncomeTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        var visible = Component.literal("Balance: $1.55B");
        check(BalanceComponents.read(visible).amount()==1_550_000_000d,"Plain abbreviated balance preserved");
        var hover = visible.copy().withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(Component.literal("$1,545,063,209"))));
        check(BalanceComponents.read(hover).amount()==1_545_063_209d&&!BalanceComponents.read(hover).rounded(),"Received full balance hover preferred");
        var sibling = Component.literal("Balance: ").append(Component.literal("$1.55B").withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(Component.literal("Balance: $1,545,063,209")))));
        check(BalanceComponents.read(sibling).amount()==1_545_063_209d,"Nested style hover retained");
        var mismatch = visible.copy().withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(Component.literal("$5,000"))));
        check(BalanceComponents.read(mismatch).rounded(),"Unrelated hover amount rejected");
        check(BalanceComponents.read(Component.literal("XP: 500"))==null,"Other counters rejected");
        var entries = new IncomeObservations();
        check(!entries.add("action-bar","Depth: 30",0),"No unrelated gameplay text captured");
        entries.add("sidebar","Balance: $1.55B",100);
        entries.add("sidebar","Balance: $1.55B",200);
        check(entries.entries().size()==1&&entries.entries().get(0).observations()==2,"Repeated observations counted");
        for(int i=0;i<100;i++)entries.add("action-bar","Sold crops for $"+i,300+i);
        check(entries.entries().size()==17,"Per-source cap preserves other evidence");
        entries.reset();check(entries.entries().isEmpty(),"Connection reset clears observations");
        System.out.println("PASS: passive visible/hover balances, range validation, money-text filtering, deduplication and bounded local observations");
    }
}
