package com.skyblocktoolkit;

public final class BoneLeggingsTrackerTest {
    public static void main(String[] args) {
        if (!BoneLeggingsTracker.matchesItemText("§dBone §lLeggings") ||
                !BoneLeggingsTracker.matchesItemText("Burning Rush Rune") ||
                BoneLeggingsTracker.matchesItemText("Diamond Leggings"))
            throw new AssertionError("Server item matching must ignore legacy formatting and reject ordinary leggings");
        testPreviewLabel();
        testStylesAndLabels();
        testSeasonalRegistryLookup();
        testSearchDiscovery();
        testPotionIconsSearch();
        testNullSafety();
        System.out.println("PASS: BoneLeggingsTracker preview label, styles, null safety, seasonal registry lookup, and search discovery");
    }

    private static void testPotionIconsSearch() {
        var results = ToolkitSearch.find("potion icons");
        if (results.isEmpty() || !results.getFirst().setting().equals("Potion icons")) {
            throw new AssertionError("Search for 'potion icons' failed");
        }
    }

    private static void testPreviewLabel() {
        if (!BoneLeggingsTracker.previewLabel().isEmpty()) {
            throw new AssertionError("Default effect icons must not include a title or checkmark");
        }
    }

    private static void testStylesAndLabels() {
        for (boolean speed : new boolean[]{false, true}) {
            for (int style : new int[]{0, 2}) {
                if (!BoneLeggingsTracker.formatLabel(style, speed).isEmpty())
                    throw new AssertionError("Badge styles must stay text-free");
            }
            for (int style : new int[]{1, 3}) {
                String expected = speed ? "§bSpeed" : "§dRegen";
                if (!expected.equals(BoneLeggingsTracker.formatLabel(style, speed)))
                    throw new AssertionError("Only speed or regeneration may be shown");
            }
        }
        if (!BoneLeggingsTracker.hasIcon(0) || !BoneLeggingsTracker.hasIcon(1) || !BoneLeggingsTracker.hasIcon(2) || BoneLeggingsTracker.hasIcon(3))
            throw new AssertionError("hasIcon mismatch for styles");
        var cfg = ToolkitConfig.get();
        for (int style = 0; style < 4; style++) {
            cfg.boneLeggingsHudStyle = style;
            cfg.boneLeggingsBackground = false;
            int width = BoneLeggingsTracker.getWidth(null), height = BoneLeggingsTracker.getHeight();
            cfg.boneLeggingsBackground = true;
            if (BoneLeggingsTracker.getWidth(null) != width + 6 || BoneLeggingsTracker.getHeight() != height + 4)
                throw new AssertionError("Background padding must match renderer in style " + style);
        }
        cfg.boneLeggingsHudStyle = 0;
        cfg.boneLeggingsBackground = false;
    }

    private static void testSeasonalRegistryLookup() {
        var byPants = SeasonalRegistry.find("bone_pants");
        if (byPants == null || !"Bone Leggings".equals(byPants.name())) {
            throw new AssertionError("Failed to lookup bone_pants in SeasonalRegistry");
        }
        var byLeggings = SeasonalRegistry.find("bone_leggings");
        if (byLeggings == null || !"Bone Leggings".equals(byLeggings.name())) {
            throw new AssertionError("Failed to lookup bone_leggings alias in SeasonalRegistry");
        }
    }

    private static void testSearchDiscovery() {
        var results = ToolkitSearch.find("bone leggings");
        if (results.isEmpty()) {
            throw new AssertionError("Expected search results for 'bone leggings'");
        }
        boolean hasOverlay = false;
        for (var r : results) {
            if ("HUD overlay".equals(r.setting()) || "Bone Leggings".equals(r.setting())) {
                hasOverlay = true;
                break;
            }
        }
        if (!hasOverlay) {
            throw new AssertionError("Search for 'bone leggings' missing HUD overlay entry");
        }

        var speedResults = ToolkitSearch.find("speed vi");
        if (speedResults.isEmpty()) {
            throw new AssertionError("Expected search results for 'speed vi'");
        }
    }

    private static void testNullSafety() {
        if (BoneLeggingsTracker.shouldShow(null)) {
            // By default boneLeggingsAlwaysVisible is false, so null client returns false
            throw new AssertionError("shouldShow(null) must be false by default");
        }
        String label = BoneLeggingsTracker.currentLabel(null);
        if (label == null) {
            throw new AssertionError("currentLabel(null) must return a non-null label");
        }
    }
}
