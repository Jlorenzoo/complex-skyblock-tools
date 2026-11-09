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
    int ticks,stage=Boolean.getBoolean("toolkit.voicePreviewOnly")?600:Boolean.getBoolean("toolkit.roundedPreviewOnly")?400:Boolean.getBoolean("toolkit.alertPreviewOnly")?129:(Boolean.getBoolean("toolkit.testVoicechat")?-2:0);
    Path output=Path.of(System.getProperty("toolkit.preview"));
    String statsFixtureId;
    private void checkVoidRod() throws Exception {
        var rod=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FISHING_ROD);
        rod.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("֎ ").append(
                    net.minecraft.network.chat.Component.literal("Void Fishing Rod").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE))
                    .append(" ֎"));
        check(com.fishingpings.FishingSignals.voidRod(rod.getHoverName().getString()),"Actual server rod component passes the seasonal sound guard");
        var meter=new net.minecraft.client.gui.components.LerpingBossEvent(java.util.UUID.randomUUID(),
                net.minecraft.network.chat.Component.literal("֎ What Lurks Below X - Depth 1.5 blocks"),
                0.05f,net.minecraft.world.BossEvent.BossBarColor.PURPLE,net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS,false,false,false);
        var signals=new com.fishingpings.FishingSignals();var hook=java.util.UUID.randomUUID();
        check(!signals.ready(hook,com.fishingpings.FishingSignals.depth(meter.getName().getString()),30,0),"Server meter starts shallow");
        meter.setName(net.minecraft.network.chat.Component.literal("֎ What Lurks Below X - Depth: 30.0 blocks ✔"));
        check(signals.ready(hook,com.fishingpings.FishingSignals.depth(meter.getName().getString()),30,1000),"Server rod and bossbar reach ready cue");
        check(!signals.ready(hook,30,30,1100),"Server ready cue fires once");
        ToolkitMod.FISHING.previewDepth();
        Files.writeString(output.resolve("void-rod.txt"),"PASS: server Void Fishing Rod custom-name component, real bossbar depth threshold, single ready cue and sound preview submission\n");
    }
    private void checkPassiveIncome(net.minecraft.client.Minecraft mc) throws Exception {
        check(java.util.Arrays.stream(net.minecraft.client.multiplayer.ClientPacketListener.class.getDeclaredMethods())
                .anyMatch(m->m.getName().contains("toolkit$incomeActionBar")),"Production action-bar packet observer is remapped");
        var hover=net.minecraft.network.chat.Component.literal("Balance: $1.55B")
                .withStyle(s->s.withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(net.minecraft.network.chat.Component.literal("$1,545,063,209"))));
        BoosterStatsManager.resetConnection();
        BoosterStatsManager.checkScoreboardComponent(hover);
        check(BoosterStatsManager.getLastKnownBalance()==1_545_063_209d&&!BoosterStatsManager.isBalanceRounded(),
                "Production sidebar handler reads already-received full hover balance");
        PassiveIncomeCapture.reset();PassiveIncomeCapture.tick(mc,true);
        PassiveIncomeCapture.observe("action-bar",net.minecraft.network.chat.Component.literal("Sold crops for $1,234"));
        var saved=PassiveIncomeCapture.class.getDeclaredField("saved");saved.setAccessible(true);saved.setLong(null,0);
        PassiveIncomeCapture.tick(mc,true);
        String capture=Files.readString(mc.gameDirectory.toPath().resolve("config/skyblocktoolkit-income-observations.json"));
        check(capture.contains("Sold crops for $1,234")&&capture.contains("no commands"),"Money evidence written locally without requests");
        PassiveIncomeCapture.reset();BoosterStatsManager.resetConnection();
        Files.writeString(output.resolve("passive-income.txt"),"PASS: received component hover precision and bounded local cash observations; no network commands\n");
    }

    private void checkRoundedIncome() throws Exception {
        BoosterStatsManager.resetConnection();
        GoldShardTracker.reset();
        BoosterStatsManager.checkScoreboardLine("§6Balance: §a$2.34B");
        check(BoosterStatsManager.isBalanceRounded() && BoosterStatsManager.getLastKnownBalance()==2_340_000_000d,
                "Real sidebar handler accepts billion balances");
        check(BoosterStatsManager.averageLabel().contains("waiting"), "HUD shows when rounded cash is still waiting");
        long now=System.currentTimeMillis();
        BoosterStatsManager.farmingActivity(now);
        check(BoosterStatsManager.earningsActivity.select(now,true,false,false,false,false)==EarningsActivity.Tracker.CROPS,
                "Farming HUD stays visible without a rounded balance change");
        BoosterStatsManager.checkScoreboardLine("Balance: $2,340,123,456.78");
        check(!BoosterStatsManager.isBalanceRounded(), "Full cash restores exact observation mode");
        BoosterStatsManager.checkScoreboardLine("Balance: $2.34B");
        Files.writeString(output.resolve("rounded-income.txt"),"PASS: billion sidebar parsing, precision rebasing, waiting label and farming visibility without a balance change\n");
    }
    private void roundedPreview(net.minecraft.client.Minecraft mc) throws Exception {
        if(stage==432){
            if(!Files.exists(output.resolve("screenshots/rounded-3-light-compact.png")))return;
            Files.writeString(output.resolve("PASS.txt"),"PASS: rounded warmup/steady estimates, shard-only cash-waiting HUD and money tests in both themes and layouts\n");
            System.exit(0);
        }
        int sample=(stage-400)/2, kind=sample/4;
        boolean light=sample%2==1, compact=sample%4>=2;
        if(stage%2==0){
            checkRoundedIncome();
            var cfg=ToolkitConfig.get();cfg.lightMode=light;cfg.cropEarningsHud=true;cfg.includeGoldShards=false;cfg.rateTestXp=false;
            long now=System.currentTimeMillis();
            if(kind>0&&kind<3){
                for(String name:new String[]{"average","baseAverage"}){
                    var f=BoosterStatsManager.class.getDeclaredField(name);f.setAccessible(true);
                    ((RollingEarnings)f.get(null)).reset(now-10000);
                }
                var settled=BoosterStatsManager.class.getDeclaredMethod("onSettled",MoneyLedger.Receipt.class);
                settled.setAccessible(true);
                settled.invoke(null,new MoneyLedger.Receipt(1000000,0,-1,-1,now-20000,now-10000,true));
                check(BoosterStatsManager.averageLabel().contains("waiting"),"First partial rounded bucket stays in warmup");
                settled.invoke(null,new MoneyLedger.Receipt(1000000,0,-1,-1,now-10000,now,true));
                BoosterStatsManager.tick();
                check(BoosterStatsManager.averageLabel().contains("~$"),"Rounded HUD marks its numerical estimate");
                // Real server formatter drops trailing zeroes below a billion too.
                for(String balance:new String[]{"$79.19M","$79.2M","$79.21M","$80M","$80.01M"}){
                    BoosterStatsManager.checkScoreboardLine("Balance: "+balance);
                    check(BoosterStatsManager.averageLabel().contains("~$"),"Printed precision reset cash average: "+balance);
                }
            }
            if(kind==2){
                for(var entry:java.util.Map.<String,Object>of("running",false,"completed",true,"startTimeMs",now-60000,"stopTimeMs",now,"baseEarned",1000000d,"totalEarned",1000000d,"shardsEarned",0L,"moneyEstimated",true).entrySet()){
                    var f=RateTracker.class.getDeclaredField(entry.getKey());f.setAccessible(true);f.set(null,entry.getValue());
                }
                check(RateTracker.formatDisplayedRate().startsWith("~$"),"Money test marks its numerical estimate");
            }
            if(kind==3){
                cfg.includeGoldShards=true;cfg.goldShardPrice=125000;
                var field=GoldShardTracker.class.getDeclaredField("earnings");field.setAccessible(true);
                var earnings=(GoldShardEarnings)field.get(null);earnings.reset(now-20000);
                earnings.chat(1,now-10000,-1);
                check(BoosterStatsManager.averageLabel().startsWith("Shards ~$7.50M/h")
                        &&BoosterStatsManager.averageLabel().contains("cash --"),
                        "Shard value cannot make unmeasured cash appear ready");
            }
            mc.getWindow().setWindowed(compact?1000:1280,800);mc.options.guiScale().set(compact?3:2);mc.resizeGui();
            ModConfig.getInstance().setAveragePosition(12,90);
            mc.setScreenAndShow(kind==2?new BoosterStatsScreen(null,true):new ToolkitScreen(null,5));
        }else{
            capture(mc,"rounded-"+kind+"-"+(light?"light":"dark")+"-"+(compact?"compact":"full"));
            if(sample==15){stage++;return;}
        }
        stage++;
    }
    private void voicePreview(net.minecraft.client.Minecraft mc) throws Exception {
        if(stage==612){
            if(!Files.exists(output.resolve("screenshots/voice-states-3.png")))return;
            Files.writeString(output.resolve("PASS.txt"),"PASS: optional real voice-chat reflection, voice HUD settings/search/persistence and six routing/error states in full/compact light/dark layouts\n");
            System.exit(0);return;
        }
        int sample=(stage-600)/3,step=(stage-600)%3;
        var cfg=ToolkitConfig.get();
        if(step==0){
            check(VoiceMusicHud.readVoice().available(),"Optional bridge reads installed Simple Voice Chat without a hard dependency");
            cfg.lightMode=sample%2==1;cfg.voiceMusicHud=true;
            mc.getWindow().setWindowed(sample>=2?1000:1280,800);mc.options.guiScale().set(sample>=2?3:2);mc.resizeGui();
            mc.setScreenAndShow(new ToolkitScreen(null,4));search(mc,"voice music");press(mc,"HUD & Overlays / Voice & music status");
            assertControls(mc);
            press(mc,"Voice & music status");check(!cfg.voiceMusicHud,"Voice HUD toggle disables");
            press(mc,"Voice & music status");check(cfg.voiceMusicHud,"Voice HUD toggle enables");
            var ctor=ToolkitConfig.class.getDeclaredConstructor();ctor.setAccessible(true);
            check(ctor.newInstance().voiceMusicHud,"Voice HUD preference persists");
        }else if(step==1){
            capture(mc,"voice-settings-"+sample);
            mc.setScreenAndShow(new ToolkitScreen(null,5));
            int x=VoiceMusicHud.x(mc,true),y=VoiceMusicHud.y(mc);
            mc.gui.screen().mouseClicked(new MouseButtonEvent(x+6,y+12,new MouseButtonInfo(0,0)),false);
            mc.gui.screen().mouseDragged(new MouseButtonEvent(x+26,y+36,new MouseButtonInfo(0,0)),20,24);
            mc.gui.screen().mouseReleased(new MouseButtonEvent(x+26,y+36,new MouseButtonInfo(0,0)));
            check(cfg.voiceHudY==y+24,"Voice HUD drags in the real editor");
            mc.setScreenAndShow(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Voice status preview")){
                @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float tick){
                    var p=ToolkitTheme.palette();g.fill(0,0,width,height,p.sidebar());
                    g.text(mc.font,"Voice & music · routing states",12,12,p.text(),false);
                    var v=new VoiceMusicState.Voice(true,true,false,false,false,false,false,true,"SteelSeries Sonar - Stream");
                    long now=System.currentTimeMillis();
                    for(int i=0;i<6;i++){
                        var m=new VoiceMusicState.Mix(true,now,true,false,(i&1)!=0,(i&2)!=0,true,false);
                        var current=i==4?new VoiceMusicState.Voice(true,true,false,false,false,false,false,true,"Sonar - Microphone"):
                            i==5?new VoiceMusicState.Voice(true,false,false,false,false,false,false,false,""):v;
                        VoiceMusicHud.preview=VoiceMusicState.display(current,m,now);cfg.voiceHudX=12;cfg.voiceHudY=30+i*35;
                        VoiceMusicHud.render(g,mc,false);
                    }
                    VoiceMusicHud.preview=null;cfg.voiceHudX=12;cfg.voiceHudY=110;
                }
            });
        }else capture(mc,"voice-states-"+sample);
        stage++;
    }
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

    private void checkTreasureSettings(net.minecraft.client.Minecraft mc) throws Exception {
        var cfg=ToolkitConfig.get();
        mc.setScreenAndShow(new ToolkitScreen(null,4));search(mc,"treasurechest timer");press(mc,"HUD & Overlays / Treasurechest timer");
        boolean enabled=cfg.treasureChestHud;press(mc,"Treasurechest timer");check(cfg.treasureChestHud!=enabled,"Treasure HUD toggles");
        search(mc,"chest details duration");press(mc,"HUD & Overlays / Chest details duration");
        int before=cfg.treasureDetailsSeconds;press(mc,"Chest details duration");check(cfg.treasureDetailsSeconds!=before,"Details duration adjustable");
        search(mc,"treasure hud scale");press(mc,"HUD & Overlays / Treasure HUD scale");
        double scaleBefore=cfg.treasureScale;press(mc,"Treasure HUD scale");check(cfg.treasureScale!=scaleBefore,"HUD scale changes and shows actual percentage");
        cfg.treasureDetailsSeconds=120;cfg.treasureHudX=45;cfg.treasureHudY=62;cfg.treasureScale=.8;cfg.save();
        var ctor=ToolkitConfig.class.getDeclaredConstructor();ctor.setAccessible(true);var reloaded=ctor.newInstance();
        check(reloaded.treasureChestHud==cfg.treasureChestHud&&reloaded.treasureDetailsSeconds==120&&reloaded.treasureHudX==45&&reloaded.treasureScale==.8,"Treasure settings persist");
        TreasureChestTracker.connection(new Object());
        var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CLOCK);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Next Chest Spawn"));
        stack.set(net.minecraft.core.component.DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(java.util.List.of(net.minecraft.network.chat.Component.literal("§b✦ Time: 55m 37s"))));
        TreasureChestTracker.readMenu(java.util.List.of(stack),1000);
        check(TreasureChestTracker.STATE.timer(1000).contains("0:55:37"),"Real item component lore syncs countdown");
        TreasureChestTracker.readMenu(java.util.List.of(stack),3000);
        check(TreasureChestTracker.STATE.timer(3000).contains("0:55:35"),"Frozen menu lore cannot extend timer");
        TreasureChestTracker.connection(null);check(TreasureChestTracker.STATE.timer(3000).contains("sync"),"Connection resets tracker");
        cfg.treasureHudX=180;cfg.treasureHudY=140;cfg.treasureScale=1;
        mc.setScreenAndShow(new ToolkitScreen(null,5));var layoutScreen=(ToolkitScreen)mc.gui.screen();
        layoutScreen.mouseClicked(new MouseButtonEvent(185,145,new MouseButtonInfo(0,0)),false);
        layoutScreen.mouseDragged(new MouseButtonEvent(205,165,new MouseButtonInfo(0,0)),20,20);
        layoutScreen.mouseReleased(new MouseButtonEvent(205,165,new MouseButtonInfo(0,0)));
        check(cfg.treasureHudX==200&&cfg.treasureHudY==160,"Treasure HUD drags on isolated canvas");
        cfg.treasureHudX=-1;cfg.treasureHudY=48;cfg.treasureScale=1;cfg.treasureChestHud=enabled;cfg.save();
        Files.writeString(outputPath().resolve("treasure-settings.txt"),"PASS: actual clock item lore, frozen lore, connection reset, settings widgets and saved preferences\n");
    }

    private void checkShardSettings(net.minecraft.client.Minecraft mc) throws Exception {
        var cfg=ToolkitConfig.get();boolean enabled=cfg.includeGoldShards;double price=cfg.goldShardPrice;
        mc.setScreenAndShow(new ToolkitScreen(null,2));
        search(mc,"gold shards income");press(mc,"Skills / Include gold shards in income");
        press(mc,"Include gold shards in income");
        check(cfg.includeGoldShards!=enabled,"Shard inclusion widget works");
        search(mc,"gold shard value");press(mc,"Skills / Gold shard value ($ each)");
        var field=mc.gui.screen().children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox e&&e.getMessage().getString().equals("Gold shard value ($ each)"))
            .map(c->(net.minecraft.client.gui.components.EditBox)c).findFirst().orElseThrow();
        field.setValue("125k");check(cfg.goldShardPrice==125000,"Shard price accepts suffix");
        field.setValue("-2");check(cfg.goldShardPrice==125000,"Invalid shard price preserves value");
        cfg.save();var ctor=ToolkitConfig.class.getDeclaredConstructor();ctor.setAccessible(true);
        var reload=ctor.newInstance();check(reload.includeGoldShards==cfg.includeGoldShards&&reload.goldShardPrice==125000,"Shard preferences persist");
        cfg.includeGoldShards=true;check(GoldShardTracker.value(4)==500000,"Shard valuation");
        cfg.includeGoldShards=false;check(GoldShardTracker.value(4)==0,"Disabled excludes item value");
        // Exercise the real rate calculation without starting or joining a world.
        var fields=new java.util.HashMap<String,Object>();
        for(String name:new String[]{"running","completed","startTimeMs","stopTimeMs","baseEarned","totalEarned","shardsEarned","testId"}){
            var f=RateTracker.class.getDeclaredField(name);f.setAccessible(true);fields.put(name,f.get(null));
        }
        try{
            long start=System.currentTimeMillis()-60000;
            for(var entry:java.util.Map.<String,Object>of("running",false,"completed",true,"startTimeMs",start,"stopTimeMs",start+60000,"baseEarned",1000.0,"totalEarned",1200.0,"shardsEarned",0L,"testId",77).entrySet()){
                var f=RateTracker.class.getDeclaredField(entry.getKey());f.setAccessible(true);f.set(null,entry.getValue());
            }
            RateTracker.onShardGain(4,start+5000,77);RateTracker.onShardGain(9,start+70000,77);RateTracker.onShardGain(9,start+5000,76);
            check(RateTracker.getShardsEarned()==4,"Rate sample rejects wrong id and out-of-window drops");
            cfg.includeGoldShards=true;
            check(RateTracker.getBaseEarned()==501000&&RateTracker.getTotalEarned()==501200&&RateTracker.getCurrentRatePerHour()==30060000,"Shard value joins base and actual income without booster multiplication");
            cfg.includeGoldShards=false;check(Math.abs(RateTracker.getCurrentRatePerHour()-60000)<.001,"Toggle removes shards from measured rate");
        }finally{
            for(var entry:fields.entrySet()){var f=RateTracker.class.getDeclaredField(entry.getKey());f.setAccessible(true);f.set(null,entry.getValue());}
            cfg.includeGoldShards=enabled;cfg.goldShardPrice=price;cfg.save();
        }
        mc.setScreenAndShow(new ToolkitScreen(null,1));
        Files.writeString(outputPath().resolve("gold-shards.txt"),"PASS: real shard settings, price validation/persistence, rate sample boundaries and optional valuation\n");
    }

    private static Path outputPath() { return Path.of(System.getProperty("toolkit.preview")); }
    @Override public void onInitializeClient(){
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            try{
                if(mc.gui.overlay()!=null||++ticks<100)return;
                if(ticks%Integer.getInteger("toolkit.menuTickInterval",20)!=0)return;
                if(stage>=600){voicePreview(mc);return;}
                if(stage>=400){roundedPreview(mc);return;}
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
                    checkShardSettings(mc);checkTreasureSettings(mc);checkPassiveIncome(mc);checkRoundedIncome();checkVoidRod();
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
                    check(java.util.Arrays.stream(net.minecraft.client.gui.screens.ChatScreen.class.getDeclaredMethods()).anyMatch(m->m.getName().contains("toolkit$onRenderHead")),"Production chat blackout mixin applies");
                    mc.options.guiScale().set(2);mc.resizeGui();
                    mc.setScreenAndShow(new ToolkitScreen(null,0));stage++;return;
                }
                if(stage==1){
                    press(mc,"Booster timers");
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
                    var chatCtor=java.util.Arrays.stream(net.minecraft.client.gui.screens.ChatScreen.class.getDeclaredConstructors()).findFirst().orElseThrow();
                    chatCtor.setAccessible(true);
                    var chatArgs=new Object[chatCtor.getParameterCount()];chatArgs[0]="";
                    for(int i=1;i<chatArgs.length;i++)chatArgs[i]=false;
                    var chatScreen=(net.minecraft.client.gui.screens.ChatScreen)chatCtor.newInstance(chatArgs);
                    BlackoutManager.setActive(true);
                    chatScreen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_F6,0,0));
                    check(!BlackoutManager.isActive(),"Blackout wake key in chat screen toggles blackout off");
                    var chatCfg=ToolkitConfig.get();
                    check(!chatCfg.blackoutChatOverlay,"Overlay chat defaults to disabled");
                    chatCfg.blackoutChatOverlay=true;chatCfg.save();
                    var chatConfigCtor=ToolkitConfig.class.getDeclaredConstructor();chatConfigCtor.setAccessible(true);
                    var reloadedChatCfg=chatConfigCtor.newInstance();
                    check(reloadedChatCfg.blackoutChatOverlay,"Overlay chat persists");
                    chatCfg.blackoutChatOverlay=false;chatCfg.save();
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
                    press(mc,"Active HUD elements");
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
                    press(mc,"Farming");
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
                    mc.setScreenAndShow(new ToolkitScreen(null,10));press(mc,"Keyboard shortcuts");scrollBottom(mc);
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
                    for(int category:new int[]{1,2,3,4,6}){
                        mc.setScreenAndShow(new ToolkitScreen(null,category));
                        if(category==6)press(mc,"Keyboard shortcuts");
                        check(mc.gui.screen().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().startsWith("Sell all key:")).count()==(category==6?1:0),"Single sell-all selector in Utilities");
                        search(mc,"sell all");
                        check(mc.gui.screen().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().contains(" / ")).count()==1,"Sell all returns one global setting from every category");
                        press(mc,"Utilities / Sell all key");
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
                    for(int page:new int[]{1,2,3,4,6,7,9,10,11,12}){
                        mc.setScreenAndShow(new ToolkitScreen(null,page));assertControls(mc);scrollBottom(mc);assertControls(mc);
                    }
                    mc.setScreenAndShow(new ToolkitScreen(null,10));press(mc,"Keyboard shortcuts");scrollBottom(mc);stage++;return;
                }
                if(stage==49){capture(mc,"general-dark-compact");switchTheme(mc);stage++;return;}
                if(stage==50){capture(mc,"general-light-compact");stage++;return;}
                if(stage==51){
                    mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(2);mc.resizeGui();
                    mc.setScreenAndShow(new ToolkitScreen(null,6));
                    press(mc,"Auction notifications");
                    mc.gui.screen().mouseScrolled(mc.gui.screen().width/2,mc.gui.screen().height/2,0,-10);
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
                if(stage>=80&&stage<128){
                    if(stage==80){
                        mc.getWindow().setWindowed(1280,800);mc.options.guiScale().set(2);mc.resizeGui();
                        ToolkitConfig.get().lightMode=false;
                        mc.setScreenAndShow(new ToolkitScreen(null,3));
                        stage++;return;
                    }
                    if(stage==81){
                        capture(mc,"seasonals-overview-dark");
                        scrollBottom(mc);press(mc,"Bone Leggings");
                        stage++;return;
                    }
                    if(stage==82){
                        capture(mc,"seasonals-bone-pants-expanded");
                        ToolkitConfig.get().lightMode=true;
                        mc.setScreenAndShow(new ToolkitScreen(null,3));scrollBottom(mc);press(mc,"Bone Leggings");scrollBottom(mc);
                        stage++;return;
                    }
                    if(stage==83 || stage==84 || stage==85){
                        capture(mc,"bone-settings-"+stage);
                        if(stage==83){mc.getWindow().setWindowed(1000,800);mc.options.guiScale().set(3);mc.resizeGui();}
                        ToolkitConfig.get().lightMode=stage==83;
                        mc.setScreenAndShow(new ToolkitScreen(null,3));scrollBottom(mc);press(mc,"Bone Leggings");scrollBottom(mc);
                        stage++;return;
                    }
                    if(stage==86){
                        var sample = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_LEGGINGS);
                        sample.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("§dBone §lLeggings"));
                        sample.set(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(0xB143C9));
                        sample.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
                        sample.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,net.minecraft.resources.Identifier.withDefaultNamespace("leather_leggings"));
                        var expected = sample.copyWithCount(1);
                        BoneLeggingsTracker.rememberServerStack(sample);
                        sample.set(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(0xFFFFFF));
                        check(net.minecraft.world.item.ItemStack.isSameItemSameComponents(expected,BoneLeggingsTracker.getItemStack(null)),"Captured sprite retains model, original dye and glint independently of the source stack");
                        check(net.minecraft.world.item.ItemStack.isSameItemSameComponents(expected,SeasonalRegistry.find("bone_pants").icon()),"Seasonal card shares the complete captured item");
                        Files.writeString(output.resolve("bone-sprite-components.txt"),"PASS: complete model, dye, name and glint components retained; Seasonal card uses captured stack; synthetic offline fixture, not a live server capture\n");
                        mc.setScreenAndShow(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Bone icon states")) {
                            @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float tick) {
                                g.text(font,"Sample dye + glint (offline)",20,3,0xFFFFFFFF,true);
                                for(int theme=0;theme<2;theme++) {
                                    int y=20+theme*90;
                                    g.fill(0,y-5,width,y+80,theme==0?0xFF1B1D21:0xFFF9F9FC);
                                    for(int state=0;state<2;state++) {
                                        int x=20+state*100;
                                        g.text(font,new String[]{"Speed","Regen"}[state],x,y,theme==0?0xFFE4E7EC:0xFF232632,false);
                                        BoneLeggingsTracker.renderEffectIcon(g,BoneLeggingsTracker.getItemStack(mc),x,y+18,state==0,false);
                                        BoneLeggingsTracker.renderEffectIcon(g,BoneLeggingsTracker.getItemStack(mc),x,y+48,state==0,true);
                                    }
                                }
                            }
                        });stage++;return;
                    }
                    if(stage==87){capture(mc,"bone-effect-icons");stage=128;return;}
                    stage=128;return;
                }
                if(stage==128){
                    if(Boolean.getBoolean("toolkit.testStartupConnect"))check("1".equals(System.getProperty("toolkit.startupProbeCount")),"Startup never reconnects during later menu use");
                    check(Files.exists(output.resolve("screenshots/coconut-settings-light-compact.png")),"Coconut settings compact screenshot written");
                    stage=129;return;
                }
                if(stage>=129 && stage<=145){
                    if(stage>129)capture(mc,"booster-toast-"+(stage-130));
                    if(stage==145){VisualAlertOverlay.reset();stage=146;return;}
                    ToolkitConfig.get().moneyVisualAlert=true;
                    int sample=stage-129;
                    boolean compact=sample>=8;
                    ToolkitConfig.get().lightMode=(sample/4)%2==1;
                    mc.getWindow().setWindowed(compact?1000:1280,800);
                    mc.options.guiScale().set(compact?3:2);mc.resizeGui();
                    mc.setScreenAndShow(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Booster notification preview")){
                        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float tick){
                            var palette=ToolkitTheme.palette();
                            // Contrasting backdrops expose opacity, clipping and readable text.
                            g.fill(0,0,width,height,palette.panel());
                            for(int x=0;x<width;x+=24)g.fill(x,0,x+12,height,palette.surface());
                            switch(sample%4){
                                case 0 -> VisualAlertOverlay.previewCoconutAlert();
                                case 1 -> VisualAlertOverlay.triggerMoneyBooster(75,"Money booster item with a deliberately long source name to check compact clipping",300);
                                case 2 -> VisualAlertOverlay.previewXpAlert();
                                case 3 -> VisualAlertOverlay.previewExpiringAlert();
                            }
                            try{
                                var start=VisualAlertOverlay.class.getDeclaredField("startTime");start.setAccessible(true);
                                start.setLong(null,System.currentTimeMillis()-300);
                            }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
                            VisualAlertOverlay.render(g,width,height);
                        }
                    });
                    stage++;return;
                }
                if(stage>=146&&stage<=150){
                    if(stage>146){capture(mc,"gold-shards-"+(stage-147));assertControls(mc);}
                    if(stage==150){stage=151;return;}
                    int sample=stage-146;
                    ToolkitConfig.get().lightMode=sample%2==1;
                    mc.getWindow().setWindowed(sample>=2?1000:1280,800);
                    mc.options.guiScale().set(sample>=2?3:2);mc.resizeGui();
                    mc.setScreenAndShow(new ToolkitScreen(null,2));
                    search(mc,"gold shards income");press(mc,"Skills / Include gold shards in income");
                    stage++;return;
                }
                if(stage>=151&&stage<=155){
                    if(stage>151){capture(mc,"treasure-settings-"+(stage-152));assertControls(mc);}
                    if(stage==155){stage=156;return;}
                    int sample=stage-151;ToolkitConfig.get().lightMode=sample%2==1;
                    mc.getWindow().setWindowed(sample>=2?1000:1280,800);mc.options.guiScale().set(sample>=2?3:2);mc.resizeGui();
                    mc.setScreenAndShow(new ToolkitScreen(null,4));search(mc,"treasure thirty seconds");press(mc,"HUD & Overlays / Only show last 30 seconds");
                    boolean oldThirty=ToolkitConfig.get().treasureLastThirtySeconds;
                    press(mc,"Only show last 30 seconds");
                    check(ToolkitConfig.get().treasureLastThirtySeconds!=oldThirty,"Final 30 seconds toggle");
                    var ctor=ToolkitConfig.class.getDeclaredConstructor();ctor.setAccessible(true);
                    check(ctor.newInstance().treasureLastThirtySeconds==!oldThirty,"Final 30 seconds preference persists");
                    TreasureChestTracker.STATE.reset();TreasureChestTracker.STATE.sync(60,System.currentTimeMillis());
                    ToolkitConfig.get().treasureLastThirtySeconds=true;
                    check(TreasureChestTracker.lines(false).isEmpty(),"Hidden countdown has no HUD rows");
                    check(!TreasureChestTracker.lines(true).isEmpty(),"Editor preview stays visible");
                    TreasureChestTracker.STATE.sync(29,System.currentTimeMillis());
                    check(TreasureChestTracker.lines(false).getFirst().contains("0:00:29"),"Countdown appears in final 30 seconds");
                    TreasureChestTracker.STATE.reset();ToolkitConfig.get().treasureLastThirtySeconds=oldThirty;ToolkitConfig.get().save();
                    stage++;return;
                }
                if(stage>=156&&stage<=160){
                    if(stage>156)capture(mc,"treasure-hud-"+(stage-157));
                    if(stage==160){stage=161;return;}
                    int sample=stage-156;ToolkitConfig.get().lightMode=sample%2==1;
                    mc.getWindow().setWindowed(sample>=2?1000:1280,800);mc.options.guiScale().set(sample>=2?3:2);mc.resizeGui();
                    mc.setScreenAndShow(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Treasure HUD preview")){
                        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float tick){
                            g.fill(0,0,width,height,ToolkitTheme.palette().panel());
                            for(int x=0;x<width;x+=24)g.fill(x,0,x+12,height,ToolkitTheme.palette().surface());
                            TreasureChestTracker.render(g,mc,true);
                        }
                    });stage++;return;
                }
                if(stage>=161&&stage<230){
                    String[][] destinations={{"1","Booster timers"},{"1","Coconut alerts"},{"1","Earnings & statistics"},{"2","Farming"},{"2","Fishing"},{"2","Fish sell prices"},{"2","Mining & XP"},{"4","Active HUD elements"},{"4","Layout & positioning"},{"4","Treasure chests"},{"6","Keyboard shortcuts"},{"6","Chat selection & copy"},{"6","Auction notifications"},{"6","Display & visuals"},{"6","Blackout (AFK)"},{"3","Void Rod"},{"3","Bone Leggings"}};
                    int index=stage-161;
                    if(index>0){capture(mc,"topic-"+(index-1));assertControls(mc);scrollBottom(mc);assertControls(mc);}
                    if(index==destinations.length*4){stage=230;return;}
                    int sample=index/destinations.length;String[] destination=destinations[index%destinations.length];
                    ToolkitConfig.get().lightMode=sample%2==1;
                    mc.getWindow().setWindowed(sample>=2?1000:1280,800);mc.options.guiScale().set(sample>=2?3:2);mc.resizeGui();
                    int page=Integer.parseInt(destination[0]);mc.setScreenAndShow(new ToolkitScreen(null,page));
                    var screen=mc.gui.screen();
                    check(screen.children().stream().noneMatch(c->c instanceof net.minecraft.client.gui.components.AbstractSliderButton),"Overview hides unrelated controls");
                    pressTopic(mc,destination[1]);assertControls(mc);
                    screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));
                    check(mc.gui.screen()==screen,"Escape returns to overview without closing");
                    pressTopic(mc,destination[1]);
                    // Switching categories and back remembers the topic; clicking the active category returns to overview.
                    if(page==2&&destination[1].equals("Mining & XP")){
                        press(mc,"Boost");press(mc,"Skills");
                        check(screen.children().stream().anyMatch(c->c instanceof AbstractWidget w&&ToolkitScreen.searchTarget(w,"XP per hour HUD")),"Category remembers selected topic");
                    }
                    // Re-selecting the active category gives the overview even after switching tabs.
                    String category=page==1?"Boost":page==2?"Skills":page==4?"HUD":page==3?"Season":"Util";
                    press(mc,category);
                    check(screen.children().stream().noneMatch(c->c instanceof AbstractWidget w&&w.getMessage().getString().startsWith("< All ")),"Active category returns to overview");
                    pressTopic(mc,destination[1]);
                    stage++;return;
                }
                if(stage>=230&&stage<250){
                    int sample=(stage-230)/5,step=(stage-230)%5;
                    if(step==0){
                        ToolkitConfig.get().lightMode=sample%2==1;
                        mc.getWindow().setWindowed(sample>=2?1000:1280,800);mc.options.guiScale().set(sample>=2?3:2);mc.resizeGui();
                        mc.setScreenAndShow(new ToolkitScreen(null,2));pressTopic(mc,"Farming");
                    }else if(step==1){
                        capture(mc,"focused-farming-"+sample);assertControls(mc);
                        check(mc.gui.screen().children().stream().noneMatch(c->c instanceof AbstractWidget w&&ToolkitScreen.searchTarget(w,"Gold shard value ($ each)")),"Secondary shard fields start hidden");
                        pressTopic(mc,"Gold shards & income");
                        check(mc.gui.screen().getFocused() instanceof Button b&&b.getMessage().getString().startsWith("Gold shards & income:"),"Disclosure retains keyboard focus after expanding");
                    }else if(step==2){
                        capture(mc,"focused-shards-"+sample);assertControls(mc);
                        var screen=mc.gui.screen();
                        var back=screen.children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().startsWith("< All ")).map(c->(Button)c).findFirst().orElseThrow();
                        int backY=back.getY();scrollBottom(mc);
                        check(back.visible&&back.getY()==backY,"Topic breadcrumb stays visible while scrolling");
                        press(mc,"< All ");
                        check(screen.children().stream().noneMatch(c->c instanceof net.minecraft.client.gui.components.EditBox e&&e.getMessage().getString().equals("Gold shard value ($ each)")),"Fixed Back returns to overview");
                        search(mc,"chat opacity");press(mc,"Utilities / Chat opacity");
                        check(screen.children().stream().anyMatch(c->c instanceof AbstractWidget w&&w.visible&&ToolkitScreen.searchTarget(w,"Chat opacity")),"Search expands Chat appearance and reveals its slider");
                    }else if(step==3){
                        capture(mc,"focused-chat-"+sample);assertControls(mc);
                        pressTopic(mc,"Chat appearance");
                        check(mc.gui.screen().children().stream().noneMatch(c->c instanceof net.minecraft.client.gui.components.AbstractSliderButton),"Closing appearance removes hidden sliders from keyboard navigation");
                        switchTheme(mc);
                        check(mc.gui.screen().children().stream().noneMatch(c->c instanceof net.minecraft.client.gui.components.AbstractSliderButton),"Theme retains disclosure state");
                        switchTheme(mc);
                        search(mc,"bone display style");press(mc,"Seasonals / Display style");
                    }else{
                        capture(mc,"focused-seasonal-"+sample);assertControls(mc);scrollBottom(mc);assertControls(mc);
                        check(mc.gui.screen().children().stream().anyMatch(c->c instanceof Button b&&b.visible&&b.getMessage().getString().startsWith("< All Seasonals")),"Seasonal Back remains visible");
                        mc.setScreenAndShow(new ToolkitScreen(null,2));pressTopic(mc,"Fishing");pressTopic(mc,"Fish sell prices");
                        check(mc.gui.screen().children().stream().anyMatch(c->c instanceof net.minecraft.client.gui.components.EditBox e&&e.getMessage().getString().equals("Cod · 60%")),"Fishing links directly to its price topic");
                    }
                    stage++;return;
                }
                if(stage>=250 && stage<254){
                    if(stage>250)capture(mc,"blackout-chat-"+(stage-251));
                    int sample=stage-250;
                    var cfg=ToolkitConfig.get();cfg.lightMode=sample%2==1;cfg.blackoutChatOverlay=sample%2==1;cfg.blackoutOpacity=1;cfg.blackoutHint=false;
                    if(sample==2){mc.getWindow().setWindowed(1000,800);mc.options.guiScale().set(3);mc.resizeGui();}
                    mc.gui.hud.getChat().addMessage(net.minecraft.network.chat.Component.literal("Blackout chat remains visible"));
                    var ctor=net.minecraft.client.gui.screens.ChatScreen.class.getConstructors()[0];
                    Object[] args=new Object[ctor.getParameterCount()];args[0]="Typing during blackout";for(int i=1;i<args.length;i++)args[i]=false;
                    mc.setScreenAndShow((net.minecraft.client.gui.screens.ChatScreen)ctor.newInstance(args));
                    BlackoutManager.setActive(true);
                    stage++;return;
                }
                if(stage==254){capture(mc,"blackout-chat-3");BlackoutManager.setActive(false);stage++;return;}
                if(stage>=255&&stage<275){
                    int sample=(stage-255)/5,step=(stage-255)%5;
                    if(step==0){
                        if(stage==255){
                            var history=ItemStatsTracker.history();long now=System.currentTimeMillis();
                            var p=history.register(ItemStatsTracker.scope(),"0123456789abcdef","Oni Fishing Rod","HAND","{\"id\":\"minecraft:fishing_rod\"}");
                            statsFixtureId=p.id;
                            history.rename(p,"My Oni Rod");
                            history.start(p,now-300000,ItemStatsLocation.REALM);history.tick(now-1000);
                            history.money(1500000,50,now-299000,now-1000);history.shards(64,now-10000);history.fish(120,240000,now-10000);history.stop(now,"Stopped");
                            history.start(p,now-600000,ItemStatsLocation.OVERWORLD);history.tick(now-400000);history.money(300000,0,now-599000,now-400000);history.stop(now-400000,"Stopped");
                            history.register(ItemStatsTracker.scope(),"fedcba9876543210","Unused fishing rod","HAND","{\"id\":\"minecraft:fishing_rod\"}");
                            history.register(ItemStatsTracker.scope(),"0123456789unused","Yellow Shulker Box","Inventory","{\"id\":\"minecraft:yellow_shulker_box\"}");
                            var armor=history.register(ItemStatsTracker.scope(),"0123456789armor","Bone Leggings","LEGS","{\"id\":\"minecraft:netherite_leggings\"}");
                            history.start(armor,now-800000,ItemStatsLocation.REALM);history.tick(now-700000);history.shards(1,now-750000);history.stop(now-700000,"Stopped");
                            var shulker=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.YELLOW_SHULKER_BOX);
                            shulker.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Named storage box"));
                            check(!ItemStatsTracker.trackable(shulker),"Real named shulker stack is not an earning tool");
                            check(!ItemStatsTracker.trackable(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_LEGGINGS)),"Real armor stack is not credited with held-tool income");
                            check(ItemStatsTracker.trackable(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_HOE)),"Real hoe remains detectable before its first result");
                        }
                        ToolkitConfig.get().lightMode=sample%2==1;
                        mc.getWindow().setWindowed(sample>=2?1000:1280,800);mc.options.guiScale().set(sample>=2?3:2);mc.resizeGui();
                        ToolkitConfig.get().includeGoldShards=true;ToolkitConfig.get().goldShardPrice=125000;
                        mc.setScreenAndShow(new ToolkitScreen(null,13));
                    }else if(step==1){
                        capture(mc,"stats-library-"+sample);assertControls(mc);
                        check(mc.gui.screen().children().stream().noneMatch(c->c instanceof Button b&&(b.getMessage().getString().contains("Unused fishing rod")||b.getMessage().getString().contains("Yellow Shulker Box")||b.getMessage().getString().contains("Bone Leggings"))),"Stats excludes unused tools, storage and legacy armor rewards");
                        pressTopic(mc,ItemStatsTracker.history().profile(statsFixtureId).title());assertControls(mc);
                        check(mc.gui.screen().children().stream().noneMatch(c->c instanceof Button b&&(b.getMessage().getString().contains("Start session")||b.getMessage().getString().contains("Register"))),"Stats has no registration or manual session controls");
                        press(mc,"Area: All");check(mc.gui.screen().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().endsWith(" · Stopped")).count()==1,"Overworld filter selects only Overworld sessions");
                        press(mc,"Area: Overworld");check(mc.gui.screen().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().endsWith(" · Stopped")).count()==1,"Realm filter selects only Realm sessions");
                    }else if(step==2){
                        capture(mc,"stats-item-"+sample);assertControls(mc);scrollBottom(mc);assertControls(mc);
                    }else if(step==3){
                        capture(mc,"stats-history-"+sample);
                        String sessionTitle=mc.gui.screen().children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().endsWith(" · Stopped")).map(c->((Button)c).getMessage().getString()).findFirst().orElseThrow();
                        check(sessionTitle.startsWith("$114.00M/h"),"Session headline includes cash plus configured shard value over five minutes");
                        pressTopic(mc,sessionTitle);assertControls(mc);
                    }else{
                        capture(mc,"stats-session-"+sample);assertControls(mc);scrollBottom(mc);assertControls(mc);capture(mc,"stats-session-breakdown-"+sample);
                        mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));
                        check(mc.gui.screen().children().stream().anyMatch(c->c instanceof Button b&&b.getMessage().getString().equals("< All items")),"Escape returns to item statistics");
                        mc.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0));
                        check(mc.gui.screen().children().stream().anyMatch(c->c instanceof Button b&&b.getMessage().getString().equals("Current item")),"Escape returns to automatic item library");
                    }
                    stage++;return;
                }
                if(stage>=275&&stage<291){
                    int sample=(stage-275)/4,step=(stage-275)%4;
                    if(step==0){
                        ToolkitConfig.get().lightMode=sample%2==1;
                        mc.getWindow().setWindowed(sample>=2?1000:1280,800);mc.options.guiScale().set(sample>=2?3:2);mc.resizeGui();
                        mc.setScreenAndShow(new ToolkitScreen(null,3));
                    }else if(step==1){
                        capture(mc,"seasonals-oni-overview-"+sample);assertControls(mc);
                        pressTopic(mc,"Oni Rod");assertControls(mc);
                    }else if(step==2){
                        capture(mc,"seasonals-oni-settings-"+sample);assertControls(mc);
                        mc.setScreenAndShow(new ToolkitScreen(null,2));pressTopic(mc,"Fishing");
                    }else{
                        capture(mc,"fishing-low-bite-"+sample);assertControls(mc);
                        if(sample==0){
                            var fishing=ToolkitMod.FISHING.settings();
                            boolean original=fishing.lowBitePing;
                            float oldVolume=fishing.biteSoundVolume;boolean oldChime=fishing.oniEnergyAlert;
                            fishing.biteSoundVolume=3.5f;fishing.oniEnergyAlert=false;
                            pressTopic(mc,"Low bite ping");
                            var reloaded=new com.fishingpings.FishingConfig();
                            check(fishing.lowBitePing!=original&&reloaded.lowBitePing==fishing.lowBitePing,"Low bite ping toggle survives reload");
                            check(reloaded.biteSoundVolume==3.5f&&!reloaded.oniEnergyAlert,"Existing fishing audio settings survive reload");
                            fishing.biteSoundVolume=oldVolume;fishing.oniEnergyAlert=oldChime;
                            pressTopic(mc,"Low bite ping");
                            for(String name:new String[]{"bite_ping","oni_approach_chime","oni_full_chime"}){
                                var id=net.minecraft.resources.Identifier.fromNamespaceAndPath("skyblocktoolkit",name);
                                var sound=mc.getSoundManager().getSoundEvent(id);
                                check(sound!=null&&sound.getSound(net.minecraft.util.RandomSource.create()).shouldPreload(),"Fishing cue preloads: "+name);
                                var event=net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id);
                                var result=mc.getSoundManager().play(new AlertSound(event,1,0.45f,2));
                                check(result==net.minecraft.client.sounds.SoundEngine.PlayResult.STARTED,"Fishing cue starts through the sound engine: "+name);
                            }
                        }
                        pressTopic(mc,"Oni Rod settings");
                        check(mc.gui.screen().children().stream().anyMatch(c->c instanceof AbstractWidget w&&ToolkitScreen.searchTarget(w,"Oni Rod full-energy chime")),"Fishing shortcut opens Oni Rod settings");
                    }
                    stage++;return;
                }
                if(stage==291){
                    Files.writeString(output.resolve("STATS-PASS.txt"),"PASS: reward-gated automatic Stats; no unused tools, storage or legacy armor rows; real named-stack eligibility; no registration/start controls; Overworld/Realm filters, session history and details in both themes and compact/full layouts\n");
                    for(int sample=0;sample<16;sample++)check(Files.exists(output.resolve("screenshots/booster-toast-"+sample+".png")),"Booster toast rendered: "+sample);
                    Files.writeString(output.resolve("PASS.txt"),"PASS: fixed breadcrumbs, disclosure focus/state, named search groups, single-item seasonal pages, related topic links; isolated topics, category memory, Escape/back navigation, every topic in both themes and layouts; global setting search, one sell-all selector, every control destination, Ctrl+F, Escape, no-match state and non-overlapping full/compact widgets; sell all key registration, assignment, persistence, cancel/unbind and safe menu click draining; coconut toggle, threshold validation, persistence and sound preview; light/dark full/compact menus and booster notifications; XP/money rate selection, independent XP HUD, timer ledger isolation, crop visibility, HUD editing and blackout\n");
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
        var screen=mc.gui.screen();screen.mouseScrolled(screen.width/2,screen.height/2,0,-100);
    }
    void switchTheme(net.minecraft.client.Minecraft mc){
        for(var child:mc.gui.screen().children())if(child instanceof Button b&&(b.getMessage().getString().equals("Dark mode")||b.getMessage().getString().equals("Light mode"))){b.onPress(null);return;}
        throw new AssertionError("theme switch missing");
    }
    void pressTopic(net.minecraft.client.Minecraft mc,String title){
        var screen=mc.gui.screen();screen.mouseScrolled(screen.width/2,screen.height/2,0,100);
        for(int i=0;i<12;i++){
            for(var child:screen.children())if(child instanceof Button b&&b.visible&&(b.getMessage().getString().equals(title)||b.getMessage().getString().startsWith(title+": "))){b.onPress(null);return;}
            screen.mouseScrolled(screen.width/2,screen.height/2,0,-1);
        }
        throw new AssertionError("topic missing: "+title);
    }
    void press(net.minecraft.client.Minecraft mc,String text){
        for(var child:mc.gui.screen().children())if(child instanceof Button b&&b.visible&&b.active&&b.getMessage().getString().startsWith(text)){b.onPress(null);return;}
        throw new AssertionError("button missing: "+text);
    }
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
}
