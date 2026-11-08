package com.skyblocktoolkit;

import com.google.gson.*;

public final class VoiceMusicStateTest {
    static final long NOW=10000;
    static VoiceMusicState.Voice voice(boolean muted,boolean ptt,boolean down,boolean group,String input){
        return new VoiceMusicState.Voice(true,true,false,muted,ptt,down,group,true,input);
    }
    static VoiceMusicState.Mix mix(boolean music,boolean mic){return new VoiceMusicState.Mix(true,NOW,true,false,music,mic,true,false);}
    static void check(boolean b){if(!b)throw new AssertionError();}
    static String title(VoiceMusicState.Voice v,VoiceMusicState.Mix m){return VoiceMusicState.display(v,m,NOW).title();}
    public static void main(String[] args){
        var stream=voice(false,false,false,false,"OpenAL Soft on SteelSeries Sonar - Stream");
        check(title(stream,mix(false,false)).equals("OFF"));
        check(title(stream,mix(true,false)).equals("MUSIC"));
        check(title(stream,mix(false,true)).equals("MIC"));
        check(title(stream,mix(true,true)).equals("MUSIC + MIC"));
        check(title(voice(true,false,false,false,stream.input()),mix(true,true)).equals("OFF"));
        check(title(voice(true,true,false,false,stream.input()),mix(true,true)).equals("OFF"));
        check(title(voice(true,true,true,false,stream.input()),mix(true,true)).equals("MUSIC + MIC"));
        check(title(voice(false,false,false,false,"Sonar - Microphone"),mix(true,true)).equals("CHECK INPUT"));
        check(title(voice(false,false,false,false,""),mix(true,true)).equals("CHECK INPUT"));
        check(VoiceMusicState.display(voice(false,false,false,true,stream.input()),mix(true,true),NOW).detail().startsWith("Group"));
        check(VoiceMusicState.display(stream,mix(true,true),NOW).detail().startsWith("Proximity"));
        check(VoiceMusicState.display(stream,mix(true,true),NOW+4001).title().equals("MIX UNKNOWN"));
        check(title(stream,VoiceMusicState.Mix.unavailable()).equals("MIX UNKNOWN"));
        check(title(stream,new VoiceMusicState.Mix(true,NOW,false,false,true,true,true,false)).equals("CHECK MIX"));
        check(title(stream,new VoiceMusicState.Mix(true,NOW,true,true,true,true,true,false)).equals("OFF"));
        check(VoiceMusicState.display(stream,new VoiceMusicState.Mix(true,NOW,true,false,true,false,true,true),NOW).warning());
        for(String bad:new String[]{"https://example.com:123","http://localhost:3","http://127.0.0.1:3@evil.test:3","http://127.0.0.1:3/path","file:///tmp/sonar","http://127.0.0.1:3?redirect=evil"}){
            try{SonarStatus.localUri(bad);throw new AssertionError(bad);}catch(IllegalArgumentException expected){}
        }
        check(SonarStatus.localUri("https://127.0.0.1:6327").getPort()==6327);
        JsonArray routes=JsonParser.parseString("[{streamRedirectionId:'streaming',isRunning:true,status:[{role:'media',isEnabled:true},{role:'chatCapture',isEnabled:true}]}]").getAsJsonArray();
        JsonObject volumes=JsonParser.parseString("{masters:{stream:{streaming:{volume:1,muted:false}}},devices:{media:{stream:{streaming:{volume:0.25,muted:false}}},chatCapture:{stream:{streaming:{volume:1,muted:true}}}}}").getAsJsonObject();
        JsonArray sessions=JsonParser.parseString("[{role:'media',audioSessions:[{isSystemSound:false,state:'active',processName:'Spotify'},{isSystemSound:false,state:'active',processName:'Firefox'}]}]").getAsJsonArray();
        var parsed=SonarStatus.parse(true,routes,volumes,sessions,NOW);
        check(parsed.running()&&parsed.music()&&!parsed.mic()&&parsed.spotifyActive()&&parsed.otherMedia());
        volumes.getAsJsonObject("masters").getAsJsonObject("stream").getAsJsonObject("streaming").addProperty("volume",0);
        check(SonarStatus.parse(true,routes,volumes,sessions,NOW).masterMuted());
        check(!SonarStatus.parse(false,routes,volumes,sessions,NOW).running());
        System.out.println("PASS: voice/music source combinations, real mute/PTT/group state, wrong input, stale/unknown mix, routing parser and loopback restrictions");
    }
}
