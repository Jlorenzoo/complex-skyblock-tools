package com.skyblocktoolkit;

/** Optional launch connection with one retry for Complex's initial proxy timeout. */
public final class StartupJoin {
    public static final String COMPLEX_SKYBLOCK="skyblock.mc-complex.com";
    static final String REDIRECT_TIMEOUT="! Failed to go to server Skyblock: Redirect timed out";
    private final String address;
    private boolean finished,redirectTimeout,retryUsed;
    private int titleTicks,retryWindow;

    public StartupJoin(String address) {
        this.address=address==null?"":address.trim();
        finished=this.address.isEmpty();
    }

    public boolean isComplexSkyblock() {
        return address.equalsIgnoreCase(COMPLEX_SKYBLOCK)||address.equalsIgnoreCase(COMPLEX_SKYBLOCK+":25565");
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
