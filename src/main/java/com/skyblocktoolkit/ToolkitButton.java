package com.skyblocktoolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.function.*;
/** Native keyboard/narration behavior, with quiet actions, switches and module tiles. */
public final class ToolkitButton extends Button {
    private final int kind;
    private final boolean selected;
    private final BooleanSupplier state;
    private final String label,description;
    private final ItemStack icon;
    private Supplier<String> value;
    private boolean dropdown;
    private ToolkitButton(int x,int y,int w,int h,String label,OnPress press,int kind,boolean selected,BooleanSupplier state,ItemStack icon,String description){
        super(x,y,w,h,Component.literal(label),press,DEFAULT_NARRATION);this.kind=kind;this.selected=selected;this.state=state;this.icon=icon;this.description=description;this.label=label;
    }
    public static ToolkitButton action(String label,OnPress press,int x,int y,int w,int h){return new ToolkitButton(x,y,w,h,label,press,0,false,null,null,null);}
    public static ToolkitButton nav(String label,OnPress press,int x,int y,int w,int h,boolean selected){return new ToolkitButton(x,y,w,h,label,press,1,selected,null,null,null);}
    public static ToolkitButton toggle(String label,OnPress press,int x,int y,int w,int h,BooleanSupplier state){return new ToolkitButton(x,y,w,h,label,press,2,false,state,null,null);}
    public static ToolkitButton toggle(String label,String description,OnPress press,int x,int y,int w,BooleanSupplier state){
        return new ToolkitButton(x,y,w,28,label,press,2,false,state,null,description);
    }
    public static ToolkitButton tile(String label,String description,ItemStack icon,OnPress press,int x,int y,int w,int h){return new ToolkitButton(x,y,w,h,label,press,3,false,null,icon,description);}
    public static ToolkitButton themeSwitch(OnPress press,int x,int y,boolean lightMode){return new ToolkitButton(x,y,60,22,lightMode?"Light mode":"Dark mode",press,4,lightMode,null,null,null);}
    public static ToolkitButton seasonalCard(String name,String subtitle,ItemStack icon,boolean expanded,OnPress press,int x,int y,int w){
        return new ToolkitButton(x,y,w,32,name,press,6,expanded,null,icon,subtitle);
    }
    public static ToolkitButton topic(String title,String summary,OnPress press,int x,int y,int w){
        ItemStack icon=new ItemStack(switch(title){
            case "Booster timers" -> Items.CLOCK;
            case "Coconut alerts" -> Items.COCOA_BEANS;
            case "Earnings & statistics", "Item prices" -> Items.GOLD_INGOT;
            case "Farming" -> Items.WHEAT;
            case "Fishing" -> Items.FISHING_ROD;
            case "Mining & XP" -> Items.EXPERIENCE_BOTTLE;
            case "Active HUD elements" -> Items.SPYGLASS;
            case "Layout & positioning" -> Items.COMPASS;
            case "Treasure chests" -> Items.CHEST;
            case "Keyboard shortcuts" -> Items.REPEATER;
            case "Chat selection & copy" -> Items.WRITABLE_BOOK;
            case "Auction notifications" -> Items.BELL;
            case "Display & visuals" -> Items.SUNFLOWER;
            default -> Items.ENDER_EYE;
        });
        return new ToolkitButton(x,y,w,40,title,press,7,false,null,icon,summary);
    }
    public static ToolkitButton searchResult(String setting,String path,String accessibleTitle,OnPress press,int x,int y,int w){
        var button=new ToolkitButton(x,y,w,32,setting,press,10,false,null,null,path);
        button.setMessage(Component.literal(accessibleTitle));return button;
    }
    public static ToolkitButton breadcrumb(String category,String topic,OnPress press,int x,int y,int w){
        var button=new ToolkitButton(x,y,w,20,"< All "+category,press,9,false,null,null,topic);
        button.setMessage(Component.literal("< All "+category+" / "+topic));return button;
    }
    public static ToolkitButton disclosure(String title,boolean open,OnPress press,int x,int y,int w){
        var button=new ToolkitButton(x,y,w,20,title,press,8,open,null,null,null);
        button.setMessage(Component.literal(title+": "+(open?"expanded":"collapsed")));return button;
    }
    public static ToolkitButton setting(String label,Supplier<String> value,OnPress press,int x,int y,int w){
        var button=new ToolkitButton(x,y,w,20,label+": "+value.get(),press,5,false,null,null,label);
        button.value=value;return button;
    }
    public static ToolkitButton dropdown(String label,Supplier<String> value,OnPress press,int x,int y,int w){
        var button=setting(label,value,press,x,y,w);
        button.dropdown=true;
        return button;
    }
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float tick){
        var font=Minecraft.getInstance().font;boolean hover=isHoveredOrFocused()&&active;
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        if(kind==8||kind==9){
            var palette=ToolkitTheme.palette();
            ToolkitTheme.rounded(g,x,y,w,h,3,hover?palette.hover():palette.surface());
            if(isFocused())g.fill(x+4,y+h-1,x+w-4,y+h,palette.accent());
            if(kind==9){
                String back=ToolkitTheme.clipText(font,Component.literal(label),w/2);
                ToolkitTheme.text(g,font,back,x+6,y+6,palette.muted());
                int offset=ToolkitTheme.textWidth(font,back)+18;
                ToolkitTheme.text(g,font,ToolkitTheme.clipText(font,Component.literal(description),w-offset-8),x+offset,y+6,palette.text());
            }else{
                ToolkitTheme.text(g,font,ToolkitTheme.clipText(font,Component.literal(label),w-30),x+6,y+6,selected?palette.accent():palette.text());
                ToolkitTheme.text(g,font,selected?"-":"+",x+w-14,y+6,palette.muted());
            }
            return;
        }
        if(kind==5){
            var palette=ToolkitTheme.palette();String current=value.get();
            setMessage(Component.literal(description+": "+current));
            int arrowSpace=dropdown?14:0;
            int valueWidth=Math.min(w/2,Math.max(62,ToolkitTheme.textWidth(font,current)+16+arrowSpace));
            ToolkitTheme.text(g,font,description,x+6,y+6,palette.text());
            ToolkitTheme.rounded(g,x+w-valueWidth,y+1,valueWidth,18,3,hover?palette.hover():palette.surface());
            String clipped=ToolkitTheme.clipText(font,Component.literal(current),valueWidth-12-arrowSpace);
            ToolkitTheme.text(g,font,clipped,x+w-(valueWidth+arrowSpace+ToolkitTheme.textWidth(font,clipped))/2,y+6,palette.accent());
            if(dropdown){
                g.fill(x+w-12,y+8,x+w-5,y+9,palette.accent());
                g.fill(x+w-11,y+9,x+w-6,y+10,palette.accent());
                g.fill(x+w-10,y+10,x+w-7,y+11,palette.accent());
            }
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
                g.fill(x,y+4,x+2,y+h-4,ToolkitTheme.palette().brand());
            }else if(hover)ToolkitTheme.rounded(g,x,y,w,h,4,ToolkitTheme.palette().surface());
            int pad=w<70?4:8;
            String clipped=ToolkitTheme.clipText(font,getMessage(),w-pad*2);
            ToolkitTheme.text(g,font,clipped,x+pad,y+(h-8)/2,selected?ToolkitTheme.palette().accent():ToolkitTheme.palette().muted());return;
        }
        if(kind!=2||hover){
            ToolkitTheme.rounded(g,x,y,w,h,3,hover?ToolkitTheme.palette().hover():ToolkitTheme.palette().surface());
            if(isFocused())g.fill(x+4,y+h-1,x+w-4,y+h,ToolkitTheme.palette().accent());
        }
        if(kind==2){
            boolean enabled=state.getAsBoolean();setMessage(Component.literal(label+": "+(enabled?"on":"off")));
            int cy=description==null?y+h/2:y+9;
            ToolkitTheme.text(g,font,ToolkitTheme.clipText(font,Component.literal(label),w-39),x+6,description==null?y+(h-8)/2:y+5,ToolkitTheme.palette().text());
            if(description!=null)ToolkitTheme.hint(g,font,description,x+6,y+15,w-12);
            ToolkitTheme.rounded(g,x+w-26,cy-5,20,10,5,enabled?ToolkitTheme.palette().accentTrack():ToolkitTheme.palette().track());
            ToolkitTheme.rounded(g,x+w-(enabled?14:24),cy-3,6,6,3,ToolkitTheme.palette().onAccent());return;
        }
        if(kind==3){
            boolean hasIcon=!icon.isEmpty();
            if(hasIcon)g.item(icon,x+10,y+12);
            ToolkitTheme.text(g,font,ToolkitTheme.clipText(font,Component.literal(label),w-(hasIcon?52:32)),x+(hasIcon?34:12),y+16,ToolkitTheme.palette().text());
            ToolkitTheme.text(g,font,">",x+w-16,y+16,ToolkitTheme.palette().muted());
            ToolkitTheme.text(g,font,ToolkitTheme.clipText(font,Component.literal(description),w-24),x+12,y+43,ToolkitTheme.palette().muted());return;
        }
        if(kind==6||kind==7||kind==10){
            var palette=ToolkitTheme.palette();
            ToolkitTheme.rounded(g,x,y,w,h,4,hover?palette.hover():palette.surface());
            if(isFocused())g.fill(x+4,y+h-1,x+w-4,y+h,palette.accent());
            boolean hasIcon=icon!=null&&!icon.isEmpty();
            if(kind==7){
                ToolkitTheme.rounded(g,x+6,y+6,28,28,4,palette.accentSurface());
                ToolkitTheme.line(g,x+w-32,y+h-1,x+w-12,y+1,palette.motif());
                g.fill(x+8,y+h-1,x+(hover?w-8:40),y+h,hover?palette.brand():palette.motif());
            }
            if(hasIcon)g.item(icon,x+(kind==7?12:8),y+(h-16)/2);
            int textX=x+(kind==7?44:hasIcon?30:8);
            int maxTextW=w-(kind==7?70:hasIcon?48:26);
            ToolkitTheme.text(g,font,ToolkitTheme.clipText(font,Component.literal(label),maxTextW),textX,y+(kind==7?9:6),selected?palette.accent():palette.text());
            ToolkitTheme.text(g,font,ToolkitTheme.clipText(font,Component.literal(description),maxTextW),textX,y+(kind==7?24:18),palette.muted());
            String chevron=kind==7||kind==10?">":selected?"▾":"▸";
            ToolkitTheme.text(g,font,chevron,x+w-14,y+(h-8)/2,palette.muted());return;
        }
        String text=ToolkitTheme.clipText(font,getMessage(),w-12);
        ToolkitTheme.text(g,font,text,x+(w-ToolkitTheme.textWidth(font,text))/2,y+(h-8)/2,active?ToolkitTheme.palette().text():ToolkitTheme.palette().muted());
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
