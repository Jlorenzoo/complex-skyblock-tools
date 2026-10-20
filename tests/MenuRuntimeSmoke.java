package com.skyblocktoolkit;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import com.moneybooster.*;
import java.nio.file.*;
import org.lwjgl.glfw.GLFW;

/** Offline disposable client: exercise real widgets, input and rendering without a server. */
public final class MenuRuntimeSmoke implements ClientModInitializer {
    int ticks,stage;
    Path output=Path.of(System.getProperty("toolkit.preview"));
    @Override public void onInitializeClient(){
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            try{
                if(mc.gui.overlay()!=null||++ticks<100)return;
                if(ticks%20!=0)return;
                if(stage==0){
                    mc.options.guiScale().set(2);mc.resizeGui();
                    mc.setScreenAndShow(new ToolkitScreen(null,0));stage++;return;
                }
                if(stage<=5){
                    capture(mc,"menu-"+(stage-1));
                    mc.setScreenAndShow(new ToolkitScreen(null,stage));stage++;return;
                }
                if(stage==6){
                    capture(mc,"hud-layout");
                    var screen=(ToolkitScreen)mc.gui.screen();
                    check(screen.children().isEmpty(),"HUD canvas has no toolbar widgets");
                    var cfg=ToolkitConfig.get();boolean old=cfg.boosterHud;
                    int bx=BoosterHudOverlay.getBarX(screen.width),by=BoosterHudOverlay.getBarY(mc);
                    int ex=bx+BoosterHudOverlay.BAR_WIDTH+15<=screen.width?bx+BoosterHudOverlay.BAR_WIDTH+5:Math.max(1,bx-14);
                    screen.mouseClicked(new MouseButtonEvent(ex+5,by-5,new MouseButtonInfo(0,0)),false);
                    check(cfg.boosterHud!=old,"HUD timer eye toggles visibility");
                    screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));
                    check(mc.gui.screen()==screen&&!screen.children().isEmpty(),"HUD Escape returns to modules");
                    BlackoutManager.setActive(true);
                    var blackout=mc.gui.screen();
                    for(int i=0;i<25;i++){blackout.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));blackout.onClose();}
                    check(BlackoutManager.isActive()&&mc.gui.screen()==blackout,"25 Escape presses preserve blackout");
                    blackout.keyPressed(new KeyEvent(GLFW.GLFW_KEY_F6,0,0));
                    check(!BlackoutManager.isActive()&&mc.gui.screen()==screen,"Wake key restores menu");
                    mc.setScreenAndShow(new ToolkitScreen(null,2));
                    var fish=(ToolkitScreen)mc.gui.screen();
                    for(var child:fish.children())if(child instanceof Button b&&b.getMessage().getString().equals("Advanced...")){b.onPress(null);break;}
                    stage++;return;
                }
                if(stage==7){capture(mc,"fishing-advanced");mc.getWindow().setWindowed(1000,800);mc.options.guiScale().set(3);mc.resizeGui();mc.setScreenAndShow(new ToolkitScreen(null,2));stage++;return;}
                if(stage==8){capture(mc,"fishing-compact");stage++;return;}
                if(stage==9){
                    check(Files.exists(output.resolve("screenshots/fishing-compact.png")),"compact screenshot written");
                    mc.setScreenAndShow(new ToolkitScreen(null,8));stage++;return;
                }
                if(stage==10){
                    capture(mc,"hud-elements");
                    var cfg=ToolkitConfig.get();cfg.cropEarningsHud=false;cfg.fishEarningsHud=false;
                    var screen=mc.gui.screen();
                    for(var child:screen.children())if(child instanceof Button b&&b.getMessage().getString().startsWith("Fishing earnings")){b.onPress(null);break;}
                    check(cfg.fishEarningsHud&&!cfg.cropEarningsHud,"Fish toggle leaves crop hidden");
                    for(var child:screen.children())if(child instanceof Button b&&b.getMessage().getString().startsWith("Crop earnings")){b.onPress(null);break;}
                    check(cfg.fishEarningsHud&&cfg.cropEarningsHud,"Crop and fish can both be visible");
                    cfg.fishHudX=91;cfg.fishHudY=87;cfg.save();
                    var ctor=ToolkitConfig.class.getDeclaredConstructor();ctor.setAccessible(true);var saved=ctor.newInstance();
                    check(saved.cropEarningsHud&&saved.fishEarningsHud&&saved.fishHudX==91&&saved.fishHudY==87,"Independent HUD settings persist");
                    mc.setScreenAndShow(new ToolkitScreen(null,5));screen=mc.gui.screen();
                    String label=BoosterStatsManager.averageLabel();int ax=BoosterHudOverlay.averageX(mc,label),ay=BoosterHudOverlay.averageY(mc);
                    screen.mouseClicked(new MouseButtonEvent(ax+10,ay+4,new MouseButtonInfo(0,0)),false);
                    screen.mouseDragged(new MouseButtonEvent(ax+40,ay+28,new MouseButtonInfo(0,0)),30,24);
                    screen.mouseReleased(new MouseButtonEvent(ax+40,ay+28,new MouseButtonInfo(0,0)));
                    check(cfg.fishHudX==91&&cfg.fishHudY==87,"Dragging crops leaves fish position unchanged");
                    check(ModConfig.getInstance().averageX()==ax+30,"Crop HUD drag moves its own position");
                    stage++;return;
                }
                if(stage==11){
                    check(Files.exists(output.resolve("screenshots/hud-elements.png")),"HUD mini menu screenshot written");
                    Files.writeString(output.resolve("PASS.txt"),"PASS: real rendered module menus and HUD mini menu; independent crop/fish toggles, positions and persistence; HUD eye; toolbar-free canvas; Escape return; 25 blackout Escape presses; F6 wake\n");
                    System.out.println("PASS: menu runtime interactions and screenshots");System.exit(0);
                }

            }catch(Throwable e){e.printStackTrace();System.exit(1);}
        });
    }
    void capture(net.minecraft.client.Minecraft mc,String name){
        Screenshot.grab(output.toFile(),name+".png",mc.gameRenderer.mainRenderTarget(),1,msg->{});
    }
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
}
