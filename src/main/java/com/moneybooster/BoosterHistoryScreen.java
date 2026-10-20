package com.moneybooster;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class BoosterHistoryScreen extends Screen {
    private final Screen parent;
    private int x, y, w, page, panelH, footerY, perPage, detailScroll;
    private long selected = -1;
    private final Button[] rows = new Button[4];
    private final Map<Long, ItemStack> icons = new HashMap<>();
    private Button previous, next, back, filter;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM HH:mm:ss");
    public BoosterHistoryScreen(Screen parent) { super(Component.literal("Booster item history")); this.parent = parent; }
    @Override protected void init() {
        var layout=MenuLayout.of(width,height,240);w=layout.width();x=layout.x();y=layout.y();panelH=layout.height();footerY=layout.footerY();
        perPage=Math.max(1,Math.min(4,(footerY-y-76)/27));
        filter=addRenderableWidget(FlatButton.flat(Component.literal("Money boosts only"),b->{
            ModConfig.getInstance().toggleHistoryFilter();page=0;selected=-1;detailScroll=0;
        }).bounds(x+12,y+25,w-24,20).build());
        for (int i = 0; i < rows.length; i++) {
            final int row = i;
            rows[i] = addRenderableWidget(FlatButton.flat(Component.empty(), b -> {
                var list = BoosterStatsManager.history().newestFirst(ModConfig.getInstance().isHistoryMoneyOnly()); int index = page * perPage + row;
                if (index < list.size()) selected = list.get(index).id; detailScroll=0;
            }).bounds(x + 10, y + 58 + i * 27, w - 20, 25).build());
        }
        previous = addRenderableWidget(FlatButton.flat(Component.literal("Previous"), b -> { if (page > 0) page--; }).bounds(x + 12, footerY, 78, 20).build());
        next = addRenderableWidget(FlatButton.flat(Component.literal("Next"), b -> page++).bounds(x + w - 90, footerY, 78, 20).build());
        back = addRenderableWidget(FlatButton.flat(Component.literal("Back"), b -> {
            if (selected >= 0) selected = -1; else onClose();
        }).bounds(x + (w - 90) / 2, footerY, 90, 20).build());
    }
    private ItemStack icon(BoosterHistory.Entry e) { return icons.computeIfAbsent(e.id, key -> BoosterSources.icon(e)); }
    private void text(GuiGraphicsExtractor g, String value, int tx, int ty, int maxWidth, int color) {
        g.text(font, font.substrByWidth(Component.literal(value), Math.max(0, maxWidth)).getString(), tx, ty, color, false);
    }
    private void row(GuiGraphicsExtractor g, int yy, String label, String value) {
        g.text(font, label, x + 12, yy, 0xFF9DA4AE, false);
        text(g, value, x + w - 12 - font.width(value), yy, w - 24, 0xFFDAE0E8);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float tick) {
        var list = BoosterStatsManager.history().newestFirst(ModConfig.getInstance().isHistoryMoneyOnly());
        int pages = Math.max(1, (list.size() + perPage - 1) / perPage); page = Math.min(page, pages - 1);
        BoosterHistory.Entry detail = BoosterStatsManager.history().find(selected);
        boolean details = detail != null;
        filter.visible=!details;
        filter.setMessage(Component.literal(ModConfig.getInstance().isHistoryMoneyOnly()?"Show: Money boosts only":"Show: All consumables"));
        previous.visible = next.visible = !details; previous.active = page > 0; next.active = page + 1 < pages;
        for (int i = 0; i < rows.length; i++) rows[i].visible = !details && i<perPage && page * perPage + i < list.size();
        g.fill(0, 0, width, height, 0x9906090E);
        g.fill(x, y, x + w, y + panelH, 0xF21B1D21);

        g.text(font, details ? "Activation details" : "Booster item history", x + 12, y + 11, 0xFFF1F4F7, false);
        if(details)g.text(font, "Extra booster earnings / shop trades excluded", x + 12, y + 26, 0xFF9DA4AE, false);
        super.extractRenderState(g, mx, my, tick);
        if (details) {
            detailScroll=Math.max(0,Math.min(detailScroll,Math.max(0,y+182-(footerY-18))));
            g.enableScissor(x+8,y+45,x+w-8,footerY-18);g.pose().pushMatrix();g.pose().translate(0,-detailScroll);
            g.item(icon(detail), x + 14, y + 49);
            text(g, detail.source, x + 38, y + 49, w - 50, 0xFFF1F4F7);
            text(g, detail.itemName, x + 38, y + 62, w - 50, 0xFF9DA4AE);
            row(g, y + 84, "Boost", detail.percentage > 0 ? "+" + detail.percentage + "%" : "No money boost");
            row(g, y + 101, "Announced duration", detail.durationSeconds < 0 ? "Unknown" : time(detail.durationSeconds));
            row(g, y + 118, "Time tracked", time(detail.elapsedSeconds(System.currentTimeMillis())));
            row(g, y + 135, "Booster bonus (est.)", detail.excluded?"Unavailable":"+" + BoosterStatsManager.formatMoney(detail.bonus));
            text(g, detail.status + " / " + DATE.format(Instant.ofEpochMilli(detail.started).atZone(ZoneId.systemDefault())), x + 12, y + 154, w - 24, 0xFF9DA4AE);
            text(g, detail.excluded?detail.exclusionReason:detail.percentage == 0 ? "Effect: " + detail.effect : detail.inferredSource ? "Source matched to an item used within 5s." : "Source: " + detail.source, x + 12, y + 170, w - 24, 0xFF9DA4AE);
            g.pose().popMatrix();g.disableScissor();
            if(panelH<230)g.text(font,"Scroll for details",x+12,footerY-12,0xFF87919D,false);
        } else {
            g.text(font, "Item / boost / time", x + 12, y + 46, 0xFF9DA4AE, false);
            g.text(font, "Bonus", x + w - 12 - font.width("Bonus"), y + 46, 0xFF9DA4AE, false);
            for (int i = 0; i < perPage; i++) {
                int index = page * perPage + i; if (index >= list.size()) break;
                var e = list.get(index); int ry = y + 58 + i * 27;
                g.item(icon(e), x + 15, ry + 5);
                text(g, e.source, x + 38, ry + 4, w - 156, 0xFFF1F4F7);
                String subtitle = e.percentage > 0 ? "+" + e.percentage + "% / " + (e.durationSeconds < 0 ? "?" : time(e.durationSeconds)) : e.effect;
                text(g, subtitle, x + 38, ry + 16, w - 156, 0xFF9DA4AE);
                String bonus = e.excluded?"Unavailable":"+" + BoosterStatsManager.formatMoney(e.bonus);
                g.text(font, bonus, x + w - 17 - font.width(bonus), ry + 9, com.skyblocktoolkit.ToolkitTheme.ACCENT, false);
            }
            if (list.isEmpty()) {
                g.text(font, ModConfig.getInstance().isHistoryMoneyOnly()?"No money boosts recorded yet.":"New item uses will appear here.", x + 12, y + 83, 0xFFDAE0E8, false);
                g.text(font, "Older totals have no per-item records.", x + 12, y + 99, 0xFF9DA4AE, false);
            }
            if(panelH>=132)g.text(font, "Page " + (page + 1) + "/" + pages + " / click a row for details", x + 12, footerY-12, 0xFF9DA4AE, false);
        }
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        if(selected>=0&&mx>=x&&mx<=x+w&&my>=y+45&&my<footerY){detailScroll+=(int)(-vertical*8);return true;}
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    static String time(long seconds) { return seconds < 60 ? seconds + "s" : seconds / 60 + "m " + seconds % 60 + "s"; }
    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
