package com.skyblocktoolkit;

import com.google.gson.JsonParser;
import java.util.Set;

/** Tools can produce measurable income. Custom names/lore alone do not establish that. */
public final class ItemStatsEligibility {
    private static final Set<String> IMPLEMENTS=Set.of("fishing_rod","bow","crossbow","trident","mace","shears");
    private ItemStatsEligibility(){}
    public static boolean tool(String id){
        if(id==null)return false;
        String name=id.substring(id.indexOf(':')+1);
        return IMPLEMENTS.contains(name)||name.endsWith("_hoe")||name.endsWith("_pickaxe")||name.endsWith("_axe")
            ||name.endsWith("_shovel")||name.endsWith("_sword")||name.endsWith("_spear");
    }
    public static boolean profile(ItemStatsHistory.Profile profile){
        return tool(id(profile));
    }
    public static boolean farming(ItemStatsHistory.Profile profile){String id=id(profile);return id!=null&&id.endsWith("_hoe");}
    public static boolean fishing(ItemStatsHistory.Profile profile){String id=id(profile);return id!=null&&id.substring(id.indexOf(':')+1).equals("fishing_rod");}
    private static String id(ItemStatsHistory.Profile profile){
        if(profile==null||profile.icon==null)return null;
        try{var item=JsonParser.parseString(profile.icon).getAsJsonObject();return item.has("id")?item.get("id").getAsString():null;}
        catch(Exception ignored){return null;}
    }
}
