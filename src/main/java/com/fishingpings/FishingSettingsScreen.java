package com.fishingpings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FishingSettingsScreen extends Screen {
    private final FishingPingsMod mod;
    private final Screen parent;
    private int x,y,w,h,footer,scroll;
    private Button depth,warning,snap,minus,plus,testDepth,testWarning,testSnap;
    private AbstractSliderButton volume;
    public FishingSettingsScreen(FishingPingsMod mod,Screen parent){super(Component.literal("Fishing Pings"));this.mod=mod;this.parent=parent;}
    @Override protected void init(){
        w=Math.min(340,width-12);h=Math.min(244,height-12);x=(width-w)/2;y=(height-h)/2;footer=y+h-28;
        int half=(w-28)/2;
        depth=addRenderableWidget(FlatButton.flat(Component.literal("Depth ping"),b->{mod.settings().depthEnabled=!mod.settings().depthEnabled;mod.settings().save();refresh();}).bounds(x+12,y+38,half,20).build());
        warning=addRenderableWidget(FlatButton.flat(Component.literal("Area warning"),b->{mod.settings().warningEnabled=!mod.settings().warningEnabled;mod.warningChanged();mod.settings().save();refresh();}).bounds(x+16+half,y+38,half,20).build());
        snap=addRenderableWidget(FlatButton.flat(Component.literal("Line snapped: on"),b->{mod.settings().snapEnabled=!mod.settings().snapEnabled;mod.settings().save();refresh();}).bounds(x+12,y+64,w-24,20).build());
        volume=addRenderableWidget(new AbstractSliderButton(x+12,y+90,w-24,20,Component.literal("Volume"),mod.settings().volume){
            @Override protected void updateMessage(){setMessage(Component.literal("Volume: "+Math.round(value*100)+"%"));}
            @Override protected void applyValue(){mod.settings().volume=(float)value;updateMessage();}
            @Override public void onRelease(net.minecraft.client.input.MouseButtonEvent e){super.onRelease(e);mod.settings().save();}
            @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
                g.fill(getX(),getY(),getRight(),getBottom(),com.skyblocktoolkit.ToolkitTheme.palette().surface());
                int knob=getX()+(int)((getWidth()-4)*value);g.fill(knob,getY()+getHeight()-3,knob+4,getBottom(),com.skyblocktoolkit.ToolkitTheme.palette().onAccent());
                g.centeredText(font,getMessage(),getX()+getWidth()/2,getY()+6,com.skyblocktoolkit.ToolkitTheme.palette().text());
            }
        });
        volume.setMessage(Component.literal("Volume: "+Math.round(mod.settings().volume*100)+"%"));
        minus=addRenderableWidget(FlatButton.flat(Component.literal("−"),b->{mod.settings().target=Math.max(1,mod.settings().target-1);mod.settings().save();}).bounds(x+12,y+116,28,20).build());
        plus=addRenderableWidget(FlatButton.flat(Component.literal("+"),b->{mod.settings().target=Math.min(1000,mod.settings().target+1);mod.settings().save();}).bounds(x+w-40,y+116,28,20).build());
        testDepth=addRenderableWidget(FlatButton.flat(Component.literal("Preview depth ping"),b->mod.previewDepth()).bounds(x+12,y+142,half,20).build());
        testWarning=addRenderableWidget(FlatButton.flat(Component.literal("Preview area warning"),b->mod.previewWarning()).bounds(x+16+half,y+142,half,20).build());
        testSnap=addRenderableWidget(FlatButton.flat(Component.literal("Preview snapped line"),b->mod.previewSnap()).bounds(x+12,y+168,w-24,20).build());
        addRenderableWidget(FlatButton.flat(Component.literal("Done"),b->onClose()).bounds(x+(w-90)/2,footer,90,20).build());refresh();
    }
    private boolean fits(int yy){return yy>=y+36&&yy+20<=footer-8;}
    private void refresh(){
        scroll=Math.clamp(scroll,0,Math.max(0,y+190-(footer-8)));
        depth.setMessage(Component.literal("Depth ping: "+(mod.settings().depthEnabled?"on":"off")));
        warning.setMessage(Component.literal("Area warning: "+(mod.settings().warningEnabled?"on":"off")));
        depth.setY(y+38-scroll);warning.setY(y+38-scroll);depth.visible=warning.visible=fits(depth.getY());
        snap.setY(y+64-scroll);snap.visible=fits(snap.getY());snap.setMessage(Component.literal("Line snapped: "+(mod.settings().snapEnabled?"on":"off")));
        volume.setY(y+90-scroll);volume.visible=fits(volume.getY());
        minus.setY(y+116-scroll);plus.setY(y+116-scroll);minus.visible=plus.visible=fits(minus.getY());
        testDepth.setY(y+142-scroll);testWarning.setY(y+142-scroll);testDepth.visible=testWarning.visible=fits(testDepth.getY());
        testSnap.setY(y+168-scroll);testSnap.visible=fits(testSnap.getY());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        refresh();g.fill(0,0,width,height,com.skyblocktoolkit.ToolkitTheme.palette().scrim());g.fill(x,y,x+w,y+h,com.skyblocktoolkit.ToolkitTheme.palette().panel());
        g.text(font,"Fishing Pings",x+12,y+10,com.skyblocktoolkit.ToolkitTheme.palette().text(),false);
        g.text(font,"Quiet cues for depth, area and snapped lines",x+12,y+23,com.skyblocktoolkit.ToolkitTheme.palette().muted(),false);
        g.enableScissor(x+8,y+36,x+w-8,footer-8);
        g.centeredText(font,Component.literal("Target depth: "+String.format(java.util.Locale.ROOT,"%.0f",mod.settings().target)+" blocks"),x+w/2,y+122-scroll,com.skyblocktoolkit.ToolkitTheme.palette().text());
        g.disableScissor();super.extractRenderState(g,mx,my,tick);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){if(mx>=x&&mx<=x+w&&my>=y+36&&my<footer){scroll+=(int)(-vertical*8);refresh();return true;}return super.mouseScrolled(mx,my,horizontal,vertical);}
    @Override public void onClose(){mod.settings().save();Minecraft.getInstance().setScreenAndShow(parent);}
    @Override public boolean isPauseScreen(){return parent!=null&&parent.isPauseScreen();}
}
