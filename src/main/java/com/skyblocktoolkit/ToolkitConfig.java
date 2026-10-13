package com.skyblocktoolkit;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
public final class ToolkitConfig {
    private static final ToolkitConfig INSTANCE=new ToolkitConfig();
    public boolean boosterHud=true,chatEnabled=true,wordSelect=true;
    public int earningsMode=0; // 0=automatic, 1=farming, 2=fishing
    public int highlight=0;
    public double highlightOpacity=.39;
    public static ToolkitConfig get(){return INSTANCE;}
    private final Path path=FabricLoader.getInstance().getConfigDir().resolve("skyblocktoolkit.json");
    private ToolkitConfig(){
        if(Files.exists(path))try{
            var j=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if(j.has("boosterHud"))boosterHud=j.get("boosterHud").getAsBoolean();
            if(j.has("chatEnabled"))chatEnabled=j.get("chatEnabled").getAsBoolean();
            if(j.has("wordSelect"))wordSelect=j.get("wordSelect").getAsBoolean();
            if(j.has("earningsMode"))earningsMode=Math.clamp(j.get("earningsMode").getAsInt(),0,2);
            if(j.has("highlight"))highlight=Math.clamp(j.get("highlight").getAsInt(),0,2);
            if(j.has("highlightOpacity")){double v=j.get("highlightOpacity").getAsDouble();if(Double.isFinite(v))highlightOpacity=Math.clamp(v,.1,.8);}
        }catch(Exception e){System.err.println("[SkyblockToolkit] Settings could not be read: "+e.getMessage());}
    }
    public int highlightColor(){int[] colors={0x0078D7,0x89BBA0,0xC0C8D0};return ((int)(highlightOpacity*255)<<24)|colors[highlight];}
    public String highlightName(){return new String[]{"Blue","Sage","White"}[highlight];}
    public void save(){
        Path temp=null;try{
            Files.createDirectories(path.getParent());temp=Files.createTempFile(path.getParent(),"toolkit-",".tmp");
            var j=new JsonObject();j.addProperty("boosterHud",boosterHud);j.addProperty("chatEnabled",chatEnabled);j.addProperty("wordSelect",wordSelect);j.addProperty("highlight",highlight);j.addProperty("earningsMode",earningsMode);j.addProperty("highlightOpacity",highlightOpacity);
            Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(j)+"\n");
            try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception e){System.err.println("[SkyblockToolkit] Settings could not be saved: "+e.getMessage());}
        finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(Exception ignored){}}
    }
}
