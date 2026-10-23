package com.skyblocktoolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.function.*;
/** Native keyboard/narration behavior, with quiet actions, switches and module tiles. */
public final class ToolkitButton extends Button {
    private final int kind;
    private final boolean selected;
    private final BooleanSupplier state;
    private final String label,description;
    private final ItemStack icon;
    private Supplier<String> value;
    private ToolkitButton(int x,int y,int w,int h,String label,OnPress press,int kind,boolean selected,BooleanSupplier state,ItemStack icon,String description){
        super(x,y,w,h,Component.literal(label),press,DEFAULT_NARRATION);this.kind=kind;this.selected=selected;this.state=state;this.icon=icon;this.description=description;this.label=label;
    }
    public static ToolkitButton action(String label,OnPress press,int x,int y,int w,int h){return new ToolkitButton(x,y,w,h,label,press,0,false,null,null,null);}
    public static ToolkitButton nav(String label,OnPress press,int x,int y,int w,int h,boolean selected){return new ToolkitButton(x,y,w,h,label,press,1,selected,null,null,null);}
    public static ToolkitButton toggle(String label,OnPress press,int x,int y,int w,int h,BooleanSupplier state){return new ToolkitButton(x,y,w,h,label,press,2,false,state,null,null);}
    public static ToolkitButton tile(String label,String description,ItemStack icon,OnPress press,int x,int y,int w,int h){return new ToolkitButton(x,y,w,h,label,press,3,false,null,icon,description);}
    public static ToolkitButton themeSwitch(OnPress press,int x,int y,boolean lightMode){return new ToolkitButton(x,y,60,22,lightMode?"Light mode":"Dark mode",press,4,lightMode,null,null,null);}
    public static ToolkitButton setting(String label,Supplier<String> value,OnPress press,int x,int y,int w){
        var button=new ToolkitButton(x,y,w,20,label+": "+value.get(),press,5,false,null,null,label);
        button.value=value;return button;
    }
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float tick){
        var font=Minecraft.getInstance().font;boolean hover=isHoveredOrFocused()&&active;
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        if(kind==5){
            var palette=ToolkitTheme.palette();String current=value.get();
            setMessage(Component.literal(description+": "+current));
            int valueWidth=Math.min(w/2,Math.max(62,font.width(current)+16));
            g.text(font,description,x+6,y+6,palette.text(),false);
            ToolkitTheme.rounded(g,x+w-valueWidth,y+1,valueWidth,18,3,hover?palette.hover():palette.surface());
            String clipped=font.substrByWidth(Component.literal(current),valueWidth-12).getString();
            g.text(font,clipped,x+w-(valueWidth+font.width(clipped))/2,y+6,palette.accent(),false);
            return;
        }
        if(kind==4){
            var palette=ToolkitTheme.palette();
            ToolkitTheme.rounded(g,x,y,w,h,h/2,hover?palette.accent():palette.border());
            ToolkitTheme.rounded(g,x+1,y+1,w-2,h-2,h/2-1,palette.surface());
            int segment=(w-4)/2;
            ToolkitTheme.rounded(g,x+2+(selected?0:segment),y+2,segment,h-4,(h-4)/2,palette.accentTrack());
            sun(g,x+2+segment/2,y+h/2,selected?palette.onAccent():palette.muted());
            moon(g,x+2+segment+segment/2,y+h/2,selected?palette.muted():palette.onAccent(),selected?palette.surface():palette.accentTrack());
            return;
        }
        if(kind==1){
            if(selected){
                ToolkitTheme.rounded(g,x,y,w,h,3,ToolkitTheme.palette().accentSurface());
            }else if(hover)ToolkitTheme.rounded(g,x,y,w,h,4,ToolkitTheme.palette().surface());
            int pad=w<70?4:8;
            String clipped=font.substrByWidth(getMessage(),w-pad*2).getString();
            g.text(font,clipped,x+pad,y+(h-8)/2,selected?ToolkitTheme.palette().accent():ToolkitTheme.palette().muted(),false);return;
        }
        if(kind!=2||hover){
            ToolkitTheme.rounded(g,x,y,w,h,3,hover?ToolkitTheme.palette().hover():ToolkitTheme.palette().surface());
            if(isFocused())g.fill(x+4,y+h-1,x+w-4,y+h,ToolkitTheme.palette().accent());
        }
        if(kind==2){
            boolean enabled=state.getAsBoolean();setMessage(Component.literal(label+": "+(enabled?"on":"off")));
            g.text(font,font.substrByWidth(Component.literal(label),w-39).getString(),x+6,y+(h-8)/2,ToolkitTheme.palette().text(),false);
            ToolkitTheme.rounded(g,x+w-26,y+(h-10)/2,20,10,5,enabled?ToolkitTheme.palette().accentTrack():com.skyblocktoolkit.ToolkitTheme.palette().track());
            ToolkitTheme.rounded(g,x+w-(enabled?14:24),y+(h-6)/2,6,6,3,ToolkitTheme.palette().onAccent());return;
        }
        if(kind==3){
            boolean hasIcon=!icon.isEmpty();
            if(hasIcon)g.item(icon,x+10,y+12);
            g.text(font,font.substrByWidth(Component.literal(label),w-(hasIcon?52:32)).getString(),x+(hasIcon?34:12),y+16,ToolkitTheme.palette().text(),false);
            g.text(font,">",x+w-16,y+16,ToolkitTheme.palette().muted(),false);
            g.text(font,font.substrByWidth(Component.literal(description),w-24).getString(),x+12,y+43,ToolkitTheme.palette().muted(),false);return;
        }
        String text=font.substrByWidth(getMessage(),w-12).getString();
        g.text(font,text,x+(w-font.width(text))/2,y+(h-8)/2,active?ToolkitTheme.palette().text():ToolkitTheme.palette().muted(),false);
    }
    private static void sun(GuiGraphicsExtractor g,int cx,int cy,int color){
        ToolkitTheme.rounded(g,cx-3,cy-3,6,6,3,color);
        g.fill(cx-1,cy-7,cx+1,cy-5,color);g.fill(cx-1,cy+5,cx+1,cy+7,color);
        g.fill(cx-7,cy-1,cx-5,cy+1,color);g.fill(cx+5,cy-1,cx+7,cy+1,color);
        g.fill(cx-5,cy-5,cx-4,cy-4,color);g.fill(cx+4,cy-5,cx+5,cy-4,color);
        g.fill(cx-5,cy+4,cx-4,cy+5,color);g.fill(cx+4,cy+4,cx+5,cy+5,color);
    }
    private static void moon(GuiGraphicsExtractor g,int cx,int cy,int color,int background){
        ToolkitTheme.rounded(g,cx-5,cy-6,11,12,5,color);
        ToolkitTheme.rounded(g,cx-1,cy-7,9,10,4,background);
    }
}
