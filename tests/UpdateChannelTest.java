package com.skyblocktoolkit;

public final class UpdateChannelTest {
    public static void main(String[] args) {
        check(UpdateChannel.releaseVersion("v1.1.0-mc1.21.11").equals("1.1.0"));
        check(UpdateChannel.releaseVersion("v1.2.0").equals("1.2.0"));
        check(UpdateChannel.releaseVersion("../../bad").isEmpty());
        check(UpdateChannel.releaseVersion("v1.1.44-mc1.21.11").equals("1.1.44"));
        check(UpdateChannel.matches("skyblock-toolkit-1.1.44+mc1.21.11.jar", "1.1.44", "1.21.11"));
        check(UpdateChannel.compareVersions("1.1.44", "1.1.0") > 0);
        check(UpdateChannel.compareVersions("1.1.45", "1.1.44") > 0);
        check(UpdateChannel.compareVersions("1.1.100", "1.1.99") > 0);
        check(UpdateChannel.compareVersions("1.2.1", "1.1.999") > 0);
        check(UpdateChannel.compareVersions("1.1.44", "1.1.44") == 0);
        check(UpdateChannel.compareVersions("1.1.43", "1.1.44") < 0);
        check(UpdateChannel.matches("skyblock-toolkit-1.2.0+mc1.21.11.jar", "1.2.0", "1.21.11"));
        check(!UpdateChannel.matches("skyblock-toolkit-1.2.0.jar", "1.2.0", "1.21.11"));
        check(!UpdateChannel.matches("skyblock-toolkit-1.2.0+mc1.21.11.jar", "1.2.0", "26.2"));
        check(UpdateChannel.matches("skyblock-toolkit-1.2.0.jar", "1.2.0", "26.2"));
        check(UpdateChannel.matches("skyblock-toolkit-1.2.0+mc26.2.jar", "1.2.0", "26.2"));
        check(!UpdateChannel.matches("skyblock-toolkit-1.2.0+mc1.21.11-sources.jar", "1.2.0", "1.21.11"));
        check(!UpdateChannel.matches("skyblock-toolkit-1.2.0+mc1.21.1.jar", "1.2.0", "1.21.11"));
        check(!UpdateChannel.matches("skyblock-toolkit-1.3.0+mc1.21.11.jar", "1.2.0", "1.21.11"));
        System.out.println("PASS: releases and update assets stay on the correct Minecraft version");
    }
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }
}
