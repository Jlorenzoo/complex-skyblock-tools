package com.skyblocktoolkit;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
public final class ToolkitConfig {
    private static final ToolkitConfig INSTANCE=new ToolkitConfig();
    public boolean boosterHud=true,chatEnabled=true,wordSelect=true;
    public boolean xpBoosterHud=true,boosterReminder=false;
    public boolean lightMode=false;
    public String startupServer=""; // Optional profile-specific auto-join; empty keeps the title screen.
    public boolean coconutSoundEnabled=true;
    public int coconutSoundThreshold=0;
    public boolean xpEarningsHud=false,xpIncludesBoosters=true,rateTestXp=false;
    public int xpHudX=12,xpHudY=74;
    public boolean cropEarningsHud=com.moneybooster.ModConfig.getInstance().isAverageEnabled();
    public boolean fishEarningsHud=com.moneybooster.ModConfig.getInstance().isAverageEnabled(),rateTestHud=true;
    public boolean clearHarvest=true;
    public int fishHudX=12,fishHudY=58;
    public boolean ahSoundEnabled=true;
    public int ahSoundType=0; // 0=Pling, 1=Level Up, 2=Orb, 3=Amethyst, 4=Bell
    public double ahSoundVolume=1.0;
    public double readySoundGain=1.9,areaSoundGain=1.9,snapSoundGain=1.9,ahSoundGain=1.9;
    public double blackoutOpacity=1.0;
    public boolean blackoutHint=true;
    public int highlight=0;
    public double highlightOpacity=.39;
    public static ToolkitConfig get(){return INSTANCE;}
    private final Path path=FabricLoader.getInstance().getConfigDir().resolve("skyblocktoolkit.json");
    private ToolkitConfig(){
        if(Files.exists(path))try{
            var j=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if(j.has("boosterHud"))boosterHud=j.get("boosterHud").getAsBoolean();
            xpBoosterHud=j.has("xpBoosterHud")?j.get("xpBoosterHud").getAsBoolean():boosterHud;
            if(j.has("boosterReminder"))boosterReminder=j.get("boosterReminder").getAsBoolean();
            if(j.has("lightMode"))lightMode=j.get("lightMode").getAsBoolean();
            if(j.has("coconutSoundEnabled"))coconutSoundEnabled=j.get("coconutSoundEnabled").getAsBoolean();
            if(j.has("coconutSoundThreshold"))coconutSoundThreshold=Math.clamp(j.get("coconutSoundThreshold").getAsInt(),0,100000);
            if(j.has("xpEarningsHud"))xpEarningsHud=j.get("xpEarningsHud").getAsBoolean();
            if(j.has("xpIncludesBoosters"))xpIncludesBoosters=j.get("xpIncludesBoosters").getAsBoolean();
            if(j.has("rateTestXp"))rateTestXp=j.get("rateTestXp").getAsBoolean();
            if(j.has("xpHudX"))xpHudX=Math.clamp(j.get("xpHudX").getAsInt(),0,10000);
            if(j.has("xpHudY"))xpHudY=Math.clamp(j.get("xpHudY").getAsInt(),0,10000);
            if(j.has("cropEarningsHud"))cropEarningsHud=j.get("cropEarningsHud").getAsBoolean();
            if(j.has("fishEarningsHud"))fishEarningsHud=j.get("fishEarningsHud").getAsBoolean();
            if(j.has("rateTestHud"))rateTestHud=j.get("rateTestHud").getAsBoolean();
            if(j.has("clearHarvest"))clearHarvest=j.get("clearHarvest").getAsBoolean();
            if(j.has("fishHudX"))fishHudX=Math.clamp(j.get("fishHudX").getAsInt(),0,10000);
            if(j.has("fishHudY"))fishHudY=Math.clamp(j.get("fishHudY").getAsInt(),0,10000);
            if(j.has("chatEnabled"))chatEnabled=j.get("chatEnabled").getAsBoolean();
            if(j.has("wordSelect"))wordSelect=j.get("wordSelect").getAsBoolean();
            if(j.has("ahSoundEnabled"))ahSoundEnabled=j.get("ahSoundEnabled").getAsBoolean();
            if(j.has("ahSoundType"))ahSoundType=Math.clamp(j.get("ahSoundType").getAsInt(),0,4);
            if(j.has("ahSoundVolume")){double v=j.get("ahSoundVolume").getAsDouble();if(Double.isFinite(v))ahSoundVolume=Math.clamp(v,0.0,1.0);}
            readySoundGain=readGain(j,"readySoundGain");areaSoundGain=readGain(j,"areaSoundGain");
            snapSoundGain=readGain(j,"snapSoundGain");ahSoundGain=readGain(j,"ahSoundGain");
            if(j.has("blackoutOpacity")){double v=j.get("blackoutOpacity").getAsDouble();if(Double.isFinite(v))blackoutOpacity=Math.clamp(v,0.0,1.0);}
            if(j.has("blackoutHint"))blackoutHint=j.get("blackoutHint").getAsBoolean();
            if(j.has("highlight"))highlight=Math.clamp(j.get("highlight").getAsInt(),0,2);
            if(j.has("highlightOpacity")){double v=j.get("highlightOpacity").getAsDouble();if(Double.isFinite(v))highlightOpacity=Math.clamp(v,.1,.8);}
            if(j.has("startupServer")&&!j.get("startupServer").isJsonNull())startupServer=j.get("startupServer").getAsString().trim();
        }catch(Exception e){System.err.println("[SkyblockToolkit] Settings could not be read: "+e.getMessage());}
    }
    public int highlightColor(){int[] colors={0x0078D7,0x89BBA0,0xC0C8D0};return ((int)(highlightOpacity*255)<<24)|colors[highlight];}
    private static double readGain(JsonObject json,String key){
        if(!json.has(key))return 1.9;
        double value=json.get(key).getAsDouble();return Double.isFinite(value)?Math.clamp(value,0,3):1.9;
    }
    public String highlightName(){return new String[]{"Blue","Sage","White"}[highlight];}
    public String ahSoundName(){return new String[]{"Pling","Level Up","Orb Pickup","Amethyst Chime","Bell"}[ahSoundType];}
    public void save(){
        Path temp=null;try{
            Files.createDirectories(path.getParent());temp=Files.createTempFile(path.getParent(),"toolkit-",".tmp");
            var j=new JsonObject();j.addProperty("boosterHud",boosterHud);j.addProperty("chatEnabled",chatEnabled);j.addProperty("wordSelect",wordSelect);
            j.addProperty("xpBoosterHud",xpBoosterHud);j.addProperty("boosterReminder",boosterReminder);
            j.addProperty("lightMode",lightMode);
            j.addProperty("startupServer",startupServer);
            j.addProperty("coconutSoundEnabled",coconutSoundEnabled);j.addProperty("coconutSoundThreshold",coconutSoundThreshold);
            j.addProperty("xpEarningsHud",xpEarningsHud);j.addProperty("xpIncludesBoosters",xpIncludesBoosters);
            j.addProperty("rateTestXp",rateTestXp);j.addProperty("xpHudX",xpHudX);j.addProperty("xpHudY",xpHudY);
            j.addProperty("cropEarningsHud",cropEarningsHud);j.addProperty("fishEarningsHud",fishEarningsHud);j.addProperty("rateTestHud",rateTestHud);
            j.addProperty("clearHarvest",clearHarvest);
            j.addProperty("fishHudX",fishHudX);j.addProperty("fishHudY",fishHudY);
            j.addProperty("ahSoundEnabled",ahSoundEnabled);j.addProperty("ahSoundType",ahSoundType);j.addProperty("ahSoundVolume",ahSoundVolume);
            j.addProperty("readySoundGain",readySoundGain);j.addProperty("areaSoundGain",areaSoundGain);
            j.addProperty("snapSoundGain",snapSoundGain);j.addProperty("ahSoundGain",ahSoundGain);
            j.addProperty("blackoutOpacity",blackoutOpacity);
            j.addProperty("blackoutHint",blackoutHint);
            j.addProperty("highlight",highlight);j.addProperty("highlightOpacity",highlightOpacity);
            Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(j)+"\n");
            try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception e){System.err.println("[SkyblockToolkit] Settings could not be saved: "+e.getMessage());}
        finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(Exception ignored){}}
    }
}
