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
        new Entry(1,"Boosters","Booster timers","Money timer","money booster visibility hud timer"),
        new Entry(1,"Boosters","Booster timers","XP timer","experience booster visibility hud timer xp"),
        new Entry(1,"Boosters","Booster timers","Ending banner","ending warning title banner booster expiring alert"),
        new Entry(1,"Boosters","Booster timers","Ending screen flash","ending warning screen flash border glow booster expiring"),
        new Entry(1,"Boosters","Booster timers","Money alert","money booster activation title banner visual alert"),
        new Entry(1,"Boosters","Booster timers","XP alert","xp experience booster activation title banner visual alert expire flash"),
        new Entry(1,"Boosters","Booster timers","5-second reminder","five ending highlight soft reminder booster"),
        new Entry(1,"Boosters","Booster timers","Preview XP alert","xp experience visual flash banner test preview"),
        new Entry(1,"Boosters","Booster timers","Preview ending","expiring ending warning flash banner test preview"),
        new Entry(1,"Boosters","Earnings & statistics","History","session bonus latest past earnings booster"),
        new Entry(1,"Boosters","Earnings & statistics","Rate tests","hourly average base rate test tracker statistics"),
        new Entry(1,"Boosters","Earnings & statistics","Include boosters in crop average","money base earnings crop average booster"),
        new Entry(1,"Boosters","Coconut alerts","Coconut sound","money alert enabled coconut sound notification"),
        new Entry(1,"Boosters","Coconut alerts","Coconut banner","money alert title banner coconut notification visual"),
        new Entry(1,"Boosters","Coconut alerts","Coconut screen flash","money alert screen flash border glow coconut visual"),
        new Entry(1,"Boosters","Coconut alerts","Minimum boost (%)","coconut money percentage threshold"),
        new Entry(1,"Boosters","Coconut alerts","Preview sound","coconut alert sound test"),
        new Entry(1,"Boosters","Coconut alerts","Preview coconut","coconut visual flash banner test preview"),
        new Entry(2,"Skills","Farming","Clear Harvest textures","crop visibility immature dim desaturate farm farming clear harvest textures"),
        new Entry(2,"Skills","Farming","Crop earnings HUD","money average visibility farm farming crop earnings hud"),
        new Entry(2,"Skills","Farming","Harvest per hour HUD","crops harvested crop/h harvest per hour hud farm farming rate"),
        new Entry(2,"Skills","Farming","Include boosters in crop average","farming money base earnings crop average booster"),
        new Entry(2,"Skills","Fishing","Depleted area","warning alert enabled depleted area sound audio fishing"),
        new Entry(2,"Skills","Fishing","Fishing volume","sound audio volume fishing loudness"),
        new Entry(2,"Skills","Fishing","Preview depleted warning","warning depleted sound preview test fishing"),
        new Entry(2,"Skills","Fish sell prices","Cod · 60%","cod sell price earnings calculate fish value fishing"),
        new Entry(2,"Skills","Fish sell prices","Salmon · 25%","salmon sell price earnings calculate fish value fishing"),
        new Entry(2,"Skills","Fish sell prices","Puffer · 13%","puffer sell price earnings calculate fish value fishing"),
        new Entry(2,"Skills","Fish sell prices","Tropical · 2%","tropical sell price earnings calculate fish value fishing"),
        new Entry(2,"Skills","Fish sell prices","Earnings window","fishing earnings window 60s 5m calculate fish value"),
        new Entry(2,"Skills","Mining & XP","XP per hour HUD","experience earnings visibility xp per hour hud mining"),
        new Entry(2,"Skills","Mining & XP","Include XP boosters","experience base average include xp boosters mining"),
        new Entry(3,"Seasonals","Void Rod","Ready to reel","depth alert enabled ready reel sound audio void rod seasonal fishing"),
        new Entry(3,"Seasonals","Void Rod","Snapped line (Void Rod)","snap alert enabled void rod line broken break audio sound seasonal fishing"),
        new Entry(3,"Seasonals","Void Rod","- 1 block","reel depth target decrease void rod seasonal fishing"),
        new Entry(3,"Seasonals","Void Rod","+ 1 block","reel depth target increase void rod seasonal fishing"),
        new Entry(3,"Seasonals","Void Rod","Preview ready","reel ready sound preview test void rod seasonal fishing"),
        new Entry(3,"Seasonals","Void Rod","Preview snap","reel line snap sound preview test void rod seasonal fishing"),
        new Entry(3,"Seasonals","Bone Leggings","Audio chime alert","bone pants leggings burning rush rune seasonal armor speed boost speed alert audio chime sound"),
        new Entry(3,"Seasonals","Bone Leggings","Display style","bone pants leggings display style effect icons speed trails regen regeneration heart status toggle hud overlay"),
        new Entry(3,"Seasonals","Bone Leggings","Subtle background","bone pants leggings background glass translucent dark hud overlay"),
        new Entry(3,"Seasonals","Bone Leggings","HUD overlay","bone pants leggings speed 6 vi regen regeneration heart hud overlay top right seasonal armor on off"),
        new Entry(3,"Seasonals","Bone Leggings","Always visible","bone pants leggings speed 6 vi preview always visible hud overlay"),
        new Entry(4,"HUD & Overlays","Active HUD elements","Money timer","money booster visibility hud timer overlay"),
        new Entry(4,"HUD & Overlays","Active HUD elements","XP timer","experience booster visibility hud timer xp overlay"),
        new Entry(4,"HUD & Overlays","Active HUD elements","Crop earnings","money average visibility farm farming crop earnings hud overlay"),
        new Entry(4,"HUD & Overlays","Active HUD elements","Fishing earnings","money estimate visibility fishing earnings hud overlay"),
        new Entry(4,"HUD & Overlays","Active HUD elements","XP per hour","experience earnings visibility xp per hour hud mining overlay"),
        new Entry(4,"HUD & Overlays","Active HUD elements","Harvest / hour","crops harvested crop/h harvest per hour hud farm farming rate overlay"),
        new Entry(4,"HUD & Overlays","Active HUD elements","Bone Leggings","bone leggings pants speed 6 vi hud overlay active elements"),
        new Entry(4,"HUD & Overlays","Active HUD elements","Rate test info","hourly average base rate test tracker statistics hud overlay"),
        new Entry(4,"HUD & Overlays","Active HUD elements","Potion icons","potion icons status effect hud vanilla top right boxes effects buff debuff"),
        new Entry(4,"HUD & Overlays","Layout & positioning","Edit HUD positions","hud layout edit move positions drag canvas screen"),
        new Entry(4,"HUD & Overlays","Layout & positioning","Reset HUD positions","layout reset defaults restore hud positions defaults"),
        new Entry(6,"Utilities","Chat selection & copy","Chat selection","enabled drag copy clipboard ctrl text select chat copy"),
        new Entry(6,"Utilities","Chat selection & copy","Select word","double triple click word line expand selection chat copy"),
        new Entry(6,"Utilities","Chat selection & copy","Quick /gc code click","gc gift code card tebex copy click redeem menu quick chat copy selection"),
        new Entry(6,"Utilities","Chat selection & copy","Highlight color","selection theme style chat copy"),
        new Entry(6,"Utilities","Chat selection & copy","Highlight strength","selection opacity alpha strength visibility chat copy"),
        new Entry(6,"Utilities","Chat selection & copy","Chat size","scale font size height zoom chat copy"),
        new Entry(6,"Utilities","Chat selection & copy","Chat width","width horizontal dimensions size chat copy"),
        new Entry(6,"Utilities","Chat selection & copy","Chat opacity","opacity transparency background alpha chat copy"),
        new Entry(6,"Utilities","Auction notifications","Sale alert","notification enabled ah auction house sale sound"),
        new Entry(6,"Utilities","Auction notifications","Sound","audio type sound orb bell chime auction"),
        new Entry(6,"Utilities","Auction notifications","Volume","audio volume loudness auction sound"),
        new Entry(6,"Utilities","Auction notifications","Sound amplification","gain amplification boost louder auction"),
        new Entry(6,"Utilities","Auction notifications","Preview sale sound","sound audio preview test auction"),
        new Entry(6,"Utilities","Blackout (AFK)","Turn blackout on","dim screen audio afk blackout activate start"),
        new Entry(6,"Utilities","Blackout (AFK)","Opacity","dim screen opacity darkness transparency blackout"),
        new Entry(6,"Utilities","Display & visuals","Fullbright","fullbright gamma brightness lighting night vision dark light caves"),
        new Entry(6,"Utilities","Display & visuals","Fullbright key","fullbright key binding toggle shortcut"),
        new Entry(6,"Utilities","Keyboard shortcuts","Menu key","binding shortcut toolkit menu open gui key general"),
        new Entry(6,"Utilities","Keyboard shortcuts","Sell all key","sellall command binding shortcut farming boosters general"),
        new Entry(6,"Utilities","Keyboard shortcuts","Wake key","blackout dim binding shortcut wake key exit afk"),
        new Entry(6,"Utilities","Keyboard shortcuts","Fullbright key","fullbright lighting toggle binding shortcut key")
    );
    public static List<Entry> find(String query){return ENTRIES.stream().filter(e->e.matches(query)).toList();}
    private ToolkitSearch(){}
}
