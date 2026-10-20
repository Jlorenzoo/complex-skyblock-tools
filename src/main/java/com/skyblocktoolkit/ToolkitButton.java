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
    private ToolkitButton(int x,int y,int w,int h,String label,OnPress press,int kind,boolean selected,BooleanSupplier state,ItemStack icon,String description){
        super(x,y,w,h,Component.literal(label),press,DEFAULT_NARRATION);this.kind=kind;this.selected=selected;this.state=state;this.icon=icon;this.description=description;this.label=label;
    }
    public static ToolkitButton action(String label,OnPress press,int x,int y,int w,int h){return new ToolkitButton(x,y,w,h,label,press,0,false,null,null,null);}
    public static ToolkitButton nav(String label,OnPress press,int x,int y,int w,int h,boolean selected){return new ToolkitButton(x,y,w,h,label,press,1,selected,null,null,null);}
    public static ToolkitButton toggle(String label,OnPress press,int x,int y,int w,int h,BooleanSupplier state){return new ToolkitButton(x,y,w,h,label,press,2,false,state,null,null);}
    public static ToolkitButton tile(String label,String description,ItemStack icon,OnPress press,int x,int y,int w,int h){return new ToolkitButton(x,y,w,h,label,press,3,false,null,icon,description);}
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float tick){
        var font=Minecraft.getInstance().font;boolean hover=isHoveredOrFocused()&&active;
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        if(kind==1){
            if(selected){
                ToolkitTheme.rounded(g,x,y,w,h,4,ToolkitTheme.RED_PRIMARY);
                ToolkitTheme.rounded(g,x+1,y+1,w-2,h-2,3,ToolkitTheme.RED_DARK);
            }else if(hover)ToolkitTheme.rounded(g,x,y,w,h,4,ToolkitTheme.SURFACE);
            int pad=w<70?4:8;
            String clipped=font.substrByWidth(getMessage(),w-pad*2).getString();
            g.text(font,clipped,x+pad,y+(h-8)/2,selected?ToolkitTheme.WHITE_PURE:ToolkitTheme.MUTED,false);return;
        }
        if(kind!=2||hover)ToolkitTheme.rounded(g,x,y,w,h,4,hover?ToolkitTheme.HOVER:ToolkitTheme.SURFACE);
        if(kind==2){
            g.fill(x+8,y+h-1,x+w-8,y+h,0x185E6872);
            boolean enabled=state.getAsBoolean();setMessage(Component.literal(label+": "+(enabled?"on":"off")));
            g.text(font,font.substrByWidth(Component.literal(label),w-39).getString(),x+6,y+(h-8)/2,ToolkitTheme.TEXT,false);
            ToolkitTheme.rounded(g,x+w-29,y+(h-12)/2,24,12,6,enabled?ToolkitTheme.RED_ACTIVE_TRACK:0xFF353940);
            ToolkitTheme.rounded(g,x+w-(enabled?15:25),y+(h-8)/2,8,8,4,ToolkitTheme.WHITE_PURE);return;
        }
        if(kind==3){
            boolean hasIcon=!icon.isEmpty();
            if(hasIcon)g.item(icon,x+10,y+12);
            g.text(font,font.substrByWidth(Component.literal(label),w-(hasIcon?52:32)).getString(),x+(hasIcon?34:12),y+16,ToolkitTheme.TEXT,false);
            g.text(font,">",x+w-16,y+16,ToolkitTheme.MUTED,false);
            g.text(font,font.substrByWidth(Component.literal(description),w-24).getString(),x+12,y+43,ToolkitTheme.MUTED,false);return;
        }
        g.centeredText(font,font.substrByWidth(getMessage(),w-12).getString(),x+w/2,y+(h-8)/2,active?ToolkitTheme.TEXT:ToolkitTheme.FAINT);
    }
}
