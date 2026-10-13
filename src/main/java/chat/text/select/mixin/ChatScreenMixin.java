/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.ActiveTextCollector
 *  net.minecraft.client.gui.ActiveTextCollector$ClickableStyleFinder
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.components.ChatComponent
 *  net.minecraft.client.gui.components.ChatComponent$DisplayMode
 *  net.minecraft.client.gui.screens.ChatScreen
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.input.KeyEvent
 *  net.minecraft.client.input.MouseButtonEvent
 *  net.minecraft.client.multiplayer.chat.GuiMessage$Line
 *  net.minecraft.network.chat.Style
 *  net.minecraft.util.ARGB
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 *  net.minecraft.util.Util
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package chat.text.select.mixin;

import chat.text.select.mixin.ChatComponentAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ChatScreen.class})
public abstract class ChatScreenMixin
extends Screen {
    @Shadow
    private ChatComponent.DisplayMode displayMode;
    @Unique
    private int chatSelect$startIdx = -1;
    @Unique
    private int chatSelect$endIdx = -1;
    @Unique
    private int chatSelect$startCharIdx = 0;
    @Unique
    private int chatSelect$endCharIdx = 0;
    @Unique
    private boolean chatSelect$dragging = false;
    @Unique
    private static final long chatSelect$DOUBLE_CLICK_MS = 350L;
    @Unique
    private static final double chatSelect$DOUBLE_CLICK_DIST = 8.0;
    @Unique
    private long chatSelect$lastClickTime = 0L;
    @Unique
    private double chatSelect$lastClickX = 0.0;
    @Unique
    private double chatSelect$lastClickY = 0.0;

    protected ChatScreenMixin() {
        super(null);
    }

    @Unique
    private static LineMetrics chatSelect$buildMetrics(FormattedCharSequence content, Font font) {
        ArrayList<Integer> codePoints = new ArrayList<Integer>();
        ArrayList styles = new ArrayList();
        content.accept((index, style, codePoint) -> {
            codePoints.add(codePoint);
            styles.add(style);
            return true;
        });
        float[] cumulative = new float[codePoints.size() + 1];
        for (int i = 0; i < codePoints.size(); ++i) {
            float width = font.getSplitter().stringWidth(FormattedCharSequence.codepoint((int)((Integer)codePoints.get(i)), (Style)((Style)styles.get(i))));
            cumulative[i + 1] = cumulative[i] + width;
        }
        return new LineMetrics(codePoints, cumulative);
    }

    @Unique
    private static int chatSelect$charIndexAtX(LineMetrics metrics, double localX) {
        int count = metrics.charCount();
        for (int i = 0; i < count; ++i) {
            float mid = (metrics.cumulativeWidth()[i] + metrics.cumulativeWidth()[i + 1]) / 2.0f;
            if (!(localX < (double)mid)) continue;
            return i;
        }
        return count;
    }

    @Unique
    private int chatSelect$screenYToTrimmedIndex(double screenY) {
        Minecraft mc = Minecraft.getInstance();
        double scale = (Double)mc.options.chatScale().get();
        double spacing = (Double)mc.options.chatLineSpacing().get();
        int entryHeight = (int)(9.0 * (spacing + 1.0));
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int chatBottomLocal = Mth.floor((float)((float)(screenHeight - 40) / (float)scale));
        double localY = screenY / scale;
        int displayIdx = (int)Math.floor(((double)chatBottomLocal - localY) / (double)entryHeight);
        int scroll = ((ChatComponentAccessor)mc.gui.hud.getChat()).chattextselect$getChatScrollbarPos();
        return displayIdx + scroll;
    }

    @Unique
    private double chatSelect$screenXToLocalX(double screenX) {
        double scale = (Double)Minecraft.getInstance().options.chatScale().get();
        return screenX / scale - 4.0;
    }

    @Unique
    private Style chatSelect$clickableStyleAt(double screenX, double screenY) {
        Minecraft mc = Minecraft.getInstance();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(this.getFont(), (int)screenX, (int)screenY).includeInsertions(false);
        mc.gui.hud.getChat().captureClickableText((ActiveTextCollector)finder, screenHeight, mc.gui.hud.getGuiTicks(), this.displayMode);
        Style style = finder.result();
        return style != null && style.getClickEvent() != null ? style : null;
    }

    @Unique
    private boolean chatSelect$isInChatHistory(double screenX, double screenY) {
        Minecraft mc = Minecraft.getInstance();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        if (screenY >= (double)(screenHeight - 40)) {
            return false;
        }
        double scale = (Double)mc.options.chatScale().get();
        int chatWidthScreen = (int)((double)ChatComponent.getWidth((double)((Double)mc.options.chatWidth().get())) * scale);
        return screenX >= 0.0 && screenX <= (double)(chatWidthScreen + 8);
    }

    @Unique
    private static boolean chatSelect$isWordChar(int codePoint) {
        return !Character.isWhitespace(codePoint) && !Character.isISOControl(codePoint);
    }

    @Unique
    private void chatSelect$selectWordAt(double screenX, double screenY) {
        int right;
        int charIdx;
        int left;
        Minecraft mc = Minecraft.getInstance();
        List<GuiMessage.Line> lines = ((ChatComponentAccessor)mc.gui.hud.getChat()).chattextselect$getTrimmedMessages();
        int idx = this.chatSelect$screenYToTrimmedIndex(screenY);
        if (idx < 0 || idx >= lines.size()) {
            return;
        }
        LineMetrics metrics = ChatScreenMixin.chatSelect$buildMetrics(lines.get(idx).content(), this.getFont());
        for (left = charIdx = Mth.clamp((int)ChatScreenMixin.chatSelect$charIndexAtX(metrics, this.chatSelect$screenXToLocalX(screenX)), (int)0, (int)metrics.charCount()); left > 0 && ChatScreenMixin.chatSelect$isWordChar(metrics.codePoints().get(left - 1)); --left) {
        }
        for (right = charIdx; right < metrics.charCount() && ChatScreenMixin.chatSelect$isWordChar(metrics.codePoints().get(right)); ++right) {
        }
        if (left == right) {
            return;
        }
        this.chatSelect$startIdx = idx;
        this.chatSelect$endIdx = idx;
        this.chatSelect$startCharIdx = left;
        this.chatSelect$endCharIdx = right;
        this.chatSelect$dragging = false;
    }

    @Unique
    private void chatSelect$clearSelection() {
        this.chatSelect$startIdx = -1;
        this.chatSelect$endIdx = -1;
        this.chatSelect$dragging = false;
    }

    @Inject(method={"mouseClicked"}, at={@At(value="HEAD")}, cancellable=true)
    private void chatSelect$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!com.skyblocktoolkit.ToolkitConfig.get().chatEnabled) {this.chatSelect$clearSelection();return;}
        if (event.button() != 0) {
            return;
        }
        boolean inHistory = this.chatSelect$isInChatHistory(event.x(), event.y());
        long now = Util.getMillis();
        double dist = Math.hypot(event.x() - this.chatSelect$lastClickX, event.y() - this.chatSelect$lastClickY);
        boolean ownDoubleClick = now - this.chatSelect$lastClickTime <= 350L && dist <= 8.0;
        this.chatSelect$lastClickTime = now;
        this.chatSelect$lastClickX = event.x();
        this.chatSelect$lastClickY = event.y();
        if (inHistory && com.skyblocktoolkit.ToolkitConfig.get().wordSelect && (doubleClick || ownDoubleClick)) {
            if (inHistory) {
                this.chatSelect$selectWordAt(event.x(), event.y());
            } else {
                this.chatSelect$clearSelection();
            }
            cir.setReturnValue(true);
            return;
        }
        if (inHistory && this.chatSelect$clickableStyleAt(event.x(), event.y()) != null) {
            this.chatSelect$clearSelection();
            return;
        }
        if (inHistory) {
            List<GuiMessage.Line> lines = ((ChatComponentAccessor)Minecraft.getInstance().gui.hud.getChat()).chattextselect$getTrimmedMessages();
            int idx = this.chatSelect$screenYToTrimmedIndex(event.y());
            if (idx >= 0 && idx < lines.size()) {
                LineMetrics metrics = ChatScreenMixin.chatSelect$buildMetrics(lines.get(idx).content(), this.getFont());
                int charIdx = ChatScreenMixin.chatSelect$charIndexAtX(metrics, this.chatSelect$screenXToLocalX(event.x()));
                this.chatSelect$startIdx = idx;
                this.chatSelect$endIdx = idx;
                this.chatSelect$startCharIdx = charIdx;
                this.chatSelect$endCharIdx = charIdx;
                this.chatSelect$dragging = true;
                return;
            }
        }
        this.chatSelect$clearSelection();
    }

    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!com.skyblocktoolkit.ToolkitConfig.get().chatEnabled) {this.chatSelect$clearSelection();return super.mouseDragged(event,dx,dy);}
        if (this.chatSelect$dragging && event.button() == 0) {
            List<GuiMessage.Line> lines = ((ChatComponentAccessor)Minecraft.getInstance().gui.hud.getChat()).chattextselect$getTrimmedMessages();
            if (lines.isEmpty()) {
                return true;
            }
            int idx = Mth.clamp((int)this.chatSelect$screenYToTrimmedIndex(event.y()), (int)0, (int)(lines.size() - 1));
            LineMetrics metrics = ChatScreenMixin.chatSelect$buildMetrics(lines.get(idx).content(), this.getFont());
            this.chatSelect$endIdx = idx;
            this.chatSelect$endCharIdx = ChatScreenMixin.chatSelect$charIndexAtX(metrics, this.chatSelect$screenXToLocalX(event.x()));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.chatSelect$dragging = false;
        }
        return super.mouseReleased(event);
    }

    @Inject(method={"keyPressed"}, at={@At(value="HEAD")}, cancellable=true)
    private void chatSelect$onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        int bottomChar;
        if (!com.skyblocktoolkit.ToolkitConfig.get().chatEnabled) {this.chatSelect$clearSelection();return;}
        if (event.key() != 67 || !event.hasControlDown()) {
            return;
        }
        if (this.chatSelect$startIdx < 0 || this.chatSelect$endIdx < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        List<GuiMessage.Line> lines = ((ChatComponentAccessor)mc.gui.hud.getChat()).chattextselect$getTrimmedMessages();
        if (lines.isEmpty()) {
            return;
        }
        int lo = Mth.clamp((int)Math.min(this.chatSelect$startIdx, this.chatSelect$endIdx), (int)0, (int)(lines.size() - 1));
        int hi = Mth.clamp((int)Math.max(this.chatSelect$startIdx, this.chatSelect$endIdx), (int)0, (int)(lines.size() - 1));
        int topChar = this.chatSelect$startIdx >= this.chatSelect$endIdx ? this.chatSelect$startCharIdx : this.chatSelect$endCharIdx;
        int n = bottomChar = this.chatSelect$startIdx >= this.chatSelect$endIdx ? this.chatSelect$endCharIdx : this.chatSelect$startCharIdx;
        if (hi == lo) {
            int a = Math.min(topChar, bottomChar);
            int b = Math.max(topChar, bottomChar);
            topChar = a;
            bottomChar = b;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = hi; i >= lo; --i) {
            LineMetrics metrics = ChatScreenMixin.chatSelect$buildMetrics(lines.get(i).content(), this.getFont());
            int from = i == hi ? Mth.clamp((int)topChar, (int)0, (int)metrics.charCount()) : 0;
            int to = i == lo ? Mth.clamp((int)bottomChar, (int)0, (int)metrics.charCount()) : metrics.charCount();
            for (int c = from; c < to; ++c) {
                sb.appendCodePoint(metrics.codePoints().get(c));
            }
            if (i <= lo) continue;
            sb.append(lines.get(i).endOfEntry() ? "\n" : " ");
        }
        mc.keyboardHandler.setClipboard(sb.toString());
        cir.setReturnValue(true);
        cir.cancel();
    }

    @Inject(method={"extractRenderState"}, at={@At(value="TAIL")})
    private void chatSelect$onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int bottomChar;
        if (!com.skyblocktoolkit.ToolkitConfig.get().chatEnabled) {this.chatSelect$clearSelection();return;}
        if (this.chatSelect$startIdx < 0 || this.chatSelect$endIdx < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ChatComponent chat = mc.gui.hud.getChat();
        List<GuiMessage.Line> lines = ((ChatComponentAccessor)chat).chattextselect$getTrimmedMessages();
        if (lines.isEmpty()) {
            return;
        }
        int scroll = ((ChatComponentAccessor)chat).chattextselect$getChatScrollbarPos();
        float scale = (float)((Double)mc.options.chatScale().get()).doubleValue();
        int entryHeight = (int)(9.0 * ((Double)mc.options.chatLineSpacing().get() + 1.0));
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int chatBottomLocal = Mth.floor((float)((float)(screenHeight - 40) / scale));
        int linesPerPage = chat.getLinesPerPage();
        int maxWidth = Mth.ceil((float)((float)ChatComponent.getWidth((double)((Double)mc.options.chatWidth().get())) / scale));
        int lo = Math.min(this.chatSelect$startIdx, this.chatSelect$endIdx);
        int hi = Math.max(this.chatSelect$startIdx, this.chatSelect$endIdx);
        int topChar = this.chatSelect$startIdx >= this.chatSelect$endIdx ? this.chatSelect$startCharIdx : this.chatSelect$endCharIdx;
        int n = bottomChar = this.chatSelect$startIdx >= this.chatSelect$endIdx ? this.chatSelect$endCharIdx : this.chatSelect$startCharIdx;
        if (hi == lo) {
            int a = Math.min(topChar, bottomChar);
            int b = Math.max(topChar, bottomChar);
            topChar = a;
            bottomChar = b;
        }
        int highlightColor = com.skyblocktoolkit.ToolkitConfig.get().highlightColor();
        Font font = this.getFont();
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(4.0f, 0.0f);
        for (int tmIdx = lo; tmIdx <= hi; ++tmIdx) {
            float xEnd;
            float xStart;
            int displayIdx;
            if (tmIdx < 0 || tmIdx >= lines.size() || (displayIdx = tmIdx - scroll) < 0 || displayIdx >= linesPerPage) continue;
            int entryBottom = chatBottomLocal - displayIdx * entryHeight;
            int entryTop = entryBottom - entryHeight;
            if (hi == lo) {
                LineMetrics metrics = ChatScreenMixin.chatSelect$buildMetrics(lines.get(tmIdx).content(), font);
                xStart = metrics.widthTo(topChar);
                xEnd = metrics.widthTo(bottomChar);
            } else if (tmIdx == hi) {
                LineMetrics metrics = ChatScreenMixin.chatSelect$buildMetrics(lines.get(tmIdx).content(), font);
                xStart = metrics.widthTo(topChar);
                xEnd = maxWidth + 8;
            } else if (tmIdx == lo) {
                LineMetrics metrics = ChatScreenMixin.chatSelect$buildMetrics(lines.get(tmIdx).content(), font);
                xStart = -4.0f;
                xEnd = metrics.widthTo(bottomChar);
            } else {
                xStart = -4.0f;
                xEnd = maxWidth + 8;
            }
            graphics.fill((int)xStart, entryTop, (int)xEnd, entryBottom, highlightColor);
        }
        graphics.pose().popMatrix();
    }

    @Unique
    private record LineMetrics(List<Integer> codePoints, float[] cumulativeWidth) {
        int charCount() {
            return this.codePoints.size();
        }

        float widthTo(int idx) {
            return this.cumulativeWidth[Mth.clamp((int)idx, (int)0, (int)this.codePoints.size())];
        }
    }
}

