package com.skyblocktoolkit;
import net.minecraft.client.gui.GuiGraphicsExtractor;
/** Restrained shared colors and small native-pixel corner radii. */
public final class ToolkitTheme {
    public static final int PANEL=0xC91B1D21,SIDEBAR=0xD417191D,SURFACE=0xB823262B,HOVER=0xDA2C3036;
    public static final int TEXT=0xFFE7EAEE,MUTED=0xFF9DA4AE,ACCENT=0xFFB0CABA,FAINT=0xFF6F7782;
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
