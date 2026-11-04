package com.skyblocktoolkit;

public final class BoneLeggingsTrackerTest {
    public static void main(String[] args) {
        testPreviewLabel();
        testSeasonalRegistryLookup();
        testSearchDiscovery();
        testPotionIconsSearch();
        testNullSafety();
        System.out.println("PASS: BoneLeggingsTracker preview label, null safety, seasonal registry lookup, and search discovery");
    }

    private static void testPotionIconsSearch() {
        var results = ToolkitSearch.find("potion icons");
        if (results.isEmpty() || !results.getFirst().setting().equals("Potion icons")) {
            throw new AssertionError("Search for 'potion icons' failed");
        }
    }

    private static void testPreviewLabel() {
        if (!"§b⚡ Speed VI".equals(BoneLeggingsTracker.previewLabel())) {
            throw new AssertionError("Preview label mismatch: " + BoneLeggingsTracker.previewLabel());
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
