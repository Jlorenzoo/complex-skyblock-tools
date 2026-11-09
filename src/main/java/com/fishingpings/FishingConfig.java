package com.fishingpings;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
public final class FishingConfig {
    public boolean depthEnabled=true,warningEnabled=true,snapEnabled=true;
    public float volume=0.18f;
    public float biteSoundVolume=2f;
    public enum BiteSound {
        SHORT_SPLASH("Short splash"), VANILLA_SPLASH("Vanilla splash"), LOW_PING("Low ping");

        public final String label;
        BiteSound(String label){this.label=label;}
        public BiteSound next(){return switch(this){
            case SHORT_SPLASH -> VANILLA_SPLASH;
            case VANILLA_SPLASH -> LOW_PING;
            case LOW_PING -> SHORT_SPLASH;
        };}
    }
    public BiteSound biteSound=BiteSound.SHORT_SPLASH;
    public boolean oniEnergyAlert=true;
    public double target=30;
    public double codPrice,salmonPrice,pufferPrice,tropicalPrice;
    public double oniShardPrice,oniVoteKeyPrice;
    public int earningsWindow=60;
    public double averageFishPrice(){return FishingEarnings.weightedPrice(codPrice,salmonPrice,pufferPrice,tropicalPrice);}
    public boolean hasPrices(){return averageFishPrice()>0;}
    public boolean hasOniPrices(){return hasPrices()||oniShardPrice>0||oniVoteKeyPrice>0;}
    double oniPrice(OniEarnings.Reward reward){
        return switch(reward){
            case COD -> codPrice;
            case SALMON -> salmonPrice;
            case PUFFERFISH -> pufferPrice;
            case TROPICAL_FISH -> tropicalPrice;
            case GOLD_SHARD -> oniShardPrice;
            case VOTE_KEY -> oniVoteKeyPrice;
        };
    }
    private final Path path=FabricLoader.getInstance().getConfigDir().resolve("fishingpings.json");
    public FishingConfig(){
        if(Files.exists(path))try{
            var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if(json.has("depthEnabled"))depthEnabled=json.get("depthEnabled").getAsBoolean();
            if(json.has("snapEnabled"))snapEnabled=json.get("snapEnabled").getAsBoolean();
            if(json.has("warningEnabled"))warningEnabled=json.get("warningEnabled").getAsBoolean();
            if(json.has("volume")){float v=json.get("volume").getAsFloat();if(Float.isFinite(v))volume=Math.clamp(v,0,3);}
            if(json.has("biteSoundVolume")){
                float value=json.get("biteSoundVolume").getAsFloat();
                if(Float.isFinite(value))biteSoundVolume=Math.clamp(value,0,4);
            }
            if(json.has("oniEnergyAlert"))oniEnergyAlert=json.get("oniEnergyAlert").getAsBoolean();
            if(json.has("lowBitePing")&&!json.get("lowBitePing").getAsBoolean())biteSound=BiteSound.VANILLA_SPLASH;
            if(json.has("biteSound")&&json.get("biteSound").isJsonPrimitive()){
                try{biteSound=BiteSound.valueOf(json.get("biteSound").getAsString());}
                catch(IllegalArgumentException ignored){}
            }
            for(String key:new String[]{"codPrice","salmonPrice","pufferPrice","tropicalPrice","oniShardPrice","oniVoteKeyPrice"}){
                if(!json.has(key))continue;double v=json.get(key).getAsDouble();if(!Double.isFinite(v)||v<0||v>1e12)continue;
                switch(key){case "codPrice"->codPrice=v;case "salmonPrice"->salmonPrice=v;case "pufferPrice"->pufferPrice=v;case "tropicalPrice"->tropicalPrice=v;case "oniShardPrice"->oniShardPrice=v;case "oniVoteKeyPrice"->oniVoteKeyPrice=v;}
            }
            if(json.has("earningsWindow"))earningsWindow=json.get("earningsWindow").getAsInt()==300?300:60;
            if(json.has("target")){double v=json.get("target").getAsDouble();if(Double.isFinite(v))target=Math.clamp(v,1,1000);}
        }catch(Exception e){System.err.println("[FishingPings] Could not load settings: "+e.getMessage());}
    }
    public void save(){
        Path temporary=null;
        try{
            Files.createDirectories(path.getParent());temporary=Files.createTempFile(path.getParent(),"fishingpings-",".tmp");
            var json=new JsonObject();json.addProperty("depthEnabled",depthEnabled);json.addProperty("warningEnabled",warningEnabled);json.addProperty("snapEnabled",snapEnabled);json.addProperty("volume",volume);json.addProperty("target",target);json.addProperty("codPrice",codPrice);json.addProperty("salmonPrice",salmonPrice);json.addProperty("pufferPrice",pufferPrice);json.addProperty("tropicalPrice",tropicalPrice);json.addProperty("earningsWindow",earningsWindow);
            json.addProperty("biteSoundVolume",biteSoundVolume);
            json.addProperty("biteSound",biteSound.name());
            json.addProperty("oniEnergyAlert",oniEnergyAlert);
            json.addProperty("oniShardPrice",oniShardPrice);
            json.addProperty("oniVoteKeyPrice",oniVoteKeyPrice);
            Files.writeString(temporary,new GsonBuilder().setPrettyPrinting().create().toJson(json)+"\n");
            try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception e){System.err.println("[FishingPings] Could not save settings: "+e.getMessage());}
        finally{if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(Exception ignored){}}
    }
}
