package com.skyblocktoolkit;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Owns input while blacked out, so Escape cannot reach a pause screen underneath. */
public final class BlackoutScreen extends Screen {
    final Screen previous;
    public BlackoutScreen(Screen previous){super(Component.literal("Blackout"));this.previous=previous;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        BlackoutManager.render(g,width,height);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(ToolkitMod.blackoutKey.matches(event))BlackoutManager.setActive(false);
        return true;
    }
    @Override public boolean canInterruptWithAnotherScreen(){return false;}
    @Override public boolean shouldCloseOnEsc(){return false;}
    @Override public void onClose(){} // Only the configured wake key can dismiss the overlay.
    @Override public boolean isPauseScreen(){return false;}
}
