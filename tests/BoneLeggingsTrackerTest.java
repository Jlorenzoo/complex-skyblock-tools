package com.skyblocktoolkit;

public final class BoneLeggingsTrackerTest {
    public static void main(String[] args) {
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
        if (!"Bone Leggings: §a✔ On".equals(BoneLeggingsTracker.previewLabel())) {
            throw new AssertionError("Preview label mismatch: " + BoneLeggingsTracker.previewLabel());
        }
    }

    private static void testStylesAndLabels() {
        if (!"Bone Leggings: §a✔ On".equals(BoneLeggingsTracker.formatLabel(0, true))) {
            throw new AssertionError("Style 0 active label mismatch");
        }
        if (!"Bone Leggings: §c✖ Off".equals(BoneLeggingsTracker.formatLabel(0, false))) {
            throw new AssertionError("Style 0 inactive label mismatch");
        }
        if (!"§a✔ On".equals(BoneLeggingsTracker.formatLabel(1, true))) {
            throw new AssertionError("Style 1 active label mismatch");
        }
        if (!"§c✖ Off".equals(BoneLeggingsTracker.formatLabel(1, false))) {
            throw new AssertionError("Style 1 inactive label mismatch");
        }
        if (!"§a✔".equals(BoneLeggingsTracker.formatLabel(2, true))) {
            throw new AssertionError("Style 2 active label mismatch");
        }
        if (!"§c✖".equals(BoneLeggingsTracker.formatLabel(2, false))) {
            throw new AssertionError("Style 2 inactive label mismatch");
        }
        if (!"Bone Leggings: §aOn".equals(BoneLeggingsTracker.formatLabel(3, true))) {
            throw new AssertionError("Style 3 active label mismatch");
        }
        if (!"Bone Leggings: §cOff".equals(BoneLeggingsTracker.formatLabel(3, false))) {
            throw new AssertionError("Style 3 inactive label mismatch");
        }
        if (!BoneLeggingsTracker.hasIcon(0) || !BoneLeggingsTracker.hasIcon(1) || !BoneLeggingsTracker.hasIcon(2) || BoneLeggingsTracker.hasIcon(3)) {
            throw new AssertionError("hasIcon mismatch for styles");
        }
        if (BoneLeggingsTracker.getWidth(null) <= 0 || BoneLeggingsTracker.getHeight() <= 0) {
            throw new AssertionError("Invalid width/height from null mc");
        }
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
        if (label == null || label.isEmpty()) {
            throw new AssertionError("currentLabel(null) must return fallback label");
        }
    }
}
