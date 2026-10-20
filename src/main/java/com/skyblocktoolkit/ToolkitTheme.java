package com.skyblocktoolkit;
import net.minecraft.client.gui.GuiGraphicsExtractor;
/** Restrained shared colors and small native-pixel corner radii. */
public final class ToolkitTheme {
    public static ToolkitPalette palette(){return ToolkitPalette.of(ToolkitConfig.get().lightMode);}
    public static final int MONEY_TIMER=0xFF67C982,MONEY_TRACK=0xFF10291B,MONEY_HIGHLIGHT=0xFF8DDEA3,MONEY_SHADOW=0xFF398953;
    public static final int XP_TIMER=0xFF9D9AEB,XP_TRACK=0xFF201D35,XP_HIGHLIGHT=0xFFBBB8FA,XP_SHADOW=0xFF6C68A8;
    private ToolkitTheme(){}
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
