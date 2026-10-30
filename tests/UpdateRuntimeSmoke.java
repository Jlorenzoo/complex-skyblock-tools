package com.skyblocktoolkit.smoke;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/** Opt-in real Fabric check against the public release API; no game/server login. */
public final class UpdateRuntimeSmoke implements PreLaunchEntrypoint {
    @Override public void onPreLaunch() {
        try {
            Class<?> updater = Class.forName("com.skyblocktoolkit.UpdateManager");
            updater.getMethod("checkForUpdatesAsync").invoke(null);
            long deadline = System.nanoTime() + java.time.Duration.ofSeconds(30).toNanos();
            String state;
            do {
                Thread.sleep(100);
                state = updater.getMethod("getState").invoke(null).toString();
            } while (state.equals("CHECKING") && System.nanoTime() < deadline);
            String expected = System.getProperty("toolkit.expectedUpdateState", "UP_TO_DATE");
            if (!state.equals(expected)) throw new AssertionError("Expected " + expected + ", got " + state + ": " + updater.getMethod("getLastError").invoke(null));
            String version = updater.getMethod("getLatestVersion").invoke(null).toString();
            String prefix = System.getProperty("toolkit.expectedUpdateVersion");
            if (prefix != null && !version.equals(prefix) && !version.startsWith(prefix + "-mc"))
                throw new AssertionError("Unexpected latest version: " + version);
            if (state.equals("UPDATE_AVAILABLE")) {
                String url = updater.getMethod("getDownloadUrl").invoke(null).toString();
                String minecraft = System.getProperty("toolkit.expectedMinecraft");
                if (minecraft == null || !java.net.URI.create(url).getPath().endsWith("+mc" + minecraft + ".jar"))
                    throw new AssertionError("Unexpected compatible asset: " + url);
            }
            System.out.println("PASS: real Fabric updater " + state + ", latest " + version);
            System.exit(0);
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }
}
