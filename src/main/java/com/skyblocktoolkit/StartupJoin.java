package com.skyblocktoolkit;

/** One optional connection per launch, after the title screen finishes loading. */
public final class StartupJoin {
    private final String address;
    private boolean finished;
    private int titleTicks;

    public StartupJoin(String address) {
        this.address=address==null?"":address.trim();
        finished=this.address.isEmpty();
    }

    public String poll(boolean loading,boolean titleScreen,boolean otherConnection) {
        if(finished)return null;
        if(otherConnection){finished=true;return null;}
        if(loading||!titleScreen){titleTicks=0;return null;}
        if(++titleTicks<20)return null;
        finished=true;
        return address;
    }
}
