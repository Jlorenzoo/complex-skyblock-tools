package com.skyblocktoolkit;

import java.util.Locale;

/** Complex's published allowed-mod list does not approve this custom counter. */
public final class CropStatsPolicy {
    private CropStatsPolicy() {}
    public static boolean requiresApproval(String address) {
        if (address == null) return false;
        String host = address.strip().toLowerCase(Locale.ROOT).split(":", 2)[0];
        while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        return host.equals("mc-complex.com") || host.endsWith(".mc-complex.com");
    }
}
