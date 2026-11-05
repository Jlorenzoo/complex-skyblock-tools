package com.skyblocktoolkit;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.TreeMap;

/** A configuration fingerprint, never a claim that two equal stacks are the same physical item. */
public final class ItemStatsIdentity {
    private ItemStatsIdentity() {}
    public static String fingerprint(JsonElement encoded) {
        JsonObject item=encoded.getAsJsonObject().deepCopy();
        item.remove("count");
        if(item.has("components")) {
            JsonObject components=item.getAsJsonObject("components");
            components.remove("minecraft:damage");
            components.remove("minecraft:repair_cost");
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
