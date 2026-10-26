package com.skyblocktoolkit;

import java.util.List;
import java.util.Locale;

/** Search destinations refer to section names, never fragile pixel offsets. */
public final class ToolkitSearch {
    public record Entry(int page,String category,String section,String keywords){
        public String title(){return category+" / "+section;}
        public boolean matches(String query){
            String haystack=(title()+" "+keywords).toLowerCase(Locale.ROOT);
            return query.toLowerCase(Locale.ROOT).strip().isEmpty() ||
                java.util.Arrays.stream(query.toLowerCase(Locale.ROOT).strip().split("\\s+")).allMatch(haystack::contains);
        }
    }
    public static final List<Entry> ENTRIES=List.of(
        new Entry(1,"Boosters","Timers","money xp visibility hud five 5 second reminder ending"),
        new Entry(1,"Boosters","Earnings & history","session bonus latest history rate tests average base include boosters"),
        new Entry(1,"Boosters","Coconut alerts","sound preview minimum boost percentage threshold money"),
        new Entry(1,"Boosters","Quick selling","sell all sellall key binding shortcut"),
        new Entry(9,"Farming","Crop visibility","clear harvest textures immature dim desaturate"),
        new Entry(9,"Farming","Crop earnings","money hud average rate tests include boosters base sell all sellall key binding shortcut"),
        new Entry(11,"Mining","Minecraft XP","experience xp per hour hud include boosters rate tests"),

        new Entry(2,"Fishing","Catch alerts","ready reel depleted area snapped line sound volume preview depth target"),
        new Entry(2,"Fishing","Earnings & prices","fish prices cod salmon puffer tropical sell window hud"),
        new Entry(3,"Chat Copy","Selection","enabled drag copy clipboard ctrl word double triple click"),
        new Entry(3,"Chat Copy","Appearance","highlight color opacity chat size width"),
        new Entry(4,"Auction","Sale notifications","alert sound volume preview gain amplification"),
        new Entry(7,"Blackout","Blackout controls","dim screen opacity wake key hint audio"),
        new Entry(10,"General","Keyboard shortcuts","menu key sell all sellall wake blackout key binding"),
        new Entry(10,"General","HUD & layout","hud elements visibility edit positions reset crop fishing xp money")
    );
    public static List<Entry> find(String query){return ENTRIES.stream().filter(e->e.matches(query)).toList();}
    private ToolkitSearch(){}
}
