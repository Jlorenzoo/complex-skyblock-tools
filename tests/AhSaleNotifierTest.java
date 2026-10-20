package com.skyblocktoolkit;

public class AhSaleNotifierTest {
    public static void main(String[] args) {
        // 1. Complex Skyblock Sales ($ and GC)
        assertTrue(AhSaleNotifier.isAhSaleMessage("(/ah)  [Pharaoh➕] 1o74 purchased your listing of ♦ VOTE KEY ♦ x64 for: $34,000,000."), "CS dollar sale");
        assertTrue(AhSaleNotifier.isAhSaleMessage("(/ah)  Player purchased your listing of Key x64 for: 6 GC."), "CS GC sale");
        assertTrue(AhSaleNotifier.isAhSaleMessage("§6(/ah) §ePlayer §fpurchased your listing of §bDiamond x64 §ffor: §a$10,000."), "CS color codes");

        // 2. Hypixel Skyblock Sales
        assertTrue(AhSaleNotifier.isAhSaleMessage("[Auction] Player bought Aspect of the End for 250,000 coins!"), "Hypixel bought");
        assertTrue(AhSaleNotifier.isAhSaleMessage("[Auction] Your Aspect of the End sold to Player for 250,000 coins!"), "Hypixel sold to");
        assertTrue(AhSaleNotifier.isAhSaleMessage("[Auction] You sold Aspect of the End for 250,000 coins!"), "Hypixel you sold");

        // 3. Other server formats
        assertTrue(AhSaleNotifier.isAhSaleMessage("Auction > Player purchased your listing of Diamond for $5,000."), "Auction > prefix");
        assertTrue(AhSaleNotifier.isAhSaleMessage("You sold Diamond in the auction for 5000 coins"), "You sold in auction");

        // 4. False positives / Spoofs / Other messages
        assertFalse(AhSaleNotifier.isAhSaleMessage("[Member] Player: (/ah)  1o74 purchased your listing of Key for: $10."), "Chat spoof");
        assertFalse(AhSaleNotifier.isAhSaleMessage("Player: [Auction] I bought your sword"), "Chat spoof auction");
        assertFalse(AhSaleNotifier.isAhSaleMessage("You paid a listing fee of $19,000 (2%)"), "Listing fee");
        assertFalse(AhSaleNotifier.isAhSaleMessage("(/pay) ✔ [Mummy] eloise_13 has sent you: $16,000,000 ($27,817,725 ➜ $43,817,725)"), "Pay message");
        assertFalse(AhSaleNotifier.isAhSaleMessage("You purchased Diamond for $5,000 on /ah"), "Bought item yourself");
        assertFalse(AhSaleNotifier.isAhSaleMessage(""), "Empty string");
        assertFalse(AhSaleNotifier.isAhSaleMessage(null), "Null");

        System.out.println("PASS: All AH sale pattern tests passed!");
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + label + " expected TRUE, got FALSE");
        }
    }

    private static void assertFalse(boolean condition, String label) {
        if (condition) {
            throw new AssertionError("Assertion failed: " + label + " expected FALSE, got TRUE");
        }
    }
}
