package com.skyblocktoolkit;

import com.moneybooster.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import java.util.function.*;
import org.lwjgl.glfw.GLFW;

/** One navigation surface for all modules and their live HUD placement. */
public final class ToolkitScreen extends Screen {
    private final Screen parent;
    private int page,scroll,contentHeight,dragging;
    private double grabX,grabY;
    private boolean binding,fishEarnings;
    private final List<EditBox> priceFields=new ArrayList<>();
    private ToolkitLayout layout;
    private final List<Control> controls=new ArrayList<>();
    private final List<Label> labels=new ArrayList<>();
    private record Control(AbstractWidget widget,int top,int rowHeight){}
    private record Label(Supplier<String> text,int top,int color,float scale,int offset){
        Label(Supplier<String> text,int top,int color,float scale){this(text,top,color,scale,0);}
    }
    private static final String[] PAGES={"Overview","Boosters","Fishing","Chat Copy","HUD layout"};
    public ToolkitScreen(Screen parent,int page){super(Component.literal("Skyblock Toolkit"));this.parent=parent;this.page=Math.clamp(page,0,4);}
    @Override protected void init(){
        layout=ToolkitLayout.of(width,height);controls.clear();labels.clear();priceFields.clear();contentHeight=0;
        if(page==4){initLayout();return;}
        int x=layout.x(),y=layout.y(),w=layout.width();
        for(int i=0;i<PAGES.length;i++){
            final int target=i;int bx=layout.compact()?x+12+i*((w-24)/5):x+12;
            int by=layout.compact()?y+38:y+62+i*29,bw=layout.compact()?(w-24)/5-2:104;
            String title=layout.compact()?new String[]{"Mods","Boost","Fish","Chat","HUD"}[i]:PAGES[i];
            addRenderableWidget(ToolkitButton.nav(title,button->go(target),bx,by,bw,24,page==i));
        }
        addRenderableWidget(ToolkitButton.action("Close",b->onClose(),x+w-76,layout.footerY(),64,20));
        addRenderableWidget(ToolkitButton.action("Menu key",b->{binding=true;},layout.bodyX(),layout.footerY(),76,20));
        switch(page){case 0->overview();case 1->money();case 2->fishing();case 3->chat();}
        updateControls();
    }
    private void go(int next){persist();page=next;scroll=0;dragging=0;binding=false;rebuildWidgets();}
    private void label(Supplier<String> text,int top,int color,float scale){labels.add(new Label(text,top,color,scale));contentHeight=Math.max(contentHeight,top+(int)(10*scale));}
    private void label(String text,int top){label(()->text,top,ToolkitTheme.MUTED,1);}
    private void button(String text,int top,Runnable action){button(()->text,top,0,layout.bodyWidth(),action);}
    private void button(Supplier<String> text,int top,int offset,int w,Runnable action){
        Button b=addRenderableWidget(ToolkitButton.action(text.get(),unused->{action.run();persist();updateControls();},layout.bodyX()+offset,layout.bodyY()+top-scroll,w,24));
        controls.add(new Control(b,top,24));contentHeight=Math.max(contentHeight,top+24);
        // Button labels are refreshed through the same render-time supplier list.
        labels.add(new Label(()->{b.setMessage(Component.literal(text.get()));return "";},top,0,1));
    }
    private void pair(Supplier<String> a,Runnable aa,Supplier<String> b,Runnable bb,int top){int half=(layout.bodyWidth()-6)/2;button(a,top,0,half,aa);button(b,top,half+6,half,bb);}
    private void slider(String name,int top,double value,DoubleConsumer apply){
        AbstractSliderButton slider=addRenderableWidget(new AbstractSliderButton(layout.bodyX(),layout.bodyY()+top-scroll,layout.bodyWidth(),22,Component.literal(name),value){
            protected void updateMessage(){setMessage(Component.literal(name+": "+Math.round(value*100)+"%"));}
            protected void applyValue(){apply.accept(value);updateMessage();}
            public void onRelease(MouseButtonEvent e){super.onRelease(e);persist();}
            public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
                ToolkitTheme.rounded(g,getX(),getY(),getWidth(),getHeight(),3,ToolkitTheme.SURFACE);
                g.text(font,getMessage(),getX()+8,getY()+5,ToolkitTheme.TEXT,false);
                int start=getX()+8,end=getRight()-8,knob=start+(int)((end-start)*value);
                g.fill(start,getBottom()-5,end,getBottom()-3,0xFF454B53);g.fill(start,getBottom()-5,knob,getBottom()-3,0xFF8CA798);
                ToolkitTheme.rounded(g,knob-2,getBottom()-6,4,4,2,ToolkitTheme.TEXT);
            }
        });slider.setMessage(Component.literal(name+": "+Math.round(value*100)+"%"));controls.add(new Control(slider,top,22));contentHeight=Math.max(contentHeight,top+22);
    }
    private String on(boolean value){return value?"on":"off";}
    private void toggle(String text,int top,BooleanSupplier state,Runnable action){
        var b=addRenderableWidget(ToolkitButton.toggle(text,unused->{action.run();persist();updateControls();},layout.bodyX(),layout.bodyY()+top-scroll,layout.bodyWidth(),26,state));
        controls.add(new Control(b,top,26));contentHeight=Math.max(contentHeight,top+26);
    }
    private void tile(String text,String description,net.minecraft.world.item.Item item,int top,int offset,int w,Runnable action){
        var b=addRenderableWidget(ToolkitButton.tile(text,description,new net.minecraft.world.item.ItemStack(item),unused->{action.run();},layout.bodyX()+offset,layout.bodyY()+top-scroll,w,100));
        controls.add(new Control(b,top,100));contentHeight=Math.max(contentHeight,top+100);
    }
    private void overview(){
        label("Latest booster bonus",0);
        label(()->{var e=BoosterStatsManager.history().latestMoneyBooster();return e==null?"--":"+"+BoosterStatsManager.formatMoney(e.bonus);},18,ToolkitTheme.ACCENT,1.7f);
        label("Estimated extra earnings from your last booster",42);
        if(layout.viewport()>=112){
            int card=(layout.bodyWidth()-16)/3;
            tile("Boosters","Bonus & rate",net.minecraft.world.item.Items.COCOA_BEANS,68,0,card,()->go(1));
            tile("Fishing","Cues & value",net.minecraft.world.item.Items.FISHING_ROD,68,card+8,card,()->go(2));
            tile("Chat Copy","Select & copy",net.minecraft.world.item.Items.PAPER,68,(card+8)*2,card,()->go(3));
            button("Edit HUD layout",184,()->go(4));label("Place the timer and earnings exactly where you want.",218);
        }else{
            button("Boosters   >",66,()->go(1));button("Fishing   >",98,()->go(2));button("Chat Copy   >",130,()->go(3));button("Edit HUD layout",170,()->go(4));
        }
    }
    private void money(){
        var cfg=ModConfig.getInstance();var toolkit=ToolkitConfig.get();
        label("Latest booster bonus",0);
        label(()->{var e=BoosterStatsManager.history().latestMoneyBooster();return e==null?"--":"+"+BoosterStatsManager.formatMoney(e.bonus);},17,ToolkitTheme.ACCENT,1.7f);
        label(()->{var e=BoosterStatsManager.history().latestMoneyBooster();return e==null?"No booster yet":e.source+" / +"+e.percentage+"%";},40,ToolkitTheme.MUTED,1);
        label(()->"Total bonus: +"+BoosterStatsManager.formatMoney(BoosterStatsManager.getSessionBonusEarned()),60,ToolkitTheme.TEXT,1);
        toggle("Booster timer",83,()->toolkit.boosterHud,()->toolkit.boosterHud=!toolkit.boosterHud);
        toggle("Earnings HUD",115,cfg::isAverageEnabled,cfg::toggleAverage);
        button(()->cfg.includesBoostersInAverage()?"Average includes boosters":"Average excludes boosters",147,0,layout.bodyWidth(),cfg::toggleAverageBoosters);
        button("Item history & details",181,()->Minecraft.getInstance().setScreenAndShow(new BoosterHistoryScreen(this)));
        button("Rate tests & totals",213,()->Minecraft.getInstance().setScreenAndShow(new BoosterStatsScreen(this)));
        button("Arrange timer & average",245,()->go(4));
        button(()->"HUD mode: "+new String[]{"Automatic","Farming","Fishing"}[toolkit.earningsMode],277,0,layout.bodyWidth(),()->toolkit.earningsMode=(toolkit.earningsMode+1)%3);
        label("Farming: last 10s. Fishing: configured window.",310);
    }
    private void fishing(){
        if(fishEarnings){fishPrices();return;}
        var mod=ToolkitMod.FISHING;var cfg=mod.settings();
        pair(()->"Sound cues",()->{},()->"Fish earnings",()->{fishEarnings=true;scroll=0;rebuildWidgets();},0);
        label("Sound cues",32);
        toggle("Ready to reel",54,()->cfg.depthEnabled,()->cfg.depthEnabled=!cfg.depthEnabled);
        toggle("Depleted area",86,()->cfg.warningEnabled,()->{cfg.warningEnabled=!cfg.warningEnabled;mod.warningChanged();});
        toggle("Snapped / missing meter",118,()->cfg.snapEnabled,()->cfg.snapEnabled=!cfg.snapEnabled);
        slider("Cue volume",152,cfg.volume,v->cfg.volume=(float)v);
        pair(()->"Depth -",()->cfg.target=Math.max(1,cfg.target-1),()->"Depth +",()->cfg.target=Math.min(1000,cfg.target+1),184);
        label(()->"Ready depth: "+(int)cfg.target+" blocks",214,ToolkitTheme.TEXT,1);
        pair(()->"Preview ready",mod::previewDepth,()->"Preview area",mod::previewWarning,238);
        button("Preview snapped line",270,mod::previewSnap);
        label("Missing depth meter: early cue after 1.5s.",305);
    }
    private void priceField(String name,int top,DoubleSupplier read,DoubleConsumer write){
        labels.add(new Label(()->name,top+6,ToolkitTheme.TEXT,1,22));
        EditBox field=addRenderableWidget(new EditBox(font,layout.bodyX()+112,layout.bodyY()+top+6-scroll,Math.max(40,layout.bodyWidth()-112),14,Component.literal(name+" price per fish")));
        field.setBordered(false);field.setMaxLength(24);field.setValue(String.format(java.util.Locale.ROOT,"%.2f",read.getAsDouble()));
        field.setResponder(text->{double value=com.fishingpings.FishingEarnings.price(text);field.setTextColor(Double.isFinite(value)?0xFFE6ECF2:0xFFFF9F99);if(Double.isFinite(value))write.accept(value);});
        priceFields.add(field);controls.add(new Control(field,top+6,14));contentHeight=Math.max(contentHeight,top+22);
    }
    private void fishPrices(){
        var cfg=ToolkitMod.FISHING.settings();var hud=ModConfig.getInstance();var toolkit=ToolkitConfig.get();
        pair(()->"Sound cues",()->{fishEarnings=false;scroll=0;rebuildWidgets();},()->"Fish earnings",()->{},0);
        label("Sell price per fish ($ each; accepts 1.5k / 2m)",32);
        priceField("Cod · 60%",52,()->cfg.codPrice,v->cfg.codPrice=v);
        priceField("Salmon · 25%",80,()->cfg.salmonPrice,v->cfg.salmonPrice=v);
        priceField("Puffer · 13%",108,()->cfg.pufferPrice,v->cfg.pufferPrice=v);
        priceField("Tropical · 2%",136,()->cfg.tropicalPrice,v->cfg.tropicalPrice=v);
        pair(()->"Window: "+(cfg.earningsWindow==60?"60s":"5m"),()->cfg.earningsWindow=cfg.earningsWindow==60?300:60,
            ()->"Earnings HUD: "+on(hud.isAverageEnabled()),hud::toggleAverage,168);
        button(()->"HUD mode: "+new String[]{"Automatic","Farming","Fishing"}[toolkit.earningsMode],196,0,layout.bodyWidth(),()->toolkit.earningsMode=(toolkit.earningsMode+1)%3);
        label(()->"Weighted value / fish: "+BoosterStatsManager.formatMoney(cfg.averageFishPrice()),230,ToolkitTheme.ACCENT,1);
        label(()->{long n=ToolkitMod.FISHING_EARNINGS.count(System.currentTimeMillis(),cfg.earningsWindow);return n+" fish in last "+(cfg.earningsWindow==60?"60s":"5m")+" · "+BoosterStatsManager.formatMoney(n*cfg.averageFishPrice());},248,ToolkitTheme.TEXT,1);
        label(()->ToolkitMod.fishingLabel(),266,ToolkitTheme.TEXT,1);
        label(()->priceFields.stream().anyMatch(f->!Double.isFinite(com.fishingpings.FishingEarnings.price(f.getValue())))?"Invalid price: enter a positive dollar amount.":"* Warming up. Based on fish mix; no booster added.",286,ToolkitTheme.MUTED,1);
        label("For stack prices, divide by 64 before entering.",304);
        button("Arrange earnings HUD",324,()->go(4));
    }
    private void chat(){
        var cfg=ToolkitConfig.get();var options=Minecraft.getInstance().options;
        label("Select in chat · Ctrl+C to copy",0);
        toggle("Chat selection",22,()->cfg.chatEnabled,()->cfg.chatEnabled=!cfg.chatEnabled);
        toggle("Double-click word",54,()->cfg.wordSelect,()->cfg.wordSelect=!cfg.wordSelect);
        button(()->"Selection color: "+cfg.highlightName(),88,0,layout.bodyWidth(),()->cfg.highlight=(cfg.highlight+1)%3);
        slider("Selection opacity",122,(cfg.highlightOpacity-.1)/.7,v->cfg.highlightOpacity=.1+v*.7);
        slider("Chat size",154,options.chatScale().get(),v->options.chatScale().set(Math.max(.1,v)));
        slider("Chat width",186,options.chatWidth().get(),v->options.chatWidth().set(v));
        slider("Chat opacity",218,options.chatOpacity().get(),v->options.chatOpacity().set(v));
        label("Chat appearance also updates Minecraft settings.",253);
    }
    private void initLayout(){
        int half=Math.min(112,(width-32)/3);
        addRenderableWidget(FlatButton.flat(Component.literal("Back to tools"),b->go(0)).bounds(8,8,half,22).build());
        addRenderableWidget(FlatButton.flat(Component.literal("Timer: "+on(ToolkitConfig.get().boosterHud)),b->{ToolkitConfig.get().boosterHud=!ToolkitConfig.get().boosterHud;persist();rebuildWidgets();}).bounds(12+half,8,half,22).build());
        addRenderableWidget(FlatButton.flat(Component.literal("Average: "+on(ModConfig.getInstance().isAverageEnabled())),b->{ModConfig.getInstance().toggleAverage();rebuildWidgets();}).bounds(16+half*2,8,half,22).build());
        addRenderableWidget(FlatButton.flat(Component.literal("Reset positions"),b->{var c=ModConfig.getInstance();c.setPosition(ModConfig.Position.TOP_CENTER);c.setOffset(0,0);c.setAveragePosition(12,42);persist();}).bounds(width-120,height-30,112,22).build());
    }
    private void updateControls(){
        scroll=Math.clamp(scroll,0,layout.maxScroll(contentHeight));
        for(var row:controls){row.widget().setY(layout.bodyY()+row.top()-scroll);row.widget().visible=layout.fits(row.top(),row.rowHeight(),scroll);
            if(!row.widget().visible&&getFocused()==row.widget())setFocused(null);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        if(page==4){renderLayout(g,mx,my,tick);return;}
        updateControls();int x=layout.x(),y=layout.y(),w=layout.width(),h=layout.height();
        g.fill(0,0,width,height,0x78090B0F);
        ToolkitTheme.rounded(g,x-2,y+3,w+4,h+2,7,0x30000000);
        ToolkitTheme.rounded(g,x,y,w,h,6,ToolkitTheme.PANEL);
        if(!layout.compact()){
            ToolkitTheme.rounded(g,x,y,120,h,6,ToolkitTheme.SIDEBAR);g.fill(x+112,y,x+120,y+h,ToolkitTheme.SIDEBAR);
            g.text(font,"SKYBLOCK",x+12,y+17,ToolkitTheme.TEXT,false);g.text(font,"Toolkit",x+12,y+30,ToolkitTheme.MUTED,false);
            g.text(font,page==0?"Your mods":PAGES[page],layout.bodyX(),y+18,ToolkitTheme.TEXT,false);
            g.text(font,new String[]{"All your tools in one place","Extra earnings and your overlays","Sound cues and estimated fish value","Selection and chat appearance",""}[page],layout.bodyX(),y+34,ToolkitTheme.MUTED,false);
        }else g.text(font,"Skyblock Toolkit",x+12,y+15,ToolkitTheme.TEXT,false);
        g.enableScissor(layout.bodyX(),layout.bodyY(),layout.bodyX()+layout.bodyWidth(),layout.bodyY()+layout.viewport());
        for(var label:labels){String text=label.text().get();if(text.isEmpty())continue;
            g.pose().pushMatrix();g.pose().translate(layout.bodyX()+label.offset(),layout.bodyY()+label.top()-scroll);g.pose().scale(label.scale(),label.scale());
            String clipped=font.substrByWidth(Component.literal(text),(int)((layout.bodyWidth()-label.offset())/label.scale())).getString();
            g.text(font,clipped,0,0,label.color(),false);g.pose().popMatrix();
        }
        if(page==2&&fishEarnings){
            net.minecraft.world.item.Item[] fish={net.minecraft.world.item.Items.COD,net.minecraft.world.item.Items.SALMON,net.minecraft.world.item.Items.PUFFERFISH,net.minecraft.world.item.Items.TROPICAL_FISH};
            for(var field:priceFields)if(field.visible)ToolkitTheme.rounded(g,field.getX()-6,field.getY()-6,field.getWidth()+6,24,3,field.isFocused()?ToolkitTheme.HOVER:ToolkitTheme.SURFACE);
            for(int i=0;i<fish.length;i++)g.item(new net.minecraft.world.item.ItemStack(fish[i]),layout.bodyX(),layout.bodyY()+55+i*28-scroll);
        }
        g.disableScissor();
        if(layout.maxScroll(contentHeight)>0){int track=layout.viewport(),thumb=Math.max(12,track*track/contentHeight),ty=layout.bodyY()+scroll*(track-thumb)/layout.maxScroll(contentHeight);g.fill(x+w-5,ty,x+w-3,ty+thumb,0xFF555E67);}
        super.extractRenderState(g,mx,my,tick);
        if(!layout.compact())g.text(font,"HUD layout",x+12,layout.footerY()+6,ToolkitTheme.FAINT,false);
        String key=ToolkitMod.menuKey.getTranslatedKeyMessage().getString();
        g.text(font,font.substrByWidth(Component.literal(key),Math.max(0,layout.bodyWidth()-158)).getString(),layout.bodyX()+84,layout.footerY()+6,ToolkitTheme.MUTED,false);
        if(binding){ToolkitTheme.rounded(g,x,y,w,h,6,ToolkitTheme.PANEL);g.centeredText(font,Component.literal("Press your menu key"),x+w/2,y+h/2-12,0xFFE6EBF0);g.centeredText(font,Component.literal("Escape cancels · Delete clears"),x+w/2,y+h/2+8,ToolkitTheme.MUTED);}
    }
    private void renderLayout(GuiGraphicsExtractor g,int mx,int my,float tick){
        var mc=Minecraft.getInstance();g.fill(0,0,width,height,0x7006090E);
        int bx=BoosterHudOverlay.getBarX(width),by=BoosterHudOverlay.getBarY(mc);
        BoosterHudOverlay.renderBar(g,mc,bx,by,"+39% Booster: 0m 42s",.7f,false,false);
        g.outline(bx-4,by-12,BoosterHudOverlay.BAR_WIDTH+8,24,ToolkitConfig.get().boosterHud?0xFFA6BBAD:0xFF626C76);
        String avg=BoosterStatsManager.averageLabel();int ax=BoosterHudOverlay.averageX(mc,avg),ay=BoosterHudOverlay.averageY(mc);
        g.text(font,avg,ax,ay,0xFFFFFFFF,true);g.outline(ax-3,ay-3,font.width(avg)+6,15,ModConfig.getInstance().isAverageEnabled()?0xFFA6BBAD:0xFF626C76);
        g.text(font,"Drag the timer or earnings text. Positions save immediately.",8,height-48,0xFFD6DEE7,true);
        g.text(font,"Grey outline = hidden in-game",8,height-30,ToolkitTheme.MUTED,true);
        super.extractRenderState(g,mx,my,tick);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        if(!binding&&page!=4&&mx>=layout.bodyX()&&mx<=layout.bodyX()+layout.bodyWidth()&&my>=layout.bodyY()&&my<layout.footerY()){scroll+=(int)(-vertical*26);updateControls();return true;}
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(binding){if(event.key()!=GLFW.GLFW_KEY_ESCAPE){int key=event.key()==GLFW.GLFW_KEY_DELETE||event.key()==GLFW.GLFW_KEY_BACKSPACE?GLFW.GLFW_KEY_UNKNOWN:event.key();ToolkitMod.menuKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));KeyMapping.resetMapping();Minecraft.getInstance().options.save();}binding=false;return true;}
        return super.keyPressed(event);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean handled){
        if(binding)return true;
        if(page==4&&event.button()==0){
            for(var child:children())if(child instanceof AbstractWidget w&&w.visible&&w.isMouseOver(event.x(),event.y()))return super.mouseClicked(event,handled);
            var mc=Minecraft.getInstance();String label=BoosterStatsManager.averageLabel();int ax=BoosterHudOverlay.averageX(mc,label),ay=BoosterHudOverlay.averageY(mc);
            if(event.x()>=ax-3&&event.x()<=ax+font.width(label)+3&&event.y()>=ay-3&&event.y()<=ay+12){dragging=2;grabX=event.x()-ax;grabY=event.y()-ay;return true;}
            int bx=BoosterHudOverlay.getBarX(width),by=BoosterHudOverlay.getBarY(mc);
            if(event.x()>=bx-4&&event.x()<=bx+BoosterHudOverlay.BAR_WIDTH+4&&event.y()>=by-12&&event.y()<=by+12){var cfg=ModConfig.getInstance();cfg.setOffset(bx-10,by-16);cfg.setPosition(ModConfig.Position.TOP_LEFT);dragging=1;grabX=event.x()-bx;grabY=event.y()-by;return true;}
        }
        return super.mouseClicked(event,handled);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        var cfg=ModConfig.getInstance();
        if(dragging==2){cfg.setAveragePosition(Math.clamp((int)(event.x()-grabX),2,Math.max(2,width-font.width(BoosterStatsManager.averageLabel())-2)),Math.clamp((int)(event.y()-grabY),2,Math.max(2,height-11)));return true;}
        if(dragging==1){cfg.setOffset(Math.clamp((int)(event.x()-grabX),2,Math.max(2,width-BoosterHudOverlay.BAR_WIDTH-2))-10,Math.clamp((int)(event.y()-grabY),14,Math.max(14,height-24))-16);return true;}
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event){if(dragging!=0){dragging=0;persist();return true;}persist();return super.mouseReleased(event);}
    private void persist(){ToolkitConfig.get().save();ModConfig.getInstance().save();ToolkitMod.FISHING.settings().save();Minecraft.getInstance().options.save();}
    @Override public void removed(){persist();}
    @Override public void onClose(){persist();Minecraft.getInstance().setScreenAndShow(parent);}
    @Override public boolean isPauseScreen(){return parent!=null&&parent.isPauseScreen();}
}
