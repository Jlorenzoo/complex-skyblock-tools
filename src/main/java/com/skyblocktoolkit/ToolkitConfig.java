package com.skyblocktoolkit;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.EnumMap;
import com.skyblocktoolkit.ToolkitAudio.Cue;
import com.skyblocktoolkit.ToolkitAudio.Sample;
public final class ToolkitConfig {
    private static final ToolkitConfig INSTANCE=new ToolkitConfig();
    public boolean boosterHud=true,chatEnabled=true,wordSelect=true,gcCodeClick=true;
    public boolean xpBoosterHud=true,boosterReminder=false,boosterExpiringAlert=true,expiringFlash=false;
    public boolean lightMode=false;
    public boolean voiceMusicHud=false;
    public int voiceHudX=12,voiceHudY=110;
    public String startupServer=""; // Optional profile-specific auto-join; empty keeps the title screen.
    public boolean startupServerPacks=true; // Accept packs only when auto-joining Complex.
    public boolean coconutSoundEnabled=true,coconutVisualAlert=true,coconutFlash=false,moneyVisualAlert=true,xpVisualAlert=true;
    public int coconutSoundThreshold=0;
    public boolean xpEarningsHud=false,xpIncludesBoosters=true,rateTestXp=false;
    public int xpHudX=12,xpHudY=74;
    public boolean cropEarningsHud=com.moneybooster.ModConfig.getInstance().isAverageEnabled();
    public boolean cropHarvestHud=true;
    public boolean includeGoldShards=false;
    public double goldShardPrice,voteKeyPrice;
    public int cropHarvestHudX=12,cropHarvestHudY=90;
    public boolean fishEarningsHud=com.moneybooster.ModConfig.getInstance().isAverageEnabled(),rateTestHud=true;
    public boolean clearHarvest=true;
    public int fishHudX=12,fishHudY=58;
    public boolean ahSoundEnabled=true;
    private final EnumMap<Cue,Sample> alertSounds=new EnumMap<>(Cue.class);
    public double ahSoundVolume=1.0;
    public double readySoundGain=1.9,areaSoundGain=1.9,snapSoundGain=1.9,ahSoundGain=1.9;
    public double blackoutOpacity=1.0;
    public boolean blackoutHint=true;
    public boolean blackoutChatOverlay=false;
    public int highlight=0;
    public double highlightOpacity=.39;
    public boolean boneLeggingsSpeedAlert=true;
    public boolean boneLeggingsHud=true,boneLeggingsAlwaysVisible=false;
    public int boneLeggingsHudX=-1,boneLeggingsHudY=12;
    public int boneLeggingsHudStyle=0; // 0=Leggings & effect badge, 1=Icon & Mode, 2=Effect badge only, 3=Mode only
    public boolean boneLeggingsBackground=false;
    public boolean treasureChestHud=false,treasureLastThirtySeconds=false,treasureShowChests=true,treasureInferOpens=true,treasureBackground=true;
    public int treasureDetailsSeconds=120,treasureHudX=-1,treasureHudY=48;
    public double treasureScale=1;
    public boolean fullBright=false;
    public boolean showStatusEffects=true;
    public static ToolkitConfig get(){return INSTANCE;}
    private final Path path;
    private ToolkitConfig(){
        Path p = null;
        try {
            var loader = FabricLoader.getInstance();
            if (loader != null) {
                var dir = loader.getConfigDir();
                if (dir != null) p = dir.resolve("skyblocktoolkit.json");
            }
        } catch (Throwable ignored) {}
        this.path = p;
        boolean sharedPricesSaved=false;
        if(path!=null&&Files.exists(path))try{
            var j=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if(j.has("boosterHud"))boosterHud=j.get("boosterHud").getAsBoolean();
            xpBoosterHud=j.has("xpBoosterHud")?j.get("xpBoosterHud").getAsBoolean():boosterHud;
            if(j.has("boosterReminder"))boosterReminder=j.get("boosterReminder").getAsBoolean();
            if(j.has("boosterExpiringAlert"))boosterExpiringAlert=j.get("boosterExpiringAlert").getAsBoolean();
            if(j.has("expiringFlash"))expiringFlash=j.get("expiringFlash").getAsBoolean();
            if(j.has("lightMode"))lightMode=j.get("lightMode").getAsBoolean();
            if(j.has("voiceMusicHud"))voiceMusicHud=j.get("voiceMusicHud").getAsBoolean();
            if(j.has("voiceHudX"))voiceHudX=Math.clamp(j.get("voiceHudX").getAsInt(),4,10000);
            if(j.has("voiceHudY"))voiceHudY=Math.clamp(j.get("voiceHudY").getAsInt(),4,10000);
            if(j.has("coconutSoundEnabled"))coconutSoundEnabled=j.get("coconutSoundEnabled").getAsBoolean();
            if(j.has("coconutVisualAlert"))coconutVisualAlert=j.get("coconutVisualAlert").getAsBoolean();
            if(j.has("coconutFlash"))coconutFlash=j.get("coconutFlash").getAsBoolean();
            if(j.has("moneyVisualAlert"))moneyVisualAlert=j.get("moneyVisualAlert").getAsBoolean();
            if(j.has("xpVisualAlert"))xpVisualAlert=j.get("xpVisualAlert").getAsBoolean();
            if(j.has("coconutSoundThreshold"))coconutSoundThreshold=Math.clamp(j.get("coconutSoundThreshold").getAsInt(),0,100000);
            if(j.has("xpEarningsHud"))xpEarningsHud=j.get("xpEarningsHud").getAsBoolean();
            if(j.has("xpIncludesBoosters"))xpIncludesBoosters=j.get("xpIncludesBoosters").getAsBoolean();
            if(j.has("rateTestXp"))rateTestXp=j.get("rateTestXp").getAsBoolean();
            if(j.has("xpHudX"))xpHudX=Math.clamp(j.get("xpHudX").getAsInt(),0,10000);
            if(j.has("xpHudY"))xpHudY=Math.clamp(j.get("xpHudY").getAsInt(),0,10000);
            if(j.has("cropEarningsHud"))cropEarningsHud=j.get("cropEarningsHud").getAsBoolean();
            if(j.has("includeGoldShards"))includeGoldShards=j.get("includeGoldShards").getAsBoolean();
            goldShardPrice=readPrice(j,"goldShardPrice");
            sharedPricesSaved=j.has("voteKeyPrice");
            voteKeyPrice=readPrice(j,"voteKeyPrice");
            if(j.has("cropHarvestHud"))cropHarvestHud=j.get("cropHarvestHud").getAsBoolean();
            if(j.has("cropHarvestHudX"))cropHarvestHudX=Math.clamp(j.get("cropHarvestHudX").getAsInt(),0,10000);
            if(j.has("cropHarvestHudY"))cropHarvestHudY=Math.clamp(j.get("cropHarvestHudY").getAsInt(),0,10000);
            if(j.has("fishEarningsHud"))fishEarningsHud=j.get("fishEarningsHud").getAsBoolean();
            if(j.has("rateTestHud"))rateTestHud=j.get("rateTestHud").getAsBoolean();
            if(j.has("clearHarvest"))clearHarvest=j.get("clearHarvest").getAsBoolean();
            if(j.has("fishHudX"))fishHudX=Math.clamp(j.get("fishHudX").getAsInt(),0,10000);
            if(j.has("fishHudY"))fishHudY=Math.clamp(j.get("fishHudY").getAsInt(),0,10000);
            if(j.has("chatEnabled"))chatEnabled=j.get("chatEnabled").getAsBoolean();
            if(j.has("wordSelect"))wordSelect=j.get("wordSelect").getAsBoolean();
            if(j.has("gcCodeClick"))gcCodeClick=j.get("gcCodeClick").getAsBoolean();
            if(j.has("ahSoundEnabled"))ahSoundEnabled=j.get("ahSoundEnabled").getAsBoolean();
            readAlertSounds(j);
            if(j.has("ahSoundVolume")){double v=j.get("ahSoundVolume").getAsDouble();if(Double.isFinite(v))ahSoundVolume=Math.clamp(v,0.0,1.0);}
            readySoundGain=readGain(j,"readySoundGain");areaSoundGain=readGain(j,"areaSoundGain");
            snapSoundGain=readGain(j,"snapSoundGain");ahSoundGain=readGain(j,"ahSoundGain");
            if(j.has("blackoutOpacity")){double v=j.get("blackoutOpacity").getAsDouble();if(Double.isFinite(v))blackoutOpacity=Math.clamp(v,0.0,1.0);}
            if(j.has("blackoutHint"))blackoutHint=j.get("blackoutHint").getAsBoolean();
            if(j.has("blackoutChatOverlay"))blackoutChatOverlay=j.get("blackoutChatOverlay").getAsBoolean();
            if(j.has("highlight"))highlight=Math.clamp(j.get("highlight").getAsInt(),0,2);
            if(j.has("highlightOpacity")){double v=j.get("highlightOpacity").getAsDouble();if(Double.isFinite(v))highlightOpacity=Math.clamp(v,.1,.8);}
            if(j.has("boneLeggingsSpeedAlert"))boneLeggingsSpeedAlert=j.get("boneLeggingsSpeedAlert").getAsBoolean();
            if(j.has("boneLeggingsHud"))boneLeggingsHud=j.get("boneLeggingsHud").getAsBoolean();
            if(j.has("boneLeggingsAlwaysVisible"))boneLeggingsAlwaysVisible=j.get("boneLeggingsAlwaysVisible").getAsBoolean();
            if(j.has("boneLeggingsHudX"))boneLeggingsHudX=Math.clamp(j.get("boneLeggingsHudX").getAsInt(),-1,10000);
            if(j.has("boneLeggingsHudY"))boneLeggingsHudY=Math.clamp(j.get("boneLeggingsHudY").getAsInt(),0,10000);
            if(j.has("boneLeggingsHudStyle"))boneLeggingsHudStyle=Math.clamp(j.get("boneLeggingsHudStyle").getAsInt(),0,3);
            if(j.has("boneLeggingsBackground"))boneLeggingsBackground=j.get("boneLeggingsBackground").getAsBoolean();
            if(j.has("treasureChestHud"))treasureChestHud=j.get("treasureChestHud").getAsBoolean();
            if(j.has("treasureLastThirtySeconds"))treasureLastThirtySeconds=j.get("treasureLastThirtySeconds").getAsBoolean();
            if(j.has("treasureShowChests"))treasureShowChests=j.get("treasureShowChests").getAsBoolean();
            if(j.has("treasureInferOpens"))treasureInferOpens=j.get("treasureInferOpens").getAsBoolean();
            if(j.has("treasureBackground"))treasureBackground=j.get("treasureBackground").getAsBoolean();
            if(j.has("treasureDetailsSeconds"))treasureDetailsSeconds=Math.clamp(j.get("treasureDetailsSeconds").getAsInt(),30,300);
            if(j.has("treasureHudX"))treasureHudX=Math.clamp(j.get("treasureHudX").getAsInt(),-1,10000);
            if(j.has("treasureHudY"))treasureHudY=Math.clamp(j.get("treasureHudY").getAsInt(),0,10000);
            if(j.has("treasureScale")){double v=j.get("treasureScale").getAsDouble();if(Double.isFinite(v))treasureScale=Math.clamp(v,.6,1.5);}
            if(j.has("fullBright"))fullBright=j.get("fullBright").getAsBoolean();
            if(j.has("showStatusEffects"))showStatusEffects=j.get("showStatusEffects").getAsBoolean();
            if(j.has("startupServer")&&!j.get("startupServer").isJsonNull())startupServer=j.get("startupServer").getAsString().trim();
            if(j.has("startupServerPacks"))startupServerPacks=j.get("startupServerPacks").getAsBoolean();
        }catch(Exception e){System.err.println("[SkyblockToolkit] Settings could not be read: "+e.getMessage());}
        if(path!=null&&!sharedPricesSaved){
            Path legacy=path.resolveSibling("fishingpings.json");
            if(Files.exists(legacy))try{
                var prices=JsonParser.parseString(Files.readString(legacy)).getAsJsonObject();
                if(goldShardPrice==0)goldShardPrice=readPrice(prices,"oniShardPrice");
                voteKeyPrice=readPrice(prices,"oniVoteKeyPrice");
            }catch(Exception e){System.err.println("[SkyblockToolkit] Legacy item prices could not be read: "+e.getMessage());}
        }
    }
    private static double readPrice(JsonObject json,String key){
        if(!json.has(key))return 0;
        double value=json.get(key).getAsDouble();
        return Double.isFinite(value)&&value>=0&&value<=1e12?value:0;
    }
    public int highlightColor(){int[] colors={0x0078D7,0x89BBA0,0xC0C8D0};return ((int)(highlightOpacity*255)<<24)|colors[highlight];}
    private static double readGain(JsonObject json,String key){
        if(!json.has(key))return 1.9;
        double value=json.get(key).getAsDouble();return Double.isFinite(value)?Math.clamp(value,0,3):1.9;
    }
    public String highlightName(){return new String[]{"Blue","Sage","White"}[highlight];}
    private void readAlertSounds(JsonObject settings){
        if(settings.has("ahSoundType")&&settings.get("ahSoundType").isJsonPrimitive()){
            try{
                int old=settings.get("ahSoundType").getAsInt();
                if(old>=1&&old<=4)alertSounds.put(Cue.AUCTION,Cue.AUCTION.choices.get(3+old));
            }catch(NumberFormatException ignored){}
        }
        if(!settings.has("alertSounds")||!settings.get("alertSounds").isJsonObject())return;
        var saved=settings.getAsJsonObject("alertSounds");
        for(Cue cue:Cue.values()){
            if(!saved.has(cue.name())||!saved.get(cue.name()).isJsonPrimitive())continue;
            try{
                Sample sample=Sample.valueOf(saved.get(cue.name()).getAsString());
                if(cue.choices.contains(sample))alertSounds.put(cue,sample);
            }catch(IllegalArgumentException ignored){}
        }
    }
    public Sample audioChoice(Cue cue){return alertSounds.getOrDefault(cue,cue.choices.getFirst());}
    void selectAudio(Cue cue,Sample sample){
        if(cue.choices.contains(sample))alertSounds.put(cue,sample);
    }
    public void save(){
        if(path==null)return;
        Path temp=null;try{
            Files.createDirectories(path.getParent());temp=Files.createTempFile(path.getParent(),"toolkit-",".tmp");
            var j=new JsonObject();j.addProperty("boosterHud",boosterHud);j.addProperty("chatEnabled",chatEnabled);j.addProperty("wordSelect",wordSelect);
            j.addProperty("gcCodeClick",gcCodeClick);
            j.addProperty("xpBoosterHud",xpBoosterHud);j.addProperty("boosterReminder",boosterReminder);
            j.addProperty("boosterExpiringAlert",boosterExpiringAlert);
            j.addProperty("expiringFlash",expiringFlash);
            j.addProperty("lightMode",lightMode);
            j.addProperty("voiceMusicHud",voiceMusicHud);j.addProperty("voiceHudX",voiceHudX);j.addProperty("voiceHudY",voiceHudY);
            j.addProperty("startupServer",startupServer);j.addProperty("startupServerPacks",startupServerPacks);
            j.addProperty("coconutSoundEnabled",coconutSoundEnabled);j.addProperty("coconutVisualAlert",coconutVisualAlert);
            j.addProperty("coconutFlash",coconutFlash);
            j.addProperty("moneyVisualAlert",moneyVisualAlert);j.addProperty("xpVisualAlert",xpVisualAlert);
            j.addProperty("coconutSoundThreshold",coconutSoundThreshold);
            j.addProperty("xpEarningsHud",xpEarningsHud);j.addProperty("xpIncludesBoosters",xpIncludesBoosters);
            j.addProperty("rateTestXp",rateTestXp);j.addProperty("xpHudX",xpHudX);j.addProperty("xpHudY",xpHudY);
            j.addProperty("cropEarningsHud",cropEarningsHud);
            j.addProperty("cropHarvestHud",cropHarvestHud);
            j.addProperty("includeGoldShards",includeGoldShards);j.addProperty("goldShardPrice",goldShardPrice);
            j.addProperty("voteKeyPrice",voteKeyPrice);
            j.addProperty("cropHarvestHudX",cropHarvestHudX);
            j.addProperty("cropHarvestHudY",cropHarvestHudY);
            j.addProperty("fishEarningsHud",fishEarningsHud);j.addProperty("rateTestHud",rateTestHud);
            j.addProperty("clearHarvest",clearHarvest);
            j.addProperty("fishHudX",fishHudX);j.addProperty("fishHudY",fishHudY);
            j.addProperty("ahSoundEnabled",ahSoundEnabled);j.addProperty("ahSoundVolume",ahSoundVolume);
            var sounds=new JsonObject();
            alertSounds.forEach((cue,sample)->sounds.addProperty(cue.name(),sample.name()));
            j.add("alertSounds",sounds);
            j.addProperty("readySoundGain",readySoundGain);j.addProperty("areaSoundGain",areaSoundGain);
            j.addProperty("snapSoundGain",snapSoundGain);j.addProperty("ahSoundGain",ahSoundGain);
            j.addProperty("blackoutOpacity",blackoutOpacity);
            j.addProperty("blackoutHint",blackoutHint);
            j.addProperty("blackoutChatOverlay",blackoutChatOverlay);
            j.addProperty("highlight",highlight);j.addProperty("highlightOpacity",highlightOpacity);
            j.addProperty("boneLeggingsSpeedAlert",boneLeggingsSpeedAlert);
            j.addProperty("boneLeggingsHud",boneLeggingsHud);
            j.addProperty("boneLeggingsAlwaysVisible",boneLeggingsAlwaysVisible);
            j.addProperty("boneLeggingsHudX",boneLeggingsHudX);
            j.addProperty("boneLeggingsHudY",boneLeggingsHudY);
            j.addProperty("boneLeggingsHudStyle",boneLeggingsHudStyle);
            j.addProperty("boneLeggingsBackground",boneLeggingsBackground);
            j.addProperty("treasureLastThirtySeconds",treasureLastThirtySeconds);
            j.addProperty("treasureChestHud",treasureChestHud);j.addProperty("treasureShowChests",treasureShowChests);
            j.addProperty("treasureInferOpens",treasureInferOpens);j.addProperty("treasureBackground",treasureBackground);
            j.addProperty("treasureDetailsSeconds",treasureDetailsSeconds);j.addProperty("treasureScale",treasureScale);
            j.addProperty("treasureHudX",treasureHudX);j.addProperty("treasureHudY",treasureHudY);
            j.addProperty("fullBright",fullBright);
            j.addProperty("showStatusEffects",showStatusEffects);
            Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(j)+"\n");
            try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception e){System.err.println("[SkyblockToolkit] Settings could not be saved: "+e.getMessage());}
        finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(Exception ignored){}}
    }
}
