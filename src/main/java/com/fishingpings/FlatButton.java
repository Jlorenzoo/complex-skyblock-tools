package com.fishingpings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
/** Native button input/narration with a simple fill instead of stretched sprite borders. */
public final class FlatButton extends Button {
    private FlatButton(int x,int y,int w,int h,Component label,OnPress press) { super(x,y,w,h,label,press,DEFAULT_NARRATION); }
    public static Builder flat(Component label,OnPress press) { return new Builder(label,press); }
    public static final class Builder {
        private final Component label; private final OnPress press; private int x,y,w,h;
        Builder(Component label,OnPress press) { this.label=label;this.press=press; }
        public Builder bounds(int x,int y,int w,int h) { this.x=x;this.y=y;this.w=w;this.h=h;return this; }
        public FlatButton build() { return new FlatButton(x,y,w,h,label,press); }
    }
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float tick) {
        boolean empty=getMessage().getString().isEmpty();
        int fill=isHoveredOrFocused() && active ? 0xFF2C3036 : empty ? 0xFF1D2025 : 0xFF23262B;
        com.skyblocktoolkit.ToolkitTheme.rounded(g,getX(),getY(),getWidth(),getHeight(),3,fill);
        if (!empty) g.centeredText(Minecraft.getInstance().font,getMessage(),getX()+getWidth()/2,getY()+(getHeight()-8)/2,active?0xFFE7EAEE:0xFF7E8792);
    }
}
