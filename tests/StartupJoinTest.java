package com.skyblocktoolkit;

public final class StartupJoinTest {
    public static void main(String[] args){
        var disabled=new StartupJoin("");
        for(int i=0;i<100;i++)check(disabled.poll(false,true,false)==null,"Disabled by default");
        var startup=new StartupJoin("  example.invalid:25566  ");
        for(int i=0;i<40;i++)check(startup.poll(true,true,false)==null,"Wait for resource loading");
        for(int i=0;i<19;i++)check(startup.poll(false,true,false)==null,"Wait for stable title screen");
        check(startup.poll(false,false,false)==null,"Menu navigation resets readiness");
        for(int i=0;i<19;i++)check(startup.poll(false,true,false)==null,"Readiness restarts after a menu");
        check("example.invalid:25566".equals(startup.poll(false,true,false)),"Address and explicit port retained");
        for(int i=0;i<100;i++)check(startup.poll(false,true,false)==null,"Never reconnect after first attempt");
        var manual=new StartupJoin("example.invalid");
        check(manual.poll(false,false,true)==null,"Existing/manual/quick-play connection wins");
        for(int i=0;i<40;i++)check(manual.poll(false,true,false)==null,"No retry after disconnect or failed manual connection");
        var hub=new StartupJoin("  HUB.MC-COMPLEX.COM:25565  ");
        check(hub.isComplexServer()&&!hub.isComplexSkyblock(),"Hub accepts packs without Skyblock redirect retries");
        for(int i=0;i<20;i++)hub.poll(false,true,false);
        hub.onSystemMessage(StartupJoin.REDIRECT_TIMEOUT);
        check(!hub.takeRedirectRetry(true),"Hub never retries a Skyblock redirect");
        check(!startup.isComplexServer(),"Custom startup servers keep their normal pack policy");
        var complex=new StartupJoin("Skyblock.MC-Complex.com:25565");
        for(int i=0;i<20;i++)complex.poll(false,true,false);
        complex.onSystemMessage("Redirect timed out");
        check(!complex.takeRedirectRetry(true),"Unrelated messages cannot trigger a retry");
        complex.onSystemMessage(StartupJoin.REDIRECT_TIMEOUT);
        check(complex.takeRedirectRetry(true),"Retry the initial Complex proxy timeout");
        for(int i=0;i<19;i++)check(complex.poll(false,true,false)==null,"Retry waits for stable title");
        check(complex.poll(false,true,false)!=null,"Retry connects to Complex");
        complex.onSystemMessage(StartupJoin.REDIRECT_TIMEOUT);
        check(!complex.takeRedirectRetry(true),"Never retry twice");
        var expired=new StartupJoin(StartupJoin.COMPLEX_SKYBLOCK);
        for(int i=0;i<420;i++)expired.poll(false,true,false);
        expired.onSystemMessage(StartupJoin.REDIRECT_TIMEOUT);
        check(!expired.takeRedirectRetry(true),"Ignore errors outside the initial startup window");
        var cancelled=new StartupJoin(StartupJoin.COMPLEX_SKYBLOCK);
        for(int i=0;i<20;i++)cancelled.poll(false,true,false);
        cancelled.onSystemMessage(StartupJoin.REDIRECT_TIMEOUT);
        check(!cancelled.takeRedirectRetry(false)&&!cancelled.takeRedirectRetry(true),"Never replace a different world or interrupt an open screen");
        System.out.println("PASS: startup join is optional, waits for title/loading, respects existing connections and bounds Complex redirect retries");
    }
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
}
