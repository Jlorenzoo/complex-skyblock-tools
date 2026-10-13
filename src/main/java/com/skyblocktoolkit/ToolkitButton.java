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
            if(selected||hover)ToolkitTheme.rounded(g,x,y,w,h,3,selected?0xFF292E32:ToolkitTheme.SURFACE);
            if(selected)g.fill(x,y+5,x+2,y+h-5,ToolkitTheme.ACCENT);
            String clipped=font.substrByWidth(getMessage(),w-16).getString();
            g.text(font,clipped,x+9,y+(h-8)/2,selected?ToolkitTheme.TEXT:ToolkitTheme.MUTED,false);return;
        }
        ToolkitTheme.rounded(g,x,y,w,h,4,hover?ToolkitTheme.HOVER:ToolkitTheme.SURFACE);
        if(kind==2){
            boolean enabled=state.getAsBoolean();setMessage(Component.literal(label+": "+(enabled?"on":"off")));
            g.text(font,font.substrByWidth(Component.literal(label),w-48).getString(),x+10,y+(h-8)/2,ToolkitTheme.TEXT,false);
            ToolkitTheme.rounded(g,x+w-34,y+(h-12)/2,24,12,6,enabled?0xFF688C78:0xFF434950);
            ToolkitTheme.rounded(g,x+w-(enabled?20:30),y+(h-8)/2,8,8,4,0xFFE4E9E5);return;
        }
        if(kind==3){
            g.item(icon,x+(w-16)/2,y+13);
            g.centeredText(font,Component.literal(label),x+w/2,y+37,ToolkitTheme.TEXT);
            g.centeredText(font,font.substrByWidth(Component.literal(description),w-12).getString(),x+w/2,y+54,ToolkitTheme.MUTED);
            g.centeredText(font,Component.literal("Settings  >"),x+w/2,y+h-20,hover?ToolkitTheme.ACCENT:ToolkitTheme.MUTED);return;
        }
        g.centeredText(font,font.substrByWidth(getMessage(),w-12).getString(),x+w/2,y+(h-8)/2,active?ToolkitTheme.TEXT:ToolkitTheme.FAINT);
    }
}
