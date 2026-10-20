package com.moneybooster;
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
        int fill=isHoveredOrFocused() && active ? com.skyblocktoolkit.ToolkitTheme.palette().hover() : com.skyblocktoolkit.ToolkitTheme.palette().surface();
        com.skyblocktoolkit.ToolkitTheme.rounded(g,getX(),getY(),getWidth(),getHeight(),3,fill);
        var font=Minecraft.getInstance().font;
        if (!empty) g.text(font,getMessage(),getX()+(getWidth()-font.width(getMessage()))/2,getY()+(getHeight()-8)/2,active?com.skyblocktoolkit.ToolkitTheme.palette().text():com.skyblocktoolkit.ToolkitTheme.palette().muted(),false);
    }
}
