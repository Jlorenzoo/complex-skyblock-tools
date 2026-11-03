package com.skyblocktoolkit.chat;

import com.skyblocktoolkit.AlertSound;
import com.skyblocktoolkit.ToolkitConfig;
import com.skyblocktoolkit.mixin.ChatComponentAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Native chat text selection engine for Skyblock Toolkit.
 * Handles screen-to-chat coordinate translation, character metrics,
 * word/line selection gestures, clipboard copying, and GC code quick-redeem.
 */
public class ChatSelection {
    private static final long MULTI_CLICK_INTERVAL_MS = 350L;
    private static final double MULTI_CLICK_MAX_DISTANCE = 8.0;

    public static final Pattern GC_CODE_PATTERN = Pattern.compile(
        "\\b(?:\\d{16}|(?:\\d{4}[ -]){3}\\d{4}|(?:[A-Za-z0-9]{4}-){3}[A-Za-z0-9]{4})\\b"
    );

    public record GcCodeMatch(int lineIdx, int startChar, int endChar, String rawCode, String cleanCode, float xStart, float xEnd, int yTop, int yBottom) {}

    public static String cleanCode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace(" ", "").replace("-", "");
    }

    public static String lineToString(LineMetrics metrics) {
        StringBuilder sb = new StringBuilder(metrics.charCount());
        for (int cp : metrics.codePoints()) {
            sb.appendCodePoint(cp);
        }
        return sb.toString();
    }

    private int startLine = -1;
    private int endLine = -1;
    private int startChar = 0;
    private int endChar = 0;
    private boolean dragging = false;

    private long lastClickTime = 0L;
    private double lastClickX = 0.0;
    private double lastClickY = 0.0;
    private int clickCount = 0;

    public record LineMetrics(List<Integer> codePoints, float[] cumulativeWidth) {
        public int charCount() {
            return codePoints.size();
        }

        public float widthTo(int idx) {
            return cumulativeWidth[Mth.clamp(idx, 0, codePoints.size())];
        }
    }

    public static LineMetrics buildMetrics(FormattedCharSequence content, Font font) {
        List<Integer> codePoints = new ArrayList<>();
        List<Style> styles = new ArrayList<>();
        content.accept((index, style, cp) -> {
            codePoints.add(cp);
            styles.add(style);
            return true;
        });

        float[] cumulative = new float[codePoints.size() + 1];
        for (int i = 0; i < codePoints.size(); i++) {
            float width = font.getSplitter().stringWidth(FormattedCharSequence.codepoint(codePoints.get(i), styles.get(i)));
            cumulative[i + 1] = cumulative[i] + width;
        }
        return new LineMetrics(codePoints, cumulative);
    }

    public static int charIndexAtX(LineMetrics metrics, double localX) {
        int count = metrics.charCount();
        for (int i = 0; i < count; i++) {
            float mid = (metrics.cumulativeWidth()[i] + metrics.cumulativeWidth()[i + 1]) / 2.0f;
            if (localX < (double) mid) {
                return i;
            }
        }
        return count;
    }

    public boolean hasSelection() {
        return startLine >= 0 && endLine >= 0 && (startLine != endLine || startChar != endChar);
    }

    public void clear() {
        startLine = -1;
        endLine = -1;
        startChar = 0;
        endChar = 0;
        dragging = false;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void stopDragging() {
        dragging = false;
    }

    public int screenYToLineIndex(Minecraft mc, double screenY) {
        double scale = mc.options.chatScale().get();
        double spacing = mc.options.chatLineSpacing().get();
        int entryHeight = (int) (9.0 * (spacing + 1.0));
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int chatBottomLocal = Mth.floor((float) (screenHeight - 40) / (float) scale);
        double localY = screenY / scale;
        int displayIdx = (int) Math.floor(((double) chatBottomLocal - localY) / (double) entryHeight);
        int scroll = ((ChatComponentAccessor) mc.gui.hud.getChat()).toolkit$getChatScrollbarPos();
        return displayIdx + scroll;
    }

    public double screenXToLocalX(Minecraft mc, double screenX) {
        double scale = mc.options.chatScale().get();
        return screenX / scale - 4.0;
    }

    public boolean isInChatArea(Minecraft mc, double screenX, double screenY) {
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        if (screenY >= (double) (screenHeight - 40)) {
            return false;
        }
        double scale = mc.options.chatScale().get();
        int chatWidthScreen = (int) ((double) ChatComponent.getWidth((Double) mc.options.chatWidth().get()) * scale);
        return screenX >= 0.0 && screenX <= (double) (chatWidthScreen + 8);
    }

    private static boolean isWordChar(int codePoint) {
        return !Character.isWhitespace(codePoint) && !Character.isISOControl(codePoint);
    }

    public void onMouseClicked(Minecraft mc, Font font, double screenX, double screenY, boolean doubleClickDetected) {
        if (!isInChatArea(mc, screenX, screenY)) {
            clear();
            return;
        }

        long now = Util.getMillis();
        double dist = Math.hypot(screenX - lastClickX, screenY - lastClickY);
        if (now - lastClickTime <= MULTI_CLICK_INTERVAL_MS && dist <= MULTI_CLICK_MAX_DISTANCE) {
            clickCount++;
        } else {
            clickCount = 1;
        }
        lastClickTime = now;
        lastClickX = screenX;
        lastClickY = screenY;

        List<GuiMessage.Line> lines = ((ChatComponentAccessor) mc.gui.hud.getChat()).toolkit$getTrimmedMessages();
        int lineIdx = screenYToLineIndex(mc, screenY);
        if (lineIdx < 0 || lineIdx >= lines.size()) {
            clear();
            return;
        }

        LineMetrics metrics = buildMetrics(lines.get(lineIdx).content(), font);
        int charIdx = charIndexAtX(metrics, screenXToLocalX(mc, screenX));

        // Triple click: select full line
        if (clickCount >= 3) {
            startLine = lineIdx;
            endLine = lineIdx;
            startChar = 0;
            endChar = metrics.charCount();
            dragging = false;
            return;
        }

        // Double click: select word
        if ((clickCount == 2 || doubleClickDetected) && ToolkitConfig.get().wordSelect) {
            int left = Mth.clamp(charIdx, 0, metrics.charCount());
            int right = left;

            while (left > 0 && isWordChar(metrics.codePoints().get(left - 1))) {
                left--;
            }
            while (right < metrics.charCount() && isWordChar(metrics.codePoints().get(right))) {
                right++;
            }

            if (left != right) {
                startLine = lineIdx;
                endLine = lineIdx;
                startChar = left;
                endChar = right;
                dragging = false;
                return;
            }
        }

        // Single click: start drag anchor
        startLine = lineIdx;
        endLine = lineIdx;
        startChar = charIdx;
        endChar = charIdx;
        dragging = true;
    }

    public void onMouseDragged(Minecraft mc, Font font, double screenX, double screenY) {
        if (!dragging) {
            return;
        }
        List<GuiMessage.Line> lines = ((ChatComponentAccessor) mc.gui.hud.getChat()).toolkit$getTrimmedMessages();
        if (lines.isEmpty()) {
            return;
        }

        int lineIdx = Mth.clamp(screenYToLineIndex(mc, screenY), 0, lines.size() - 1);
        LineMetrics metrics = buildMetrics(lines.get(lineIdx).content(), font);
        endLine = lineIdx;
        endChar = charIndexAtX(metrics, screenXToLocalX(mc, screenX));
    }

    public boolean copySelection(Minecraft mc, Font font) {
        if (!hasSelection()) {
            return false;
        }

        List<GuiMessage.Line> lines = ((ChatComponentAccessor) mc.gui.hud.getChat()).toolkit$getTrimmedMessages();
        if (lines.isEmpty()) {
            return false;
        }

        int lo = Mth.clamp(Math.min(startLine, endLine), 0, lines.size() - 1);
        int hi = Mth.clamp(Math.max(startLine, endLine), 0, lines.size() - 1);
        int topChar = startLine >= endLine ? startChar : endChar;
        int bottomChar = startLine >= endLine ? endChar : startChar;

        if (hi == lo) {
            int a = Math.min(topChar, bottomChar);
            int b = Math.max(topChar, bottomChar);
            topChar = a;
            bottomChar = b;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = hi; i >= lo; i--) {
            LineMetrics metrics = buildMetrics(lines.get(i).content(), font);
            int from = (i == hi) ? Mth.clamp(topChar, 0, metrics.charCount()) : 0;
            int to = (i == lo) ? Mth.clamp(bottomChar, 0, metrics.charCount()) : metrics.charCount();

            for (int c = from; c < to; c++) {
                sb.appendCodePoint(metrics.codePoints().get(c));
            }

            if (i > lo) {
                sb.append(lines.get(i).endOfEntry() ? "\n" : " ");
            }
        }

        if (sb.length() > 0) {
            mc.keyboardHandler.setClipboard(sb.toString());
            return true;
        }
        return false;
    }

    public GcCodeMatch findGcCodeAt(Minecraft mc, Font font, double screenX, double screenY) {
        if (!isInChatArea(mc, screenX, screenY)) {
            return null;
        }

        ChatComponent chat = mc.gui.hud.getChat();
        int scroll = ((ChatComponentAccessor) chat).toolkit$getChatScrollbarPos();
        int linesPerPage = chat.getLinesPerPage();
        int lineIdx = screenYToLineIndex(mc, screenY);
        int displayIdx = lineIdx - scroll;
        if (displayIdx < 0 || displayIdx >= linesPerPage) {
            return null;
        }

        List<GuiMessage.Line> lines = ((ChatComponentAccessor) chat).toolkit$getTrimmedMessages();
        if (lineIdx < 0 || lineIdx >= lines.size()) {
            return null;
        }

        float scale = (float) (double) mc.options.chatScale().get();
        int entryHeight = (int) (9.0 * ((Double) mc.options.chatLineSpacing().get() + 1.0));
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int chatBottomLocal = Mth.floor((float) (screenHeight - 40) / scale);
        int entryBottom = chatBottomLocal - displayIdx * entryHeight;
        int entryTop = entryBottom - entryHeight;
        int maxWidth = Mth.ceil((float) ChatComponent.getWidth((Double) mc.options.chatWidth().get()) / scale);

        LineMetrics metrics = buildMetrics(lines.get(lineIdx).content(), font);
        String lineText = lineToString(metrics);
        double localX = screenXToLocalX(mc, screenX);
        int charIdx = charIndexAtX(metrics, localX);

        Matcher matcher = GC_CODE_PATTERN.matcher(lineText);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            float xStart = metrics.widthTo(start);
            float xEnd = metrics.widthTo(end);

            boolean isHit = (charIdx >= start && charIdx <= end) ||
                (localX >= (double) xStart - 3.0 && localX <= (double) xEnd + 3.0) ||
                (lineText.trim().equals(matcher.group()) && localX >= -4.0 && localX <= maxWidth + 8);

            if (isHit) {
                return new GcCodeMatch(lineIdx, start, end, matcher.group(), cleanCode(matcher.group()), xStart, xEnd, entryTop, entryBottom);
            }
        }

        // Multi-line wrapped message entry fallback
        int entryStartIndex = lineIdx;
        while (entryStartIndex < lines.size() - 1 && !lines.get(entryStartIndex + 1).endOfEntry()) {
            entryStartIndex++;
        }
        int entryEndIndex = lineIdx;
        while (entryEndIndex > 0 && !lines.get(entryEndIndex).endOfEntry()) {
            entryEndIndex--;
        }
        if (entryStartIndex != entryEndIndex) {
            StringBuilder fullText = new StringBuilder();
            for (int i = entryStartIndex; i >= entryEndIndex; i--) {
                fullText.append(lineToString(buildMetrics(lines.get(i).content(), font)));
            }
            Matcher fullMatcher = GC_CODE_PATTERN.matcher(fullText.toString());
            if (fullMatcher.find()) {
                return new GcCodeMatch(lineIdx, 0, metrics.charCount(), fullMatcher.group(), cleanCode(fullMatcher.group()), 0f, (float) maxWidth, entryTop, entryBottom);
            }
        }

        return null;
    }

    public static boolean redeemGcCode(Minecraft mc, String code) {
        if (code == null || code.isBlank() || mc == null) {
            return false;
        }
        String clean = cleanCode(code);
        if (clean.isEmpty()) {
            return false;
        }

        // 1. Copy clean code to system clipboard
        if (mc.keyboardHandler != null) {
            mc.keyboardHandler.setClipboard(clean);
        }

        // 2. Close chat screen
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen) {
            mc.setScreenAndShow(null);
        }

        // 3. Send /gc command to server
        if (mc.getConnection() != null) {
            mc.getConnection().sendCommand("gc");
        }

        // 4. Play confirmation chime
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(new AlertSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.8f, 1.0f, 1.0));
        }

        // 5. Display overlay notification
        if (mc.player != null) {
            mc.player.sendOverlayMessage(Component.literal("§aCopied GC code §e" + clean + " §aand opened §e/gc"));
        }

        return true;
    }

    public boolean handleGcCodeClick(Minecraft mc, Font font, double screenX, double screenY) {
        GcCodeMatch match = findGcCodeAt(mc, font, screenX, screenY);
        if (match == null) {
            return false;
        }
        clear();
        return redeemGcCode(mc, match.cleanCode());
    }

    public void render(GuiGraphicsExtractor graphics, Font font, Minecraft mc, int mouseX, int mouseY) {
        if (!ToolkitConfig.get().chatEnabled) {
            return;
        }

        ChatComponent chat = mc.gui.hud.getChat();
        List<GuiMessage.Line> lines = ((ChatComponentAccessor) chat).toolkit$getTrimmedMessages();
        if (lines.isEmpty()) {
            return;
        }

        int scroll = ((ChatComponentAccessor) chat).toolkit$getChatScrollbarPos();
        float scale = (float) (double) mc.options.chatScale().get();
        int entryHeight = (int) (9.0 * ((Double) mc.options.chatLineSpacing().get() + 1.0));
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int chatBottomLocal = Mth.floor((float) (screenHeight - 40) / scale);
        int linesPerPage = chat.getLinesPerPage();
        int maxWidth = Mth.ceil((float) ChatComponent.getWidth((Double) mc.options.chatWidth().get()) / scale);

        GcCodeMatch hoveredMatch = null;
        if (ToolkitConfig.get().gcCodeClick) {
            boolean inChat = isInChatArea(mc, mouseX, mouseY);
            int mouseLine = inChat ? screenYToLineIndex(mc, mouseY) : -1;
            double localMouseX = inChat ? screenXToLocalX(mc, mouseX) : -1000.0;

            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            graphics.pose().translate(4.0f, 0.0f);

            int minVisible = scroll;
            int maxVisible = Math.min(lines.size() - 1, scroll + linesPerPage - 1);

            for (int idx = minVisible; idx <= maxVisible; idx++) {
                int displayIdx = idx - scroll;
                int entryBottom = chatBottomLocal - displayIdx * entryHeight;
                int entryTop = entryBottom - entryHeight;

                LineMetrics metrics = buildMetrics(lines.get(idx).content(), font);
                String lineText = lineToString(metrics);
                Matcher matcher = GC_CODE_PATTERN.matcher(lineText);
                while (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();
                    float xStart = metrics.widthTo(start);
                    float xEnd = metrics.widthTo(end);

                    boolean isHovered = (idx == mouseLine) && (
                        (localMouseX >= (double) xStart - 3.0 && localMouseX <= (double) xEnd + 3.0) ||
                        (lineText.trim().equals(matcher.group()) && localMouseX >= -4.0 && localMouseX <= maxWidth + 8)
                    );

                    if (isHovered) {
                        graphics.fill((int) xStart - 2, entryTop + 1, (int) xEnd + 2, entryBottom, 0x442ECC71);
                        graphics.fill((int) xStart - 2, entryBottom - 2, (int) xEnd + 2, entryBottom, 0xFF2ECC71);
                        hoveredMatch = new GcCodeMatch(idx, start, end, matcher.group(), cleanCode(matcher.group()), xStart, xEnd, entryTop, entryBottom);
                    } else {
                        graphics.fill((int) xStart, entryBottom - 1, (int) xEnd, entryBottom, 0xCC2ECC71);
                    }
                }
            }

            graphics.pose().popMatrix();
        }

        if (hasSelection()) {
            int lo = Math.min(startLine, endLine);
            int hi = Math.max(startLine, endLine);
            int topChar = startLine >= endLine ? startChar : endChar;
            int bottomChar = startLine >= endLine ? endChar : startChar;

            if (hi == lo) {
                int a = Math.min(topChar, bottomChar);
                int b = Math.max(topChar, bottomChar);
                topChar = a;
                bottomChar = b;
            }

            int highlightColor = ToolkitConfig.get().highlightColor();

            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            graphics.pose().translate(4.0f, 0.0f);

            for (int idx = lo; idx <= hi; idx++) {
                if (idx < 0 || idx >= lines.size()) {
                    continue;
                }
                int displayIdx = idx - scroll;
                if (displayIdx < 0 || displayIdx >= linesPerPage) {
                    continue;
                }

                int entryBottom = chatBottomLocal - displayIdx * entryHeight;
                int entryTop = entryBottom - entryHeight;

                float xStart;
                float xEnd;

                LineMetrics metrics = buildMetrics(lines.get(idx).content(), font);
                if (hi == lo) {
                    xStart = metrics.widthTo(topChar);
                    xEnd = metrics.widthTo(bottomChar);
                } else if (idx == hi) {
                    xStart = metrics.widthTo(topChar);
                    xEnd = maxWidth + 8;
                } else if (idx == lo) {
                    xStart = -4.0f;
                    xEnd = metrics.widthTo(bottomChar);
                } else {
                    xStart = -4.0f;
                    xEnd = maxWidth + 8;
                }

                graphics.fill((int) xStart, entryTop, (int) xEnd, entryBottom, highlightColor);
            }

            graphics.pose().popMatrix();
        }

        if (hoveredMatch != null) {
            graphics.setTooltipForNextFrame(
                font,
                Component.literal("§a§lClick to copy & open /gc\n§7GC Code: §e" + hoveredMatch.cleanCode()),
                mouseX,
                mouseY
            );
        }
    }

    public void render(GuiGraphicsExtractor graphics, Font font, Minecraft mc) {
        render(graphics, font, mc, -1000, -1000);
    }
}
