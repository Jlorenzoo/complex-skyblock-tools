package com.skyblocktoolkit;

/** Release asset names keep clients on their Minecraft compatibility line. */
public final class UpdateChannel {
    private UpdateChannel() {}

    public static String releaseVersion(String tag) {
        String version = tag.replaceFirst("^[vV]", "").replaceFirst("-mc[0-9.]+$", "");
        if (!version.matches("[0-9]+(\\.[0-9]+)*")) return "";
        return version;
    }

    public static String jarName(String version, String minecraft) {
        return "complexify-" + version + (minecraft.equals("26.2") ? "" : "+mc" + minecraft) + ".jar";
    }

    public static boolean matches(String asset, String version, String minecraft) {
        if (version.isEmpty()) return false;
        String c1 = "complexify-" + version + (minecraft.equals("26.2") ? "" : "+mc" + minecraft) + ".jar";
        String c2 = "complexify-" + version + "+mc" + minecraft + ".jar";
        String s1 = "skyblock-toolkit-" + version + (minecraft.equals("26.2") ? "" : "+mc" + minecraft) + ".jar";
        String s2 = "skyblock-toolkit-" + version + "+mc" + minecraft + ".jar";
        return asset.equals(c1) || asset.equals(c2) || asset.equals(s1) || asset.equals(s2);
    }
    public static int compareVersions(String v1, String v2) {
        if (v1 == null) return v2 == null ? 0 : -1;
        if (v2 == null) return 1;
        v1 = v1.trim().replaceFirst("^[vV]", "");
        v2 = v2.trim().replaceFirst("^[vV]", "");
        String[] parts1 = v1.split("[-+.]");
        String[] parts2 = v2.split("[-+.]");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            String p1 = i < parts1.length ? parts1[i] : "0";
            String p2 = i < parts2.length ? parts2[i] : "0";
            try {
                int n1 = Integer.parseInt(p1);
                int n2 = Integer.parseInt(p2);
                if (n1 != n2) return Integer.compare(n1, n2);
            } catch (NumberFormatException e) {
                int cmp = p1.compareToIgnoreCase(p2);
                if (cmp != 0) return cmp;
            }
        }
        return 0;
    }
}
