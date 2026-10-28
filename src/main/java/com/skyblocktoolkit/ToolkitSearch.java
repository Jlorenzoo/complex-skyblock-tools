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
        new Entry(1,"Boosters","Timers","Money timer","money booster visibility hud timer"),
        new Entry(1,"Boosters","Timers","XP timer","experience booster visibility hud timer xp"),
        new Entry(1,"Boosters","Timers","5-second reminder","five ending highlight soft reminder booster"),
        new Entry(1,"Boosters","Earnings & history","History","session bonus latest past earnings booster"),
        new Entry(1,"Boosters","Earnings & history","Rate tests","hourly average base rate test tracker statistics"),
        new Entry(1,"Boosters","Earnings & history","Include boosters in crop average","money base earnings crop average booster"),
        new Entry(1,"Boosters","Coconut alerts","Coconut sound","money alert enabled coconut sound notification"),
        new Entry(1,"Boosters","Coconut alerts","Minimum boost (%)","coconut money percentage threshold"),
        new Entry(1,"Boosters","Coconut alerts","Preview sound","coconut alert sound test"),
        new Entry(9,"Farming","Crop visibility","Clear Harvest textures","crop visibility immature dim desaturate farm farming clear harvest textures"),
        new Entry(9,"Farming","Crop tracking","Crop earnings HUD","money average visibility farm farming crop earnings hud"),
        new Entry(9,"Farming","Crop tracking","Harvest per hour HUD","crops harvested crop/h harvest per hour hud farm farming rate"),
        new Entry(11,"Mining","Minecraft XP","XP per hour HUD","experience earnings visibility xp per hour hud mining"),
        new Entry(11,"Mining","Minecraft XP","Include XP boosters","experience base average include xp boosters mining"),
        new Entry(2,"Fishing","Catch alerts","Ready to reel","depth alert enabled ready reel sound audio"),
        new Entry(2,"Fishing","Catch alerts","Depleted area","warning alert enabled depleted area sound audio"),
        new Entry(2,"Fishing","Catch alerts","Snapped line (Void Rod)","snap alert enabled void rod line broken audio sound"),
        new Entry(2,"Fishing","Catch alerts","Volume","sound audio volume fishing loudness"),
        new Entry(2,"Fishing","Earnings & prices","Fishing earnings HUD","money estimate visibility fishing earnings hud overlay"),
        new Entry(2,"Fishing","Earnings & prices","Fish prices","cod salmon puffer tropical sell price earnings window calculate fish value"),
        new Entry(2,"Fishing","Advanced settings","- 1 block","reel depth target decrease"),
        new Entry(2,"Fishing","Advanced settings","+ 1 block","reel depth target increase"),
        new Entry(2,"Fishing","Advanced settings","Preview","reel ready sound preview test"),
        new Entry(2,"Fishing","Advanced settings","Alert sounds","gain amplification snapped depleted audio alert sounds volume"),
        new Entry(3,"Chat Copy","Selection","Chat selection","enabled drag copy clipboard ctrl text select"),
        new Entry(3,"Chat Copy","Selection","Select word","double triple click word line expand selection"),
        new Entry(3,"Chat Copy","Appearance","Highlight color","selection theme style"),
        new Entry(3,"Chat Copy","Appearance","Highlight strength","selection opacity alpha strength visibility"),
        new Entry(3,"Chat Copy","Appearance","Chat size","scale font size height zoom"),
        new Entry(3,"Chat Copy","Appearance","Chat width","width horizontal dimensions size"),
        new Entry(3,"Chat Copy","Appearance","Chat opacity","opacity transparency background alpha"),
        new Entry(4,"Auction","Sale notifications","Sale alert","notification enabled ah auction house sale sound"),
        new Entry(4,"Auction","Sale notifications","Sound","audio type sound orb bell chime auction"),
        new Entry(4,"Auction","Sale notifications","Volume","audio volume loudness auction sound"),
        new Entry(4,"Auction","Sale notifications","Sound amplification","gain amplification boost louder auction"),
        new Entry(4,"Auction","Sale notifications","Preview","sound audio preview test auction"),
        new Entry(7,"Blackout","Blackout controls","Turn blackout on","dim screen audio afk blackout activate start"),
        new Entry(7,"Blackout","Blackout controls","Opacity","dim screen opacity darkness transparency"),
        new Entry(7,"Blackout","Blackout controls","Wake hint","wake key reminder hint message afk"),
        new Entry(10,"General","Keyboard shortcuts","Menu key","binding shortcut toolkit menu open gui key"),
        new Entry(10,"General","Keyboard shortcuts","Sell all key","sellall command binding shortcut farming boosters"),
        new Entry(10,"General","Keyboard shortcuts","Wake key","blackout dim binding shortcut wake key exit"),
        new Entry(10,"General","HUD & layout","HUD elements","visibility money xp crop fishing rate tests hud elements"),
        new Entry(10,"General","HUD & layout","Edit positions","hud layout edit move positions drag"),
        new Entry(10,"General","HUD & layout","Reset HUD positions","layout reset defaults restore hud positions")
    );
    public static List<Entry> find(String query){return ENTRIES.stream().filter(e->e.matches(query)).toList();}
    private ToolkitSearch(){}
}
