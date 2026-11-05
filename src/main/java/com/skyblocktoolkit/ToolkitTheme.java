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
        int cy=y+h-108;
        line(g,x+12,cy+27,x+54,cy+5,p.motif());
        line(g,x+54,cy+5,x+118,cy+51,p.motif());
        line(g,x+118,cy+51,x+37,cy+69,p.motif());
        crystal(g,x+38,cy+7,29,57);
        crystal(g,x+82,cy+29,19,34);
        crystal(g,x+14,cy+51,12,22);
        g.fill(x+46,cy+76,x+82,cy+77,p.motif());
        g.fill(x+61,cy+75,x+67,cy+78,p.brand());
        g.fill(x+132,y+12,x+133,y+h-12,p.border());
    }
    /** Facets are filled scanline triangles, matching Minecraft's native pixel grid. */
    private static void triangle(GuiGraphicsExtractor g,int ax,int ay,int bx,int by,int cx,int cy,int color){
        int low=Math.min(ay,Math.min(by,cy)),high=Math.max(ay,Math.max(by,cy));
        for(int y=low;y<high;y++){
            double scan=y+.5,min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
            if((ay<=scan&&scan<by)||(by<=scan&&scan<ay)){double v=ax+(scan-ay)*(bx-ax)/(by-ay);min=Math.min(min,v);max=Math.max(max,v);}
            if((by<=scan&&scan<cy)||(cy<=scan&&scan<by)){double v=bx+(scan-by)*(cx-bx)/(cy-by);min=Math.min(min,v);max=Math.max(max,v);}
            if((cy<=scan&&scan<ay)||(ay<=scan&&scan<cy)){double v=cx+(scan-cy)*(ax-cx)/(ay-cy);min=Math.min(min,v);max=Math.max(max,v);}
            if(min<=max)g.fill((int)Math.floor(min),y,(int)Math.ceil(max),y+1,color);
        }
    }
    public static void crystal(GuiGraphicsExtractor g,int x,int y,int w,int h){
        var p=palette();int tipX=x+w*3/4,midX=x+w/3,midY=y+h/2;
        triangle(g,tipX,y,x,midY,midX,midY+2,p.facetLight());
        triangle(g,tipX,y,midX,midY+2,x+w,midY+6,p.facetDark());
        triangle(g,x,midY,midX,midY+2,x+w/4,y+h,p.facetDark());
        triangle(g,midX,midY+2,x+w,midY+6,x+w/4,y+h,p.motif());
        line(g,tipX,y,x,midY,p.accent());
        line(g,x,midY,x+w/4,y+h,p.facetLight());
        line(g,tipX,y,midX,midY+2,p.facetLight());
        line(g,midX,midY+2,x+w/4,y+h,p.facetLight());
    }
    public static void brandHeader(GuiGraphicsExtractor g,int x,int y,int w,boolean compact){
        var p=palette();
        int left=compact?x+1:x+133,height=compact?35:61;
        g.fill(left,y+1,x+w-5,y+height,p.accentSurface());
        line(g,x+w-170,y+height-1,x+w-124,y+5,p.motif());
        line(g,x+w-124,y+5,x+w-82,y+height-1,p.motif());
        crystal(g,x+w-146,y+5,compact?19:28,compact?26:45);
        crystal(g,x+w-112,y+15,compact?10:16,compact?17:29);
        g.fill(left,y+height,x+w-6,y+height+1,p.motif());
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
