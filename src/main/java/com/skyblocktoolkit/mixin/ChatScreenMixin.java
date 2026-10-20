package com.skyblocktoolkit.mixin;

import com.skyblocktoolkit.ToolkitConfig;
import com.skyblocktoolkit.chat.ChatSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into ChatScreen to provide native text selection, drag gestures,
 * multi-click word/line expansion, and cross-platform clipboard copying.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Shadow
    private ChatComponent.DisplayMode displayMode;

    @Unique
    private final ChatSelection toolkit$selection = new ChatSelection();

    protected ChatScreenMixin() {
        super(null);
    }

    @Unique
    private Style toolkit$getClickableStyleAt(double screenX, double screenY) {
        Minecraft mc = Minecraft.getInstance();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(this.getFont(), (int) screenX, (int) screenY).includeInsertions(false);
        mc.gui.hud.getChat().captureClickableText(finder, screenHeight, mc.gui.hud.getGuiTicks(), this.displayMode);
        Style style = finder.result();
        return (style != null && style.getClickEvent() != null) ? style : null;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void toolkit$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!ToolkitConfig.get().chatEnabled) {
            toolkit$selection.clear();
            return;
        }
        if (event.button() != 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (toolkit$selection.isInChatArea(mc, event.x(), event.y())) {
            // Allow clickable links to take priority on single click if no prior selection
            if (toolkit$getClickableStyleAt(event.x(), event.y()) != null && !toolkit$selection.hasSelection()) {
                toolkit$selection.clear();
                return;
            }

            toolkit$selection.onMouseClicked(mc, this.getFont(), event.x(), event.y(), doubleClick);
            // If double or triple clicked, we consumed the click for selection
            if (toolkit$selection.hasSelection() && !toolkit$selection.isDragging()) {
                cir.setReturnValue(true);
            }
        } else {
            toolkit$selection.clear();
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!ToolkitConfig.get().chatEnabled) {
            toolkit$selection.clear();
            return super.mouseDragged(event, dx, dy);
        }
        if (toolkit$selection.isDragging() && event.button() == 0) {
            toolkit$selection.onMouseDragged(Minecraft.getInstance(), this.getFont(), event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            toolkit$selection.stopDragging();
        }
        return super.mouseReleased(event);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void toolkit$onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!ToolkitConfig.get().chatEnabled) {
            toolkit$selection.clear();
            return;
        }

        // Cross-platform copy detection (Ctrl+C / Cmd+C)
        if (event.isCopy() || (event.key() == 67 && event.hasControlDown())) {
            if (toolkit$selection.copySelection(Minecraft.getInstance(), this.getFont())) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void toolkit$onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        toolkit$selection.render(graphics, this.getFont(), Minecraft.getInstance());
    }
}
