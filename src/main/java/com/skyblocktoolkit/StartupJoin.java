package com.skyblocktoolkit;

/** Optional launch connection with one retry for Complex's initial proxy timeout. */
public final class StartupJoin {
    public static final String COMPLEX_HUB="hub.mc-complex.com";
    public static final String COMPLEX_SKYBLOCK="skyblock.mc-complex.com";
    static final String REDIRECT_TIMEOUT="! Failed to go to server Skyblock: Redirect timed out";
    enum Destination {
        OFF("Off", ""),
        HUB("Complex Hub", COMPLEX_HUB),
        SKYBLOCK("Complex Skyblock", COMPLEX_SKYBLOCK);

        final String label;
        final String address;

        Destination(String label, String address) {
            this.label = label;
            this.address = address;
        }

        static Destination fromAddress(String address) {
            if (address == null || address.isBlank()) return OFF;
            String host = address.strip();
            for (Destination destination : values()) {
                if (destination == OFF) continue;
                if (host.equalsIgnoreCase(destination.address) || host.equalsIgnoreCase(destination.address + ":25565")) {
                    return destination;
                }
            }
            return null;
        }
    }

    private final String address;
    private final Destination destination;
    private boolean finished,redirectTimeout,retryUsed;
    private int titleTicks,retryWindow;

    public StartupJoin(String address) {
        this.address=address==null?"":address.trim();
        destination=Destination.fromAddress(this.address);
        finished=this.address.isEmpty();
    }

    public boolean isComplexSkyblock() {
        return destination==Destination.SKYBLOCK;
    }

    boolean isComplexServer() {
        return destination==Destination.HUB||destination==Destination.SKYBLOCK;
    }

    public String poll(boolean loading,boolean titleScreen,boolean otherConnection) {
        if(retryWindow>0)retryWindow--;
        if(finished)return null;
        if(otherConnection){finished=true;return null;}
        if(loading||!titleScreen){titleTicks=0;return null;}
        if(++titleTicks<20)return null;
        finished=true;
        // The observed proxy error arrives seconds after connecting. Only the
        // initial attempt can arm this short window, never the retry.
        if(isComplexSkyblock()&&!retryUsed)retryWindow=20*20;
        return address;
    }

    public void onSystemMessage(String message) {
        if(retryWindow>0&&REDIRECT_TIMEOUT.equals(message.strip()))redirectTimeout=true;
    }

    public boolean takeRedirectRetry(boolean stillInStartupWorld) {
        if(!redirectTimeout||retryWindow<=0)return false;
        retryWindow=0;
        retryUsed=true;
        if(!stillInStartupWorld)return false;
        finished=false;
        titleTicks=0;
        return true;
    }
}
