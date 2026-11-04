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
    private int bindingTarget; // 0=none, 1=menu, 2=blackout, 3=sellall
    private boolean fishEarnings;
    private final Set<Integer> expandedPages=new HashSet<>();
    private final Set<String> expandedSeasonals=new HashSet<>();
    private int advancedDivider=-1;
    private String query="",jumpSection="",jumpSetting="";
    private EditBox searchBox;
    private Button clearSearch;
    private int savedScroll,layoutReturnPage=1;
    private final List<EditBox> priceFields=new ArrayList<>();
    private final List<EditBox> settingFields=new ArrayList<>();
    private ToolkitLayout layout;
    private final List<Control> controls=new ArrayList<>();
    private final List<Label> labels=new ArrayList<>();
    private record Control(AbstractWidget widget,int top,int rowHeight){}
    private record Label(Supplier<String> text,int top,int color,float scale,int offset){
        Label(Supplier<String> text,int top,int color,float scale){this(text,top,color,scale,0);}
    }
    private static final String[] PAGES={"Boosters","Boosters","Skills","Seasonals","HUD & Overlays","HUD layout","Utilities"};
    private static final String[] DESCRIPTIONS={
        "",
        "Timers, coconut alerts & session statistics",
        "Farming, fishing & mining helpers",
        "Seasonal items, custom tools & unique gear",
        "On-screen displays & layout configuration",
        "Interactive HUD placement canvas",
        "Chat selection, auction sales & blackout mode"
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
            default->Math.clamp(p,1,6);
        };
    }
    @Override protected void init(){
        layout=ToolkitLayout.hub(width,height);controls.clear();labels.clear();priceFields.clear();settingFields.clear();contentHeight=0;advancedDivider=-1;
        if(page==5){initLayout();return;}
        int x=layout.x(),y=layout.y(),w=layout.width();
        addThemeButton(x,y,w);
        int[] targets={1,2,3,4,6};String[] names={"Boosters","Skills","Seasonals","HUD & Overlays","Utilities"};
        String[] compactNames={"Boost","Skills","Season","HUD","Utils"};
        for(int i=0;i<targets.length;i++){
            final int target=targets[i];int bx=layout.compact()?x+6+i*((w-12)/targets.length):x+4;
            int by=layout.compact()?y+36:y+66+i*24,bw=layout.compact()?(w-12)/targets.length-2:104;
            var nav=addRenderableWidget(ToolkitButton.nav(layout.compact()?compactNames[i]:names[i],button->go(target),bx,by,bw,20,page==target&&query.isBlank()));
            nav.setTooltip(Tooltip.create(Component.literal(names[i])));
        }
        addRenderableWidget(ToolkitButton.action("Edit HUD",b->go(5),x+4,layout.footerY(),96,20));
        addRenderableWidget(ToolkitButton.action("Close",b->onClose(),x+w-76,layout.footerY(),64,20));
        if(UpdateManager.isUpdateAvailable()){
            String updateText=UpdateManager.getState()==UpdateManager.State.DOWNLOADING?"Downloading...":
                              UpdateManager.getState()==UpdateManager.State.READY_RESTART?"Restart Game":
                              "Update v"+UpdateManager.getLatestVersion();
            int ux=x+104,uw=Math.max(70,(x+w-80)-ux);
            addRenderableWidget(ToolkitButton.action(updateText,b->{
                UpdateManager.triggerUpdateFromCommand();
                if(UpdateManager.getState()==UpdateManager.State.READY_RESTART)onClose();
            },ux,layout.footerY(),Math.min(uw,104),20));
        }

        initSearch();
        buildBody();
    }
    private void go(int next){persist();if(next==5)layoutReturnPage=page==5?1:page;page=next==0?1:next;scroll=0;dragging=0;bindingTarget=0;fishEarnings=false;query="";jumpSection="";rebuildWidgets();}
    private void initSearch(){
        int sx,sy,sw,editW,clearX,clearY,clearW,clearH;
        if(!layout.compact()){
            sx=layout.x()+4;sy=layout.y()+42;sw=104;
            editW=sw-22;clearW=14;clearH=16;
            clearX=sx+sw-16;clearY=sy+2;
        }else{
            sx=layout.bodyX();sy=layout.y()+60;sw=layout.bodyWidth();
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
        for(var control:controls)removeWidget(control.widget());
        controls.clear();labels.clear();priceFields.clear();settingFields.clear();contentHeight=0;advancedDivider=-1;
    }
    private void buildBody(){
        if(!query.isBlank()){
            var results=ToolkitSearch.find(query);
            label(results.isEmpty()?"No settings found. Try sound, HUD or key.":results.size()+(results.size()==1?" setting found":" settings found")+" · select to open",0);
            int top=18;
            for(var result:results){
                button(()->result.title(),top,0,layout.bodyWidth(),()->{
                    persist();page=result.page();
                    query="";scroll=0;
                    jumpSection=result.section();jumpSetting=result.setting();
                    if(result.page()==3){
                        for(var it:SeasonalRegistry.ITEMS){
                            if(it.name().equalsIgnoreCase(result.section())||it.id().equalsIgnoreCase(result.section())
                                ||(it.id().contains("bone")&&result.section().toLowerCase().contains("bone")))
                                expandedSeasonals.add(it.id());
                        }
                    }
                    rebuildWidgets();
                });
                top+=24;
            }
        }else{
            if(page==3&&!jumpSection.isEmpty()){
                for(var it:SeasonalRegistry.ITEMS){
                    if(it.name().equalsIgnoreCase(jumpSection)||it.id().equalsIgnoreCase(jumpSection)
                        ||(it.id().contains("bone")&&jumpSection.toLowerCase().contains("bone")))
                        expandedSeasonals.add(it.id());
                }
            }
            switch(page){
                case 1->boosters();
                case 2->skills();
                case 3->seasonals();
                case 4->hudOverlays();
                case 6->utilities();
                default->boosters();
            }
        }
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
    private void section(String title,int top){
        label(()->title,top,ToolkitTheme.palette().accent(),1);
        if(title.equals(jumpSection))scroll=top;
    }
    private void addThemeButton(int x,int y,int w){
        var cfg=ToolkitConfig.get();
        var button=addRenderableWidget(ToolkitButton.themeSwitch(b->{
            cfg.lightMode=!cfg.lightMode;persist();rebuildWidgets();
        },x+w-72,y+10,cfg.lightMode));
        button.setTooltip(Tooltip.create(Component.literal("Switch to "+(cfg.lightMode?"dark":"light")+" theme")));
    }
    private void label(Supplier<String> text,int top,int color,float scale){labels.add(new Label(text,top,color,scale));contentHeight=Math.max(contentHeight,top+(int)(10*scale));}
    private void label(String text,int top){label(()->text,top,ToolkitTheme.palette().muted(),1);}
    private void button(String text,int top,Runnable action){button(()->text,top,0,Math.min(144,layout.bodyWidth()),action);}
    private void button(Supplier<String> text,int top,int offset,int w,Runnable action){
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
                g.text(font,percent,getRight()-6-font.width(percent),getY()+6,palette.muted(),false);
                int end=getRight()-44,start=getX()+4,knob=start+(int)((end-start)*value),cy=getY()+10;
                g.fill(start,cy-1,end,cy+1,palette.track());g.fill(start,cy-1,knob,cy+1,palette.accentTrack());
                ToolkitTheme.rounded(g,knob-2,cy-3,4,6,2,palette.accent());

            }
        });slider.setMessage(Component.literal(name+": "+Math.round(setting*100)+"%"));controls.add(new Control(slider,top,20));contentHeight=Math.max(contentHeight,top+20);
    }
    private void toggle(String text,int top,BooleanSupplier state,Runnable action){toggleAt(text,top,0,layout.bodyWidth(),state,action);}
    private void toggleCell(String text,int top,int column,BooleanSupplier state,Runnable action){
        int half=(layout.bodyWidth()-10)/2;toggleAt(text,top,column*(half+10),half,state,action);
    }
    private void toggleAt(String text,int top,int offset,int width,BooleanSupplier state,Runnable action){
        var b=addRenderableWidget(ToolkitButton.toggle(text,unused->{action.run();persist();updateControls();},layout.bodyX()+offset,layout.bodyY()+top-scroll,width,20,state));
        controls.add(new Control(b,top,20));contentHeight=Math.max(contentHeight,top+20);
    }
    private void boosters(){
        var cfg=ModConfig.getInstance();var toolkit=ToolkitConfig.get();
        section("Booster timers",0);
        toggleCell("Money timer",16,0,()->toolkit.boosterHud,()->toolkit.boosterHud=!toolkit.boosterHud);
        toggleCell("XP timer",16,1,()->toolkit.xpBoosterHud,()->toolkit.xpBoosterHud=!toolkit.xpBoosterHud);
        toggleCell("Ending banner",40,0,()->toolkit.boosterExpiringAlert,()->toolkit.boosterExpiringAlert=!toolkit.boosterExpiringAlert);
        toggleCell("Ending screen flash",40,1,()->toolkit.expiringFlash,()->toolkit.expiringFlash=!toolkit.expiringFlash);
        toggleCell("Money alert",64,0,()->toolkit.moneyVisualAlert,()->toolkit.moneyVisualAlert=!toolkit.moneyVisualAlert);
        toggleCell("XP alert",64,1,()->toolkit.xpVisualAlert,()->toolkit.xpVisualAlert=!toolkit.xpVisualAlert);
        toggle("5-second reminder",88,()->toolkit.boosterReminder,()->toolkit.boosterReminder=!toolkit.boosterReminder);
        label("Soft highlight & visual alert when a visible timer is ending.",110);
        label(()->timerStatus(BoosterTimers.Type.MONEY),126,ToolkitTheme.palette().moneyText(),1);
        label(()->timerStatus(BoosterTimers.Type.XP),138,ToolkitTheme.palette().xpText(),1);
        pair(()->"Preview XP alert",VisualAlertOverlay::previewXpAlert,()->"Preview ending",VisualAlertOverlay::previewExpiringAlert,152);

        section("Coconut alerts",180);
        toggleCell("Coconut sound",196,0,()->toolkit.coconutSoundEnabled,()->toolkit.coconutSoundEnabled=!toolkit.coconutSoundEnabled);
        toggleCell("Coconut banner",196,1,()->toolkit.coconutVisualAlert,()->toolkit.coconutVisualAlert=!toolkit.coconutVisualAlert);
        toggle("Coconut screen flash",220,()->toolkit.coconutFlash,()->toolkit.coconutFlash=!toolkit.coconutFlash);
        thresholdField(244);
        label("Alert at or above this %. 0 = every money boost.",272);
        pair(()->"Preview sound",CoconutNotifier::preview,()->"Preview coconut",VisualAlertOverlay::previewCoconutAlert,288);

        section("Earnings & statistics",316);
        pair(()->"History",()->Minecraft.getInstance().setScreenAndShow(new BoosterHistoryScreen(this)),()->"Rate tests",()->Minecraft.getInstance().setScreenAndShow(new BoosterStatsScreen(this,true)),332);
        toggle("Include boosters in crop average",354,cfg::includesBoostersInAverage,cfg::toggleAverageBoosters);
        label(()->"Session bonus: +"+BoosterStatsManager.formatMoney(BoosterStatsManager.getSessionBonusEarned()),380,ToolkitTheme.palette().text(),1);
        label(()->{var e=BoosterStatsManager.history().latestMoneyBooster();return "Latest bonus: "+(e==null?"--":"+"+BoosterStatsManager.formatMoney(e.bonus));},394,ToolkitTheme.palette().muted(),1);
    }
    private void skills(){
        var toolkit=ToolkitConfig.get();var money=ModConfig.getInstance();
        var fishing=ToolkitMod.FISHING;var fishCfg=fishing.settings();

        section("Farming",0);
        toggle("Clear Harvest textures",16,ClearHarvest::isEnabled,()->ClearHarvest.toggle(Minecraft.getInstance()));
        label("Dim immature crops to spot ready plants.",40);
        toggleCell("Crop earnings HUD",56,0,()->toolkit.cropEarningsHud,()->toolkit.cropEarningsHud=!toolkit.cropEarningsHud);
        toggleCell("Harvest per hour HUD",56,1,()->toolkit.cropHarvestHud,()->toolkit.cropHarvestHud=!toolkit.cropHarvestHud);
        label("Appears while harvesting (> 1,000/h).",80);
        label(()->CropHarvestTracker.averageLabel(),94,ToolkitTheme.palette().accent(),1);
        toggle("Include boosters in crop average",110,money::includesBoostersInAverage,money::toggleAverageBoosters);

        section("Fishing",138);
        toggle("Depleted area",154,()->fishCfg.warningEnabled,()->{fishCfg.warningEnabled=!fishCfg.warningEnabled;fishing.warningChanged();});
        slider("Fishing volume",178,fishCfg.volume,3,v->fishCfg.volume=(float)v);
        button("Preview depleted warning",202,fishing::previewWarning);
        button("Void Rod settings (Seasonal)",226,()->go(3));

        section("Fish sell prices",254);
        label("Sell price per fish ($ each; accepts 1.5k / 2m)",270);
        priceField("Cod · 60%",284,()->fishCfg.codPrice,v->fishCfg.codPrice=v);
        priceField("Salmon · 25%",306,()->fishCfg.salmonPrice,v->fishCfg.salmonPrice=v);
        priceField("Puffer · 13%",328,()->fishCfg.pufferPrice,v->fishCfg.pufferPrice=v);
        priceField("Tropical · 2%",350,()->fishCfg.tropicalPrice,v->fishCfg.tropicalPrice=v);
        setting("Earnings window",376,()->fishCfg.earningsWindow==60?"60 seconds":"5 minutes",()->fishCfg.earningsWindow=fishCfg.earningsWindow==60?300:60);
        label(()->"Weighted value / fish: "+BoosterStatsManager.formatMoney(fishCfg.averageFishPrice()),402,ToolkitTheme.palette().accent(),1);
        label(()->{long n=ToolkitMod.FISHING_EARNINGS.count(System.currentTimeMillis(),fishCfg.earningsWindow);return n+" fish in last "+(fishCfg.earningsWindow==60?"60s":"5m")+" · "+BoosterStatsManager.formatMoney(n*fishCfg.averageFishPrice());},416,ToolkitTheme.palette().text(),1);
        label(()->fishCfg.hasPrices()?ToolkitMod.fishingLabel():"Fishing: set fish prices",430,ToolkitTheme.palette().text(),1);

        section("Mining & XP",450);
        toggle("XP per hour HUD",466,()->toolkit.xpEarningsHud,()->toolkit.xpEarningsHud=!toolkit.xpEarningsHud);
        label("Appears while you collect XP.",490);
        label(()->XpTracker.averageLabel(),504,ToolkitTheme.palette().xpText(),1);
        toggle("Include XP boosters",520,()->toolkit.xpIncludesBoosters,()->toolkit.xpIncludesBoosters=!toolkit.xpIncludesBoosters);
    }
    private void seasonals(){
        section("Seasonal Items",0);
        pair(()->"Expand all",()->{for(var it:SeasonalRegistry.ITEMS)expandedSeasonals.add(it.id());clearBody();buildBody();},
             ()->"Collapse all",()->{expandedSeasonals.clear();clearBody();buildBody();},16);
        label("Manage item-specific triggers, alerts and settings.",40);
        int top=54;
        for(var item:SeasonalRegistry.ITEMS){
            boolean expanded=expandedSeasonals.contains(item.id());
            var card=addRenderableWidget(ToolkitButton.seasonalCard(
                item.name(),
                item.subtitle(),
                item.icon(),
                expanded,
                b->{
                    if(expandedSeasonals.contains(item.id()))expandedSeasonals.remove(item.id());
                    else expandedSeasonals.add(item.id());
                    clearBody();buildBody();
                },
                layout.bodyX(),
                layout.bodyY()+top-scroll,
                layout.bodyWidth()
            ));
            controls.add(new Control(card,top,32));
            contentHeight=Math.max(contentHeight,top+32);
            top+=36;
            if(expanded){
                top=item.buildSettings(this,top);
                top+=8;
            }
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
    public int buildBoneLeggingsSettings(int top){
        var cfg=ToolkitConfig.get();
        toggle("Burning Rush Rune",top,()->cfg.boneLeggingsSpeedAlert,()->cfg.boneLeggingsSpeedAlert=!cfg.boneLeggingsSpeedAlert);
        top+=24;
        label("Triggers a speed cue when Burning Rush activates.",top);
        top+=14;
        label("Grants Regeneration or a health-draining Speed boost.",top);
        top+=14;
        return top;
    }
    private void hudOverlays(){
        var cfg=ToolkitConfig.get();
        section("Active HUD elements",0);
        toggleCell("Money timer",16,0,()->cfg.boosterHud,()->cfg.boosterHud=!cfg.boosterHud);
        toggleCell("XP timer",16,1,()->cfg.xpBoosterHud,()->cfg.xpBoosterHud=!cfg.xpBoosterHud);
        toggleCell("Crop earnings",40,0,()->cfg.cropEarningsHud,()->cfg.cropEarningsHud=!cfg.cropEarningsHud);
        toggleCell("Fishing earnings",40,1,()->cfg.fishEarningsHud,()->cfg.fishEarningsHud=!cfg.fishEarningsHud);
        toggleCell("XP per hour",64,0,()->cfg.xpEarningsHud,()->cfg.xpEarningsHud=!cfg.xpEarningsHud);
        toggleCell("Harvest / hour",64,1,()->cfg.cropHarvestHud,()->cfg.cropHarvestHud=!cfg.cropHarvestHud);
        toggle("Rate test info",88,()->cfg.rateTestHud,()->cfg.rateTestHud=!cfg.rateTestHud);

        section("Layout & positioning",116);
        button("Edit HUD positions",132,()->go(5));
        label("Drag elements directly on-screen to reposition them.",156);
        label("Click the eye icons on the canvas to toggle visibility.",168);
        label("Restore defaults",188);
        int resetWidth=Math.min(144,layout.bodyWidth());
        button(()->"Reset HUD positions",182,layout.bodyWidth()-resetWidth,resetWidth,()->{
            var c=ModConfig.getInstance();c.setPosition(ModConfig.Position.TOP_CENTER);c.setOffset(0,0);c.setAveragePosition(12,42);
            cfg.fishHudX=12;cfg.fishHudY=58;cfg.xpHudX=12;cfg.xpHudY=74;cfg.cropHarvestHudX=12;cfg.cropHarvestHudY=90;
        });
    }
    private void utilities(){
        var cfg=ToolkitConfig.get();var options=Minecraft.getInstance().options;
        section("Chat selection & copy",0);
        toggle("Chat selection",16,()->cfg.chatEnabled,()->cfg.chatEnabled=!cfg.chatEnabled);
        toggle("Select word",40,()->cfg.wordSelect,()->cfg.wordSelect=!cfg.wordSelect);
        toggle("Quick /gc code click",64,()->cfg.gcCodeClick,()->cfg.gcCodeClick=!cfg.gcCodeClick);
        label("Click a 16-digit gift code to copy & open /gc",88);
        label("Drag to select · Ctrl+C / Cmd+C to copy",100);
        label("Double-click a word · Triple-click a line",112);
        setting("Highlight color",130,cfg::highlightName,()->cfg.highlight=(cfg.highlight+1)%3);
        slider("Highlight strength",154,(cfg.highlightOpacity-.1)/.7,1,v->cfg.highlightOpacity=.1+v*.7);
        slider("Chat size",178,options.chatScale().get(),1,v->options.chatScale().set(Math.max(.1,v)));
        slider("Chat width",200,options.chatWidth().get(),1,v->options.chatWidth().set(v));
        slider("Chat opacity",224,options.chatOpacity().get(),1,v->options.chatOpacity().set(v));

        section("Auction notifications",252);
        toggle("Sale alert",268,()->cfg.ahSoundEnabled,()->cfg.ahSoundEnabled=!cfg.ahSoundEnabled);
        setting("Sound",292,cfg::ahSoundName,()->cfg.ahSoundType=(cfg.ahSoundType+1)%5);
        slider("Volume",314,cfg.ahSoundVolume,1,v->cfg.ahSoundVolume=v);
        slider("Sound amplification",338,cfg.ahSoundGain,3,v->cfg.ahSoundGain=v);
        button("Preview sale sound",362,AhSaleNotifier::playPreviewSound);

        section("Blackout (AFK)",390);
        button("Turn blackout on",406,()->{persist();BlackoutManager.setActive(true);onClose();});
        slider("Opacity",430,cfg.blackoutOpacity,1,v->cfg.blackoutOpacity=v);
        toggle("Wake hint",454,()->cfg.blackoutHint,()->cfg.blackoutHint=!cfg.blackoutHint);
        setting("Wake key",478,()->ToolkitMod.blackoutKey.getTranslatedKeyMessage().getString(),()->bindingTarget=2);
        label("Dim the game while keeping audio active.",504);

        section("Keyboard shortcuts",524);
        setting("Menu key",540,()->ToolkitMod.menuKey.getTranslatedKeyMessage().getString(),()->bindingTarget=1);
        sellAllBinding(564);
        setting("Wake key",588,()->ToolkitMod.blackoutKey.getTranslatedKeyMessage().getString(),()->bindingTarget=2);
    }
    private void thresholdField(int top){
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
        var button=addRenderableWidget(ToolkitButton.setting(name,value,b->{action.run();persist();},layout.bodyX(),layout.bodyY()+top-scroll,layout.bodyWidth()));
        controls.add(new Control(button,top,20));contentHeight=Math.max(contentHeight,top+20);
    }
    private void sellAllBinding(int top){setting("Sell all key",top,()->ToolkitMod.sellAllKey.getTranslatedKeyMessage().getString(),()->bindingTarget=3);}
    private String timerStatus(BoosterTimers.Type type){
        long now=System.currentTimeMillis();var timers=BoosterManager.timers;var clock=timers.clock(type);
        return type.label+": "+(clock.active(now)?"+"+timers.percentage(type)+"% · "+BoosterTimers.formatTime(clock.remaining(now),clock.hasTimer(now)):"Inactive");
    }
    /** Expand in place, then bring the advanced divider and first controls into view. */
    private boolean advanced(int top){
        boolean open=expandedPages.contains(page);
        button(open?"Hide advanced":"Advanced...",top,()->{
            if(open)expandedPages.remove(page);else expandedPages.add(page);
            clearBody();buildBody();
            scroll=open?0:top+24;updateControls();
        });
        if(open){advancedDivider=top+28;section("Advanced settings",top+36);}
        return open;
    }
    private void priceField(String name,int top,DoubleSupplier read,DoubleConsumer write){
        labels.add(new Label(()->name,top+5,ToolkitTheme.palette().text(),1,6));
        EditBox field=addRenderableWidget(new EditBox(font,layout.bodyX()+layout.bodyWidth()-94,layout.bodyY()+top+5-scroll,88,14,Component.literal(name+" price per fish")));
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
        if(!layout.compact()){
            ToolkitTheme.rounded(g,x,y,112,h,6,ToolkitTheme.palette().sidebar());g.fill(x+104,y,x+112,y+h,ToolkitTheme.palette().sidebar());
            g.blit(LOGO,x+8,y+8,x+38,y+38,0f,1f,0f,1f);
            g.text(font,"COMPLEXIFY",x+42,y+13,ToolkitTheme.palette().accent(),false);
            g.text(font,"v"+UpdateManager.getCurrentVersion(),x+42,y+25,ToolkitTheme.palette().muted(),false);
            g.text(font,query.isBlank()?(page<PAGES.length?PAGES[page]:""):"Search settings",layout.bodyX(),y+18,ToolkitTheme.palette().text(),false);
            g.text(font,query.isBlank()?(page<DESCRIPTIONS.length?DESCRIPTIONS[page]:""):"Across all categories",layout.bodyX(),y+34,ToolkitTheme.palette().muted(),false);
            g.fill(layout.bodyX(),y+43,layout.bodyX()+20,y+44,ToolkitTheme.palette().accent());
            int sx=x+4,sy=y+42,sw=104;
            ToolkitTheme.rounded(g,sx,sy,sw,20,3,ToolkitTheme.palette().surface());
            if(searchBox.isFocused())g.fill(sx+4,sy+19,sx+sw-4,sy+20,ToolkitTheme.palette().accent());
            if(query.isEmpty()&&!searchBox.isFocused())g.text(font,"Search...",searchBox.getX(),searchBox.getY()+3,ToolkitTheme.palette().muted(),false);
        }else{
            g.blit(LOGO,x+8,y+10,x+26,y+28,0f,1f,0f,1f);
            g.text(font,"Complexify",x+30,y+15,ToolkitTheme.palette().text(),false);
            int sx=layout.bodyX(),sy=y+60,sw=layout.bodyWidth();
            ToolkitTheme.rounded(g,sx,sy,sw-(clearSearch!=null&&clearSearch.visible?26:0),20,3,ToolkitTheme.palette().surface());
            if(searchBox.isFocused())g.fill(sx+4,sy+19,sx+sw-(clearSearch!=null&&clearSearch.visible?30:4),sy+20,ToolkitTheme.palette().accent());
            if(query.isEmpty()&&!searchBox.isFocused())g.text(font,"Search settings...",searchBox.getX(),searchBox.getY()+3,ToolkitTheme.palette().muted(),false);
        }
        g.enableScissor(layout.bodyX(),layout.bodyY(),layout.bodyX()+layout.bodyWidth(),layout.bodyY()+layout.viewport());
        if(advancedDivider>=0){int lineY=layout.bodyY()+advancedDivider-scroll;
            g.fill(layout.bodyX(),lineY,layout.bodyX()+layout.bodyWidth(),lineY+1,ToolkitTheme.palette().border());}
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
            g.centeredText(font,Component.literal(bindingTarget==1?"Press your menu key":bindingTarget==2?"Press your black screen key":"Press your sell all key"),x+w/2,y+h/2-12,com.skyblocktoolkit.ToolkitTheme.palette().text());
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
        return List.of(new HudPart(2,crops,BoosterHudOverlay.averageX(mc,crops),BoosterHudOverlay.averageY(mc),cfg.cropEarningsHud),
            new HudPart(3,fish,BoosterHudOverlay.fishX(mc,fish),BoosterHudOverlay.fishY(mc),cfg.fishEarningsHud),
            new HudPart(4,XpTracker.averageLabel(),BoosterHudOverlay.xpX(mc,XpTracker.averageLabel()),BoosterHudOverlay.xpY(mc),cfg.xpEarningsHud),
            new HudPart(5,harvest,BoosterHudOverlay.cropHarvestX(mc,harvest),BoosterHudOverlay.cropHarvestY(mc),cfg.cropHarvestHud));
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
            g.text(font,part.text(),part.x(),part.y(),ToolkitPalette.DARK.text(),true);
            g.outline(part.x()-3,part.y()-3,font.width(part.text())+6,15,part.visible()?ToolkitTheme.palette().accent():ToolkitTheme.palette().muted());
            eye(g,eyeX(part.x(),font.width(part.text())),part.y()-3,part.visible());
        }
        if(dragging==0)g.centeredText(font,Component.literal("Drag to move · Eye to show / hide · Esc to return"),width/2,height-14,ToolkitPalette.DARK.muted());
        super.extractRenderState(g,mx,my,tick);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        if(bindingTarget==0&&page!=5&&mx>=layout.bodyX()&&mx<=layout.bodyX()+layout.bodyWidth()&&my>=layout.bodyY()&&my<layout.footerY()){scroll+=(int)(-vertical*26);updateControls();return true;}
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(bindingTarget>0){
            if(event.key()!=GLFW.GLFW_KEY_ESCAPE){
                int key=event.key()==GLFW.GLFW_KEY_DELETE||event.key()==GLFW.GLFW_KEY_BACKSPACE?GLFW.GLFW_KEY_UNKNOWN:event.key();
                KeyMapping target=bindingTarget==1?ToolkitMod.menuKey:bindingTarget==2?ToolkitMod.blackoutKey:ToolkitMod.sellAllKey;
                target.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));
                KeyMapping.resetMapping();
                Minecraft.getInstance().options.save();
            }
            bindingTarget=0;
            return true;
        }
        if(page!=5&&event.key()==GLFW.GLFW_KEY_F&&(event.modifiers()&(GLFW.GLFW_MOD_CONTROL|GLFW.GLFW_MOD_SUPER))!=0){setFocused(searchBox);return true;}
        if(page!=5&&event.key()==GLFW.GLFW_KEY_ESCAPE&&!query.isEmpty()){searchBox.setValue("");return true;}
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
                int ex=eyeX(part.x(),font.width(part.text()));
                if(event.x()>=ex&&event.x()<ex+10&&event.y()>=part.y()-3&&event.y()<part.y()+7){
                    if(part.id()==2)cfg.cropEarningsHud=!cfg.cropEarningsHud;else if(part.id()==3)cfg.fishEarningsHud=!cfg.fishEarningsHud;else if(part.id()==4)cfg.xpEarningsHud=!cfg.xpEarningsHud;else cfg.cropHarvestHud=!cfg.cropHarvestHud;
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
            for(var part:hudParts())if(event.x()>=part.x()-3&&event.x()<=part.x()+font.width(part.text())+3&&event.y()>=part.y()-3&&event.y()<=part.y()+12){dragging=part.id();grabX=event.x()-part.x();grabY=event.y()-part.y();return true;}
            if(event.x()>=bx-4&&event.x()<=bx+BoosterHudOverlay.BAR_WIDTH+4&&event.y()>=by-12&&event.y()<=by+BoosterHudOverlay.TIMER_SPACING+10){var c=ModConfig.getInstance();c.setOffset(bx-10,by-16);c.setPosition(ModConfig.Position.TOP_LEFT);dragging=1;grabX=event.x()-bx;grabY=event.y()-by;return true;}

        }
        return super.mouseClicked(event,handled);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        var cfg=ModConfig.getInstance();
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
