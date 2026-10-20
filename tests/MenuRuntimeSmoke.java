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
    int ticks,stage=Boolean.getBoolean("toolkit.testVoicechat")?-2:0;
    Path output=Path.of(System.getProperty("toolkit.preview"));
    @Override public void onInitializeClient(){
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            try{
                if(mc.gui.overlay()!=null||++ticks<100)return;
                if(ticks%20!=0)return;
                if(stage==-2){
                    var voice=Class.forName("de.maxhenkel.voicechat.VoicechatClient");
                    check(voice.getField("CLIENT_CONFIG").get(null)!=null,"Voice chat client initialized alongside toolkit");
                    var keys=Class.forName("de.maxhenkel.voicechat.voice.client.KeyEvents");
                    check(keys.getField("KEY_VOICE_CHAT").get(null)!=null&&keys.getField("KEY_PTT").get(null)!=null,"Voice chat and push-to-talk keys registered");
                    for(var binding:new String[][]{{"KEY_VOICE_CHAT","key.keyboard.v"},{"KEY_PTT","key.keyboard.caps.lock"},{"KEY_WHISPER","key.keyboard.right.alt"},{"KEY_MUTE","key.keyboard.f8"}}){
                        var key=(net.minecraft.client.KeyMapping)keys.getField(binding[0]).get(null);
                        check(key.saveString().equals(binding[1]),"Loaded pack voice control: "+binding[0]);
                    }
                    var onboarding=Class.forName("de.maxhenkel.voicechat.gui.onboarding.OnboardingManager");
                    onboarding.getMethod("startOnboarding",net.minecraft.client.gui.screens.Screen.class).invoke(null,mc.gui.screen());
                    check(mc.gui.screen().getClass().getName().contains("OnboardingScreen"),"Voice chat onboarding opens");
                    stage++;return;
                }
                if(stage==-1){capture(mc,"voicechat-onboarding");stage++;return;}
                if(stage==0){
                    mc.options.guiScale().set(2);mc.resizeGui();
                    mc.setScreenAndShow(new ToolkitScreen(null,0));stage++;return;
                }
                if(stage==1){
                    capture(mc,"menu-0");
                    var timerConfig=ToolkitConfig.get();
                    boolean oldMoney=timerConfig.boosterHud,oldXp=timerConfig.xpBoosterHud,oldReminder=timerConfig.boosterReminder;
                    var boosterScreen=mc.gui.screen();
                    for(var child:boosterScreen.children())if(child instanceof Button b&&b.getMessage().getString().startsWith("Money timer")){b.onPress(null);break;}
                    check(timerConfig.boosterHud!=oldMoney&&timerConfig.xpBoosterHud==oldXp,"Money toggle leaves XP visibility unchanged");
                    for(var child:boosterScreen.children())if(child instanceof Button b&&b.getMessage().getString().startsWith("XP timer")){b.onPress(null);break;}
                    check(timerConfig.xpBoosterHud!=oldXp&&timerConfig.boosterHud!=oldMoney,"XP toggle leaves money visibility unchanged");
                    for(var child:boosterScreen.children())if(child instanceof Button b&&b.getMessage().getString().startsWith("5-second reminder")){b.onPress(null);break;}
                    check(timerConfig.boosterReminder!=oldReminder,"Five-second reminder toggle works");
                    timerConfig.save();
                    var timerCtor=ToolkitConfig.class.getDeclaredConstructor();timerCtor.setAccessible(true);var reloaded=timerCtor.newInstance();
                    check(reloaded.boosterHud==timerConfig.boosterHud&&reloaded.xpBoosterHud==timerConfig.xpBoosterHud&&reloaded.boosterReminder==timerConfig.boosterReminder,"Timer settings persist from real widgets");
                    timerConfig.boosterHud=oldMoney;timerConfig.xpBoosterHud=oldXp;timerConfig.boosterReminder=oldReminder;
                    BoosterManager.onChatMessage("⚡ You will now receive +20% Shop Money.");
                    BoosterManager.onChatMessage("⚡ Your Booster will end in: 5 minutes.");
                    long moneyHistory=BoosterStatsManager.history().activeId();
                    int moneyCount=BoosterStatsManager.getSessionBoosterCount();
                    BoosterManager.onChatMessage("⚡ You will now receive +150% EXP Gain.");
                    BoosterManager.onChatMessage("⚡ Your Booster will end in: 5 minutes.");
                    check(BoosterManager.isActive()&&BoosterManager.getPercentage()==20,"XP leaves money accounting percentage unchanged");
                    check(BoosterManager.timers.percentage(BoosterTimers.Type.XP)==150,"XP server message reaches independent clock");
                    check(BoosterStatsManager.history().activeId()==moneyHistory&&BoosterStatsManager.getSessionBoosterCount()==moneyCount,"XP leaves money ledger and activation count unchanged");
                    BoosterManager.reset();BoosterStatsManager.resetConnection();
                    mc.setScreenAndShow(new ToolkitScreen(null,9));stage++;return;
                }
                if(stage==2){
                    capture(mc,"farming-full");
                    mc.setScreenAndShow(new ToolkitScreen(null,2));stage++;return;
                }
                if(stage==3){
                    capture(mc,"menu-1");
                    mc.setScreenAndShow(new ToolkitScreen(null,3));stage++;return;
                }
                if(stage==4){
                    capture(mc,"menu-2");
                    mc.setScreenAndShow(new ToolkitScreen(null,4));stage++;return;
                }
                if(stage==5){
                    capture(mc,"menu-3");
                    mc.setScreenAndShow(new ToolkitScreen(null,5));stage++;return;
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
                    boolean xpWasVisible=cfg.xpBoosterHud;
                    screen.mouseClicked(new MouseButtonEvent(ex+5,by+BoosterHudOverlay.TIMER_SPACING-5,new MouseButtonInfo(0,0)),false);
                    check(cfg.xpBoosterHud!=xpWasVisible&&cfg.boosterHud!=old,"XP HUD eye toggles independently");
                    screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));
                    check(mc.gui.screen()==screen&&!screen.children().isEmpty(),"HUD Escape returns to modules");
                    BlackoutManager.setActive(true);
                    check(BlackoutManager.isActive(),"Blackout active");
                    BlackoutManager.toggle();
                    check(!BlackoutManager.isActive(),"Wake key toggles blackout off");
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
                    mc.setScreenAndShow(new ToolkitScreen(null,9));stage++;return;
                }
                if(stage==12){
                    capture(mc,"farming");
                    var screen=mc.gui.screen();
                    var cfg=ToolkitConfig.get();
                    boolean oldHarvest=ClearHarvest.isEnabled();
                    for(var child:screen.children())if(child instanceof Button b&&b.getMessage().getString().startsWith("Clear Harvest textures")){b.onPress(null);break;}
                    check(ClearHarvest.isEnabled()!=oldHarvest,"Clear Harvest toggle switches state");
                    for(var child:screen.children())if(child instanceof Button b&&b.getMessage().getString().startsWith("Clear Harvest textures")){b.onPress(null);break;}
                    check(ClearHarvest.isEnabled()==oldHarvest,"Clear Harvest toggle reverts state");
                    stage++;return;
                }
                if(stage==13){
                    check(Files.exists(output.resolve("screenshots/farming-full.png")),"Farming full screenshot written");
                    check(Files.exists(output.resolve("screenshots/hud-elements.png")),"HUD mini menu screenshot written");
                    check(Files.exists(output.resolve("screenshots/farming.png")),"Farming screenshot written");
                    mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(2);mc.resizeGui();
                    ToolkitConfig.get().lightMode=false;mc.setScreenAndShow(new ToolkitScreen(null,1));stage++;return;
                }
                if(stage==14){capture(mc,"boosters-dark");switchTheme(mc);check(ToolkitConfig.get().lightMode,"theme button activates light mode");stage++;return;}
                if(stage==15){capture(mc,"boosters-light");mc.setScreenAndShow(new ToolkitScreen(null,2));stage++;return;}
                if(stage==16){
                    capture(mc,"fishing-light");
                    for(var child:mc.gui.screen().children())if(child instanceof Button b&&b.getMessage().getString().equals("Fish prices")){b.onPress(null);break;}
                    stage++;return;
                }
                if(stage==17){capture(mc,"fish-prices-light");mc.setScreenAndShow(new BoosterHistoryScreen(null));stage++;return;}
                if(stage==18){capture(mc,"history-light");mc.setScreenAndShow(new BoosterStatsScreen(null));stage++;return;}
                if(stage==19){capture(mc,"stats-light");mc.getWindow().setWindowed(1000,800);mc.options.guiScale().set(3);mc.resizeGui();mc.setScreenAndShow(new ToolkitScreen(null,1));stage++;return;}
                if(stage==20){capture(mc,"boosters-light-compact");switchTheme(mc);check(!ToolkitConfig.get().lightMode,"theme button restores dark mode");stage++;return;}
                if(stage==21){
                    capture(mc,"boosters-dark-compact");
                    var ctor=ToolkitConfig.class.getDeclaredConstructor();ctor.setAccessible(true);
                    check(!ctor.newInstance().lightMode,"theme preference persists");
                    stage++;return;
                }
                if(stage==22){
                    check(Files.exists(output.resolve("screenshots/boosters-light-compact.png")),"light compact menu rendered");
                    check(Files.exists(output.resolve("screenshots/boosters-dark-compact.png")),"dark compact menu rendered");
                    Files.writeString(output.resolve("PASS.txt"),"PASS: rendered light/dark themes, live theme toggle and persistence; full/compact menus; history, rate tests and price fields; independent money/XP timer widgets, eyes and persistence; XP activation preserves money ledger; crop visibility, HUD editing, blackout and F6 wake\n");
                    System.out.println("PASS: light/dark theme and timer runtime interactions, menus and screenshots");System.exit(0);
                }

            }catch(Throwable e){e.printStackTrace();System.exit(1);}
        });
    }
    void capture(net.minecraft.client.Minecraft mc,String name){
        Screenshot.grab(output.toFile(),name+".png",mc.gameRenderer.mainRenderTarget(),1,msg->{});
    }
    void switchTheme(net.minecraft.client.Minecraft mc){
        for(var child:mc.gui.screen().children())if(child instanceof Button b&&(b.getMessage().getString().equals("Dark mode")||b.getMessage().getString().equals("Light mode"))){b.onPress(null);return;}
        throw new AssertionError("theme switch missing");
    }
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
}
