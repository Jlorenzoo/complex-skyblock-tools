package com.skyblocktoolkit;
/** Shared geometry, also exercised at small GUI sizes without Minecraft. */
public record ToolkitLayout(int x,int y,int width,int height,int bodyX,int bodyY,int bodyWidth,int footerY,boolean compact){
    /** The hub reserves separate rows for its heading, search and compact navigation. */
    public static ToolkitLayout hub(int sw,int sh){
        int w=Math.min(600,Math.max(100,sw-24)),h=Math.min(400,Math.max(100,sh-16));
        int x=(sw-w)/2,y=(sh-h)/2;boolean compact=w<480||h<300;
        int bx=compact?x+16:x+152;
        return new ToolkitLayout(x,y,w,h,bx,y+(compact?96:68),x+w-20-bx,y+h-32,compact);
    }
    /** Expand the established setting coordinates into a relaxed 30px row rhythm. */
    public static int settingY(int top){return Math.round(top*1.25f);}
    public static ToolkitLayout of(int sw,int sh){return of(sw,sh,250);}
    public static ToolkitLayout of(int sw,int sh,int preferredHeight){
        int w=Math.min(460,Math.max(100,sw-16)),h=Math.min(preferredHeight,Math.max(100,sh-16));
        int x=(sw-w)/2,y=(sh-h)/2;boolean compact=w<380;
        int bx=compact?x+12:x+116,by=y+(compact?72:56);
        return new ToolkitLayout(x,y,w,h,bx,by,x+w-12-bx,y+h-28,compact);
    }
    public static ToolkitLayout popup(int sw,int sh){
        int w=Math.min(328,sw-16),h=Math.min(210,sh-16),x=(sw-w)/2,y=(sh-h)/2;
        return new ToolkitLayout(x,y,w,h,x+16,y+40,w-32,y+h-16,true);
    }
    public int viewport(){return Math.max(22,footerY-bodyY-10);}
    public int maxScroll(int contentHeight){return Math.max(0,contentHeight-viewport());}
    public boolean fits(int rowTop,int rowHeight,int scroll){return rowTop-scroll>=0&&rowTop-scroll+rowHeight<=viewport();}
}
