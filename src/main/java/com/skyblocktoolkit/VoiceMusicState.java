package com.skyblocktoolkit;

/** Source switches describe routing, not a guarantee that somebody received sound. */
public final class VoiceMusicState {
    public record Voice(boolean available, boolean connected, boolean disabled, boolean muted,
                        boolean pushToTalk, boolean keyDown, boolean group, boolean inputReady,
                        String input) {}
    public record Mix(boolean available, long sampledAt, boolean running, boolean masterMuted,
                      boolean music, boolean mic, boolean spotifyActive, boolean otherMedia) {
        public static Mix unavailable(){return new Mix(false,0,false,false,false,false,false,false);}
    }
    public record Display(String title,String detail,boolean warning) {}
    public static Display display(Voice v,Mix m,long now){
        if(!v.available())return new Display("Voice unavailable","Simple Voice Chat is required",true);
        if(!v.connected())return new Display("Voice disconnected","No voice connection to the server",true);
        String audience=v.group()?"Group":"Proximity";
        if(v.disabled()||(!v.pushToTalk()&&v.muted()))return new Display("OFF",audience+" · voice chat muted",false);
        if(v.pushToTalk()&&!v.keyDown())return new Display("OFF",audience+" · push-to-talk released",false);
        if(!v.inputReady())return new Display("CHECK INPUT","Voice microphone is not ready",true);
        if(v.input()==null||!v.input().contains("SteelSeries Sonar - Stream"))
            return new Display("CHECK INPUT","Select Sonar Stream in voice settings",true);
        if(!m.available()||now-m.sampledAt()>4000||now<m.sampledAt())
            return new Display("MIX UNKNOWN","Sonar unavailable · source state unknown",true);
        if(!m.running())return new Display("CHECK MIX","Sonar Stream output is not running",true);
        boolean music=!m.masterMuted()&&m.music(),mic=!m.masterMuted()&&m.mic();
        String title=music?(mic?"MUSIC + MIC":"MUSIC"):(mic?"MIC":"OFF");
        String detail=audience+" · "+(music?(m.otherMedia()?"other apps on music channel":m.spotifyActive()?"Spotify active":"Spotify idle"):"music off");
        return new Display(title,detail,music&&m.otherMedia());
    }
    private VoiceMusicState(){}
}
