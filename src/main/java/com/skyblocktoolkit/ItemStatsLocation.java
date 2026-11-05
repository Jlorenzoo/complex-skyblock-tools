package com.skyblocktoolkit;

import java.util.List;

/** The received sidebar is authoritative; missing data is never guessed to be Overworld. */
public enum ItemStatsLocation {
    OVERWORLD("Overworld"), REALM("Pharaoh's Realm"), UNKNOWN("Unknown area");
    private final String label;
    ItemStatsLocation(String label){this.label=label;}
    public String label(){return label;}
    public static ItemStatsLocation sidebar(List<String> lines){
        if(lines==null||lines.isEmpty())return UNKNOWN;
        for(String line:lines)if(line!=null&&line.replaceAll("§.","").replace('’','\'').replace('ʼ','\'').replace('‘','\'')
            .toLowerCase(java.util.Locale.ROOT).matches(".*\\bpharaoh'?s\\s+realm\\b.*"))return REALM;
        return OVERWORLD;
    }
}
