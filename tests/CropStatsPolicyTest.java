package com.skyblocktoolkit;

public final class CropStatsPolicyTest {
    public static void main(String[] args) {
        for(String server:new String[]{"mc-complex.com","hub.mc-complex.com:25565"," SKYBLOCK.MC-COMPLEX.COM ","hub.mc-complex.com.:25565"})
            check(CropStatsPolicy.requiresApproval(server),"Complex requires approval: "+server);
        for(String server:new String[]{null,"","localhost:25565","mc-complex.com.example.org","fake-mc-complex.com","127.0.0.1"})
            check(!CropStatsPolicy.requiresApproval(server),"Do not misidentify other servers: "+server);
        System.out.println("PASS: Complex host/port/case detection and domain boundary checks");
    }
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}
