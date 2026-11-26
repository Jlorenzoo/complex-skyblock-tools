package com.skyblocktoolkit;

import com.google.gson.JsonParser;
import java.util.*;

/** Passive recognition and session lifecycle; no registration, controls or gameplay actions. */
public final class ItemStatsAutomation {
    public static final long IDLE_MS=5_000;
    public static final long GUI_IDLE_MS=1_500;
    public record Observation(String location,String slot,String fingerprint,String physicalId,String name,String icon){}
    private final ItemStatsHistory history;
    private Object connection;
    private String scope="";
    private long lastActivity=-1;
    private String currentHand;
    private ItemStatsLocation location=ItemStatsLocation.UNKNOWN;
    public ItemStatsAutomation(ItemStatsHistory history){this.history=history;}
    public String currentHand(){return currentHand;}
    public void observe(String scope,Object connection,List<Observation> inventory,boolean activity,long now){
        observe(scope,connection,ItemStatsLocation.OVERWORLD,inventory,activity,false,now);
    }
    public void observe(String scope,Object connection,ItemStatsLocation location,List<Observation> inventory,boolean activity,long now){
        observe(scope,connection,location,inventory,activity,false,now);
    }
    public void observe(String scope,Object connection,ItemStatsLocation location,List<Observation> inventory,boolean activity,boolean guiOpen,long now){
        if(this.connection!=connection||!this.scope.equals(scope)){
            end("Disconnected / changed world");this.connection=connection;this.scope=scope;lastActivity=-1;
        }
        if(this.location!=location){end("Changed area");this.location=location;lastActivity=-1;}
        currentHand=null;
        if(connection==null||scope.isEmpty()||!history.writable()){history.tick(now);return;}
        var profiles=history.profiles(scope);
        var claimed=new HashSet<String>();var resolved=new LinkedHashMap<Observation,ItemStatsHistory.Profile>();
        // Reserve unchanged inventory locations first, then follow moved items, keeping duplicates separate.
        for(var item:inventory){
            var p=profiles.stream().filter(it->!claimed.contains(it.id)&&same(it,item)&&Objects.equals(it.lastLocation,item.location())).findFirst().orElse(null);
            if(p!=null){resolved.put(item,p);claimed.add(p.id);}
        }
        for(var item:inventory){
            if(resolved.containsKey(item))continue;
            var p=profiles.stream().filter(it->!claimed.contains(it.id)&&same(it,item)).findFirst().orElse(null);
            if(p==null){p=history.register(scope,item.fingerprint(),item.name(),item.slot()==null?"Inventory":item.slot(),item.icon());p.physicalId=item.physicalId()==null?"local:"+UUID.randomUUID():item.physicalId();}
            resolved.put(item,p);claimed.add(p.id);
        }
        var earningTools=new HashSet<String>();
        for(var entry:resolved.entrySet()){
            var item=entry.getKey();var p=entry.getValue();
            if(!p.automatic){p.fingerprint=item.fingerprint();p.physicalId=item.physicalId()==null?"local:"+UUID.randomUUID():item.physicalId();p.icon=item.icon();}
            else if(!Objects.equals(p.fingerprint,item.fingerprint())){
                if(Objects.equals(p.label,p.name))p.label=item.name();
                p.name=item.name();p.fingerprint=item.fingerprint();p.icon=item.icon();history.changed();
            }
            if(!Objects.equals(p.lastLocation,item.location())||!p.automatic){p.lastLocation=item.location();p.automatic=true;history.changed();}
            if("HAND".equals(item.slot())){currentHand=p.id;earningTools.add(p.id);}
            if(item.slot()!=null&&!Objects.equals(p.slot,item.slot())){p.slot=item.slot();history.changed();}
        }
        for(var session:history.activeSessions())if(!earningTools.contains(session.profileId))history.stop(session.profileId,session.lastSeen,"Switched / unequipped");
        long grace=guiOpen?GUI_IDLE_MS:IDLE_MS;
        if(activity){lastActivity=now;for(String id:earningTools)if(!history.recording(id))history.start(history.profile(id),now,location);}
        else if(lastActivity>=0&&now-lastActivity>=grace){history.stop(Math.min(now,lastActivity+grace),"Paused while idle");lastActivity=-1;}
        history.tick(now);
    }
    private static boolean same(ItemStatsHistory.Profile p,Observation item){
        if(item.physicalId()!=null&&p.physicalId!=null&&!p.physicalId.startsWith("local:"))return p.physicalId.equals(item.physicalId());
        if(item.physicalId()==null&&p.physicalId!=null&&!p.physicalId.startsWith("local:"))return false;
        if(p.fingerprint.equals(item.fingerprint()))return true;
        if(!p.automatic&&p.icon!=null)try{return ItemStatsIdentity.automaticFingerprint(JsonParser.parseString(p.icon)).equals(item.fingerprint());}catch(Exception ignored){}
        return false;
    }
    public void end(String reason){for(var s:history.activeSessions())history.stop(s.profileId,s.lastSeen,reason);}
}
