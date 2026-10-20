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
    private int bindingTarget; // 0=none, 1=menu, 2=blackout
    private boolean fishEarnings,advanced;
    private final List<EditBox> priceFields=new ArrayList<>();
    private ToolkitLayout layout;
    private final List<Control> controls=new ArrayList<>();
    private final List<Label> labels=new ArrayList<>();
    private record Control(AbstractWidget widget,int top,int rowHeight){}
    private record Label(Supplier<String> text,int top,int color,float scale,int offset){
        Label(Supplier<String> text,int top,int color,float scale){this(text,top,color,scale,0);}
    }
    private static final String[] PAGES={"Boosters","Boosters","Fishing","Chat Copy","Auction alerts","HUD layout","Sound details","Blackout","HUD elements","Farming"};
    public ToolkitScreen(Screen parent,int page){super(Component.literal("Skyblock Toolkit"));this.parent=parent;this.page=page==0?1:Math.clamp(page,1,9);}
    @Override protected void init(){
        layout=page==8?ToolkitLayout.popup(width,height):ToolkitLayout.of(width,height,advanced||fishEarnings||page==1||page==6||page==9?286:250);controls.clear();labels.clear();priceFields.clear();contentHeight=0;
        if(page==5){initLayout();return;}
        int x=layout.x(),y=layout.y(),w=layout.width();
        addThemeButton(x,y,w);
        if(page==8){hudElements();updateControls();return;}
        int[] targets={1,9,2,3,4,7};String[] names={"Boosters","Farming","Fishing","Chat Copy","Auction","Blackout"};
        String[] compactNames={"Boost","Farm","Fish","Chat","AH","Dim"};
        for(int i=0;i<targets.length;i++){
            final int target=targets[i];int bx=layout.compact()?x+6+i*((w-12)/targets.length):x+4;
            int by=layout.compact()?y+34:y+54+i*23,bw=layout.compact()?(w-12)/targets.length-2:96;
            addRenderableWidget(ToolkitButton.nav(layout.compact()?compactNames[i]:names[i],button->go(target),bx,by,bw,layout.compact()?22:20,page==target));
        }
        addRenderableWidget(ToolkitButton.action("Edit HUD",b->go(5),x+4,layout.footerY(),96,20));
        addRenderableWidget(ToolkitButton.action("Close",b->onClose(),x+w-76,layout.footerY(),64,20));
        if(UpdateManager.isUpdateAvailable()){
            String updateText=UpdateManager.getState()==UpdateManager.State.DOWNLOADING?"Downloading...":
                              UpdateManager.getState()==UpdateManager.State.READY_RESTART?"Restart Game":
                              "Update v"+UpdateManager.getLatestVersion();
            if(!layout.compact()){
                addRenderableWidget(ToolkitButton.action(updateText,b->{
                    UpdateManager.triggerUpdateFromCommand();
                    if(UpdateManager.getState()==UpdateManager.State.READY_RESTART)onClose();
                },x+4,y+54+6*23+2,96,20));
            }else{
                int ux=x+104,uw=Math.max(70,(x+w-80)-ux);
                addRenderableWidget(ToolkitButton.action(updateText,b->{
                    UpdateManager.triggerUpdateFromCommand();
                    if(UpdateManager.getState()==UpdateManager.State.READY_RESTART)onClose();
                },ux,layout.footerY(),uw,20));
            }
        }

        switch(page){case 1->money();case 2->fishing();case 3->chat();case 4->utilities();case 6->advancedSounds();case 7->blackout();case 8->hudElements();case 9->farming();}
        updateControls();
    }
    private void go(int next){persist();page=next==0?1:next;scroll=0;dragging=0;bindingTarget=0;advanced=false;rebuildWidgets();}
    private void addThemeButton(int x,int y,int w){
        int bw=layout.compact()?64:104;
        var cfg=ToolkitConfig.get();
        var button=addRenderableWidget(ToolkitButton.action(cfg.lightMode?"Light mode":"Dark mode",b->{
            cfg.lightMode=!cfg.lightMode;persist();rebuildWidgets();
        },x+w-bw-12,y+10,bw,22));
        button.setTooltip(Tooltip.create(Component.literal("Switch to "+(cfg.lightMode?"dark":"light")+" theme")));
    }
    private void label(Supplier<String> text,int top,int color,float scale){labels.add(new Label(text,top,color,scale));contentHeight=Math.max(contentHeight,top+(int)(10*scale));}
    private void label(String text,int top){label(()->text,top,ToolkitTheme.palette().muted(),1);}
    private void button(String text,int top,Runnable action){button(()->text,top,0,Math.min(Math.max(90,font.width(text)+24),layout.bodyWidth()),action);}
    private void button(Supplier<String> text,int top,int offset,int w,Runnable action){
        Button b=addRenderableWidget(ToolkitButton.action(text.get(),unused->{action.run();persist();updateControls();},layout.bodyX()+offset,layout.bodyY()+top-scroll,w,24));
        controls.add(new Control(b,top,24));contentHeight=Math.max(contentHeight,top+24);
        // Button labels are refreshed through the same render-time supplier list.
        labels.add(new Label(()->{b.setMessage(Component.literal(text.get()));return "";},top,0,1));
    }
    private void pair(Supplier<String> a,Runnable aa,Supplier<String> b,Runnable bb,int top){
        int max=(layout.bodyWidth()-8)/2,aw=Math.min(max,font.width(a.get())+24),bw=Math.min(max,font.width(b.get())+24);
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
        AbstractSliderButton slider=addRenderableWidget(new AbstractSliderButton(layout.bodyX()+offset,layout.bodyY()+top-scroll,fieldWidth,22,Component.literal(name),normalized){
            protected void updateMessage(){setMessage(Component.literal(name+": "+Math.round(value*maximum*100)+"%"));}
            protected void applyValue(){apply.accept(value*maximum);updateMessage();}
            public void onRelease(MouseButtonEvent e){super.onRelease(e);persist();}
            public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
                ToolkitTheme.rounded(g,getX(),getY(),getWidth(),getHeight(),3,ToolkitTheme.palette().surface());
                g.text(font,getMessage(),getX()+8,getY()+5,ToolkitTheme.palette().text(),false);
                int start=getX()+8,end=getRight()-8,knob=start+(int)((end-start)*value);
                g.fill(start,getBottom()-5,end,getBottom()-3,com.skyblocktoolkit.ToolkitTheme.palette().track());g.fill(start,getBottom()-5,knob,getBottom()-3,ToolkitTheme.palette().accentTrack());
                ToolkitTheme.rounded(g,knob-2,getBottom()-6,4,4,2,ToolkitTheme.palette().onAccent());
            }
        });slider.setMessage(Component.literal(name+": "+Math.round(setting*100)+"%"));controls.add(new Control(slider,top,22));contentHeight=Math.max(contentHeight,top+22);
    }
    private void toggle(String text,int top,BooleanSupplier state,Runnable action){toggleAt(text,top,0,layout.bodyWidth(),state,action);}
    private void toggleCell(String text,int top,int column,BooleanSupplier state,Runnable action){
        int half=(layout.bodyWidth()-10)/2;toggleAt(text,top,column*(half+10),half,state,action);
    }
    private void toggleAt(String text,int top,int offset,int width,BooleanSupplier state,Runnable action){
        var b=addRenderableWidget(ToolkitButton.toggle(text,unused->{action.run();persist();updateControls();},layout.bodyX()+offset,layout.bodyY()+top-scroll,width,24,state));
        controls.add(new Control(b,top,24));contentHeight=Math.max(contentHeight,top+24);
    }
    private void actionRow(int top,String[] titles,Runnable[] actions){
        int offset=0,remaining=layout.bodyWidth()-8*(titles.length-1);
        for(int i=0;i<titles.length;i++){
            int requested=font.width(titles[i])+24,w=Math.min(requested,remaining/(titles.length-i));
            final String text=titles[i];button(()->text,top,offset,w,actions[i]);offset+=w+8;remaining-=w;
        }
    }
    private void advancedButton(int top){
        button(advanced?"Less settings":"Advanced...",top,()->{advanced=!advanced;rebuildWidgets();});
        if(advanced)button(()->"HUD elements...",top,112,Math.min(120,layout.bodyWidth()-112),()->{hudReturnPage=page;go(8);});
    }
    private void hudElements(){
        var cfg=ToolkitConfig.get();
        toggleCell("Money timer",0,0,()->cfg.boosterHud,()->cfg.boosterHud=!cfg.boosterHud);
        toggleCell("XP timer",0,1,()->cfg.xpBoosterHud,()->cfg.xpBoosterHud=!cfg.xpBoosterHud);
        toggleCell("Crop earnings",34,0,()->cfg.cropEarningsHud,()->cfg.cropEarningsHud=!cfg.cropEarningsHud);
        toggleCell("Fishing earnings",34,1,()->cfg.fishEarningsHud,()->cfg.fishEarningsHud=!cfg.fishEarningsHud);
        toggleCell("Rate test info",68,0,()->cfg.rateTestHud,()->cfg.rateTestHud=!cfg.rateTestHud);
        pair(()->"Back",()->go(hudReturnPage),()->"Edit positions",()->go(5),108);
    }
    private void money(){
        var cfg=ModConfig.getInstance();
        var toolkit=ToolkitConfig.get();
        toggleCell("Money timer",0,0,()->toolkit.boosterHud,()->toolkit.boosterHud=!toolkit.boosterHud);
        toggleCell("XP timer",0,1,()->toolkit.xpBoosterHud,()->toolkit.xpBoosterHud=!toolkit.xpBoosterHud);
        toggle("5-second reminder",34,()->toolkit.boosterReminder,()->toolkit.boosterReminder=!toolkit.boosterReminder);
        label("Soft highlight + ending cue on visible timers.",64);
        label(()->timerStatus(BoosterTimers.Type.MONEY),86,ToolkitTheme.palette().moneyText(),1);
        label(()->timerStatus(BoosterTimers.Type.XP),104,ToolkitTheme.palette().xpText(),1);
        label(()->"Session money bonus: +"+BoosterStatsManager.formatMoney(BoosterStatsManager.getSessionBonusEarned()),124,ToolkitTheme.palette().text(),1);
        label(()->{var e=BoosterStatsManager.history().latestMoneyBooster();return "Latest money bonus: "+(e==null?"--":"+"+BoosterStatsManager.formatMoney(e.bonus));},142,ToolkitTheme.palette().muted(),1);
        actionRow(168,new String[]{"History",advanced?"Less settings":"Advanced..."},new Runnable[]{()->Minecraft.getInstance().setScreenAndShow(new BoosterHistoryScreen(this)),()->{advanced=!advanced;rebuildWidgets();}});
        if(advanced){
            button("HUD elements...",202,()->{hudReturnPage=page;go(8);});
            pair(()->cfg.includesBoostersInAverage()?"With boosters":"Base earnings",cfg::toggleAverageBoosters,()->"Rate tests",()->Minecraft.getInstance().setScreenAndShow(new BoosterStatsScreen(this)),238);
        }
    }
    private String timerStatus(BoosterTimers.Type type){
        long now=System.currentTimeMillis();var timers=BoosterManager.timers;var clock=timers.clock(type);
        return type.label+": "+(clock.active(now)?"+"+timers.percentage(type)+"% · "+BoosterTimers.formatTime(clock.remaining(now),clock.hasTimer(now)):"Inactive");
    }
    private void farming(){
        var cfg=ToolkitConfig.get();int half=(layout.bodyWidth()-10)/2;
        label("Crop Visibility",0);
        label("Dim & desaturate immature crops for easy spotting.",15);
        toggle("Clear Harvest textures",30,ClearHarvest::isEnabled,()->ClearHarvest.toggle(Minecraft.getInstance()));
        label(()->ClearHarvest.isEnabled()?"Status: Active (Clear Harvest)":"Status: Vanilla textures",58,ClearHarvest.isEnabled()?ToolkitTheme.palette().accent():ToolkitTheme.palette().muted(),1);

        label("Crop Earnings",80);
        label(()->com.moneybooster.BoosterStatsManager.averageLabel(),95,ToolkitTheme.palette().text(),1.2f);
        toggleCell("Crop earnings HUD",118,0,()->cfg.cropEarningsHud,()->cfg.cropEarningsHud=!cfg.cropEarningsHud);
        button(()->"HUD elements...",118,half+10,half,()->{hudReturnPage=9;go(8);});
        pair(()->"Rate tests",()->Minecraft.getInstance().setScreenAndShow(new com.moneybooster.BoosterStatsScreen(this)),()->"Edit HUD",()->go(5),148);
    }
    private void fishing(){
        if(fishEarnings){fishPrices();return;}
        var mod=ToolkitMod.FISHING;var cfg=mod.settings();
        toggleCell("Ready to reel",0,0,()->cfg.depthEnabled,()->cfg.depthEnabled=!cfg.depthEnabled);
        toggleCell("Depleted area",0,1,()->cfg.warningEnabled,()->{cfg.warningEnabled=!cfg.warningEnabled;mod.warningChanged();});
        toggleCell("Snapped line",34,0,()->cfg.snapEnabled,()->cfg.snapEnabled=!cfg.snapEnabled);
        int half=(layout.bodyWidth()-10)/2;
        sliderAt("Volume",34,cfg.volume,3,half+10,half,v->cfg.volume=(float)v);
        actionRow(76,new String[]{"Preview","Fish prices",advanced?"Less settings":"Advanced..."},new Runnable[]{mod::previewDepth,()->{fishEarnings=true;advanced=false;scroll=0;rebuildWidgets();},()->{advanced=!advanced;rebuildWidgets();}});
        if(advanced){
            label(()->"Reel depth: "+(int)cfg.target+" blocks",116,ToolkitTheme.palette().muted(),1);
            button(()->"-",136,0,32,()->cfg.target=Math.max(1,cfg.target-1));
            button(()->"+",136,40,32,()->cfg.target=Math.min(1000,cfg.target+1));
            button(()->"Alert sounds",112,half+10,half,()->{soundReturnPage=2;go(6);});
            button(()->"HUD elements...",144,half+10,half,()->{hudReturnPage=page;go(8);});
        }
    }
    private void priceField(String name,int top,DoubleSupplier read,DoubleConsumer write){
        labels.add(new Label(()->name,top+6,ToolkitTheme.palette().text(),1,22));
        EditBox field=addRenderableWidget(new EditBox(font,layout.bodyX()+112,layout.bodyY()+top+6-scroll,Math.max(40,layout.bodyWidth()-112),14,Component.literal(name+" price per fish")));
        field.setBordered(false);field.setMaxLength(24);field.setValue(String.format(java.util.Locale.ROOT,"%.2f",read.getAsDouble()));
        field.setTextColor(ToolkitTheme.palette().text());
        field.setResponder(text->{double value=com.fishingpings.FishingEarnings.price(text);field.setTextColor(Double.isFinite(value)?com.skyblocktoolkit.ToolkitTheme.palette().text():com.skyblocktoolkit.ToolkitTheme.palette().error());if(Double.isFinite(value))write.accept(value);});
        priceFields.add(field);controls.add(new Control(field,top+6,14));contentHeight=Math.max(contentHeight,top+22);
    }
    private void fishPrices(){
        var cfg=ToolkitMod.FISHING.settings();
        button("Back to fishing",0,()->{fishEarnings=false;scroll=0;rebuildWidgets();});
        label("Sell price per fish ($ each; accepts 1.5k / 2m)",32);
        priceField("Cod · 60%",52,()->cfg.codPrice,v->cfg.codPrice=v);
        priceField("Salmon · 25%",80,()->cfg.salmonPrice,v->cfg.salmonPrice=v);
        priceField("Puffer · 13%",108,()->cfg.pufferPrice,v->cfg.pufferPrice=v);
        priceField("Tropical · 2%",136,()->cfg.tropicalPrice,v->cfg.tropicalPrice=v);
        button(()->"Window: "+(cfg.earningsWindow==60?"60s":"5m"),168,0,Math.min(160,layout.bodyWidth()),()->cfg.earningsWindow=cfg.earningsWindow==60?300:60);

        label(()->"Weighted value / fish: "+BoosterStatsManager.formatMoney(cfg.averageFishPrice()),230,ToolkitTheme.palette().accent(),1);
        label(()->{long n=ToolkitMod.FISHING_EARNINGS.count(System.currentTimeMillis(),cfg.earningsWindow);return n+" fish in last "+(cfg.earningsWindow==60?"60s":"5m")+" · "+BoosterStatsManager.formatMoney(n*cfg.averageFishPrice());},248,ToolkitTheme.palette().text(),1);
        label(()->ToolkitMod.fishingLabel(),266,ToolkitTheme.palette().text(),1);
        label(()->priceFields.stream().anyMatch(f->!Double.isFinite(com.fishingpings.FishingEarnings.price(f.getValue())))?"Invalid price: enter a positive dollar amount.":"* Warming up. Based on fish mix; no booster added.",286,ToolkitTheme.palette().muted(),1);
        label("For stack prices, divide by 64 before entering.",304);

    }
    private void chat(){
        var cfg=ToolkitConfig.get();var options=Minecraft.getInstance().options;
        label("Drag to select in chat. Ctrl+C to copy.",0);
        toggleCell("Chat selection",24,0,()->cfg.chatEnabled,()->cfg.chatEnabled=!cfg.chatEnabled);
        toggleCell("Select word",24,1,()->cfg.wordSelect,()->cfg.wordSelect=!cfg.wordSelect);
        advancedButton(64);
        if(advanced){
            int half=(layout.bodyWidth()-10)/2;
            button(()->"Color: "+cfg.highlightName(),104,0,half,()->cfg.highlight=(cfg.highlight+1)%3);
            sliderAt("Highlight",104,(cfg.highlightOpacity-.1)/.7,1,half+10,half,v->cfg.highlightOpacity=.1+v*.7);
            sliderAt("Chat size",140,options.chatScale().get(),1,0,half,v->options.chatScale().set(Math.max(.1,v)));
            sliderAt("Chat width",140,options.chatWidth().get(),1,half+10,half,v->options.chatWidth().set(v));
            sliderAt("Chat opacity",176,options.chatOpacity().get(),1,0,half,v->options.chatOpacity().set(v));
        }
    }

    private void utilities(){
        var cfg=ToolkitConfig.get();int half=(layout.bodyWidth()-10)/2;
        toggleCell("Sale alert",0,0,()->cfg.ahSoundEnabled,()->cfg.ahSoundEnabled=!cfg.ahSoundEnabled);
        button(()->"Sound: "+cfg.ahSoundName(),0,half+10,half,()->cfg.ahSoundType=(cfg.ahSoundType+1)%5);
        sliderAt("Volume",36,cfg.ahSoundVolume,1,0,half,v->cfg.ahSoundVolume=v);
        button(()->"Preview",36,half+10,half,AhSaleNotifier::playPreviewSound);
        advancedButton(76);
        if(advanced)button("Sound amplification",112,()->{soundReturnPage=4;go(6);});
    }
    private void blackout(){
        var cfg=ToolkitConfig.get();int half=(layout.bodyWidth()-10)/2;
        button(()->"Turn blackout on",0,0,half,()->{persist();BlackoutManager.setActive(true);onClose();});
        button(()->"Wake: "+ToolkitMod.blackoutKey.getTranslatedKeyMessage().getString(),0,half+10,half,()->bindingTarget=2);
        sliderAt("Opacity",36,cfg.blackoutOpacity,1,0,half,v->cfg.blackoutOpacity=v);
        advancedButton(76);
        if(advanced){
            toggleCell("Wake hint",112,0,()->cfg.blackoutHint,()->cfg.blackoutHint=!cfg.blackoutHint);
            button(()->"Menu key: "+ToolkitMod.menuKey.getTranslatedKeyMessage().getString(),112,half+10,half,()->bindingTarget=1);
            button("Reset HUD positions",148,()->{var c=ModConfig.getInstance();c.setPosition(ModConfig.Position.TOP_CENTER);c.setOffset(0,0);c.setAveragePosition(12,42);cfg.fishHudX=12;cfg.fishHudY=58;});
        }
    }
    private void advancedSounds(){
        var cfg=ToolkitConfig.get();var fishing=ToolkitMod.FISHING;
        button("Back to "+PAGES[soundReturnPage],0,()->go(soundReturnPage));
        if(soundReturnPage==2){
            slider("Ready to reel gain",38,cfg.readySoundGain,3,v->cfg.readySoundGain=v);
            button("Preview ready",66,fishing::previewDepth);
            slider("Depleted area gain",104,cfg.areaSoundGain,3,v->cfg.areaSoundGain=v);
            button("Preview area",132,fishing::previewWarning);
            slider("Snapped line gain",170,cfg.snapSoundGain,3,v->cfg.snapSoundGain=v);
            button("Preview snapped line",198,fishing::previewSnap);
        }else{
            button(()->"Sound: "+cfg.ahSoundName(),38,0,Math.min(180,layout.bodyWidth()),()->cfg.ahSoundType=(cfg.ahSoundType+1)%5);
            slider("Auction gain",76,cfg.ahSoundGain,3,v->cfg.ahSoundGain=v);
            button("Preview auction sale",108,AhSaleNotifier::playPreviewSound);
        }
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
        if(page==8){
            g.blit(LOGO,layout.bodyX(),y+12,layout.bodyX()+18,y+30,0f,1f,0f,1f);
            g.text(font,"HUD elements",layout.bodyX()+22,y+16,ToolkitTheme.palette().text(),false);
        }else if(!layout.compact()){
            ToolkitTheme.rounded(g,x,y,104,h,6,ToolkitTheme.palette().sidebar());g.fill(x+96,y,x+104,y+h,ToolkitTheme.palette().sidebar());
            g.blit(LOGO,x+8,y+10,x+44,y+46,0f,1f,0f,1f);
            g.text(font,"SKYBLOCK",x+47,y+17,ToolkitTheme.palette().accent(),false);
            g.text(font,"v"+UpdateManager.getCurrentVersion(),x+47,y+29,ToolkitTheme.palette().muted(),false);
            g.text(font,PAGES[page],layout.bodyX(),y+18,ToolkitTheme.palette().text(),false);
            String[] descriptions={"","Bonus & earnings","Catch alerts","Select & copy","Sale notifications","","Per-alert gain","Dim the game, keep audio","Choose what stays on screen","Crop visibility & earnings"};
            g.text(font,descriptions[page],layout.bodyX(),y+34,ToolkitTheme.palette().muted(),false);
        }else{
            g.blit(LOGO,x+8,y+10,x+26,y+28,0f,1f,0f,1f);
            g.text(font,"Skyblock Toolkit",x+30,y+15,ToolkitTheme.palette().text(),false);
        }
        if(page!=8&&!layout.compact())g.fill(layout.bodyX(),y+47,layout.bodyX()+28,y+49,ToolkitTheme.palette().accent());
        g.enableScissor(layout.bodyX(),layout.bodyY(),layout.bodyX()+layout.bodyWidth(),layout.bodyY()+layout.viewport());
        for(var label:labels){String text=label.text().get();if(text.isEmpty())continue;
            g.pose().pushMatrix();g.pose().translate(layout.bodyX()+label.offset(),layout.bodyY()+label.top()-scroll);g.pose().scale(label.scale(),label.scale());
            String clipped=font.substrByWidth(Component.literal(text),(int)((layout.bodyWidth()-label.offset())/label.scale())).getString();
            g.text(font,clipped,0,0,label.color(),false);g.pose().popMatrix();
        }
        if(page==2&&fishEarnings){
            net.minecraft.world.item.Item[] fish={net.minecraft.world.item.Items.COD,net.minecraft.world.item.Items.SALMON,net.minecraft.world.item.Items.PUFFERFISH,net.minecraft.world.item.Items.TROPICAL_FISH};
            for(var field:priceFields)if(field.visible)ToolkitTheme.rounded(g,field.getX()-6,field.getY()-6,field.getWidth()+6,24,3,field.isFocused()?ToolkitTheme.palette().hover():ToolkitTheme.palette().surface());
            if(Minecraft.getInstance().level!=null)for(int i=0;i<fish.length;i++)g.item(new net.minecraft.world.item.ItemStack(fish[i]),layout.bodyX(),layout.bodyY()+55+i*28-scroll);
        }
        g.disableScissor();
        if(layout.maxScroll(contentHeight)>0){int track=layout.viewport(),thumb=Math.max(12,track*track/contentHeight),ty=layout.bodyY()+scroll*(track-thumb)/layout.maxScroll(contentHeight);g.fill(x+w-5,ty,x+w-3,ty+thumb,com.skyblocktoolkit.ToolkitTheme.palette().muted());}
        super.extractRenderState(g,mx,my,tick);

        if(bindingTarget>0){
            ToolkitTheme.rounded(g,x,y,w,h,6,ToolkitTheme.palette().panel());
            g.centeredText(font,Component.literal(bindingTarget==1?"Press your menu key":"Press your black screen key"),x+w/2,y+h/2-12,com.skyblocktoolkit.ToolkitTheme.palette().text());
            g.centeredText(font,Component.literal("Escape cancels · Delete clears"),x+w/2,y+h/2+8,ToolkitTheme.palette().muted());
        }
    }
    private record HudPart(int id,String text,int x,int y,boolean visible){}
    private List<HudPart> hudParts(){
        var mc=Minecraft.getInstance();var cfg=ToolkitConfig.get();
        String crops=BoosterStatsManager.averageLabel(),fish=ToolkitMod.fishingLabel();
        return List.of(new HudPart(2,crops,BoosterHudOverlay.averageX(mc,crops),BoosterHudOverlay.averageY(mc),cfg.cropEarningsHud),
            new HudPart(3,fish,BoosterHudOverlay.fishX(mc,fish),BoosterHudOverlay.fishY(mc),cfg.fishEarningsHud));
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
                KeyMapping target=(bindingTarget==1)?ToolkitMod.menuKey:ToolkitMod.blackoutKey;
                target.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));
                KeyMapping.resetMapping();
                Minecraft.getInstance().options.save();
            }
            bindingTarget=0;
            return true;
        }
        if(page==8&&event.key()==GLFW.GLFW_KEY_ESCAPE){go(hudReturnPage);return true;}
        if(page==5&&event.key()==GLFW.GLFW_KEY_ESCAPE){go(1);return true;}
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
                    if(part.id()==2)cfg.cropEarningsHud=!cfg.cropEarningsHud;else cfg.fishEarningsHud=!cfg.fishEarningsHud;
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
        if(dragging==3){var c=ToolkitConfig.get();c.fishHudX=Math.clamp((int)(event.x()-grabX),2,Math.max(2,width-font.width(ToolkitMod.fishingLabel())-2));c.fishHudY=Math.clamp((int)(event.y()-grabY),2,Math.max(2,height-11));return true;}
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
