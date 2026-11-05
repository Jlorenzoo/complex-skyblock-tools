package com.skyblocktoolkit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;

/** Reads only received chat and the open server menu. Never sends commands or clicks. */
public final class TreasureChestTracker {
    public static final TreasureChestState STATE=new TreasureChestState();
    private static Object connection,menu;
    private static long lastSeconds=-1;
    private static int ticks;
    public static void connection(Object current){if(current!=connection){connection=current;STATE.reset();menu=null;lastSeconds=-1;}}
    public static void chat(String text){STATE.accept(text,System.currentTimeMillis());}
    public static void tick(Minecraft mc){
        connection(mc.getConnection());
        if(++ticks%10!=0)return;
        if(mc.player==null||!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)||!screen.getTitle().getString().toLowerCase(java.util.Locale.ROOT).contains("treasure")){menu=null;lastSeconds=-1;return;}
        if(menu!=screen){menu=screen;lastSeconds=-1;}
        readMenu(mc.player.containerMenu.slots.stream().filter(slot->slot.container!=mc.player.getInventory()).map(slot->slot.getItem()).toList(),System.currentTimeMillis());
    }
    static void readMenu(Iterable<net.minecraft.world.item.ItemStack> stacks,long now){
        for(var stack:stacks){
            if(!TreasureChestState.clean(stack.getHoverName().getString()).equalsIgnoreCase("Next Chest Spawn"))continue;
            var lore=stack.get(DataComponents.LORE);if(lore==null)continue;
            for(var line:lore.lines()){
                long s=TreasureChestState.menuSeconds(line.getString());
                if(s>=0&&s!=lastSeconds){STATE.sync(s,now);lastSeconds=s;}
            }
        }
    }
    public static java.util.List<String> lines(boolean preview){
        var c=ToolkitConfig.get();
        return preview?java.util.List.of("Treasure · 0:55:37","Mythic · Realm · 0–1/2 observed","Rare · Tomb · 0/2 observed","203 shards · unassigned","Pool: Epic / Legendary / Mythic"):
            STATE.lines(System.currentTimeMillis(),c.treasureDetailsSeconds,c.treasureShowChests,c.treasureInferOpens,c.treasureLastThirtySeconds);
    }
    public static int width(Minecraft mc,boolean preview){return (int)Math.ceil(Math.min(Math.min(300,(mc.getWindow().getGuiScaledWidth()-4)/ToolkitConfig.get().treasureScale),lines(preview).stream().mapToInt(mc.font::width).max().orElse(180))*ToolkitConfig.get().treasureScale);}
    public static int height(boolean preview){return (int)Math.ceil(lines(preview).size()*12*ToolkitConfig.get().treasureScale);}
    public static int x(Minecraft mc,boolean preview){var c=ToolkitConfig.get();return Math.clamp(c.treasureHudX<0?mc.getWindow().getGuiScaledWidth()-width(mc,preview)-12:c.treasureHudX,2,Math.max(2,mc.getWindow().getGuiScaledWidth()-width(mc,preview)-2));}
    public static int y(Minecraft mc,boolean preview){return Math.clamp(ToolkitConfig.get().treasureHudY,2,Math.max(2,mc.getWindow().getGuiScaledHeight()-height(preview)-2));}
    public static void render(GuiGraphicsExtractor g,Minecraft mc,boolean preview){
        var rows=lines(preview);if(rows.isEmpty())return;
        var c=ToolkitConfig.get();int x=x(mc,preview),y=y(mc,preview);float scale=(float)c.treasureScale;
        int max=(int)(Math.min(300,(mc.getWindow().getGuiScaledWidth()-4)/scale));
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);
        int w=Math.min(max,rows.stream().mapToInt(mc.font::width).max().orElse(180));
        if(c.treasureBackground)ToolkitTheme.rounded(g,-2,-2,w+4,rows.size()*12+2,3,ToolkitTheme.ALERT_SURFACE);
        for(int i=0;i<rows.size();i++)g.text(mc.font,mc.font.plainSubstrByWidth(rows.get(i),w),0,i*12,ToolkitPalette.DARK.text(),true);
        g.pose().popMatrix();
    }
}
