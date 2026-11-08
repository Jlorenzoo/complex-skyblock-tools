package com.skyblocktoolkit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.fabricmc.loader.api.FabricLoader;

/** Optional reflection bridge: no mandatory dependency or copies of voice-chat classes. */
public final class VoiceMusicHud {
    static VoiceMusicState.Display preview;
    private static VoiceMusicState.Display display=new VoiceMusicState.Display("Checking voice","Reading voice and music settings",false);
    public static void tick(){
        if(!ToolkitConfig.get().voiceMusicHud)return;
        SonarStatus.start();
        display=VoiceMusicState.display(readVoice(),SonarStatus.latest,System.currentTimeMillis());
    }
    static Object call(Object target,String name)throws ReflectiveOperationException{
        return target.getClass().getMethod(name).invoke(target);
    }
    static Object config(Object config,String field)throws ReflectiveOperationException{
        Object entry=config.getClass().getField(field).get(config);
        return Class.forName("de.maxhenkel.voicechat.configbuilder.entry.ConfigEntry").getMethod("get").invoke(entry);
    }
    static VoiceMusicState.Voice readVoice(){
        try{
            if(!FabricLoader.getInstance().isModLoaded("voicechat"))throw new ClassNotFoundException();
            Class<?> manager=Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager");
            Object state=manager.getMethod("getPlayerStateManager").invoke(null);
            Object client=manager.getMethod("getClient").invoke(null);
            Object cfg=Class.forName("de.maxhenkel.voicechat.VoicechatClient").getField("CLIENT_CONFIG").get(null);
            Object connection=client==null?null:call(client,"getConnection");
            Object mic=client==null?null:call(client,"getMicThread");
            Object ptt=manager.getMethod("getPttKeyHandler").invoke(null);
            return new VoiceMusicState.Voice(true,connection!=null&&(boolean)call(connection,"isConnected"),
                    (boolean)call(state,"isDisabled"),(boolean)call(state,"isMuted"),
                    config(cfg,"microphoneActivationType").toString().equals("PTT"),(boolean)call(ptt,"isAnyDown"),
                    call(state,"getGroup")!=null,mic!=null&&!(boolean)call(mic,"isClosed"),(String)config(cfg,"microphone"));
        }catch(ReflectiveOperationException|LinkageError|RuntimeException e){
            return new VoiceMusicState.Voice(false,false,false,false,false,false,false,false,"");
        }
    }
    static VoiceMusicState.Display current(boolean editor){
        return editor?new VoiceMusicState.Display("MUSIC + MIC","Proximity · Spotify active",false):preview!=null?preview:display;
    }
    public static int width(Minecraft mc,boolean editor){var d=current(editor);return Math.min(mc.getWindow().getGuiScaledWidth()-12,Math.max(mc.font.width(d.title()),mc.font.width(d.detail()))+12);}
    public static int height(){return 30;}
    public static int x(Minecraft mc,boolean editor){return Math.clamp(ToolkitConfig.get().voiceHudX,4,Math.max(4,mc.getWindow().getGuiScaledWidth()-width(mc,editor)-4));}
    public static int y(Minecraft mc){return Math.clamp(ToolkitConfig.get().voiceHudY,4,Math.max(4,mc.getWindow().getGuiScaledHeight()-height()-4));}
    public static void render(GuiGraphicsExtractor g,Minecraft mc,boolean editor){
        var d=current(editor);var p=ToolkitTheme.palette();int x=x(mc,editor),y=y(mc),w=width(mc,editor);
        ToolkitTheme.rounded(g,x,y,w,height(),4,p.panel());
        g.text(mc.font,mc.font.plainSubstrByWidth(d.title(),w-12),x+6,y+5,d.warning()?p.error():p.text(),false);
        g.text(mc.font,mc.font.plainSubstrByWidth(d.detail(),w-12),x+6,y+17,p.muted(),false);
    }
    private VoiceMusicHud(){}
}
