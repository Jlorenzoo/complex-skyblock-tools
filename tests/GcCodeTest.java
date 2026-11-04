package com.skyblocktoolkit;

import com.skyblocktoolkit.chat.ChatSelection;
import java.util.regex.Matcher;

public final class GcCodeTest {
    private static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }

    public static void main(String[] args) {
        // 1. Exact 16-digit code from user screenshot
        Matcher m1 = ChatSelection.GC_CODE_PATTERN.matcher("9517839063098162");
        check(m1.find(), "should match user screenshot 16-digit code");
        check(m1.group().equals("9517839063098162"), "matched content exact");
        check(ChatSelection.cleanCode(m1.group()).equals("9517839063098162"), "clean code exact");

        // 2. Formatted with spaces or hyphens (Tebex style)
        Matcher m2 = ChatSelection.GC_CODE_PATTERN.matcher("9517 8390 6309 8162");
        check(m2.find(), "should match spaced 4x4 code");
        check(ChatSelection.cleanCode(m2.group()).equals("9517839063098162"), "clean spaced code");

        Matcher m3 = ChatSelection.GC_CODE_PATTERN.matcher("9517-8390-6309-8162");
        check(m3.find(), "should match hyphenated 4x4 code");
        check(ChatSelection.cleanCode(m3.group()).equals("9517839063098162"), "clean hyphenated code");

        // 3. Embedded in realistic chat lines
        String chat1 = "[Global] Steve: 9517839063098162";
        Matcher m4 = ChatSelection.GC_CODE_PATTERN.matcher(chat1);
        check(m4.find(), "matches inside player message");
        check(m4.group().equals("9517839063098162"), "extracts code from message");

        String chat2 = "First to redeem 9517839063098162 wins!";
        Matcher m5 = ChatSelection.GC_CODE_PATTERN.matcher(chat2);
        check(m5.find(), "matches inside sentence");
        check(m5.group().equals("9517839063098162"), "extracts code from sentence");

        // 4. Negative tests - invalid formats
        check(!ChatSelection.GC_CODE_PATTERN.matcher("123456789012345").find(), "rejects 15 digits");
        check(!ChatSelection.GC_CODE_PATTERN.matcher("12345678901234567").find(), "rejects 17 digits");
        check(!ChatSelection.GC_CODE_PATTERN.matcher("12345").find(), "rejects short numbers");
        check(!ChatSelection.GC_CODE_PATTERN.matcher("x=120 y=64 z=-500").find(), "rejects coordinates");
        check(!ChatSelection.GC_CODE_PATTERN.matcher("$1,500,000").find(), "rejects prices");
        check(!ChatSelection.GC_CODE_PATTERN.matcher("12345678-1234-1234-1234-123456789012").find(), "rejects UUID");

        // 5. Settings search discovery
        var results = ToolkitSearch.find("Quick /gc code click");
        check(results.size() == 1, "finds Quick /gc code click setting");
        check(results.getFirst().page() == 4, "setting located on Utilities page 4");
        check(results.getFirst().category().equals("Utilities"), "category is Utilities");
        check(results.getFirst().section().equals("Chat selection & copy"), "section is Chat selection & copy");

        var gcSearch = ToolkitSearch.find("gc");
        check(gcSearch.stream().anyMatch(e -> e.setting().equals("Quick /gc code click")), "searchable by keyword 'gc'");

        var tebexSearch = ToolkitSearch.find("tebex");
        check(tebexSearch.stream().anyMatch(e -> e.setting().equals("Quick /gc code click")), "searchable by keyword 'tebex'");

        System.out.println("PASS: GC code recognition, cleaning, negative rejection and search discovery");
    }
}
