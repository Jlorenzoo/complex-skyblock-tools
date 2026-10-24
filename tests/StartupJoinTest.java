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
        System.out.println("PASS: startup join is optional, waits for title/loading, respects existing connections and attempts only once");
    }
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
}
