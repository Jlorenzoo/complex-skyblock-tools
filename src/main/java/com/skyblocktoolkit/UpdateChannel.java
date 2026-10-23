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
        return "skyblock-toolkit-" + version + (minecraft.equals("26.2") ? "" : "+mc" + minecraft) + ".jar";
    }

    public static boolean matches(String asset, String version, String minecraft) {
        return !version.isEmpty() && (asset.equals(jarName(version, minecraft))
            || asset.equals("skyblock-toolkit-" + version + "+mc" + minecraft + ".jar"));
    }
}
