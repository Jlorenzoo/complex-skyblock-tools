package com.moneybooster;
public record MenuLayout(int x,int y,int width,int height,int footerY) {
    public static MenuLayout of(int screenWidth,int screenHeight,int preferredHeight){
        int width=Math.min(360,Math.max(80,screenWidth-12));int height=Math.min(preferredHeight,Math.max(80,screenHeight-12));
        int x=(screenWidth-width)/2,y=(screenHeight-height)/2;
        return new MenuLayout(x,y,width,height,y+height-28);
    }
}
