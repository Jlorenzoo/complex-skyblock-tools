package com.skyblocktoolkit;

import java.util.List;
import java.util.Locale;

/** One destination per setting; aliases belong to that setting, never a whole section. */
public final class ToolkitSearch {
    public record Entry(int page,String category,String section,String setting,String keywords){
        public String title(){return category+" / "+setting;}
        public boolean matches(String query){
            String haystack=(title()+" "+keywords).toLowerCase(Locale.ROOT);
            return java.util.Arrays.stream(query.toLowerCase(Locale.ROOT).strip().split("\\s+"))
                .allMatch(haystack::contains);
        }
    }
    public static final List<Entry> ENTRIES=List.of(
        new Entry(1,"Boosters","Timers","Money timer","money booster visibility hud"),
        new Entry(1,"Boosters","Timers","XP timer","experience booster visibility hud"),
        new Entry(1,"Boosters","Timers","5-second reminder","five ending highlight"),
        new Entry(1,"Boosters","Earnings & history","History","session bonus latest"),
        new Entry(1,"Boosters","Earnings & history","Rate tests","hourly average base"),
        new Entry(1,"Boosters","Earnings & history","Include boosters in crop average","money base earnings"),
        new Entry(1,"Boosters","Coconut alerts","Coconut sound","money alert enabled"),
        new Entry(1,"Boosters","Coconut alerts","Minimum boost (%)","coconut money percentage threshold"),
        new Entry(1,"Boosters","Coconut alerts","Preview sound","coconut alert"),
        new Entry(9,"Farming","Crop visibility","Clear Harvest textures","crop visibility immature dim desaturate"),
        new Entry(9,"Farming","Crop earnings","Crop earnings HUD","money average visibility"),
        new Entry(11,"Mining","Minecraft XP","XP per hour HUD","experience earnings visibility"),
        new Entry(11,"Mining","Minecraft XP","Include XP boosters","experience base average"),
        new Entry(2,"Fishing","Catch alerts","Ready to reel","depth alert enabled"),
        new Entry(2,"Fishing","Catch alerts","Depleted area","warning alert enabled"),
        new Entry(2,"Fishing","Catch alerts","Snapped line (Void Rod)","snap alert enabled"),
        new Entry(2,"Fishing","Catch alerts","Volume","sound audio"),
        new Entry(2,"Fishing","Earnings & prices","Fishing earnings HUD","money estimate visibility"),
        new Entry(2,"Fishing","Earnings & prices","Fish prices","cod salmon puffer tropical sell price earnings window"),
        new Entry(2,"Fishing","Advanced settings","- 1 block","reel depth target decrease"),
        new Entry(2,"Fishing","Advanced settings","+ 1 block","reel depth target increase"),
        new Entry(2,"Fishing","Advanced settings","Preview","reel ready sound"),
        new Entry(2,"Fishing","Advanced settings","Alert sounds","gain amplification snapped depleted"),
        new Entry(3,"Chat Copy","Selection","Chat selection","enabled drag copy clipboard ctrl"),
        new Entry(3,"Chat Copy","Selection","Select word","double triple click"),
        new Entry(3,"Chat Copy","Appearance","Highlight color","selection"),
        new Entry(3,"Chat Copy","Appearance","Highlight strength","selection opacity"),
        new Entry(3,"Chat Copy","Appearance","Chat size","scale"),
        new Entry(3,"Chat Copy","Appearance","Chat width",""),
        new Entry(3,"Chat Copy","Appearance","Chat opacity",""),
        new Entry(4,"Auction","Sale notifications","Sale alert","notification enabled"),
        new Entry(4,"Auction","Sale notifications","Sound","audio type"),
        new Entry(4,"Auction","Sale notifications","Volume","audio"),
        new Entry(4,"Auction","Sale notifications","Sound amplification","gain"),
        new Entry(4,"Auction","Sale notifications","Preview","sound audio"),
        new Entry(7,"Blackout","Blackout controls","Turn blackout on","dim screen audio"),
        new Entry(7,"Blackout","Blackout controls","Opacity","dim screen"),
        new Entry(7,"Blackout","Blackout controls","Wake hint",""),
        new Entry(10,"General","Keyboard shortcuts","Menu key","binding shortcut toolkit"),
        new Entry(10,"General","Keyboard shortcuts","Sell all key","sellall command binding shortcut farming boosters"),
        new Entry(10,"General","Keyboard shortcuts","Wake key","blackout dim binding shortcut"),
        new Entry(10,"General","HUD & layout","HUD elements","visibility money xp crop fishing rate tests"),
        new Entry(10,"General","HUD & layout","Edit positions","hud layout"),
        new Entry(10,"General","HUD & layout","Reset HUD positions","layout")
    );
    public static List<Entry> find(String query){return ENTRIES.stream().filter(e->e.matches(query)).toList();}
    private ToolkitSearch(){}
}
