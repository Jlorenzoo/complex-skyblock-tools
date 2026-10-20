package com.moneybooster;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class BoosterStatsScreen extends Screen {
    private final Screen parent;
    private boolean isDraggingBar = false, draggingAverage;
    private double averageDragX,averageDragY;
    private double dragStartMouseX;
    private double dragStartMouseY;
    private int dragStartOffsetX;
    private int dragStartOffsetY;

    private int cardX, cardY, cardW, cardH, footerY, scroll;
    private Button btnAverage,btnPosition;
    private boolean showTest;
    private long resetConfirmUntil;
    private Button tabStats, tabTest;
    private Button btn1Min;
    private Button btn5Min;
    private Button btnFree;
    private Button btnStartStop;
    private Button btnResetStats;
    private Button btnDone;
    private Button btnHistory;
    private Button btnMoney,btnXp,btnXpBoosters;

    public BoosterStatsScreen() { this(null); }
    public BoosterStatsScreen(Screen parent) {
        super(Component.literal("Money Booster & Hourly Rate Tracker"));this.parent=parent;
    }
    public BoosterStatsScreen(Screen parent, boolean showTest) { this(parent);this.showTest=showTest; }

    @Override
    protected void init() {
        super.init();
        var layout=MenuLayout.of(width,height,304);
        cardW=layout.width();cardX=layout.x();cardY=layout.y();cardH=layout.height();footerY=layout.footerY();
        int x = cardX, y = cardY, w = cardW;
        tabStats = addRenderableWidget(FlatButton.flat(Component.literal("Earnings"), b -> { showTest = false; scroll=0; refresh(); }).bounds(x + 12, y + 38, (w - 28) / 2, 20).build());
        tabTest = addRenderableWidget(FlatButton.flat(Component.literal("Rate test"), b -> { showTest = true; scroll=0; refresh(); }).bounds(x + 16 + (w - 28) / 2, y + 38, (w - 28) / 2, 20).build());
        int third = (w - 32) / 3;
        btnMoney=addRenderableWidget(com.skyblocktoolkit.ToolkitButton.nav("Money",b->{com.skyblocktoolkit.ToolkitConfig.get().rateTestXp=false;com.skyblocktoolkit.ToolkitConfig.get().save();rebuildWidgets();},x+12,y+68,(w-28)/2,20,!RateTracker.isXpSelected()));
        btnXp=addRenderableWidget(com.skyblocktoolkit.ToolkitButton.nav("Minecraft XP",b->{com.skyblocktoolkit.ToolkitConfig.get().rateTestXp=true;com.skyblocktoolkit.ToolkitConfig.get().save();rebuildWidgets();},x+16+(w-28)/2,y+68,(w-28)/2,20,RateTracker.isXpSelected()));
        btnXpBoosters=addRenderableWidget(com.skyblocktoolkit.ToolkitButton.toggle("Include XP boosters",b->{var cfg=com.skyblocktoolkit.ToolkitConfig.get();cfg.xpIncludesBoosters=!cfg.xpIncludesBoosters;cfg.save();refresh();},x+12,y+120,w-24,24,()->com.skyblocktoolkit.ToolkitConfig.get().xpIncludesBoosters));
        btn1Min = addRenderableWidget(FlatButton.flat(Component.literal("1 min"), b -> RateTracker.setSelectedDuration(RateTracker.Duration.ONE_MIN)).bounds(x + 12, y + 94, third, 20).build());
        btn5Min = addRenderableWidget(FlatButton.flat(Component.literal("5 min"), b -> RateTracker.setSelectedDuration(RateTracker.Duration.FIVE_MIN)).bounds(x + 16 + third, y + 94, third, 20).build());
        btnFree = addRenderableWidget(FlatButton.flat(Component.literal("Free run"), b -> RateTracker.setSelectedDuration(RateTracker.Duration.FREE_RUN)).bounds(x + 20 + third * 2, y + 94, third, 20).build());
        btnStartStop = addRenderableWidget(FlatButton.flat(Component.literal("Start test"), b -> {
            if (RateTracker.isRunning() || RateTracker.isPreparing()) RateTracker.stop();
            else { RateTracker.startCountdown(); Minecraft.getInstance().setScreenAndShow(null); }
        }).bounds(x + 12, footerY, (w - 28) / 2, 20).build());
        btnResetStats = addRenderableWidget(FlatButton.flat(Component.literal("Reset totals"), b -> {
            long now = System.currentTimeMillis();
            if (now < resetConfirmUntil) { BoosterStatsManager.resetStats(); resetConfirmUntil = 0; }
            else resetConfirmUntil = now + 3000;
        }).bounds(x + 12, footerY, (w - 28) / 2, 20).build());
        btnDone = addRenderableWidget(FlatButton.flat(Component.literal("Done"), b -> onClose()).bounds(x + 16 + (w - 28) / 2, footerY, (w - 28) / 2, 20).build());
        btnHistory = addRenderableWidget(FlatButton.flat(Component.literal("View item history"), b -> Minecraft.getInstance().setScreenAndShow(new BoosterHistoryScreen(this))).bounds(x + 12, y + 157, w - 24, 20).build());
        btnAverage=addRenderableWidget(FlatButton.flat(Component.literal("Crop HUD: off"),b->{var cfg=com.skyblocktoolkit.ToolkitConfig.get();cfg.cropEarningsHud=!cfg.cropEarningsHud;cfg.save();refresh();}).bounds(x+12,y+181,(w-28)/2,20).build());
        btnPosition=addRenderableWidget(FlatButton.flat(Component.literal("With boosters"),b->{ModConfig.getInstance().toggleAverageBoosters();refresh();}).bounds(x+16+(w-28)/2,y+181,(w-28)/2,20).build());
        refresh();
    }
    private boolean inBody(int yy,int h){return yy>=cardY+58 && yy+h<=footerY-8;}
    private void refresh() {
        tabStats.active = showTest; tabTest.active = !showTest;
        boolean busy = RateTracker.isPreparing() || RateTracker.isRunning();
        btn1Min.visible = btn5Min.visible = btnFree.visible = btnStartStop.visible = showTest;
        btn1Min.active = btn5Min.active = btnFree.active = !busy && !RateTracker.isSettling();
        RateTracker.Duration d = RateTracker.getSelectedDuration();
        btn1Min.setMessage(Component.literal(d == RateTracker.Duration.ONE_MIN ? "[1 min]" : "1 min"));
        btn5Min.setMessage(Component.literal(d == RateTracker.Duration.FIVE_MIN ? "[5 min]" : "5 min"));
        btnFree.setMessage(Component.literal(d == RateTracker.Duration.FREE_RUN ? "[Free run]" : "Free run"));
        btnStartStop.setMessage(Component.literal(RateTracker.isPreparing() ? "Cancel countdown" : busy ? "Stop test" : "Start test"));
        btnStartStop.active = !RateTracker.isSettling() && Minecraft.getInstance().level != null;
        scroll=Math.max(0,Math.min(scroll,Math.max(0,cardY+(showTest?246:224)-(footerY-8))));
        btnMoney.setY(cardY+68-scroll);btnXp.setY(cardY+68-scroll);btnXpBoosters.setY(cardY+120-scroll);
        btnMoney.visible=btnXp.visible=showTest&&inBody(btnMoney.getY(),20);
        btnMoney.active=RateTracker.isXpSelected();btnXp.active=!RateTracker.isXpSelected();
        btnXpBoosters.visible=showTest&&RateTracker.isXpSelected()&&inBody(btnXpBoosters.getY(),24);
        btnHistory.setY(cardY+157-scroll);
        btnAverage.setY(cardY+181-scroll);btnPosition.setY(cardY+181-scroll);
        btnAverage.setMessage(Component.literal("Crop HUD: "+(com.skyblocktoolkit.ToolkitConfig.get().cropEarningsHud?"on":"off")));
        btnAverage.visible=!showTest&&inBody(btnAverage.getY(),20);btnPosition.visible=!showTest&&inBody(btnPosition.getY(),20);
        btnPosition.active=com.skyblocktoolkit.ToolkitConfig.get().cropEarningsHud;
        btnPosition.setMessage(Component.literal(ModConfig.getInstance().includesBoostersInAverage()?"With boosters":"Without boosters"));
        btn1Min.setY(cardY+94-scroll);btn5Min.setY(cardY+94-scroll);btnFree.setY(cardY+94-scroll);
        btn1Min.visible=btn5Min.visible=btnFree.visible=showTest&&inBody(btn1Min.getY(),20);
        btnHistory.visible = !showTest&&inBody(btnHistory.getY(),20);
        btnHistory.setMessage(Component.literal("View item history (" + BoosterStatsManager.history().newestFirst().size() + ")"));
        btnResetStats.visible = !showTest;
        btnResetStats.active = !busy && !RateTracker.isSettling();
        btnResetStats.setMessage(Component.literal(System.currentTimeMillis() < resetConfirmUntil ? "Confirm reset" : "Reset totals"));
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        refresh();
        int x = cardX, y = cardY, w = cardW;
        g.fill(0, 0, this.width, this.height, com.skyblocktoolkit.ToolkitTheme.palette().scrim());
        g.fill(x, y, x + w, y + cardH, com.skyblocktoolkit.ToolkitTheme.palette().panel());
        

        g.text(font, showTest ? "Hourly rates" : "Money Booster", x + 12, y + 11, com.skyblocktoolkit.ToolkitTheme.palette().text(), false);
        g.text(font, showTest ? "Measure money or Minecraft XP per hour" : "Your booster bonus and item history", x + 12, y + 24, com.skyblocktoolkit.ToolkitTheme.palette().muted(), false);
        g.enableScissor(x+8,y+58,x+w-8,footerY-8);
        g.pose().pushMatrix();g.pose().translate(0,-scroll);
        if (!showTest) {
            var last=BoosterStatsManager.history().latestMoneyBooster();
            g.text(font, "Latest booster bonus (estimated)", x + 12, y + 69, com.skyblocktoolkit.ToolkitTheme.palette().muted(), false);
            g.pose().pushMatrix(); g.pose().translate(x + 12, y + 83); g.pose().scale(1.65f, 1.65f);
            g.text(font, last==null?"--":"+" + BoosterStatsManager.formatMoney(last.bonus), 0, 0, com.skyblocktoolkit.ToolkitTheme.palette().accent(), false);
            g.pose().popMatrix();
            String info=last==null?"No booster recorded yet":last.source+" / +"+last.percentage+"% / "+(last.durationSeconds<0?"?":BoosterHistoryScreen.time(last.durationSeconds));
            g.text(font,font.substrByWidth(Component.literal(info),w-24).getString(),x+12,y+105,com.skyblocktoolkit.ToolkitTheme.palette().text(),false);
            int total = BoosterStatsManager.getCoconutTotalEffects(), procs = BoosterStatsManager.getCoconutBoosterProcs();
            row(g,y+124,"Session booster bonus","+"+BoosterStatsManager.formatMoney(BoosterStatsManager.getSessionBonusEarned()),com.skyblocktoolkit.ToolkitTheme.palette().text());
            row(g,y+140,"Coconut boosts",procs+"/"+total,com.skyblocktoolkit.ToolkitTheme.palette().text());
            g.text(font, "Drag the white average text to move it", x + 12, y + 211, com.skyblocktoolkit.ToolkitTheme.palette().muted(), false);
        } else {
            String status = RateTracker.isPreparing() ? "Starting in " + RateTracker.getPrepCountdownSeconds() + "s"
                : RateTracker.isRunning() ? "Running / " + RateTracker.getElapsedSeconds() + "s"
                : RateTracker.isSettling() && !RateTracker.isXpSelected() ? "Settling shop transactions..."
                : RateTracker.isCompleted() ? "Completed / " + RateTracker.getElapsedSeconds() + "s" : "Ready to start";
            boolean xp=RateTracker.isXpSelected(),boosted=com.skyblocktoolkit.ToolkitConfig.get().xpIncludesBoosters;
            g.text(font, status, x + 12, y + 154, com.skyblocktoolkit.ToolkitTheme.palette().text(), false);
            row(g, y + 174, xp?(boosted?"XP rate / hour":"Base XP / hour"):"Base rate / hour", RateTracker.formatDisplayedRate(), com.skyblocktoolkit.ToolkitTheme.palette().accent());
            row(g, y + 194, xp?(boosted?"XP gained":"Base XP gained"):"Base earned", xp?XpTracker.format(RateTracker.getXpEarned(boosted)):BoosterStatsManager.formatMoney(RateTracker.getBaseEarned()), com.skyblocktoolkit.ToolkitTheme.palette().text());
            row(g, y + 212, xp?"XP booster bonus":"Shop trades excluded", xp?XpTracker.format(RateTracker.getXpEarned(true)-RateTracker.getXpEarned(false)):String.valueOf(RateTracker.getExcludedTransactions()), com.skyblocktoolkit.ToolkitTheme.palette().accent());
            g.text(font, xp?"XP bar points; losses are ignored.":"Booster bonus removed from the base rate.", x + 12, y + 234, com.skyblocktoolkit.ToolkitTheme.palette().muted(), false);
        }
        g.pose().popMatrix();g.disableScissor();
        int maxScroll=Math.max(0,cardY+(showTest?246:224)-(footerY-8));
        if(maxScroll>0){int track=footerY-8-(cardY+58),thumb=Math.max(12,track*track/(track+maxScroll));int top=cardY+58+scroll*(track-thumb)/maxScroll;g.fill(x+w-5,top,x+w-3,top+thumb,com.skyblocktoolkit.ToolkitTheme.palette().muted());}
        Minecraft mc = Minecraft.getInstance();
        int bx = BoosterHudOverlay.getBarX(this.width), by = BoosterHudOverlay.getBarY(mc);
        // Keep the draggable preview out of the menu's controls.
        if (by + 17 < y || by - 12 > y + cardH || bx + BoosterHudOverlay.BAR_WIDTH < x || bx > x + w) {
            g.outline(bx - 3, by - 11, BoosterHudOverlay.BAR_WIDTH + 6, BoosterHudOverlay.BAR_HEIGHT + 14, 0xFF455A4F);
            BoosterHudOverlay.renderBar(g, mc, bx, by, "§a+39% Booster: §f0m 42s", 0.65f, false, false);
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if(!showTest&&com.skyblocktoolkit.ToolkitConfig.get().cropEarningsHud&&averageOutsidePanel(mc))BoosterHudOverlay.renderAverage(g,mc);
    }
    private boolean averageOutsidePanel(Minecraft mc){
        String label=BoosterStatsManager.averageLabel();int x=BoosterHudOverlay.averageX(mc,label),y=BoosterHudOverlay.averageY(mc);
        return x+font.width(label)+3<cardX||x-3>cardX+cardW||y+12<cardY||y-3>cardY+cardH;
    }
    private void row(GuiGraphicsExtractor g, int y, String label, String value, int color) {
        g.text(font, label, cardX + 12, y, com.skyblocktoolkit.ToolkitTheme.palette().muted(), false);
        g.text(font, value, cardX + cardW - 12 - font.width(value), y, color, false);
    }

    private boolean isMouseOverBar(double mouseX, double mouseY, int barX, int barY) {
        return mouseX >= barX - 5 && mouseX <= barX + BoosterHudOverlay.BAR_WIDTH + 5
                && mouseY >= barY - 12 && mouseY <= barY + BoosterHudOverlay.BAR_HEIGHT + 12;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean wasHandled) {
        Minecraft mc = Minecraft.getInstance();
        int barX = BoosterHudOverlay.getBarX(this.width);
        int barY = BoosterHudOverlay.getBarY(mc);

        if(!showTest&&com.skyblocktoolkit.ToolkitConfig.get().cropEarningsHud&&averageOutsidePanel(mc)&&event.button()==0){
            String label=BoosterStatsManager.averageLabel();int ax=BoosterHudOverlay.averageX(mc,label),ay=BoosterHudOverlay.averageY(mc);
            if(event.x()>=ax-3&&event.x()<=ax+font.width(label)+3&&event.y()>=ay-3&&event.y()<=ay+12){draggingAverage=true;averageDragX=event.x()-ax;averageDragY=event.y()-ay;return true;}
        }
        if (event.x() >= cardX && event.x() <= cardX + cardW && event.y() >= cardY && event.y() <= cardY + cardH) return super.mouseClicked(event, wasHandled);
        if (event.button() == 0 && isMouseOverBar(event.x(), event.y(), barX, barY)) {
            isDraggingBar = true;
            dragStartMouseX = event.x();
            dragStartMouseY = event.y();
            dragStartOffsetX = ModConfig.getInstance().getOffsetX();
            dragStartOffsetY = ModConfig.getInstance().getOffsetY();
            return true;
        }

        return super.mouseClicked(event, wasHandled);
    }

    @Override
    public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(x>=cardX&&x<=cardX+cardW&&y>=cardY+58&&y<footerY){int previous=scroll;int desired=scroll+(int)(-vertical*8);
            for(int top:new int[]{68,94,120,157,181}){
                int entering=cardY+top-(footerY-28);
                if(desired>previous&&previous<entering&&desired>entering)desired=entering;
            }
            scroll=desired;refresh();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if(draggingAverage){
            ModConfig.getInstance().setAveragePosition(Math.max(2,Math.min(width-font.width(BoosterStatsManager.averageLabel())-2,(int)(event.x()-averageDragX))),Math.max(2,Math.min(height-11,(int)(event.y()-averageDragY))));return true;
        }
        if (isDraggingBar) {
            double deltaX = event.x() - dragStartMouseX;
            double deltaY = event.y() - dragStartMouseY;
            ModConfig.getInstance().setOffset(dragStartOffsetX + (int) deltaX, dragStartOffsetY + (int) deltaY);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if(draggingAverage){draggingAverage=false;ModConfig.getInstance().save();return true;}
        if (isDraggingBar) {
            isDraggingBar = false;
            ModConfig.getInstance().save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        ModConfig.getInstance().save();
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
