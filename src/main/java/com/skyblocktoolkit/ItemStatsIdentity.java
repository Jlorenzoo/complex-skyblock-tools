package com.skyblocktoolkit;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.TreeMap;

/** A configuration fingerprint, never a claim that two equal stacks are the same physical item. */
public final class ItemStatsIdentity {
    private ItemStatsIdentity() {}
    private static final java.util.Set<String> COUNTERS=java.util.Set.of("cropsharvested","blocksbroken","fishcaught","kills","lastused","cooldown","currentdurability","itemxp","itemexperience");
    private static final java.util.Set<String> UNIQUE=java.util.Set.of("uuid","itemuuid","uniqueid","uniqueitemid","instanceid","iteminstanceid");
    private static String key(String value){return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]","");}
    /** Material IDs, enchant IDs and owner UUIDs are not item IDs. */
    public static String physicalId(JsonElement encoded){
        var components=encoded.getAsJsonObject().getAsJsonObject("components");
        if(components==null||!components.has("minecraft:custom_data"))return null;
        var ids=new TreeMap<String,String>();findIds(components.get("minecraft:custom_data"),"",ids);
        return ids.isEmpty()?null:canonical(new Gson().toJsonTree(ids));
    }
    private static void findIds(JsonElement value,String path,TreeMap<String,String> ids){
        if(!value.isJsonObject())return;
        for(var entry:value.getAsJsonObject().entrySet()){
            String name=entry.getKey(),normalized=key(name.contains(":")?name.substring(name.lastIndexOf(':')+1):name);
            String location=path+"/"+name;
            if(UNIQUE.contains(normalized)&&entry.getValue().isJsonPrimitive()&&entry.getValue().getAsString().length()>=8)ids.put(location,entry.getValue().getAsString());
            if(!key(name).matches(".*(?:owner|creator|player|enchant|attribute).*"))findIds(entry.getValue(),location,ids);
        }
    }
    public static String automaticFingerprint(JsonElement encoded){
        JsonObject item=encoded.getAsJsonObject().deepCopy();var components=item.getAsJsonObject("components");
        if(components!=null){
            if(components.has("minecraft:custom_data"))removeCounters(components.get("minecraft:custom_data"));
            if(components.has("minecraft:lore")&&components.get("minecraft:lore").isJsonArray()){
                var lines=new JsonArray();
                for(var line:components.getAsJsonArray("minecraft:lore")){
                    String text=plain(line).replaceAll("§.","").strip();
                    if(!text.matches("(?i)^[^\\p{L}\\p{N}]*(?:total )?(?:crops harvested|blocks (?:broken|mined)|fish caught|kills|durability|cooldown|item (?:xp|experience))\\s*[:┃|].*"))lines.add(line);
                }
                if(lines.isEmpty())components.remove("minecraft:lore");else components.add("minecraft:lore",lines);
            }
            if(components.has("minecraft:custom_data")&&components.get("minecraft:custom_data").isJsonObject()&&components.getAsJsonObject("minecraft:custom_data").isEmpty())components.remove("minecraft:custom_data");
        }
        return fingerprint(item);
    }
    private static void removeCounters(JsonElement value){
        if(value.isJsonObject())for(String name:new java.util.ArrayList<>(value.getAsJsonObject().keySet())){
            if(COUNTERS.contains(key(name.contains(":")?name.substring(name.lastIndexOf(':')+1):name)))value.getAsJsonObject().remove(name);
            else {var child=value.getAsJsonObject().get(name);removeCounters(child);if(child.isJsonObject()&&child.getAsJsonObject().isEmpty())value.getAsJsonObject().remove(name);}
        }
    }
    private static String plain(JsonElement value){
        if(value.isJsonPrimitive())return value.getAsString();
        var out=new StringBuilder();
        if(value.isJsonArray())value.getAsJsonArray().forEach(v->out.append(plain(v)));
        else if(value.isJsonObject()){var obj=value.getAsJsonObject();if(obj.has("text"))out.append(plain(obj.get("text")));if(obj.has("extra"))out.append(plain(obj.get("extra")));}
        return out.toString();
    }
    public static String fingerprint(JsonElement encoded) {
        JsonObject item=encoded.getAsJsonObject().deepCopy();
        item.remove("count");
        if(item.has("components")) {
            JsonObject components=item.getAsJsonObject("components");
            components.remove("minecraft:damage");
            components.remove("minecraft:repair_cost");
            if(components.isEmpty())item.remove("components");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical(item).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String canonical(JsonElement value) {
        if(value.isJsonObject()) {
            var sorted=new TreeMap<String,JsonElement>();value.getAsJsonObject().entrySet().forEach(e->sorted.put(e.getKey(),e.getValue()));
            var parts=new java.util.ArrayList<String>();
            sorted.forEach((k,v)->parts.add(new Gson().toJson(k)+":"+canonical(v)));
            return "{"+String.join(",",parts)+"}";
        }
        if(value.isJsonArray()) {
            var parts=new java.util.ArrayList<String>();value.getAsJsonArray().forEach(v->parts.add(canonical(v)));
            return "["+String.join(",",parts)+"]";
        }
        return value.toString();
    }
}
