package com.skyblocktoolkit;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Per-item history and bounded session receipts, independent of the booster history. */
public final class ItemStatsHistory {
    public static final class Profile {
        public String id, scope, fingerprint, name, label, slot, icon;
        public String physicalId, lastLocation;
        public boolean automatic;
        private transient boolean persisted;
        public String title(){return label+" #"+id.substring(0,6);}
    }
    public static final class Session {
        public String id, profileId, status="Recording";
        public ItemStatsLocation location=ItemStatsLocation.UNKNOWN;
        public long started, lastSeen, ended, shards, fish;
        public double cash, baseCash, fishValue;
        public boolean hasData(){return cash>0||shards>0||fish>0;}
        public long elapsed(){return Math.max(0,(ended>0?ended:lastSeen)-started);}
        public Totals totals(){return new Totals(1,elapsed(),cash,baseCash,shards,fish,fishValue);}
    }
    public record Totals(int sessions,long elapsed,double cash,double baseCash,long shards,long fish,double fishValue) {
        public double hourly(double value){return elapsed>0?value*3_600_000.0/elapsed:0;}
        /** Reprice saved counts without changing cash or applying a cash booster to shards. */
        public double income(boolean base,boolean includeShards,double shardPrice){return (base?baseCash:cash)+(includeShards?shards*shardPrice:0);}
    }
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final class Data {int schema=1;List<Profile> profiles=new ArrayList<>();List<Session> sessions=new ArrayList<>();}
    private final Path path;
    private Data data=new Data();
    private final List<Session> active=new ArrayList<>();
    private long savedAt;
    private boolean dirty,readOnly;
    private String error="";
    public ItemStatsHistory(Path path) {
        this.path=path;
        if(!Files.exists(path))return;
        try {
            Data loaded=GSON.fromJson(Files.readString(path),Data.class);
            if(loaded==null||loaded.schema!=1||loaded.profiles==null||loaded.sessions==null)throw new IllegalArgumentException("Unsupported stats file");
            Set<String> ids=new HashSet<>();
            for(var p:loaded.profiles){
                if(p==null||p.id==null||p.id.length()<6||!ids.add(p.id)||p.scope==null||p.fingerprint==null||p.fingerprint.length()<8||p.label==null||p.slot==null)throw new IllegalArgumentException("Invalid item profile");
                p.persisted=true;
            }
            Set<String> sessionIds=new HashSet<>();
            for(var s:loaded.sessions) {
                if(s==null||s.id==null||!sessionIds.add(s.id)||!ids.contains(s.profileId)||s.started<0||s.lastSeen<s.started||s.ended<0||s.ended>0&&s.ended<s.started||s.shards<0||s.fish<0||!valid(s.cash)||!valid(s.baseCash)||!valid(s.fishValue))throw new IllegalArgumentException("Invalid session");
                if(s.location==null)s.location=ItemStatsLocation.UNKNOWN;
                if(s.ended==0){s.ended=Math.max(s.started,s.lastSeen);s.status="Interrupted";dirty=true;}
            }
            data=loaded;
        } catch(Exception e){readOnly=true;error="Stats could not be loaded; original file preserved.";System.err.println("[Complexify] "+error+" "+e.getMessage());}
    }
    private static boolean valid(double value){return Double.isFinite(value)&&value>=0;}
    public String error(){return error;}
    public Profile register(String scope,String fingerprint,String name,String slot,String icon) {
        if(readOnly)throw new IllegalStateException(error);
        var p=new Profile();p.id=UUID.randomUUID().toString();p.scope=scope;p.fingerprint=fingerprint;
        p.name=p.label=name;p.slot=slot;p.icon=icon;data.profiles.add(p);dirty=true;return p;
    }
    public List<Profile> profiles(String scope){return data.profiles.stream().filter(p->p.scope.equals(scope)).toList();}
    public boolean hasData(String profileId,ItemStatsLocation location){return data.sessions.stream().anyMatch(s->s.profileId.equals(profileId)&&(location==null||s.location==location)&&s.hasData());}
    /** Recognition candidates stay internal until a relevant, positive result has been observed. */
    public List<Profile> visibleProfiles(String scope,ItemStatsLocation location){
        Map<String,Long> latestEarnings=new HashMap<>();
        for(var s:data.sessions)
            if((location==null||s.location==location)&&s.hasData())
                latestEarnings.merge(s.profileId,s.started,Math::max);
        return profiles(scope).stream()
            .filter(p->latestEarnings.containsKey(p.id)&&ItemStatsEligibility.profile(p))
            .sorted(Comparator.comparingLong((Profile p)->latestEarnings.get(p.id)).reversed())
            .toList();
    }
    public long earningSessions(){return active.stream().filter(s->s.hasData()&&ItemStatsEligibility.profile(profile(s.profileId))).count();}
    public Profile profile(String id){return data.profiles.stream().filter(p->p.id.equals(id)).findFirst().orElse(null);}
    public List<Session> sessions(String profileId){return sessions(profileId,null);}
    public List<Session> sessions(String profileId,ItemStatsLocation location){
        return data.sessions.reversed().stream()
            .filter(s->s.profileId.equals(profileId)&&(location==null||s.location==location))
            .sorted(Comparator.comparingLong((Session s)->s.started).reversed())
            .toList();
    }
    public Session active(){return active.isEmpty()?null:active.getFirst();}
    public List<Session> activeSessions(){return List.copyOf(active);}
    public boolean recording(String profileId){return active.stream().anyMatch(s->s.profileId.equals(profileId));}
    public Profile activeProfile(){return active()==null?null:profile(active().profileId);}
    public boolean writable(){return !readOnly;}
    public void changed(){dirty=true;}
    public void rename(Profile p,String name){if(readOnly||p==null||name==null||name.isBlank())return;p.label=name.strip().substring(0,Math.min(48,name.strip().length()));dirty=true;}
    public Session start(Profile p,long now) {
        return start(p,now,ItemStatsLocation.UNKNOWN);
    }
    public Session start(Profile p,long now,ItemStatsLocation location) {
        if(readOnly||p==null||profile(p.id)!=p||recording(p.id))throw new IllegalStateException("Cannot start session");
        var s=new Session();s.id=UUID.randomUUID().toString();s.profileId=p.id;s.started=s.lastSeen=now;s.location=location;
        data.sessions.add(s);active.add(s);dirty=true;return s;
    }
    public void stop(String profileId,long now,String reason){for(var s:List.copyOf(active))if(s.profileId.equals(profileId)){s.lastSeen=s.ended=Math.max(s.started,now);s.status=reason;active.remove(s);dirty=true;}}
    public void stop(long now,String reason){for(var s:List.copyOf(active))stop(s.profileId,now,reason);}
    public void tick(long now){for(var s:active){s.lastSeen=Math.max(s.started,now);dirty=true;}if(dirty&&now-savedAt>=2000)save(now);}
    // Only a receipt wholly inside one session can be assigned. Boundary-spanning samples stay unassigned.
    private List<Session> containing(long from,long to,boolean point){
        var result=new ArrayList<Session>();var credited=new HashSet<String>();
        if(to<from)return result;
        for(int i=data.sessions.size()-1;i>=0;i--){var s=data.sessions.get(i);long end=s.ended>0?s.ended:s.lastSeen;
            if(from>=s.started&&to<=end&&(!point||s.ended==0||to<end)&&credited.add(s.profileId))result.add(s);
        }
        return result;
    }
    public void money(double cash,int percentage,long from,long to){
        if(!valid(cash)||cash==0||to<=from)return;
        for(var s:containing(from,to,false)){s.cash+=cash;s.baseCash+=cash/(1+Math.max(0,percentage)/100.0);dirty=true;}
    }
    public void shards(long count,long at){shards(count,at,p->true);}
    public void shards(long count,long at,java.util.function.Predicate<Profile> source){if(count<=0)return;for(var s:containing(at,at,true))if(source.test(profile(s.profileId))){s.shards+=count;dirty=true;}}
    public void fish(long count,double value,long at){fish(count,value,at,p->true);}
    public void fish(long count,double value,long at,java.util.function.Predicate<Profile> source){if(count<=0||!valid(value))return;for(var s:containing(at,at,true))if(source.test(profile(s.profileId))){s.fish+=count;s.fishValue+=value;dirty=true;}}
    public Totals totals(String profileId){
        return totals(profileId,null);
    }
    public Totals totals(String profileId,ItemStatsLocation location){
        long elapsed=0,shards=0,fish=0;double cash=0,base=0,value=0;int count=0;
        for(var s:data.sessions)if(s.profileId.equals(profileId)&&(location==null||s.location==location)){count++;elapsed+=s.elapsed();cash+=s.cash;base+=s.baseCash;shards+=s.shards;fish+=s.fish;value+=s.fishValue;}
        return new Totals(count,elapsed,cash,base,shards,fish,value);
    }
    public void save(long now){
        if(readOnly||!dirty)return;
        Path temporary=null;
        try {
            Data confirmed=new Data();var ids=new HashSet<String>();
            // Preserve existing history, including old broad-scan entries, but never persist new empty candidates.
            for(var p:data.profiles)if(p.persisted||hasData(p.id,null)){confirmed.profiles.add(p);ids.add(p.id);}
            for(var s:data.sessions)if(ids.contains(s.profileId))confirmed.sessions.add(s);
            if(confirmed.profiles.isEmpty()&&!Files.exists(path)){dirty=false;savedAt=now;return;}
            Files.createDirectories(path.toAbsolutePath().getParent());
            temporary=Files.createTempFile(path.toAbsolutePath().getParent(),"item-stats-",".tmp");
            Files.writeString(temporary,GSON.toJson(confirmed));
            try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}
            for(var p:confirmed.profiles)p.persisted=true;
            dirty=false;savedAt=now;error="";
        }catch(Exception e){error="Stats save failed; retrying. "+e.getMessage();}
        finally{if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(Exception ignored){}}
    }
}
