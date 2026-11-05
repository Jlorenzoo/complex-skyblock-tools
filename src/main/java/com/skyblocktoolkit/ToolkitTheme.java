package com.skyblocktoolkit;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
/** Restrained shared colors and small native-pixel corner radii. */
public final class ToolkitTheme {
    public static ToolkitPalette palette(){return ToolkitPalette.of(ToolkitConfig.get().lightMode);}
    public static final int MONEY_TIMER=0xFF67C982,MONEY_TRACK=0xFF10291B,MONEY_HIGHLIGHT=0xFF8DDEA3,MONEY_SHADOW=0xFF398953;
    public static final int XP_TIMER=0xFF9D9AEB,XP_TRACK=0xFF201D35,XP_HIGHLIGHT=0xFFBBB8FA,XP_SHADOW=0xFF6C68A8;
    // Gameplay notifications stay neutral and translucent in either menu theme.
    public static final int ALERT_SURFACE=0x6613171C,ALERT_TEXT=0xFFE4E7EC,ALERT_MUTED=0xFFC1C7D0;
    public static final float MENU_TEXT_SCALE=.85f;
    public static int textWidth(Font font,String text){return (int)Math.ceil(font.width(text)*MENU_TEXT_SCALE);}
    public static String clipText(Font font,Component text,int width){return font.substrByWidth(text,(int)(width/MENU_TEXT_SCALE)).getString();}
    public static void text(GuiGraphicsExtractor g,Font font,String text,int x,int y,int color){
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(MENU_TEXT_SCALE,MENU_TEXT_SCALE);
        g.text(font,text,0,0,color,false);g.pose().popMatrix();
    }
    private ToolkitTheme(){}
    /** Static, native-pixel geometry echoes the artwork without loading a background image. */
    public static void line(GuiGraphicsExtractor g,int x,int y,int endX,int endY,int color){
        int dx=Math.abs(endX-x),dy=-Math.abs(endY-y),sx=x<endX?1:-1,sy=y<endY?1:-1,error=dx+dy;
        while(true){
            g.fill(x,y,x+1,y+1,color);
            if(x==endX&&y==endY)break;
            int twice=2*error;
            if(twice>=dy){error+=dy;x+=sx;}
            if(twice<=dx){error+=dx;y+=sy;}
        }
    }
    public static void brandFrame(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean compact){
        var p=palette();
        // Broken crimson rails and clipped facets mirror the shield's angular framing.
        g.fill(x+12,y,x+Math.min(w-12,108),y+2,p.brand());
        line(g,x+w-42,y,x+w-20,y+12,p.motif());
        line(g,x+w-20,y+12,x+w-8,y+4,p.motif());
        g.fill(x+w-48,y+h-1,x+w-12,y+h,p.brand());
        if(compact)return;
        // Carbon weave belongs only to the navigation rail, away from setting text.
        for(int row=78;row<h-34;row+=10)
            line(g,x+2,y+row,x+128,y+Math.min(h-30,row+36),p.carbon());
        int cy=y+h-85;
        line(g,x+14,cy+12,x+48,cy-5,p.motif());
        line(g,x+48,cy-5,x+109,cy+26,p.motif());
        line(g,x+109,cy+26,x+72,cy+50,p.motif());
        line(g,x+72,cy+50,x+48,cy-5,p.motif());
        line(g,x+48,cy-5,x+53,cy+24,p.motif());
        line(g,x+53,cy+24,x+72,cy+50,p.motif());
        line(g,x+14,cy+12,x+53,cy+24,p.motif());
        g.fill(x+132,y+12,x+133,y+h-12,p.border());
    }
    public static void rounded(GuiGraphicsExtractor g,int x,int y,int w,int h,int radius,int color){
        int r=Math.clamp(radius,0,Math.min(w,h)/2);
        if(r<=0){g.fill(x,y,x+w,y+h,color);return;}
        g.fill(x,y+r,x+w,y+h-r,color);
        for(int row=0;row<r;row++){
            double distance=r-row-.5;int inset=(int)Math.ceil(r-Math.sqrt(r*r-distance*distance));
            g.fill(x+inset,y+row,x+w-inset,y+row+1,color);
            g.fill(x+inset,y+h-row-1,x+w-inset,y+h-row,color);
        }
    }
}
