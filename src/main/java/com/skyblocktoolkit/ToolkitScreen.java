package com.skyblocktoolkit;

import com.moneybooster.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import java.util.function.*;
import org.lwjgl.glfw.GLFW;

/** One navigation surface for all modules and their live HUD placement. */
public final class ToolkitScreen extends Screen {
    private static final Identifier LOGO=Identifier.fromNamespaceAndPath("skyblocktoolkit","textures/gui/logo.png");
    private final Screen parent;
    private int page,scroll,contentHeight,dragging,soundReturnPage=2,hudReturnPage=1;
    private double grabX,grabY;
    private int bindingTarget; // 0=none, 1=menu, 2=blackout, 3=sellall, 4=fullbright
    private boolean fishEarnings;
    private String statsProfileId,statsSessionId;
    private long statsUpdated;
    private ItemStatsLocation statsArea;
    private final Set<String> expandedDetails=new HashSet<>();
    private int topicOffset;
    private Button topicBack;
    private final Map<Integer,String> selectedTopics=new HashMap<>();
    private record Topic(String title,String summary){}
    private List<Topic> topics(){return switch(page){
        case 1->List.of(new Topic("Booster timers","Money & XP timers, activation and ending alerts"),new Topic("Coconut alerts","Sound, banners and minimum boost"),new Topic("Earnings & statistics","History, rate tests and crop averages"));
        case 2->List.of(new Topic("Farming","Crop visibility, earnings and gold shards"),new Topic("Fishing","Bite volume, depleted areas and Oni Rod chime"),new Topic("Fish sell prices","Fish values and estimated earnings"),new Topic("Mining & XP","XP per hour and booster accounting"));
        case 3->SeasonalRegistry.ITEMS.stream().map(item->new Topic(item.name(),item.subtitle())).toList();
        case 4->List.of(new Topic("Active HUD elements","Choose which displays appear in-game"),new Topic("Layout & positioning","Move displays or restore their positions"),new Topic("Treasure chests","Discovery details, estimates and appearance"),new Topic("Voice & music","See your microphone and music broadcast state"));
        case 6->List.of(new Topic("Keyboard shortcuts","Menu, sell all, blackout and fullbright keys"),new Topic("Chat selection & copy","Copy text, gift codes and chat appearance"),new Topic("Auction notifications","Sale sounds, volume and previews"),new Topic("Display & visuals","Fullbright and lighting"),new Topic("Blackout (AFK)","Screen dimming and wake controls"));
        default->List.of();
    };}
    private boolean topic(String title,int top){
        if(!title.equals(selectedTopics.get(page)))return false;
        // Topic content starts below the fixed breadcrumb, at its first control.
        topicOffset=top+16;
        return true;
    }
    private void topicOverview(){
        int top=0;
        for(var topic:topics()){
            String title=topic.title();
            var item=page==3?SeasonalRegistry.ITEMS.stream().filter(it->it.name().equals(title)).findFirst().orElse(null):null;
            var row=addRenderableWidget(item==null
                ?ToolkitButton.topic(title,topic.summary(),b->openTopic(title),layout.bodyX(),layout.bodyY(),layout.bodyWidth())
                :ToolkitButton.seasonalCard(title,item.subtitle(),item.icon(),false,b->openTopic(title),layout.bodyX(),layout.bodyY(),layout.bodyWidth()));
            int height=item==null?40:32;
            controls.add(new Control(row,top,height));contentHeight=Math.max(contentHeight,top+height);
            top+=height;
        }
    }
    private void openTopic(String title){
        selectedTopics.put(page,title);scroll=0;bindingTarget=0;clearBody();buildBody();
        if(!controls.isEmpty())setFocused(controls.getFirst().widget());
    }
    private void backToTopics(){
        String previous=selectedTopics.remove(page);scroll=0;bindingTarget=0;clearBody();buildBody();
        for(var row:controls)if(row.widget().getMessage().getString().equals(previous)){
            scroll=row.top();updateControls();setFocused(row.widget());break;
        }
    }
    private void topicBack(){
        topicBack=addRenderableWidget(ToolkitButton.breadcrumb(PAGES[page],selectedTopics.get(page),b->backToTopics(),layout.bodyX(),layout.bodyY(),layout.bodyWidth()));
        layout=new ToolkitLayout(layout.x(),layout.y(),layout.width(),layout.height(),layout.bodyX(),layout.bodyY()+28,layout.bodyWidth(),layout.footerY(),layout.compact());
    }
    /** Local view preferences only: searching reveals the group that owns the setting. */
    private boolean details(String title,int top,String... settings){
        String key=page+"/"+selectedTopics.get(page)+"/"+title;
        if(!jumpSetting.isEmpty()&&Arrays.asList(settings).contains(jumpSetting))expandedDetails.add(key);
        boolean open=expandedDetails.contains(key);
        int rowTop=top-topicOffset;
        var row=addRenderableWidget(ToolkitButton.disclosure(title,open,b->{
            if(open)expandedDetails.remove(key);else expandedDetails.add(key);
            clearBody();buildBody();
            for(var control:controls)if(control.widget().getMessage().getString().startsWith(title+": ")){setFocused(control.widget());break;}
        },layout.bodyX(),layout.bodyY(),layout.bodyWidth()));
        controls.add(new Control(row,rowTop,20));contentHeight=Math.max(contentHeight,rowTop+20);
        return open;
    }
    private void link(String title,int top,int target,String topic){
        button(title,top,()->{if(target==page)openTopic(topic);else{selectedTopics.put(target,topic);go(target);}});
    }
    private String query="",jumpSection="",jumpSetting="";
    private EditBox searchBox;
    private Button clearSearch;
    private int savedScroll,layoutReturnPage=1;
    private final List<EditBox> priceFields=new ArrayList<>();
    private final List<EditBox> settingFields=new ArrayList<>();
    private ToolkitLayout layout;
    private final List<Control> controls=new ArrayList<>();
    private final List<Label> labels=new ArrayList<>();
    private record Control(AbstractWidget widget,int top,int rowHeight){
        Control {top=ToolkitLayout.settingY(top);}
    }
    private record Label(Supplier<String> text,int top,int color,float scale,int offset){
        Label {top=ToolkitLayout.settingY(top);scale*=ToolkitTheme.MENU_TEXT_SCALE;}
        Label(Supplier<String> text,int top,int color,float scale){this(text,top,color,scale,0);}
    }
    private static final String[] PAGES={"Boosters","Boosters","Skills","Seasonals","HUD & Overlays","HUD layout","Utilities","","","","","","","Stats"};
    private static final String[] DESCRIPTIONS={
        "",
        "Timers, coconut alerts & session statistics",
        "Farming, fishing & mining helpers",
        "Seasonal items, custom tools & unique gear",
        "On-screen displays & layout configuration",
        "Interactive HUD placement canvas",
        "Chat selection, auction sales & blackout mode","","","","","","",
        "Individual items, earnings & past sessions"
    };
    public ToolkitScreen(Screen parent,int page){super(Component.literal("Complexify"));this.parent=parent;this.page=routePage(page);}
    private static int routePage(int p){
        return switch(p){
            case 0,1->1;
            case 2,9,11->2;
            case 3,12->3;
            case 4,8->4;
            case 5->5;
            case 6,7,10->6;
            case 13->13;
            default->Math.clamp(p,1,6);
        };
    }
    @Override protected void init(){
        layout=ToolkitLayout.hub(width,height);topicBack=null;controls.clear();labels.clear();priceFields.clear();settingFields.clear();contentHeight=0;
        if(page==5){initLayout();return;}
        int x=layout.x(),y=layout.y(),w=layout.width();
        addThemeButton(x,y,w);
        int[] targets={1,2,3,13,4,6};String[] names={"Boosters","Skills","Seasonals","Stats","HUD & Overlays","Utilities"};
        String[] compactNames={"Boost","Skills","Season","Stats","HUD","Utils"};
        for(int i=0;i<targets.length;i++){
            final int target=targets[i];int bx=layout.compact()?x+6+i*((w-12)/targets.length):x+12;
            int by=layout.compact()?y+40:y+82+i*30,bw=layout.compact()?(w-12)/targets.length-2:112;
            var nav=addRenderableWidget(ToolkitButton.nav(layout.compact()?compactNames[i]:names[i],button->go(target),bx,by,bw,20,page==target&&query.isBlank()));
            nav.setTooltip(Tooltip.create(Component.literal(names[i])));
        }
        addRenderableWidget(ToolkitButton.action("Edit HUD",b->go(5),x+12,layout.footerY(),96,20));
        addRenderableWidget(ToolkitButton.action("Close",b->onClose(),x+w-76,layout.footerY(),64,20));
        if(UpdateManager.isUpdateAvailable()){
            String updateText=UpdateManager.getState()==UpdateManager.State.DOWNLOADING?"Downloading...":
                              UpdateManager.getState()==UpdateManager.State.READY_RESTART?"Restart Game":
                              "Update v"+UpdateManager.getLatestVersion();
            int ux=x+112,uw=Math.max(70,(x+w-80)-ux);
            addRenderableWidget(ToolkitButton.action(updateText,b->{
                UpdateManager.triggerUpdateFromCommand();
                if(UpdateManager.getState()==UpdateManager.State.READY_RESTART)onClose();
            },ux,layout.footerY(),Math.min(uw,104),20));
        }

        initSearch();
        buildBody();
    }
    private void go(int next){persist();if(next==page)selectedTopics.remove(page);if(next==5)layoutReturnPage=page==5?1:page;page=next==0?1:next;scroll=0;dragging=0;bindingTarget=0;fishEarnings=false;query="";jumpSection="";rebuildWidgets();}
    private void initSearch(){
        int sx,sy,sw,editW,clearX,clearY,clearW,clearH;
        if(!layout.compact()){
            sx=layout.x()+12;sy=layout.y()+52;sw=112;
            editW=sw-22;clearW=14;clearH=16;
            clearX=sx+sw-16;clearY=sy+2;
        }else{
            sx=layout.bodyX();sy=layout.y()+68;sw=layout.bodyWidth();
            editW=sw-30;clearW=22;clearH=20;
            clearX=sx+sw-24;clearY=sy;
        }
        searchBox=addRenderableWidget(new EditBox(font,sx+6,sy+3,editW,14,Component.literal("Search all settings")));
        searchBox.setBordered(false);searchBox.setTextShadow(false);searchBox.setMaxLength(80);
        searchBox.setTextColor(ToolkitTheme.palette().text());
        searchBox.setTooltip(Tooltip.create(Component.literal("Search every category · Ctrl+F / Cmd+F")));
        searchBox.setValue(query);
        clearSearch=addRenderableWidget(ToolkitButton.action("X",b->{searchBox.setValue("");setFocused(searchBox);},clearX,clearY,clearW,clearH));
        clearSearch.setTooltip(Tooltip.create(Component.literal("Clear search (Escape)")));
        clearSearch.visible=!query.isEmpty();
        searchBox.setResponder(text->{
            if(query.isBlank()&&!text.isBlank())savedScroll=scroll;
            query=text;scroll=text.isBlank()?savedScroll:0;
            clearSearch.visible=!text.isBlank();
            clearBody();buildBody();
        });
    }
    private void clearBody(){
        if(topicBack!=null){removeWidget(topicBack);topicBack=null;}
        for(var control:controls)removeWidget(control.widget());
        controls.clear();labels.clear();priceFields.clear();settingFields.clear();contentHeight=0;
    }
    private void buildBody(){
        layout=ToolkitLayout.hub(width,height);
        topicOffset=0;
        if(!query.isBlank()){
            var results=ToolkitSearch.find(query);
            label(results.isEmpty()?"No settings found. Try sound, HUD or key.":results.size()+(results.size()==1?" setting found":" settings found")+" · select to open",0);
            int top=18;
            for(var result:results){
                var row=addRenderableWidget(ToolkitButton.searchResult(result.setting(),result.category()+" / "+result.section(),result.title(),b->{
                    persist();page=result.page();
                    query="";scroll=0;
                    jumpSection=result.section();jumpSetting=result.setting();
                    rebuildWidgets();
                },layout.bodyX(),layout.bodyY(),layout.bodyWidth()));
                controls.add(new Control(row,top,32));contentHeight=Math.max(contentHeight,top+32);
                top+=32;
            }
        }else{
            if(!jumpSection.isEmpty())selectedTopics.put(page,jumpSection);
            if(page==13)stats();
            else if(!selectedTopics.containsKey(page))topicOverview();
            else {
                topicBack();
                switch(page){
                    case 1->boosters();
                    case 2->skills();
                    case 3->seasonals();
                    case 4->hudOverlays();
                    case 6->utilities();
                    default->boosters();
                }
            }
        }
        contentHeight=ToolkitLayout.settingY(contentHeight);
        if(!jumpSetting.isEmpty()){
            for(var control:controls){
                if(searchTarget(control.widget(),jumpSetting)){scroll=control.top();break;}
            }
        }
        jumpSection="";jumpSetting="";updateControls();
    }
    static boolean searchTarget(AbstractWidget widget,String setting){
        String message=widget.getMessage().getString();
        return message.equals(setting)||message.startsWith(setting+":")||message.startsWith(setting+" ")||
            (setting.equals("Minimum boost (%)")&&message.equals("Minimum coconut money booster percentage"));
    }
    private void addThemeButton(int x,int y,int w){
        var cfg=ToolkitConfig.get();
        var button=addRenderableWidget(ToolkitButton.themeSwitch(b->{
            cfg.lightMode=!cfg.lightMode;persist();rebuildWidgets();
        },x+w-72,y+10,cfg.lightMode));
        button.setTooltip(Tooltip.create(Component.literal("Switch to "+(cfg.lightMode?"dark":"light")+" theme")));
    }
    private void label(Supplier<String> text,int top,int color,float scale){
        top-=topicOffset;labels.add(new Label(text,top,color,scale));contentHeight=Math.max(contentHeight,top+(int)(10*scale));}
    private void label(String text,int top){label(()->text,top,ToolkitTheme.palette().muted(),1);}
    private void button(String text,int top,Runnable action){button(()->text,top,0,Math.min(144,layout.bodyWidth()),action);}
    private void button(Supplier<String> text,int top,int offset,int w,Runnable action){
        top-=topicOffset;
        Button b=addRenderableWidget(ToolkitButton.action(text.get(),unused->{action.run();persist();updateControls();},layout.bodyX()+offset,layout.bodyY()+top-scroll,w,20));
        controls.add(new Control(b,top,20));contentHeight=Math.max(contentHeight,top+20);
        // Button labels are refreshed through the same render-time supplier list.
        labels.add(new Label(()->{b.setMessage(Component.literal(text.get()));return "";},top,0,1));
    }
    private void pair(Supplier<String> a,Runnable aa,Supplier<String> b,Runnable bb,int top){
        int aw=Math.min(144,(layout.bodyWidth()-8)/2),bw=aw;
        button(a,top,0,aw,aa);button(b,top,aw+8,bw,bb);
    }
    private void slider(String name,int top,double value,DoubleConsumer apply){
        slider(name,top,value,1,apply);
    }
    private void slider(String name,int top,double setting,double maximum,DoubleConsumer apply){
        sliderAt(name,top,setting,maximum,0,layout.bodyWidth(),apply);
    }
    private void sliderAt(String name,int top,double setting,double maximum,int offset,int fieldWidth,DoubleConsumer apply){
        top-=topicOffset;
        double normalized=setting/maximum;
        labels.add(new Label(()->name,top+6,ToolkitTheme.palette().text(),1,offset+6));
        AbstractSliderButton slider=addRenderableWidget(new AbstractSliderButton(layout.bodyX()+offset+fieldWidth-148,layout.bodyY()+top-scroll,148,20,Component.literal(name),normalized){
            protected void updateMessage(){setMessage(Component.literal(name+": "+Math.round(value*maximum*100)+"%"));}
            protected void applyValue(){apply.accept(value*maximum);updateMessage();}
            private void moveKnob(double mouseX){
                value=Math.clamp((mouseX-getX()-4)/(getWidth()-48),0,1);applyValue();updateMessage();
            }
            public void onClick(MouseButtonEvent e,boolean doubleClick){moveKnob(e.x());}
            protected void onDrag(MouseButtonEvent e,double dx,double dy){moveKnob(e.x());}
            public void onRelease(MouseButtonEvent e){super.onRelease(e);persist();}
            public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
                var palette=ToolkitTheme.palette();
                if(isHoveredOrFocused())ToolkitTheme.rounded(g,getX(),getY(),getWidth(),getHeight(),3,palette.surface());
                String percent=Math.round(value*maximum*100)+"%";
                ToolkitTheme.text(g,font,percent,getRight()-6-ToolkitTheme.textWidth(font,percent),getY()+6,palette.muted());
                int end=getRight()-44,start=getX()+4,knob=start+(int)((end-start)*value),cy=getY()+10;
                g.fill(start,cy-1,end,cy+1,palette.track());g.fill(start,cy-1,knob,cy+1,palette.accentTrack());
                ToolkitTheme.rounded(g,knob-2,cy-3,4,6,2,palette.accent());

            }
        });slider.setMessage(Component.literal(name+": "+Math.round(setting*100)+"%"));controls.add(new Control(slider,top,20));contentHeight=Math.max(contentHeight,top+20);
    }
    private void toggle(String text,int top,BooleanSupplier state,Runnable action){toggleAt(text,top,0,layout.bodyWidth(),state,action);}
    private void toggle(String text,String description,int top,BooleanSupplier state,Runnable action){
        top-=topicOffset;
        var b=addRenderableWidget(ToolkitButton.toggle(text,description,unused->{action.run();persist();updateControls();},layout.bodyX(),layout.bodyY(),layout.bodyWidth(),state));
        controls.add(new Control(b,top,28));contentHeight=Math.max(contentHeight,top+23);
    }
    private void hint(String text,int top){
        top-=topicOffset;
        labels.add(new Label(()->text,top,ToolkitTheme.palette().muted(),ToolkitTheme.MENU_HELP_SCALE/ToolkitTheme.MENU_TEXT_SCALE,6));
        contentHeight=Math.max(contentHeight,top+8);
    }
    private void toggleCell(String text,int top,int column,BooleanSupplier state,Runnable action){
        int half=(layout.bodyWidth()-10)/2;toggleAt(text,top,column*(half+10),half,state,action);
    }
    private void toggleAt(String text,int top,int offset,int width,BooleanSupplier state,Runnable action){
        top-=topicOffset;
        var b=addRenderableWidget(ToolkitButton.toggle(text,unused->{action.run();persist();updateControls();},layout.bodyX()+offset,layout.bodyY()+top-scroll,width,20,state));
        controls.add(new Control(b,top,20));contentHeight=Math.max(contentHeight,top+20);
    }
    private void boosters(){
        var toolkit=ToolkitConfig.get();
        if(topic("Booster timers",0)){
            toggleCell("Money timer",16,0,()->toolkit.boosterHud,()->toolkit.boosterHud=!toolkit.boosterHud);
            toggleCell("XP timer",16,1,()->toolkit.xpBoosterHud,()->toolkit.xpBoosterHud=!toolkit.xpBoosterHud);
            toggle("5-second reminder","Soft highlight when a visible timer is ending.",40,()->toolkit.boosterReminder,()->toolkit.boosterReminder=!toolkit.boosterReminder);
            label(()->timerStatus(BoosterTimers.Type.MONEY),72,ToolkitTheme.palette().moneyText(),1);
            label(()->timerStatus(BoosterTimers.Type.XP),86,ToolkitTheme.palette().xpText(),1);
            if(details("Alert appearance & previews",112,"Ending banner","Ending screen flash","Money alert","XP alert","Preview XP alert","Preview ending")){
                toggle("Ending banner",140,()->toolkit.boosterExpiringAlert,()->toolkit.boosterExpiringAlert=!toolkit.boosterExpiringAlert);
                toggle("Ending screen flash",164,()->toolkit.expiringFlash,()->toolkit.expiringFlash=!toolkit.expiringFlash);
                toggle("Money alert",188,()->toolkit.moneyVisualAlert,()->toolkit.moneyVisualAlert=!toolkit.moneyVisualAlert);
                toggle("XP alert",212,()->toolkit.xpVisualAlert,()->toolkit.xpVisualAlert=!toolkit.xpVisualAlert);
                pair(()->"Preview XP alert",VisualAlertOverlay::previewXpAlert,()->"Preview ending",VisualAlertOverlay::previewExpiringAlert,236);
            }
        }
        if(topic("Coconut alerts",0)){
            toggleCell("Coconut sound",16,0,()->toolkit.coconutSoundEnabled,()->toolkit.coconutSoundEnabled=!toolkit.coconutSoundEnabled);
            toggleCell("Coconut banner",16,1,()->toolkit.coconutVisualAlert,()->toolkit.coconutVisualAlert=!toolkit.coconutVisualAlert);
            toggle("Coconut screen flash",40,()->toolkit.coconutFlash,()->toolkit.coconutFlash=!toolkit.coconutFlash);
            thresholdField(64);
            hint("Alert at or above this %. 0 = every money boost.",84);
            pair(()->"Preview sound",CoconutNotifier::preview,()->"Preview coconut",VisualAlertOverlay::previewCoconutAlert,100);
        }
        if(topic("Earnings & statistics",0)){
            pair(()->"History",()->Minecraft.getInstance().setScreenAndShow(new BoosterHistoryScreen(this)),()->"Rate tests",()->Minecraft.getInstance().setScreenAndShow(new BoosterStatsScreen(this,true)),16);
            label(()->"Session bonus: +"+BoosterStatsManager.formatMoney(BoosterStatsManager.getSessionBonusEarned()),48,ToolkitTheme.palette().text(),1);
            label(()->{var e=BoosterStatsManager.history().latestMoneyBooster();return "Latest bonus: "+(e==null?"--":"+"+BoosterStatsManager.formatMoney(e.bonus));},64,ToolkitTheme.palette().muted(),1);
            link("Farming income settings",92,2,"Farming");
        }
    }
    private void statsRefresh(){
        String focused=getFocused() instanceof AbstractWidget w?w.getMessage().getString():null;
        clearBody();buildBody();
        if(focused!=null)for(var row:controls)if(row.widget().visible&&row.widget().getMessage().getString().equals(focused)){setFocused(row.widget());break;}
    }
    private void statsNextArea(){statsArea=statsArea==null?ItemStatsLocation.OVERWORLD:statsArea==ItemStatsLocation.OVERWORLD?ItemStatsLocation.REALM:statsArea==ItemStatsLocation.REALM?ItemStatsLocation.UNKNOWN:null;scroll=0;statsRefresh();}
    private String statsAreaName(){return statsArea==null?"All":statsArea==ItemStatsLocation.REALM?"Realm":statsArea==ItemStatsLocation.UNKNOWN?"Unknown":statsArea.label();}
    private ItemStatsHistory.Totals statsTotals(String id){return ItemStatsTracker.history().totals(id,statsArea);}
    @Override public void tick(){
        super.tick();
        long now=System.currentTimeMillis();
        if(page==13&&query.isEmpty()&&now-statsUpdated>=1000){statsUpdated=now;statsRefresh();}
    }
    private static String statsTime(long ms){long seconds=ms/1000;return String.format(java.util.Locale.ROOT,"%dh %02dm %02ds",seconds/3600,seconds/60%60,seconds%60);}
    private static String statsDate(long ms){return java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm"));}
    private String statsMoney(double cash){return BoosterStatsManager.formatMoney(cash);}
    private double statsIncome(ItemStatsHistory.Totals totals,boolean base){
        var config=ToolkitConfig.get();return totals.income(base,config.includeGoldShards,config.goldShardPrice);
    }
    private String statsRate(ItemStatsHistory.Totals totals){return statsMoney(totals.hourly(statsIncome(totals,false)))+"/h";}
    private static String statsShardRate(ItemStatsHistory.Totals totals){
        double hourly=totals.hourly(totals.shards());
        double perMinute=hourly/60;
        String minute=perMinute>0&&perMinute<0.01?"<0.01":perMinute<10
            ?String.format(java.util.Locale.ROOT,"%.2f",perMinute):CropHarvestTracker.format(perMinute);
        return "Shards: "+totals.shards()+" · "+CropHarvestTracker.format(hourly)+"/h · "+minute+"/min";
    }
    private void stats(){
        var history=ItemStatsTracker.history();var profile=history.profile(statsProfileId);
        if(profile==null){
            statsProfileId=null;statsSessionId=null;
            pair(()->"Current item",()->{statsProfileId=ItemStatsTracker.currentItem();if(statsProfileId!=null)statsArea=null;scroll=0;statsRefresh();},()->"Area: "+statsAreaName(),this::statsNextArea,0);
            for(var row:controls)if(row.widget().getMessage().getString().equals("Current item")){
                row.widget().active=ItemStatsTracker.currentItem()!=null;
                row.widget().setTooltip(Tooltip.create(Component.literal("Available after the held tool records earnings.")));
            }
            label("Items appear after their first cash, shard or catch gain.",28);
            label(ItemStatsTracker::message,44,ToolkitTheme.palette().accent(),0.85f);
            int top=60;
            var profiles=history.visibleProfiles(ItemStatsTracker.scope(),statsArea);
            if(profiles.isEmpty())label("No earnings recorded here yet. Tracking is automatic.",top);
            for(var p:profiles){
                var totals=statsTotals(p.id);
                String drops=totals.shards()>0||totals.fish()==0?totals.shards()+" shards":totals.fish()+" catches";
                String subtitle=totals.sessions()+" sessions · "+statsRate(totals)+" · "+drops;
                var row=addRenderableWidget(ToolkitButton.seasonalCard(p.title(),subtitle,ItemStatsTracker.icon(p),history.recording(p.id),b->{statsProfileId=p.id;scroll=0;statsRefresh();},layout.bodyX(),layout.bodyY(),layout.bodyWidth()));
                row.setTooltip(Tooltip.create(Component.literal(p.name+" · "+p.slot+"\n"+(p.physicalId!=null&&!p.physicalId.startsWith("local:")?"Recognized by server item ID.":"Recognized by item data and inventory continuity. Identical items without server IDs can be ambiguous."))));
                controls.add(new Control(row,top,32));contentHeight=Math.max(contentHeight,top+32);top+=30;
            }
            return;
        }
        if(statsSessionId!=null){
            var session=history.sessions(profile.id).stream().filter(s->s.id.equals(statsSessionId)).findFirst().orElse(null);
            button("< Item statistics",0,()->{statsSessionId=null;scroll=0;statsRefresh();});
            if(session==null)return;
            label(profile.title(),28);
            label(()->"Money / hour: "+statsRate(session.totals()),44,ToolkitTheme.palette().accent(),1);
            label(()->"Total income: "+statsMoney(statsIncome(session.totals(),false)),60,ToolkitTheme.palette().text(),1);
            label("Started: "+statsDate(session.started),84);
            label(()->session.status+" \u00b7 "+statsTime(session.elapsed()),100,ToolkitTheme.palette().accent(),1);
            label("Area: "+session.location.label(),116);
            label(()->"Cash: "+statsMoney(session.cash)+" \u00b7 Base: "+statsMoney(session.baseCash),140,ToolkitTheme.palette().text(),1);
            label(()->statsShardRate(session.totals()),156,ToolkitTheme.palette().text(),1);
            label(()->"Shard value: "+statsMoney(GoldShardTracker.value(session.shards))+(ToolkitConfig.get().includeGoldShards?" included":" (excluded)"),172,ToolkitTheme.palette().text(),1);
            label(()->"Fish: "+session.fish+" \u00b7 Est. value: "+statsMoney(session.fishValue),188,ToolkitTheme.palette().text(),1);
            label("Income = cash + enabled shards at your current price.",212);
            label("Fish estimate is separate. Base removes the cash boost.",226);
            label("Rounded balances limit precision; time includes idle grace.",240);
            return;
        }
        pair(()->"< All items",()->{statsProfileId=null;scroll=0;statsRefresh();},()->"Area: "+statsAreaName(),this::statsNextArea,0);
        label(profile.title(),28);
        label(()->"Avg money: "+statsRate(statsTotals(profile.id)),56,ToolkitTheme.palette().accent(),1);
        label(()->{var t=statsTotals(profile.id);return "Base + enabled shards: "+statsMoney(t.hourly(statsIncome(t,true)))+"/h";},72,ToolkitTheme.palette().text(),1);
        label(()->statsShardRate(statsTotals(profile.id)),88,ToolkitTheme.palette().text(),1);
        label(()->{var t=statsTotals(profile.id);return "Income: "+statsMoney(statsIncome(t,false))+" · Avg/session: "+statsMoney(t.sessions()>0?statsIncome(t,false)/t.sessions():0);},104,ToolkitTheme.palette().text(),1);
        label(()->{var t=statsTotals(profile.id);return t.sessions()+" sessions · "+statsTime(t.elapsed());},120,ToolkitTheme.palette().text(),1);
        label(()->{var t=statsTotals(profile.id);return "Fish: "+t.fish()+" · Est. value: "+statsMoney(t.fishValue());},136,ToolkitTheme.palette().text(),1);
        label(()->history.recording(profile.id)?"Recording automatically":"Automatic · paused / not equipped",158,ToolkitTheme.palette().accent(),0.85f);
        label("Average = total income / total time, including idle grace.",176);
        label("Past sessions · money per hour",200);
        int top=216;
        if(history.sessions(profile.id,statsArea).isEmpty())label("No sessions in this area yet. Tracking starts automatically.",top);
        for(var s:history.sessions(profile.id,statsArea)){
            var row=addRenderableWidget(ToolkitButton.topic(statsRate(s.totals())+" \u00b7 "+s.status,
                statsDate(s.started)+" \u00b7 "+s.location.label()+" \u00b7 "+statsTime(s.elapsed()),b->{statsSessionId=s.id;scroll=0;statsRefresh();},layout.bodyX(),layout.bodyY(),layout.bodyWidth()));
            controls.add(new Control(row,top,40));contentHeight=Math.max(contentHeight,top+40);top+=44;
        }
    }
    private void skills(){
        var toolkit=ToolkitConfig.get();var money=ModConfig.getInstance();
        var fishing=ToolkitMod.FISHING;var fishCfg=fishing.settings();
        if(topic("Farming",0)){
            toggle("Clear Harvest textures","Dim immature crops to spot ready plants.",16,ClearHarvest::isEnabled,()->ClearHarvest.toggle(Minecraft.getInstance()));
            int row=48;
            toggle("Crop earnings HUD",row,()->toolkit.cropEarningsHud,()->toolkit.cropEarningsHud=!toolkit.cropEarningsHud);
            toggle("Harvest per hour HUD",row+24,()->toolkit.cropHarvestHud,()->toolkit.cropHarvestHud=!toolkit.cropHarvestHud);
            toggle("Include boosters in crop average",row+48,money::includesBoostersInAverage,money::toggleAverageBoosters);
            label(CropHarvestTracker::averageLabel,row+72,ToolkitTheme.palette().accent(),1);
            int next=row+124;
            if(details("Gold shards & income",row+96,"Include gold shards in income","Gold shard value ($ each)")){
                toggle("Include gold shards in income","Silent farming drops need a held hoe.",row+124,()->toolkit.includeGoldShards,()->toolkit.includeGoldShards=!toolkit.includeGoldShards);
                priceField("Gold shard value ($ each)",row+156,()->toolkit.goldShardPrice,v->{if(v<=1e12)toolkit.goldShardPrice=v;});
                hint("Accepts 1.5k / 2m. Estimated item value.",row+180);
                label(GoldShardTracker::label,row+196,ToolkitTheme.palette().accent(),1);
                next=row+230;
            }
            button("Compare income: Rate tests",next,()->{toolkit.rateTestXp=false;Minecraft.getInstance().setScreenAndShow(new BoosterStatsScreen(this,true));});
        }
        if(topic("Fishing",0)){
            slider("Bite sound volume",16,fishCfg.biteSoundVolume,4,v->fishCfg.biteSoundVolume=(float)v);
            toggle("Low bite ping","Swap the bite splash for a short, low tone.",40,()->fishCfg.lowBitePing,()->fishCfg.lowBitePing=!fishCfg.lowBitePing);
            toggle("Depleted area",72,()->fishCfg.warningEnabled,()->{fishCfg.warningEnabled=!fishCfg.warningEnabled;fishing.warningChanged();});
            slider("Fishing volume",96,fishCfg.volume,3,v->fishCfg.volume=(float)v);
            button("Preview depleted warning",120,fishing::previewWarning);
            link("Oni Rod settings",148,3,"Oni Rod");
            link("Fish sell prices",172,2,"Fish sell prices");
            link("Void Rod settings (Seasonal)",196,3,"Void Rod");
        }
        if(topic("Fish sell prices",0)){
            label("Sell price per fish ($ each; accepts 1.5k / 2m)",16);
            priceField("Cod · 60%",36,()->fishCfg.codPrice,v->fishCfg.codPrice=v);
            priceField("Salmon · 25%",60,()->fishCfg.salmonPrice,v->fishCfg.salmonPrice=v);
            priceField("Puffer · 13%",84,()->fishCfg.pufferPrice,v->fishCfg.pufferPrice=v);
            priceField("Tropical · 2%",108,()->fishCfg.tropicalPrice,v->fishCfg.tropicalPrice=v);
            setting("Earnings window",136,()->fishCfg.earningsWindow==60?"60 seconds":"5 minutes",()->fishCfg.earningsWindow=fishCfg.earningsWindow==60?300:60);
            label(()->"Weighted value / fish: "+BoosterStatsManager.formatMoney(fishCfg.averageFishPrice()),164,ToolkitTheme.palette().accent(),1);
            label(()->fishCfg.hasPrices()?ToolkitMod.fishingLabel():"Set fish prices to estimate earnings.",180,ToolkitTheme.palette().text(),1);
        }
        if(topic("Mining & XP",0)){
            toggle("XP per hour HUD","Appears while you collect XP.",16,()->toolkit.xpEarningsHud,()->toolkit.xpEarningsHud=!toolkit.xpEarningsHud);
            toggle("Include XP boosters",48,()->toolkit.xpIncludesBoosters,()->toolkit.xpIncludesBoosters=!toolkit.xpIncludesBoosters);
            label(XpTracker::averageLabel,76,ToolkitTheme.palette().xpText(),1);
            button("Measure XP: Rate tests",108,()->{toolkit.rateTestXp=true;Minecraft.getInstance().setScreenAndShow(new BoosterStatsScreen(this,true));});
        }
    }
    private void seasonals(){
        for(var item:SeasonalRegistry.ITEMS)if(item.name().equals(selectedTopics.get(page))){
            item.buildSettings(this,0);break;
        }
    }
    public int buildVoidRodSettings(int top){
        var fishing=ToolkitMod.FISHING;var fishCfg=fishing.settings();
        toggleCell("Ready to reel",top,0,()->fishCfg.depthEnabled,()->fishCfg.depthEnabled=!fishCfg.depthEnabled);
        toggleCell("Snapped line (Void Rod)",top,1,()->fishCfg.snapEnabled,()->fishCfg.snapEnabled=!fishCfg.snapEnabled);
        top+=24;
        slider("Fishing volume",top,fishCfg.volume,3,v->fishCfg.volume=(float)v);
        top+=24;
        label("Target depth for the Void Rod reel alert.",top);
        top+=14;
        label(()->"Reel depth: "+(int)fishCfg.target+" blocks",top,ToolkitTheme.palette().text(),1);
        top+=14;
        pair(()->"- 1 block",()->fishCfg.target=Math.max(1,fishCfg.target-1),()->"+ 1 block",()->fishCfg.target=Math.min(1000,fishCfg.target+1),top);
        top+=24;
        pair(()->"Preview ready",fishing::previewDepth,()->"Preview snap",fishing::previewSnap,top);
        top+=24;
        return top;
    }
    public int buildOniRodSettings(int top){
        var fishCfg=ToolkitMod.FISHING.settings();
        toggle("Oni Rod full-energy chime","Chimes when the Cursed Energy bar fills.",top,()->fishCfg.oniEnergyAlert,()->fishCfg.oniEnergyAlert=!fishCfg.oniEnergyAlert);
        return top+28;
    }
    public int buildBoneLeggingsSettings(int top){
        var cfg=ToolkitConfig.get();
        toggle("HUD overlay",top,()->cfg.boneLeggingsHud,()->cfg.boneLeggingsHud=!cfg.boneLeggingsHud);
        toggle("Audio chime alert",top+24,()->cfg.boneLeggingsSpeedAlert,()->cfg.boneLeggingsSpeedAlert=!cfg.boneLeggingsSpeedAlert);
        button("Reposition HUD overlay",top+48,()->go(5));
        if(details("Display appearance",top+80,"Always visible","Display style","Subtle background")){
            toggle("Always visible",top+108,()->cfg.boneLeggingsAlwaysVisible,()->cfg.boneLeggingsAlwaysVisible=!cfg.boneLeggingsAlwaysVisible);
            button(()->"Display style: "+BoneLeggingsTracker.styleName(cfg.boneLeggingsHudStyle),top+132,0,layout.bodyWidth(),()->cfg.boneLeggingsHudStyle=(cfg.boneLeggingsHudStyle+1)%4);
            toggle("Subtle background",top+156,()->cfg.boneLeggingsBackground,()->cfg.boneLeggingsBackground=!cfg.boneLeggingsBackground);
            hint("Uses Minecraft's Speed / Regen effect icons.",top+180);
            return top+200;
        }
        return top+104;
    }
    private void hudOverlays(){
        var cfg=ToolkitConfig.get();
        if(topic("Active HUD elements",0)){
            toggleCell("Money timer",16,0,()->cfg.boosterHud,()->cfg.boosterHud=!cfg.boosterHud);
            toggleCell("XP timer",16,1,()->cfg.xpBoosterHud,()->cfg.xpBoosterHud=!cfg.xpBoosterHud);
            toggleCell("Crop earnings",40,0,()->cfg.cropEarningsHud,()->cfg.cropEarningsHud=!cfg.cropEarningsHud);
            toggleCell("Fishing earnings",40,1,()->cfg.fishEarningsHud,()->cfg.fishEarningsHud=!cfg.fishEarningsHud);
            toggleCell("XP per hour",64,0,()->cfg.xpEarningsHud,()->cfg.xpEarningsHud=!cfg.xpEarningsHud);
            toggleCell("Harvest / hour",64,1,()->cfg.cropHarvestHud,()->cfg.cropHarvestHud=!cfg.cropHarvestHud);
            if(details("More displays",96,"Bone Leggings","Rate test info","Potion icons","Treasurechest timer")){
                toggleCell("Bone Leggings",124,0,()->cfg.boneLeggingsHud,()->cfg.boneLeggingsHud=!cfg.boneLeggingsHud);
                toggleCell("Rate test info",124,1,()->cfg.rateTestHud,()->cfg.rateTestHud=!cfg.rateTestHud);
                toggleCell("Potion icons",148,0,()->cfg.showStatusEffects,()->cfg.showStatusEffects=!cfg.showStatusEffects);
                toggleCell("Treasurechest timer",148,1,()->cfg.treasureChestHud,()->cfg.treasureChestHud=!cfg.treasureChestHud);
            }
        }
        if(topic("Layout & positioning",0)){
            button("Edit HUD positions",16,()->go(5));
            label("Drag displays to move them; eyes toggle visibility.",44);
            button("Reset HUD positions",76,()->{
                var c=ModConfig.getInstance();c.setPosition(ModConfig.Position.TOP_CENTER);c.setOffset(0,0);c.setAveragePosition(12,42);
                cfg.fishHudX=12;cfg.fishHudY=58;cfg.xpHudX=12;cfg.xpHudY=74;cfg.cropHarvestHudX=12;cfg.cropHarvestHudY=90;
                cfg.boneLeggingsHudX=-1;cfg.boneLeggingsHudY=12;cfg.treasureHudX=-1;cfg.treasureHudY=48;cfg.voiceHudX=12;cfg.voiceHudY=110;
            });
        }
        if(topic("Voice & music",0)){
            toggle("Voice & music status","Shows source switches, mute state and input warnings.",16,()->cfg.voiceMusicHud,()->cfg.voiceMusicHud=!cfg.voiceMusicHud);
            label("Use Sonar Stream as the voice microphone for music.",56);
            label("Voice-chat mute stops every source in the mix.",76);
            label("Sonar source switches select music, mic or both.",96);
            label("Status shows routing; it does not confirm delivery.",116);
            button("Edit HUD positions",148,()->go(5));
        }
        if(topic("Treasure chests",0)){
            toggle("Treasurechest timer","Open the treasure menu to sync the countdown.",16,()->cfg.treasureChestHud,()->cfg.treasureChestHud=!cfg.treasureChestHud);
            toggle("Only show last 30 seconds",48,()->cfg.treasureLastThirtySeconds,()->cfg.treasureLastThirtySeconds=!cfg.treasureLastThirtySeconds);
            toggle("Show discovered chests",72,()->cfg.treasureShowChests,()->cfg.treasureShowChests=!cfg.treasureShowChests);
            toggle("Reward pool estimates",96,()->cfg.treasureInferOpens,()->cfg.treasureInferOpens=!cfg.treasureInferOpens);
            int next=156;
            if(details("Appearance & timing",128,"Treasure background","Chest details duration","Treasure HUD scale")){
                toggle("Treasure background",156,()->cfg.treasureBackground,()->cfg.treasureBackground=!cfg.treasureBackground);
                setting("Chest details duration",180,()->cfg.treasureDetailsSeconds+"s",()->cfg.treasureDetailsSeconds=cfg.treasureDetailsSeconds>=300?30:cfg.treasureDetailsSeconds+30);
                setting("Treasure HUD scale",204,()->Math.round(cfg.treasureScale*100)+"%",()->cfg.treasureScale=cfg.treasureScale>=1.5?.6:Math.min(1.5,Math.round((cfg.treasureScale+.1)*10)/10.0));
                next=232;
            }
            if(details("How tracking works",next)){
                label("Later two-hour cycles are marked as estimates.",next+28);
                label("0–1 opens: zero confirmed, one possible.",next+44);
                label("Unassigned rewards may belong to unseen chests.",next+60);
                label("Discoveries and rewards are tracked while online.",next+76);
            }
        }
    }
    private void utilities(){
        var cfg=ToolkitConfig.get();var options=Minecraft.getInstance().options;
        if(topic("Chat selection & copy",0)){
            toggle("Chat selection","Drag to select · Ctrl+C / Cmd+C to copy",16,()->cfg.chatEnabled,()->cfg.chatEnabled=!cfg.chatEnabled);
            toggle("Select word",48,()->cfg.wordSelect,()->cfg.wordSelect=!cfg.wordSelect);
            toggle("Quick /gc code click","Click a 16-digit gift code to copy & open /gc",72,()->cfg.gcCodeClick,()->cfg.gcCodeClick=!cfg.gcCodeClick);
            if(details("Chat appearance",112,"Highlight color","Highlight strength","Chat size","Chat width","Chat opacity")){
                setting("Highlight color",140,cfg::highlightName,()->cfg.highlight=(cfg.highlight+1)%3);
                slider("Highlight strength",164,(cfg.highlightOpacity-.1)/.7,1,v->cfg.highlightOpacity=.1+v*.7);
                slider("Chat size",188,options.chatScale().get(),1,v->options.chatScale().set(Math.max(.1,v)));
                slider("Chat width",212,options.chatWidth().get(),1,v->options.chatWidth().set(v));
                slider("Chat opacity",236,options.chatOpacity().get(),1,v->options.chatOpacity().set(v));
            }
        }
        if(topic("Auction notifications",0)){
            toggle("Sale alert",16,()->cfg.ahSoundEnabled,()->cfg.ahSoundEnabled=!cfg.ahSoundEnabled);
            setting("Sound",40,cfg::ahSoundName,()->cfg.ahSoundType=(cfg.ahSoundType+1)%5);
            slider("Volume",64,cfg.ahSoundVolume,1,v->cfg.ahSoundVolume=v);
            button("Preview sale sound",88,AhSaleNotifier::playPreviewSound);
            if(details("Extra volume boost",120,"Sound amplification")){
                slider("Sound amplification",148,cfg.ahSoundGain,3,v->cfg.ahSoundGain=v);
            }
        }
        if(topic("Display & visuals",0)){
            toggle("Fullbright","Full brightness across caves and dimensions.",16,()->cfg.fullBright,()->cfg.fullBright=!cfg.fullBright);
            link("Keyboard shortcuts",52,6,"Keyboard shortcuts");
        }
        if(topic("Blackout (AFK)",0)){
            button("Turn blackout on",16,()->{persist();BlackoutManager.setActive(true);onClose();});
            slider("Opacity",40,cfg.blackoutOpacity,1,v->cfg.blackoutOpacity=v);
            toggle("Wake hint",64,()->cfg.blackoutHint,()->cfg.blackoutHint=!cfg.blackoutHint);
            toggle("Overlay chat",88,()->cfg.blackoutChatOverlay,()->cfg.blackoutChatOverlay=!cfg.blackoutChatOverlay);
            link("Keyboard shortcuts",116,6,"Keyboard shortcuts");
            label("Dim the game while keeping audio active.",144);
        }
        if(topic("Keyboard shortcuts",0)){
            setting("Menu key",16,()->ToolkitMod.menuKey.getTranslatedKeyMessage().getString(),()->bindingTarget=1);
            sellAllBinding(40);
            setting("Wake key",64,()->ToolkitMod.blackoutKey.getTranslatedKeyMessage().getString(),()->bindingTarget=2);
            setting("Fullbright key",88,()->ToolkitMod.fullBrightKey.getTranslatedKeyMessage().getString(),()->bindingTarget=4);
        }
    }
    private void thresholdField(int top){
        top-=topicOffset;
        var cfg=ToolkitConfig.get();
        labels.add(new Label(()->"Minimum boost (%)",top+5,ToolkitTheme.palette().text(),1,6));
        EditBox field=addRenderableWidget(new EditBox(font,layout.bodyX()+layout.bodyWidth()-64,layout.bodyY()+top+5-scroll,58,14,Component.literal("Minimum coconut money booster percentage")));
        field.setBordered(false);field.setTextShadow(false);field.setTextColorUneditable(ToolkitTheme.palette().muted());
        field.setMaxLength(6);field.setValue(Integer.toString(cfg.coconutSoundThreshold));
        field.setTextColor(ToolkitTheme.palette().text());
        field.setResponder(text->{
            try{
                int value=Integer.parseInt(text);
                if(value<0||value>100000)throw new NumberFormatException();
                cfg.coconutSoundThreshold=value;field.setTextColor(ToolkitTheme.palette().text());
            }catch(NumberFormatException e){field.setTextColor(ToolkitTheme.palette().error());}
        });
        field.setTooltip(Tooltip.create(Component.literal("0 alerts for every coconut money boost. Enter 0 to 100000; invalid input keeps the last valid value.")));
        settingFields.add(field);controls.add(new Control(field,top+5,14));contentHeight=Math.max(contentHeight,top+22);
    }
    private void setting(String name,int top,Supplier<String> value,Runnable action){
        top-=topicOffset;
        var button=addRenderableWidget(ToolkitButton.setting(name,value,b->{action.run();persist();},layout.bodyX(),layout.bodyY()+top-scroll,layout.bodyWidth()));
        controls.add(new Control(button,top,20));contentHeight=Math.max(contentHeight,top+20);
    }
    private void sellAllBinding(int top){setting("Sell all key",top,()->ToolkitMod.sellAllKey.getTranslatedKeyMessage().getString(),()->bindingTarget=3);}
    private String timerStatus(BoosterTimers.Type type){
        long now=System.currentTimeMillis();var timers=BoosterManager.timers;var clock=timers.clock(type);
        return type.label+": "+(clock.active(now)?"+"+timers.percentage(type)+"% · "+BoosterTimers.formatTime(clock.remaining(now),clock.hasTimer(now)):"Inactive");
    }
    private void priceField(String name,int top,DoubleSupplier read,DoubleConsumer write){
        top-=topicOffset;
        labels.add(new Label(()->name,top+5,ToolkitTheme.palette().text(),1,6));
        EditBox field=addRenderableWidget(new EditBox(font,layout.bodyX()+layout.bodyWidth()-94,layout.bodyY()+top+5-scroll,88,14,Component.literal(name)));
        field.setBordered(false);field.setTextShadow(false);field.setMaxLength(24);field.setValue(String.format(java.util.Locale.ROOT,"%.2f",read.getAsDouble()));
        field.setTextColor(ToolkitTheme.palette().text());
        field.setResponder(text->{double value=com.fishingpings.FishingEarnings.price(text);field.setTextColor(Double.isFinite(value)?com.skyblocktoolkit.ToolkitTheme.palette().text():com.skyblocktoolkit.ToolkitTheme.palette().error());if(Double.isFinite(value))write.accept(value);});
        priceFields.add(field);controls.add(new Control(field,top+5,14));contentHeight=Math.max(contentHeight,top+22);
    }
    private void initLayout(){} // Only the HUD elements and their eye controls live on this canvas.
    private int eyeX(int x,int elementWidth){return x+elementWidth+15<=width?x+elementWidth+5:Math.max(1,x-14);}
    private void eye(GuiGraphicsExtractor g,int x,int y,boolean visible){
        int color=visible?ToolkitTheme.palette().accent():ToolkitTheme.palette().muted();
        g.fill(x+2,y+3,x+8,y+7,color);g.fill(x,y+4,x+10,y+6,color);
        g.fill(x+4,y+3,x+6,y+7,com.skyblocktoolkit.ToolkitTheme.palette().panel());
        if(!visible)for(int i=0;i<8;i++)g.fill(x+1+i,y+8-i,x+2+i,y+9-i,color);
    }
    private void updateControls(){
        scroll=Math.clamp(scroll,0,layout.maxScroll(contentHeight));
        for(var row:controls){row.widget().setY(layout.bodyY()+row.top()-scroll);row.widget().visible=layout.fits(row.top(),row.rowHeight(),scroll);
            if(!row.widget().visible&&getFocused()==row.widget())setFocused(null);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        if(page==5){renderLayout(g,mx,my,tick);return;}
        updateControls();int x=layout.x(),y=layout.y(),w=layout.width(),h=layout.height();
        g.fill(0,0,width,height,com.skyblocktoolkit.ToolkitTheme.palette().scrim());
        ToolkitTheme.rounded(g,x-2,y+3,w+4,h+2,7,com.skyblocktoolkit.ToolkitTheme.palette().shadow());
        ToolkitTheme.rounded(g,x-1,y-1,w+2,h+2,7,ToolkitTheme.palette().border());
        ToolkitTheme.rounded(g,x,y,w,h,6,ToolkitTheme.palette().panel());
        ToolkitTheme.brandHeader(g,x,y,w,layout.compact());
        if(!layout.compact()){
            ToolkitTheme.rounded(g,x,y,132,h,6,ToolkitTheme.palette().sidebar());g.fill(x+124,y,x+132,y+h,ToolkitTheme.palette().sidebar());
            ToolkitTheme.brandFrame(g,x,y,w,h,false);
            g.blit(LOGO,x+10,y+8,x+44,y+42,0f,1f,0f,1f);
            ToolkitTheme.text(g,font,"COMPLEXIFY",x+49,y+15,ToolkitTheme.palette().text());
            ToolkitTheme.text(g,font,"v"+UpdateManager.getCurrentVersion(),x+49,y+28,ToolkitTheme.palette().muted());
            g.text(font,query.isBlank()?(page<PAGES.length?PAGES[page]:""):"Search settings",layout.bodyX(),y+20,ToolkitTheme.palette().text(),false);
            ToolkitTheme.text(g,font,ToolkitTheme.clipText(font,Component.literal(query.isBlank()?topics().stream().filter(t->t.title().equals(selectedTopics.get(page))).map(Topic::summary).findFirst().orElse(page<DESCRIPTIONS.length?DESCRIPTIONS[page]:""):"Across all categories"),layout.bodyWidth()-100),layout.bodyX(),y+39,ToolkitTheme.palette().muted());
            g.fill(layout.bodyX(),y+55,layout.bodyX()+layout.bodyWidth(),y+56,ToolkitTheme.palette().border());
            g.fill(layout.bodyX(),y+55,layout.bodyX()+32,y+56,ToolkitTheme.palette().brand());
            int sx=x+12,sy=y+52,sw=112;
            ToolkitTheme.rounded(g,sx,sy,sw,20,3,ToolkitTheme.palette().surface());
            if(searchBox.isFocused())g.fill(sx+4,sy+19,sx+sw-4,sy+20,ToolkitTheme.palette().accent());
            if(query.isEmpty()&&!searchBox.isFocused())g.text(font,"Search...",searchBox.getX(),searchBox.getY()+3,ToolkitTheme.palette().muted(),false);
        }else{
            ToolkitTheme.brandFrame(g,x,y,w,h,true);
            g.blit(LOGO,x+8,y+10,x+26,y+28,0f,1f,0f,1f);
            g.text(font,"Complexify",x+30,y+15,ToolkitTheme.palette().text(),false);
            int sx=layout.bodyX(),sy=y+68,sw=layout.bodyWidth();
            ToolkitTheme.rounded(g,sx,sy,sw-(clearSearch!=null&&clearSearch.visible?26:0),20,3,ToolkitTheme.palette().surface());
            if(searchBox.isFocused())g.fill(sx+4,sy+19,sx+sw-(clearSearch!=null&&clearSearch.visible?30:4),sy+20,ToolkitTheme.palette().accent());
            if(query.isEmpty()&&!searchBox.isFocused())g.text(font,"Search settings...",searchBox.getX(),searchBox.getY()+3,ToolkitTheme.palette().muted(),false);
        }
        g.enableScissor(layout.bodyX(),layout.bodyY(),layout.bodyX()+layout.bodyWidth(),layout.bodyY()+layout.viewport());
        for(var label:labels){String text=label.text().get();if(text.isEmpty()||!layout.fits(label.top(),(int)Math.ceil(10*label.scale()),scroll))continue;
            g.pose().pushMatrix();g.pose().translate(layout.bodyX()+label.offset(),layout.bodyY()+label.top()-scroll);g.pose().scale(label.scale(),label.scale());
            String clipped=font.substrByWidth(Component.literal(text),(int)((layout.bodyWidth()-label.offset())/label.scale())).getString();
            g.text(font,clipped,0,0,label.color(),false);g.pose().popMatrix();
        }
        for(var field:settingFields)if(field.visible)ToolkitTheme.rounded(g,field.getX()-6,field.getY()-4,field.getWidth()+12,20,3,field.isFocused()?ToolkitTheme.palette().hover():ToolkitTheme.palette().surface());
        for(var field:priceFields)if(field.visible)ToolkitTheme.rounded(g,field.getX()-6,field.getY()-4,field.getWidth()+12,20,3,field.isFocused()?ToolkitTheme.palette().hover():ToolkitTheme.palette().surface());
        g.disableScissor();
        if(layout.maxScroll(contentHeight)>0){int track=layout.viewport(),thumb=Math.max(12,track*track/contentHeight),ty=layout.bodyY()+scroll*(track-thumb)/layout.maxScroll(contentHeight);g.fill(x+w-5,ty,x+w-3,ty+thumb,com.skyblocktoolkit.ToolkitTheme.palette().muted());}
        super.extractRenderState(g,mx,my,tick);

        if(bindingTarget>0){
            ToolkitTheme.rounded(g,x,y,w,h,6,ToolkitTheme.palette().panel());
            g.centeredText(font,Component.literal(switch(bindingTarget){case 1->"Press your menu key";case 2->"Press your black screen key";case 4->"Press your fullbright key";default->"Press your sell all key";}),x+w/2,y+h/2-12,com.skyblocktoolkit.ToolkitTheme.palette().text());
            g.centeredText(font,Component.literal("Escape cancels · Delete clears"),x+w/2,y+h/2+8,ToolkitTheme.palette().muted());
        }
        if(VisualAlertOverlay.isActive()){
            VisualAlertOverlay.render(g,width,height);
        }
    }
    private record HudPart(int id,String text,int x,int y,boolean visible){}
    private List<HudPart> hudParts(){
        var mc=Minecraft.getInstance();var cfg=ToolkitConfig.get();
        String crops=BoosterStatsManager.averageLabel(),fish=ToolkitMod.fishingLabel();
        if(fish.isEmpty())fish="Fishing est. ("+(ToolkitMod.FISHING.settings().earningsWindow==300?"5m":"60s")+"): $0/h";
        String harvest=CropHarvestTracker.averageLabel();
        String bone=BoneLeggingsTracker.previewLabel();
        return List.of(new HudPart(2,crops,BoosterHudOverlay.averageX(mc,crops),BoosterHudOverlay.averageY(mc),cfg.cropEarningsHud),
            new HudPart(3,fish,BoosterHudOverlay.fishX(mc,fish),BoosterHudOverlay.fishY(mc),cfg.fishEarningsHud),
            new HudPart(4,XpTracker.averageLabel(),BoosterHudOverlay.xpX(mc,XpTracker.averageLabel()),BoosterHudOverlay.xpY(mc),cfg.xpEarningsHud),
            new HudPart(5,harvest,BoosterHudOverlay.cropHarvestX(mc,harvest),BoosterHudOverlay.cropHarvestY(mc),cfg.cropHarvestHud),
            new HudPart(6,bone,BoosterHudOverlay.boneLeggingsX(mc,bone),BoosterHudOverlay.boneLeggingsY(mc),cfg.boneLeggingsHud),
            new HudPart(7,"Treasure chests",TreasureChestTracker.x(mc,true),TreasureChestTracker.y(mc,true),cfg.treasureChestHud),
            new HudPart(8,"Voice & music",VoiceMusicHud.x(mc,true),VoiceMusicHud.y(mc),cfg.voiceMusicHud));
    }
    private void renderLayout(GuiGraphicsExtractor g,int mx,int my,float tick){
        var mc=Minecraft.getInstance();g.fill(0,0,width,height,0x3506090E);
        int bx=BoosterHudOverlay.getBarX(width),by=BoosterHudOverlay.getBarY(mc);
        var cfg=ToolkitConfig.get();
        for(var type:BoosterTimers.Type.values()){
            int rowY=by+type.ordinal()*BoosterHudOverlay.TIMER_SPACING;
            boolean visible=type==BoosterTimers.Type.MONEY?cfg.boosterHud:cfg.xpBoosterHud;
            String label=type==BoosterTimers.Type.MONEY?"Money +39%: §f0m 42s":"XP +150%: §f2m 10s";
            BoosterHudOverlay.renderTimerBar(g,mc,bx,rowY,label,type==BoosterTimers.Type.MONEY?.7f:.85f,type,false);
            g.outline(bx-4,rowY-12,BoosterHudOverlay.BAR_WIDTH+8,22,visible?ToolkitTheme.palette().accent():ToolkitTheme.palette().muted());
            eye(g,eyeX(bx,BoosterHudOverlay.BAR_WIDTH),rowY-10,visible);
        }
        for(var part:hudParts()){
            if(part.id()==8){
                VoiceMusicHud.render(g,mc,true);int pw=VoiceMusicHud.width(mc,true),ph=VoiceMusicHud.height();
                g.outline(part.x()-2,part.y()-2,pw+4,ph+4,part.visible()?ToolkitTheme.palette().accent():ToolkitTheme.palette().muted());
                eye(g,eyeX(part.x(),pw),part.y()-2,part.visible());
            }else if(part.id()==7){
                TreasureChestTracker.render(g,mc,true);int pw=TreasureChestTracker.width(mc,true),ph=TreasureChestTracker.height(true);
                g.outline(part.x()-2,part.y()-2,pw+4,ph+4,part.visible()?ToolkitTheme.palette().accent():ToolkitTheme.palette().muted());
                eye(g,eyeX(part.x(),pw),part.y()-2,part.visible());
            }else if(part.id()==6){
                BoneLeggingsTracker.renderWidget(g,mc,part.x(),part.y(),true);
                int pw=BoneLeggingsTracker.getWidth(mc);
                int ph=BoneLeggingsTracker.getHeight();
                g.outline(part.x()-2,part.y()-2,pw+4,ph+4,part.visible()?ToolkitTheme.palette().accent():ToolkitTheme.palette().muted());
                eye(g,eyeX(part.x(),pw),part.y()-2,part.visible());
            }else{
                g.text(font,part.text(),part.x(),part.y(),ToolkitPalette.DARK.text(),true);
                g.outline(part.x()-3,part.y()-3,font.width(part.text())+6,15,part.visible()?ToolkitTheme.palette().accent():ToolkitTheme.palette().muted());
                eye(g,eyeX(part.x(),font.width(part.text())),part.y()-3,part.visible());
            }
        }
        if(dragging==0)g.centeredText(font,Component.literal("Drag to move · Eye to show / hide · Esc to return"),width/2,height-14,ToolkitPalette.DARK.muted());
        super.extractRenderState(g,mx,my,tick);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        if(bindingTarget==0&&page!=5&&mx>=layout.bodyX()&&mx<=layout.bodyX()+layout.bodyWidth()&&my>=layout.bodyY()&&my<layout.footerY()){scroll+=(int)(-vertical*26);updateControls();return true;}
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(page==13&&event.key()==GLFW.GLFW_KEY_ESCAPE&&query.isEmpty()&&statsProfileId!=null){
            if(statsSessionId!=null)statsSessionId=null;else statsProfileId=null;
            scroll=0;statsRefresh();return true;
        }
        if(bindingTarget>0){
            if(event.key()!=GLFW.GLFW_KEY_ESCAPE){
                int key=event.key()==GLFW.GLFW_KEY_DELETE||event.key()==GLFW.GLFW_KEY_BACKSPACE?GLFW.GLFW_KEY_UNKNOWN:event.key();
                KeyMapping target=bindingTarget==1?ToolkitMod.menuKey:bindingTarget==2?ToolkitMod.blackoutKey:bindingTarget==3?ToolkitMod.sellAllKey:ToolkitMod.fullBrightKey;
                target.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));
                KeyMapping.resetMapping();
                Minecraft.getInstance().options.save();
            }
            bindingTarget=0;
            return true;
        }
        if(page!=5&&event.key()==GLFW.GLFW_KEY_F&&(event.modifiers()&(GLFW.GLFW_MOD_CONTROL|GLFW.GLFW_MOD_SUPER))!=0){setFocused(searchBox);return true;}
        if(page!=5&&event.key()==GLFW.GLFW_KEY_ESCAPE&&!query.isEmpty()){searchBox.setValue("");return true;}
        if(page!=5&&event.key()==GLFW.GLFW_KEY_ESCAPE&&selectedTopics.containsKey(page)){backToTopics();return true;}
        if(page==5&&event.key()==GLFW.GLFW_KEY_ESCAPE){go(layoutReturnPage);return true;}
        if(page!=5&&!(getFocused() instanceof EditBox)){
            if(event.key()==GLFW.GLFW_KEY_PAGE_DOWN||event.key()==GLFW.GLFW_KEY_PAGE_UP){scroll+=(event.key()==GLFW.GLFW_KEY_PAGE_DOWN?1:-1)*Math.max(24,layout.viewport()-24);updateControls();return true;}
        }
        return super.keyPressed(event);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean handled){
        if(bindingTarget>0)return true;
        if(page==5&&event.button()==0){
            for(var child:children())if(child instanceof AbstractWidget w&&w.visible&&w.isMouseOver(event.x(),event.y()))return super.mouseClicked(event,handled);
            var mc=Minecraft.getInstance();var cfg=ToolkitConfig.get();
            int bx=BoosterHudOverlay.getBarX(width),by=BoosterHudOverlay.getBarY(mc),tx=eyeX(bx,BoosterHudOverlay.BAR_WIDTH);
            for(var part:hudParts()){
                int pw=part.id()==8?VoiceMusicHud.width(mc,true):part.id()==7?TreasureChestTracker.width(mc,true):part.id()==6?BoneLeggingsTracker.getWidth(mc):font.width(part.text());
                int ex=eyeX(part.x(),pw);
                if(event.x()>=ex&&event.x()<ex+10&&event.y()>=part.y()-3&&event.y()<part.y()+7){
                    if(part.id()==2)cfg.cropEarningsHud=!cfg.cropEarningsHud;
                    else if(part.id()==3)cfg.fishEarningsHud=!cfg.fishEarningsHud;
                    else if(part.id()==4)cfg.xpEarningsHud=!cfg.xpEarningsHud;
                    else if(part.id()==5)cfg.cropHarvestHud=!cfg.cropHarvestHud;
                    else if(part.id()==6)cfg.boneLeggingsHud=!cfg.boneLeggingsHud;
                    else if(part.id()==7)cfg.treasureChestHud=!cfg.treasureChestHud;
                    else if(part.id()==8)cfg.voiceMusicHud=!cfg.voiceMusicHud;
                    persist();return true;
                }
            }
            for(var type:BoosterTimers.Type.values()){
                int rowY=by+type.ordinal()*BoosterHudOverlay.TIMER_SPACING;
                if(event.x()>=tx&&event.x()<tx+10&&event.y()>=rowY-10&&event.y()<rowY){
                    if(type==BoosterTimers.Type.MONEY)cfg.boosterHud=!cfg.boosterHud;else cfg.xpBoosterHud=!cfg.xpBoosterHud;
                    persist();return true;
                }
            }
            for(var part:hudParts()){
                int pw=part.id()==8?VoiceMusicHud.width(mc,true):part.id()==7?TreasureChestTracker.width(mc,true):part.id()==6?BoneLeggingsTracker.getWidth(mc):font.width(part.text());
                int ph=part.id()==8?VoiceMusicHud.height():part.id()==7?TreasureChestTracker.height(true):part.id()==6?BoneLeggingsTracker.getHeight():11;
                if(event.x()>=part.x()-3&&event.x()<=part.x()+pw+3&&event.y()>=part.y()-3&&event.y()<=part.y()+ph+3){
                    dragging=part.id();grabX=event.x()-part.x();grabY=event.y()-part.y();return true;
                }
            }
            if(event.x()>=bx-4&&event.x()<=bx+BoosterHudOverlay.BAR_WIDTH+4&&event.y()>=by-12&&event.y()<=by+BoosterHudOverlay.TIMER_SPACING+10){var c=ModConfig.getInstance();c.setOffset(bx-10,by-16);c.setPosition(ModConfig.Position.TOP_LEFT);dragging=1;grabX=event.x()-bx;grabY=event.y()-by;return true;}

        }
        return super.mouseClicked(event,handled);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        var cfg=ModConfig.getInstance();
        if(dragging==8){var c=ToolkitConfig.get();var mc=Minecraft.getInstance();
            c.voiceHudX=Math.clamp((int)(event.x()-grabX),4,Math.max(4,width-VoiceMusicHud.width(mc,true)-4));
            c.voiceHudY=Math.clamp((int)(event.y()-grabY),4,Math.max(4,height-VoiceMusicHud.height()-4));return true;
        }
        if(dragging==7){var c=ToolkitConfig.get();var mc=Minecraft.getInstance();
            c.treasureHudX=Math.clamp((int)(event.x()-grabX),2,Math.max(2,width-TreasureChestTracker.width(mc,true)-2));
            c.treasureHudY=Math.clamp((int)(event.y()-grabY),2,Math.max(2,height-TreasureChestTracker.height(true)-2));return true;
        }
        if(dragging==6){
            var c=ToolkitConfig.get();
            int pw=BoneLeggingsTracker.getWidth(Minecraft.getInstance());
            int ph=BoneLeggingsTracker.getHeight();
            c.boneLeggingsHudX=Math.clamp((int)(event.x()-grabX),2,Math.max(2,width-pw-2));
            c.boneLeggingsHudY=Math.clamp((int)(event.y()-grabY),2,Math.max(2,height-ph-2));
            return true;
        }
        if(dragging==5){var c=ToolkitConfig.get();c.cropHarvestHudX=Math.clamp((int)(event.x()-grabX),2,Math.max(2,width-font.width(CropHarvestTracker.averageLabel())-2));c.cropHarvestHudY=Math.clamp((int)(event.y()-grabY),2,Math.max(2,height-11));return true;}
        if(dragging==4){var c=ToolkitConfig.get();c.xpHudX=Math.clamp((int)(event.x()-grabX),2,Math.max(2,width-font.width(XpTracker.averageLabel())-2));c.xpHudY=Math.clamp((int)(event.y()-grabY),2,Math.max(2,height-11));return true;}
        if(dragging==3){var c=ToolkitConfig.get();String fish=ToolkitMod.fishingLabel();if(fish.isEmpty())fish="Fishing est. ("+(ToolkitMod.FISHING.settings().earningsWindow==300?"5m":"60s")+"): $0/h";c.fishHudX=Math.clamp((int)(event.x()-grabX),2,Math.max(2,width-font.width(fish)-2));c.fishHudY=Math.clamp((int)(event.y()-grabY),2,Math.max(2,height-11));return true;}
        if(dragging==2){cfg.setAveragePosition(Math.clamp((int)(event.x()-grabX),2,Math.max(2,width-font.width(BoosterStatsManager.averageLabel())-2)),Math.clamp((int)(event.y()-grabY),2,Math.max(2,height-11)));return true;}
        if(dragging==1){cfg.setOffset(Math.clamp((int)(event.x()-grabX),2,Math.max(2,width-BoosterHudOverlay.BAR_WIDTH-2))-10,Math.clamp((int)(event.y()-grabY),14,Math.max(14,height-24-BoosterHudOverlay.TIMER_SPACING))-16);return true;}
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event){if(dragging!=0){dragging=0;persist();return true;}persist();return super.mouseReleased(event);}
    private void persist(){ToolkitConfig.get().save();ModConfig.getInstance().save();ToolkitMod.FISHING.settings().save();Minecraft.getInstance().options.save();}
    @Override public void removed(){persist();}
    @Override public void onClose(){persist();Minecraft.getInstance().setScreenAndShow(parent);}
    @Override public boolean isPauseScreen(){return parent!=null&&parent.isPauseScreen();}
}
