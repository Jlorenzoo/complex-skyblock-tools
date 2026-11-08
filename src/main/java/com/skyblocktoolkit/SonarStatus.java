package com.skyblocktoolkit;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/** Read-only, loopback-only Sonar polling. Never performs network I/O on the render thread. */
final class SonarStatus {
    static volatile VoiceMusicState.Mix latest=VoiceMusicState.Mix.unavailable();
    private static volatile boolean started;
    static synchronized void start(){
        if(started)return;started=true;
        if(!System.getProperty("os.name","").startsWith("Windows"))return;
        Thread worker=new Thread(()->{
            try{
                // GG's local API uses a self-signed certificate. This client is restricted to
                // literal 127.0.0.1, no credentials, no redirects and read-only requests.
                SSLContext ssl=SSLContext.getInstance("TLS");
                ssl.init(null,new TrustManager[]{new X509TrustManager(){
                    public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
                    public void checkClientTrusted(X509Certificate[] c,String a){}
                    public void checkServerTrusted(X509Certificate[] c,String a){}
                }},null);
                var client=HttpClient.newBuilder().sslContext(ssl).connectTimeout(Duration.ofMillis(700))
                        .followRedirects(HttpClient.Redirect.NEVER).build();
                while(true){
                    try{
                        if(ToolkitConfig.get().voiceMusicHud){
                            String programData=System.getenv("ProgramData");
                            if(programData==null)throw new IllegalStateException();
                            var core=JsonParser.parseString(Files.readString(Path.of(programData,"SteelSeries","GG","coreProps.json"))).getAsJsonObject();
                            URI gg=localUri("https://"+core.get("ggEncryptedAddress").getAsString());
                            var apps=get(client,gg.resolve("/subApps")).getAsJsonObject();
                            URI base=localUri(apps.getAsJsonObject("subApps").getAsJsonObject("sonar")
                                    .getAsJsonObject("metadata").get("webServerAddress").getAsString());
                            boolean stream=get(client,base.resolve("/mode")).getAsString().equals("stream");
                            var routes=get(client,base.resolve("/streamRedirections")).getAsJsonArray();
                            var volumes=get(client,base.resolve("/volumeSettings/streamer")).getAsJsonObject();
                            var sessions=get(client,base.resolve("/audioDeviceRouting")).getAsJsonArray();
                            latest=parse(stream,routes,volumes,sessions,System.currentTimeMillis());
                        }
                    }catch(Exception e){latest=VoiceMusicState.Mix.unavailable();}
                    try{Thread.sleep(1000);}catch(InterruptedException e){return;}
                }
            }catch(Exception ignored){latest=VoiceMusicState.Mix.unavailable();}
        },"Complexify Sonar status");
        worker.setDaemon(true);worker.start();
    }
    static URI localUri(String value){
        URI uri=URI.create(value);
        if(!"127.0.0.1".equals(uri.getHost())||uri.getPort()<1||uri.getPort()>65535
                ||uri.getRawUserInfo()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null
                ||!("http".equals(uri.getScheme())||"https".equals(uri.getScheme()))
                ||!(uri.getPath().isEmpty()||uri.getPath().equals("/")))throw new IllegalArgumentException("Not a local Sonar endpoint");
        return uri;
    }
    private static JsonElement get(HttpClient c,URI uri)throws Exception{
        var response=c.send(HttpRequest.newBuilder(uri).timeout(Duration.ofMillis(900)).GET().build(),HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200||response.body().length()>262144)throw new IllegalStateException("Sonar unavailable");
        return JsonParser.parseString(response.body());
    }
    static VoiceMusicState.Mix parse(boolean mode,JsonArray routes,JsonObject volumes,JsonArray sessions,long now){
        boolean running=false,music=false,mic=false,spotify=false,other=false;
        for(var item:routes){var route=item.getAsJsonObject();
            if(!route.get("streamRedirectionId").getAsString().equals("streaming"))continue;
            running=mode&&route.get("isRunning").getAsBoolean();
            for(var source:route.getAsJsonArray("status")){var s=source.getAsJsonObject();
                String role=s.get("role").getAsString();boolean enabled=s.get("isEnabled").getAsBoolean();
                if(role.equals("media"))music=enabled&&audible(volumes.getAsJsonObject("devices").getAsJsonObject(role).getAsJsonObject("stream").getAsJsonObject("streaming"));
                if(role.equals("chatCapture"))mic=enabled&&audible(volumes.getAsJsonObject("devices").getAsJsonObject(role).getAsJsonObject("stream").getAsJsonObject("streaming"));
            }
        }
        for(var item:sessions){var device=item.getAsJsonObject();
            if(!device.get("role").getAsString().equals("media"))continue;
            for(var entry:device.getAsJsonArray("audioSessions")){var s=entry.getAsJsonObject();
                if(s.get("isSystemSound").getAsBoolean()||!s.get("state").getAsString().equals("active"))continue;
                if(s.get("processName").getAsString().equalsIgnoreCase("Spotify"))spotify=true;else other=true;
            }
        }
        boolean master=!audible(volumes.getAsJsonObject("masters").getAsJsonObject("stream").getAsJsonObject("streaming"));
        return new VoiceMusicState.Mix(true,now,running,master,music,mic,spotify,other);
    }
    private static boolean audible(JsonObject v){return !v.get("muted").getAsBoolean()&&v.get("volume").getAsDouble()>0;}
}
