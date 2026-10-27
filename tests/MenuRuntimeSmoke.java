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
    private static void checkBlackoutScroll(net.minecraft.client.Minecraft mc) throws Exception {
        var previous = mc.gui.screen();
        var cfg = ToolkitConfig.get();
        double opacity = cfg.blackoutOpacity;
        boolean hint = cfg.blackoutHint;
        var hook = java.util.Arrays.stream(net.minecraft.client.MouseHandler.class.getDeclaredMethods())
                .filter(m -> m.getName().contains("toolkit$blackoutScroll")).findFirst().orElseThrow();
        hook.setAccessible(true);
        ToolkitMod.blackoutKey.setDown(true);
        var blocked = new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("scroll", true);
        hook.invoke(mc.mouseHandler, mc.getWindow().handle(), 0.0, -1.0, blocked);
        check(!blocked.isCancelled() && cfg.blackoutOpacity == opacity, "Menu scrolling remains unchanged");
        // A disconnected client turns setScreen(null) into the title screen.
        // Clear the field only for this synchronous offline input check.
        var screenField = java.util.Arrays.stream(net.minecraft.client.Minecraft.class.getDeclaredFields())
                .filter(f -> f.getType() == net.minecraft.client.gui.screens.Screen.class).findFirst().orElseThrow();
        screenField.setAccessible(true);screenField.set(mc, null);
        cfg.blackoutOpacity = .5;
        BlackoutManager.setActive(false);
        BlackoutManager.updateKey(true, true, true);
        var consumed = new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("scroll", true);
        hook.invoke(mc.mouseHandler, mc.getWindow().handle(), 0.0, -1.0, consumed);
        check(consumed.isCancelled() && cfg.blackoutOpacity == .45 && BlackoutManager.isActive(), "Held blackout scroll clears by 5% and consumes hotbar scroll");
        BlackoutManager.adjustOpacity(2);
        check(cfg.blackoutOpacity == .55, "Scroll up darkens");
        BlackoutManager.adjustOpacity(100);
        check(cfg.blackoutOpacity == 1, "Opacity clamps at full black");
        BlackoutManager.adjustOpacity(-100);
        check(cfg.blackoutOpacity == 0, "Opacity clamps at transparent");
        check(!BlackoutManager.adjustOpacity(0) && !BlackoutManager.adjustOpacity(Double.NaN), "Ignore empty and invalid scroll");
        ToolkitMod.blackoutKey.setDown(false);
        BlackoutManager.updateKey(false, false, true);
        check(BlackoutManager.isActive(), "Release after scrolling keeps blackout active");
        var ctor = ToolkitConfig.class.getDeclaredConstructor();ctor.setAccessible(true);
        check(ctor.newInstance().blackoutOpacity == 0, "Gesture saves opacity on release");
        BlackoutManager.updateKey(true, true, true);
        BlackoutManager.updateKey(false, false, true);
        check(!BlackoutManager.isActive(), "Tap still toggles off");
        BlackoutManager.updateKey(false, true, true);
        check(BlackoutManager.isActive(), "Fast tap between ticks toggles on");
        BlackoutManager.updateKey(true, true, true);
        BlackoutManager.updateKey(false, false, false);
        BlackoutManager.updateKey(false, false, true);
        check(BlackoutManager.isActive(), "Opening a menu cancels a pending tap");
        cfg.blackoutOpacity = opacity;cfg.blackoutHint = hint;cfg.save();
        BlackoutManager.setActive(false);
        mc.setScreenAndShow(previous);
        Files.writeString(outputPath().resolve("blackout-scroll.txt"), "PASS: remapped mouse mixin, menu exclusion, hotbar consumption, 5% adjustment, limits, persistence, tap and gesture release\n");
    }

    private static Path outputPath() { return Path.of(System.getProperty("toolkit.preview")); }
    @Override public void onInitializeClient(){
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            try{
                if(mc.gui.overlay()!=null||++ticks<100)return;
                if(ticks%Integer.getInteger("toolkit.menuTickInterval",20)!=0)return;
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
                    if(Boolean.getBoolean("toolkit.testStartupConnect")){
                        check("startup-test.invalid:25566".equals(System.getProperty("toolkit.startupProbeAddress")),"Startup reaches vanilla connection with the configured host and port");
                        check("1".equals(System.getProperty("toolkit.startupProbeCount")),"Startup makes exactly one connection attempt");
                        Files.writeString(output.resolve("startup-connection.txt"),"PASS: one configured startup connection reached the real vanilla entry point; network intercepted by smoke mod\n");
                    }
                    String migration=System.getProperty("toolkit.migrationOptions");
                    if(migration!=null){
                        var expected=new java.util.HashMap<String,String>();
                        for(String line:Files.readAllLines(Path.of(migration))){int colon=line.indexOf(':');if(colon>0)expected.put(line.substring(0,colon),line.substring(colon+1));}
                        int checked=0;
                        for(var key:mc.options.keyMappings){
                            String saved=expected.get("key_"+key.getName());
                            if(saved!=null){check(saved.equals(key.saveString()),"Migrated control loads: "+key.getName());checked++;}
                        }
                        check(checked>40,"Migrated vanilla and mod controls were checked");
                        mc.options.save();
                        Files.copy(mc.gameDirectory.toPath().resolve("options.txt"),output.resolve("loaded-options.txt"),StandardCopyOption.REPLACE_EXISTING);
                        Files.writeString(output.resolve("migration-controls.txt"),"PASS: "+checked+" migrated controls loaded unchanged\n");
                    }
                    check(((com.skyblocktoolkit.mixin.BossHealthOverlayAccessor)mc.gui.hud.getBossOverlay()).toolkit$getEvents().isEmpty(),"Production boss-bar accessor is remapped");
                    check(((com.skyblocktoolkit.mixin.ChatComponentAccessor)mc.gui.hud.getChat()).toolkit$getTrimmedMessages().isEmpty(),"Production chat accessor is remapped");
                    check(java.util.Arrays.stream(net.minecraft.client.gui.screens.ChatScreen.class.getDeclaredMethods()).anyMatch(m->m.getName().contains("toolkit$onMouseClicked")),"Production chat selection mixin applies");
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
                    check(reloaded.startupServer.equals(timerConfig.startupServer),"Startup server survives other settings saves");
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
                    checkBlackoutScroll(mc);
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
                    mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(2);mc.resizeGui();
                    mc.setScreenAndShow(new BoosterStatsScreen(null,true));
                    press(mc,"Minecraft XP");
                    check(RateTracker.isXpSelected(),"XP measurement selector works");
                    boolean wasBoosted=ToolkitConfig.get().xpIncludesBoosters;
                    press(mc,"Include XP boosters");
                    check(ToolkitConfig.get().xpIncludesBoosters!=wasBoosted,"XP booster toggle works");
                    var ctor=ToolkitConfig.class.getDeclaredConstructor();ctor.setAccessible(true);var saved=ctor.newInstance();
                    check(saved.rateTestXp&&saved.xpIncludesBoosters==ToolkitConfig.get().xpIncludesBoosters,"XP selection and booster preference persist");
                    stage++;return;
                }
                if(stage==23){capture(mc,"xp-rates-dark");ToolkitConfig.get().lightMode=true;mc.setScreenAndShow(new BoosterStatsScreen(null,true));stage++;return;}
                if(stage==24){capture(mc,"xp-rates-light");mc.getWindow().setWindowed(1000,800);mc.options.guiScale().set(3);mc.resizeGui();mc.setScreenAndShow(new BoosterStatsScreen(null,true));stage++;return;}
                if(stage==25){
                    capture(mc,"xp-rates-light-compact");
                    var screen=mc.gui.screen();screen.mouseScrolled(screen.width/2,screen.height/2,0,-10);
                    stage++;return;
                }
                if(stage==26){capture(mc,"xp-rates-light-compact-scrolled");ToolkitConfig.get().lightMode=false;mc.setScreenAndShow(new BoosterStatsScreen(null,true));stage++;return;}
                if(stage==27){capture(mc,"xp-rates-dark-compact");mc.gui.screen().mouseScrolled(mc.gui.screen().width/2,mc.gui.screen().height/2,0,-10);stage++;return;}
                if(stage==28){
                    capture(mc,"xp-rates-dark-compact-scrolled");
                    var cfg=ToolkitConfig.get();cfg.xpHudX=20;cfg.xpHudY=110;cfg.xpEarningsHud=true;
                    mc.setScreenAndShow(new ToolkitScreen(null,5));var screen=mc.gui.screen();
                    int fishX=cfg.fishHudX,fishY=cfg.fishHudY;
                    screen.mouseClicked(new MouseButtonEvent(25,114,new MouseButtonInfo(0,0)),false);
                    screen.mouseDragged(new MouseButtonEvent(45,134,new MouseButtonInfo(0,0)),20,20);
                    screen.mouseReleased(new MouseButtonEvent(45,134,new MouseButtonInfo(0,0)));
                    check(cfg.xpHudX==40&&cfg.xpHudY==130,"XP HUD drags independently");
                    check(cfg.fishHudX==fishX&&cfg.fishHudY==fishY,"XP drag preserves fishing HUD");
                    int eye=cfg.xpHudX+mc.font.width(XpTracker.averageLabel())+5;
                    screen.mouseClicked(new MouseButtonEvent(eye+2,cfg.xpHudY,new MouseButtonInfo(0,0)),false);
                    check(!cfg.xpEarningsHud,"XP HUD eye toggles independently");
                    var ctor=ToolkitConfig.class.getDeclaredConstructor();ctor.setAccessible(true);var saved=ctor.newInstance();
                    check(!saved.xpEarningsHud&&saved.xpHudX==40&&saved.xpHudY==130,"XP HUD position/visibility persist");
                    mc.setScreenAndShow(new BoosterStatsScreen(null,true));press(mc,"Money");check(!RateTracker.isXpSelected(),"Money rate selection remains available");
                    stage++;return;
                }
                if(stage==29){
                    capture(mc,"money-rates-dark-compact");
                    mc.setScreenAndShow(new ToolkitScreen(null,11));mc.gui.screen().mouseScrolled(mc.gui.screen().width/2,mc.gui.screen().height/2,0,-10);stage++;return;
                }
                if(stage==30){capture(mc,"xp-hud-settings-dark-compact");switchTheme(mc);stage++;return;}
                if(stage==31){capture(mc,"xp-hud-settings-light-compact");mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(2);mc.resizeGui();mc.setScreenAndShow(new ToolkitScreen(null,11));mc.gui.screen().mouseScrolled(mc.gui.screen().width/2,mc.gui.screen().height/2,0,-10);stage++;return;}
                if(stage==32){capture(mc,"xp-hud-settings-light");switchTheme(mc);stage++;return;}
                if(stage==33){
                    capture(mc,"xp-hud-settings-dark");
                    stage++;return;
                }
                if(stage==34){
                    check(Files.exists(output.resolve("screenshots/xp-hud-settings-dark.png")),"XP HUD settings screenshot written");
                    BoosterManager.onChatMessage("You have gained the effect of a 61% Money Booster for 60 seconds!");
                    long expiredAt=System.currentTimeMillis()-61000;
                    BoosterManager.timers.start(BoosterTimers.Type.MONEY,61,60,expiredAt);
                    BoosterManager.timers.observe(BoosterTimers.Type.MONEY,new BoosterSidebar.State(true,61,-1,false),System.currentTimeMillis());
                    BoosterStatsManager.tick();
                    check(!BoosterManager.isActive()&&BoosterStatsManager.history().activeId()<0,"Expired coconut stops money accounting and history despite lingering sidebar");
                    mc.setScreenAndShow(new ToolkitScreen(null,10));
                    if(System.getProperty("toolkit.migrationOptions")==null)
                        check(ToolkitMod.sellAllKey.isUnbound(),"Sell all starts unbound on a fresh profile");
                    press(mc,"Sell all key:");mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_G,0,0));
                    check(ToolkitMod.sellAllKey.saveString().equals("key.keyboard.g"),"Sell all key assigned from toolkit");
                    check(Files.readString(net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("options.txt")).contains("key_key.skyblocktoolkit.sellall:key.keyboard.g"),"Sell all key persisted to Minecraft options");
                    press(mc,"Sell all key:");mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));
                    check(ToolkitMod.sellAllKey.saveString().equals("key.keyboard.g"),"Escape cancels binding");
                    press(mc,"Sell all key:");mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_DELETE,0,0));
                    check(ToolkitMod.sellAllKey.isUnbound(),"Delete clears sell all binding");
                    press(mc,"Sell all key:");mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_G,0,0));
                    net.minecraft.client.KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G));
                    search(mc,"coconut sound");press(mc,"Boosters / Coconut sound");
                    boolean oldSound=ToolkitConfig.get().coconutSoundEnabled;press(mc,"Coconut sound");
                    check(ToolkitConfig.get().coconutSoundEnabled!=oldSound,"Coconut alert toggle");
                    var threshold=mc.gui.screen().children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox).map(c->(net.minecraft.client.gui.components.EditBox)c).filter(c->c.getMessage().getString().startsWith("Minimum coconut")).findFirst().orElseThrow();
                    threshold.setValue("61");check(ToolkitConfig.get().coconutSoundThreshold==61,"Threshold accepts exact percentage");
                    for(String invalid:new String[]{"", "-1", "100001", "abc"}){
                        threshold.setValue(invalid);check(ToolkitConfig.get().coconutSoundThreshold==61,"Invalid threshold preserves last valid value");
                    }
                    threshold.setValue("61");press(mc,"Preview sound");
                    var ctor=ToolkitConfig.class.getDeclaredConstructor();ctor.setAccessible(true);var saved=ctor.newInstance();
                    check(saved.coconutSoundThreshold==61&&saved.coconutSoundEnabled==ToolkitConfig.get().coconutSoundEnabled,"Coconut preferences persist");
                    stage++;return;
                }
                if(stage==35){
                    check(!ToolkitMod.sellAllKey.consumeClick(),"Sell all click is drained safely outside gameplay");
                    capture(mc,"coconut-settings-dark");switchTheme(mc);stage++;return;
                }
                if(stage==36){capture(mc,"coconut-settings-light");mc.getWindow().setWindowed(1000,800);mc.options.guiScale().set(3);mc.resizeGui();mc.setScreenAndShow(new ToolkitScreen(null,1));scrollBottom(mc);stage++;return;}
                if(stage==37){capture(mc,"coconut-settings-light-compact");switchTheme(mc);stage++;return;}
                if(stage==38){capture(mc,"coconut-settings-dark-compact");mc.setScreenAndShow(new ToolkitScreen(null,9));search(mc,"sell all");stage++;return;}
                if(stage==39){capture(mc,"sellall-search-dark-compact");switchTheme(mc);stage++;return;}
                if(stage==40){capture(mc,"sellall-search-light-compact");stage++;return;}
                if(stage==41){
                    mc.setScreenAndShow(new ToolkitScreen(null,1));
                    search(mc,"  MiNiMuM   boost ");
                    check(mc.gui.screen().children().stream().anyMatch(c->c instanceof Button b&&b.getMessage().getString().equals("Boosters / Minimum boost (%)")),"Search matches multiple terms across labels and keywords");
                    stage++;return;
                }
                if(stage==42){capture(mc,"search-light-compact");switchTheme(mc);stage++;return;}
                if(stage==43){
                    capture(mc,"search-dark-compact");press(mc,"Boosters / Minimum boost (%)");
                    check(searchField(mc).getValue().isEmpty(),"Opening a result clears search");
                    search(mc,"coconut sound");press(mc,"Boosters / Coconut sound");press(mc,"Coconut sound");
                    mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_F,0,GLFW.GLFW_MOD_CONTROL));
                    check(searchField(mc).isFocused(),"Ctrl+F focuses search");
                    search(mc,"no-such-setting");stage++;return;
                }
                if(stage==44){
                    capture(mc,"search-empty-dark-compact");
                    check(mc.gui.screen().children().stream().noneMatch(c->c instanceof Button b&&b.getMessage().getString().contains(" / ")),"No-match search has no stale results");
                    var sameScreen=mc.gui.screen();sameScreen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));
                    check(mc.gui.screen()==sameScreen&&searchField(mc).getValue().isEmpty(),"Escape clears search without closing menu");
                    search(mc,"coconut sound");press(mc,"Boosters / Coconut sound");press(mc,"Coconut sound");
                    mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(2);mc.resizeGui();
                    search(mc,"hud");stage++;return;
                }
                if(stage==45){capture(mc,"search-dark");switchTheme(mc);stage++;return;}
                if(stage==46){
                    capture(mc,"search-light");
                    for(int category:new int[]{1,9,11,2,3,4,7,10}){
                        mc.setScreenAndShow(new ToolkitScreen(null,category));
                        check(mc.gui.screen().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().startsWith("Sell all key:")).count()==(category==10?1:0),"Single sell-all selector in General");
                        search(mc,"sell all");
                        check(mc.gui.screen().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().contains(" / ")).count()==1,"Sell all returns one global setting from every category");
                        press(mc,"General / Sell all key");
                        check(mc.gui.screen().children().stream().anyMatch(c->c instanceof AbstractWidget w&&w.visible&&ToolkitScreen.searchTarget(w,"Sell all key")),"Sell-all search reveals binding");
                    }
                    // Every indexed destination must reveal its actual setting.
                    for(var entry:ToolkitSearch.ENTRIES){
                        search(mc,entry.title());press(mc,entry.title());
                        check(mc.gui.screen().children().stream().anyMatch(c->c instanceof AbstractWidget w&&w.visible&&ToolkitScreen.searchTarget(w,entry.setting())),"Search destination shows setting: "+entry.title());
                        assertControls(mc);
                    }
                    mc.setScreenAndShow(new ToolkitScreen(null,10));stage++;return;
                }
                if(stage==47){capture(mc,"general-light");switchTheme(mc);stage++;return;}
                if(stage==48){
                    capture(mc,"general-dark");
                    mc.getWindow().setWindowed(1000,800);mc.options.guiScale().set(3);mc.resizeGui();
                    for(int page:new int[]{1,2,3,4,7,9,10,11}){
                        mc.setScreenAndShow(new ToolkitScreen(null,page));assertControls(mc);scrollBottom(mc);assertControls(mc);
                    }
                    mc.setScreenAndShow(new ToolkitScreen(null,10));stage++;return;
                }
                if(stage==49){capture(mc,"general-dark-compact");switchTheme(mc);stage++;return;}
                if(stage==50){capture(mc,"general-light-compact");stage++;return;}
                if(stage==51){
                    mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(2);mc.resizeGui();
                    mc.setScreenAndShow(new ToolkitScreen(null,4));
                    var slider=mc.gui.screen().children().stream().filter(c->c instanceof net.minecraft.client.gui.components.AbstractSliderButton)
                        .map(c->(net.minecraft.client.gui.components.AbstractSliderButton)c).filter(c->c.getMessage().getString().startsWith("Volume:")).findFirst().orElseThrow();
                    int start=slider.getX()+4,end=slider.getRight()-44,cy=slider.getY()+10;
                    var screen=mc.gui.screen();
                    screen.mouseClicked(new MouseButtonEvent((start+end)/2,cy,new MouseButtonInfo(0,0)),false);
                    check(Math.abs(ToolkitConfig.get().ahSoundVolume-.5)<.02,"Compact slider midpoint matches click location");
                    screen.mouseDragged(new MouseButtonEvent(end,cy,new MouseButtonInfo(0,0)),50,0);
                    screen.mouseReleased(new MouseButtonEvent(end,cy,new MouseButtonInfo(0,0)));
                    check(ToolkitConfig.get().ahSoundVolume==1,"Compact slider drag reaches maximum");
                    assertControls(mc);
                    ToolkitConfig.get().lightMode=false;mc.setScreenAndShow(new ToolkitScreen(null,1));stage++;return;
                }
                if(stage>=52&&stage<80){
                    int index=stage-52;int[] pages={1,9,2,3,4,7,10};
                    String theme=(index/7)%2==0?"dark":"light";
                    capture(mc,"clean-"+pages[index%7]+"-"+theme+(index>=14?"-compact":""));
                    assertControls(mc);
                    int next=index+1;
                    if(next<28){
                        ToolkitConfig.get().lightMode=(next/7)%2==1;
                        if(next==14){mc.getWindow().setWindowed(1000,800);mc.options.guiScale().set(3);mc.resizeGui();}
                        mc.setScreenAndShow(new ToolkitScreen(null,pages[next%7]));
                    }
                    stage++;return;
                }
                if(stage>=80&&stage<96){
                    int index=stage-80;
                    int target=index%4<2?9:11;
                    ToolkitConfig.get().lightMode=(index/4)%2==1;
                    if(index==0||index==8){mc.getWindow().setWindowed(index<8?1280:1000,800);mc.options.guiScale().set(index<8?2:3);mc.resizeGui();}
                    mc.setScreenAndShow(new ToolkitScreen(null,target));
                    if(index%2==1){
                        scrollBottom(mc);press(mc,"Advanced...");
                        var scrollField=ToolkitScreen.class.getDeclaredField("scroll");scrollField.setAccessible(true);
                        check(scrollField.getInt(mc.gui.screen())>0,"Advanced scrolls to revealed settings");
                        var divider=ToolkitScreen.class.getDeclaredField("advancedDivider");divider.setAccessible(true);
                        check(divider.getInt(mc.gui.screen())>=0,"Advanced has a divider");
                    }
                    if(target==9)check(mc.gui.screen().children().stream().noneMatch(c->c instanceof Button b&&b.getMessage().getString().startsWith("XP per hour")),"Farming contains no XP control");
                    assertControls(mc);stage++;return;
                }
                if(stage>=96&&stage<128){
                    int index=(stage-96)/2;
                    int target=index%4<2?9:11;
                    if(stage%2==0){
                        ToolkitConfig.get().lightMode=(index/4)%2==1;
                        if(index==0||index==8){mc.getWindow().setWindowed(index<8?1280:1000,800);mc.options.guiScale().set(index<8?2:3);mc.resizeGui();}
                        mc.setScreenAndShow(new ToolkitScreen(null,target));
                        if(index%2==1){scrollBottom(mc);press(mc,"Advanced...");}
                    }else capture(mc,"simplified-"+target+"-"+((index/4)%2==1?"light":"dark")+(index>=8?"-compact":"")+(index%2==1?"-advanced":""));
                    stage++;return;
                }
                if(stage==128){
                    if(Boolean.getBoolean("toolkit.testStartupConnect"))check("1".equals(System.getProperty("toolkit.startupProbeCount")),"Startup never reconnects during later menu use");
                    check(Files.exists(output.resolve("screenshots/coconut-settings-light-compact.png")),"Coconut settings compact screenshot written");
                    Files.writeString(output.resolve("PASS.txt"),"PASS: global setting search, one sell-all selector, every control destination, Ctrl+F, Escape, no-match state and non-overlapping full/compact widgets; sell all key registration, assignment, persistence, cancel/unbind and safe menu click draining; coconut toggle, threshold validation, persistence and sound preview; light/dark full/compact menus; XP/money rate selection, independent XP HUD, timer ledger isolation, crop visibility, HUD editing and blackout\n");
                    System.out.println("PASS: light/dark theme and timer runtime interactions, menus and screenshots");System.exit(0);
                }

            }catch(Throwable e){e.printStackTrace();System.exit(1);}
        });
    }
    net.minecraft.client.gui.components.EditBox searchField(net.minecraft.client.Minecraft mc){
        return mc.gui.screen().children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox)
            .map(c->(net.minecraft.client.gui.components.EditBox)c).filter(c->c.getMessage().getString().equals("Search all settings")).findFirst().orElseThrow();
    }
    void search(net.minecraft.client.Minecraft mc,String query){searchField(mc).setValue(query);}
    void assertControls(net.minecraft.client.Minecraft mc){
        var screen=mc.gui.screen();
        var widgets=screen.children().stream().filter(c->c instanceof AbstractWidget w&&w.visible).map(c->(AbstractWidget)c).toList();
        for(int i=0;i<widgets.size();i++){
            var a=widgets.get(i);
            check(a.getX()>=0&&a.getY()>=0&&a.getRight()<=screen.width&&a.getBottom()<=screen.height,"Visible widget stays on screen: "+a.getMessage().getString());
            for(int j=i+1;j<widgets.size();j++){
                var b=widgets.get(j);
                check(a.getRight()<=b.getX()||b.getRight()<=a.getX()||a.getBottom()<=b.getY()||b.getBottom()<=a.getY(),"Controls do not overlap: "+a.getMessage().getString()+" / "+b.getMessage().getString());
            }
        }
    }
    void capture(net.minecraft.client.Minecraft mc,String name){
        Screenshot.grab(output.toFile(),name+".png",mc.gameRenderer.mainRenderTarget(),1,msg->{});
    }
    void scrollBottom(net.minecraft.client.Minecraft mc){
        var screen=mc.gui.screen();screen.mouseScrolled(screen.width/2,screen.height/2,0,-20);
    }
    void switchTheme(net.minecraft.client.Minecraft mc){
        for(var child:mc.gui.screen().children())if(child instanceof Button b&&(b.getMessage().getString().equals("Dark mode")||b.getMessage().getString().equals("Light mode"))){b.onPress(null);return;}
        throw new AssertionError("theme switch missing");
    }
    void press(net.minecraft.client.Minecraft mc,String text){
        for(var child:mc.gui.screen().children())if(child instanceof Button b&&b.visible&&b.active&&b.getMessage().getString().startsWith(text)){b.onPress(null);return;}
        throw new AssertionError("button missing: "+text);
    }
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
}
