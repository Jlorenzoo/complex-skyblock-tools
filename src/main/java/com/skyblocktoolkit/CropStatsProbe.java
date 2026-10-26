package com.skyblocktoolkit;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Opt-in diagnostic: establish whether a server maintains useful vanilla crop statistics. */
public final class CropStatsProbe {
    private static final long TIMEOUT = 10_000_000_000L;
    private static final long COOLDOWN = 5_000_000_000L;
    private static final Map<String, Block> CROPS = crops();
    private static final CropStatSample SAMPLE = new CropStatSample();
    private static Object connection;
    private static String pending;
    private static long requested, lastRequest;
    private static boolean hasRequested;
    private static String hoe = "";
    private static long started;
    private static CropStatSample.Result lastResult;
    private static String feedback = "Validate counts before comparing hoes.";

    private CropStatsProbe() {}

    private static Map<String, Block> crops() {
        var result = new LinkedHashMap<String, Block>();
        result.put("wheat", Blocks.WHEAT); result.put("carrots", Blocks.CARROTS);
        result.put("potatoes", Blocks.POTATOES); result.put("beetroots", Blocks.BEETROOTS);
        result.put("nether wart", Blocks.NETHER_WART); result.put("cocoa", Blocks.COCOA);
        result.put("sugar cane", Blocks.SUGAR_CANE); result.put("cactus", Blocks.CACTUS);
        result.put("pumpkins", Blocks.PUMPKIN); result.put("melons", Blocks.MELON);
        return result;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> checkContext());
    }

    public static void register(com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> root) {
        var command = ClientCommands.literal("crops").executes(ctx -> {
            say("Open Right Shift > Farming > Crop harvest test for Start, Stop, Cancel and results. The optional /toolkit crops start/stop commands use the same test. Counts need validation on this server.");
            return 1;
        });
        command.then(ClientCommands.literal("start").executes(ctx -> request("start")));
        command.then(ClientCommands.literal("stop").executes(ctx -> request("stop")));
        command.then(ClientCommands.literal("status").executes(ctx -> {
            checkContext();
            say(pending != null ? "Waiting for server statistics (up to 10 seconds)." : SAMPLE.active()
                ? "Crop-stat sample running. Use /toolkit crops stop after harvesting."
                : "No crop-stat sample running. Use /toolkit crops start.");
            return 1;
        }));
        command.then(ClientCommands.literal("cancel").executes(ctx -> { cancel(); return 1; }));
        root.then(command);
    }

    public static boolean blockedOnCurrentServer() {
        var server = Minecraft.getInstance().getCurrentServer();
        return server != null && CropStatsPolicy.requiresApproval(server.ip);
    }
    public static boolean canStart() { return connected() && !blockedOnCurrentServer() && pending == null && !SAMPLE.active() && cooledDown(); }
    public static boolean canStop() { return connected() && !blockedOnCurrentServer() && pending == null && SAMPLE.active() && cooledDown(); }
    public static boolean canCancel() { return pending != null || SAMPLE.active(); }
    private static boolean connected() { var mc=Minecraft.getInstance(); return mc.player != null && mc.getConnection() != null; }
    private static boolean cooledDown() { return !hasRequested || System.nanoTime() - lastRequest >= COOLDOWN; }
    public static void start() { request("start"); }
    public static void stop() { request("stop"); }
    public static void cancel() { reset(); say("Crop-stat test cancelled."); }
    public static String menuStatus() {
        if (blockedOnCurrentServer()) return "Complex: staff approval required";
        if (pending != null) return "Waiting for server statistics...";
        if (SAMPLE.active()) return String.format(Locale.ROOT,"Test running: %.0fs",(System.nanoTime()-started)/1_000_000_000.0);
        return lastResult != null ? "Test finished · unvalidated count" : connected() ? "Ready to test server crop counts" : "Join a server to start a test";
    }
    public static String menuTotal() { return lastResult == null ? "Crop blocks: --" : String.format(Locale.ROOT,"Crop blocks: %,d",lastResult.total()); }
    public static String menuRate() {
        return lastResult == null ? "Rate: -- blocks/h" : String.format(Locale.ROOT,"Rate: %,.0f blocks/h · %.1fs",lastResult.hourly(),lastResult.elapsedNanos()/1_000_000_000.0);
    }
    public static String menuFeedback() {
        if (blockedOnCurrentServer()) return "Blocked until Complex staff approves it.";
        if (pending == null && SAMPLE.active() && !cooledDown()) return "Stop becomes available after 5 seconds.";
        return feedback;
    }

    private static int request(String operation) {
        var mc = Minecraft.getInstance();
        checkContext();
        if (mc.player == null || mc.getConnection() == null) { say("Join a server first."); return 0; }
        if (blockedOnCurrentServer()) { say("Complex has not approved this custom counter. Ask staff before use; no statistics request sent."); return 0; }
        if (pending != null) { say("Still waiting for the previous statistics request."); return 0; }
        if (operation.equals("start") && SAMPLE.active()) { say("A sample is already running. Stop or cancel it first."); return 0; }
        if (operation.equals("stop") && !SAMPLE.active()) { say("Start a sample first: /toolkit crops start."); return 0; }
        long now = System.nanoTime();
        if (hasRequested && now - lastRequest < COOLDOWN) { say("Wait 5 seconds between statistics requests."); return 0; }
        connection = mc.getConnection();
        pending = operation;
        requested = lastRequest = now;
        hasRequested = true;
        if (operation.equals("start")) { hoe = mc.player.getMainHandItem().getHoverName().getString(); lastResult = null; }
        mc.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        say("Requesting the server's crop block statistics...");
        return 1;
    }

    private static void checkContext() {
        var mc = Minecraft.getInstance();
        if (connection != null && (mc.player == null || mc.getConnection() != connection)) {
            reset(); lastResult = null; feedback = "Connection changed; sample discarded."; hasRequested = false; return;
        }
        if (pending != null && System.nanoTime() - requested >= TIMEOUT) {
            reset();
            say("No statistics reply within 10 seconds. Sample discarded; this server may not expose vanilla statistics.");
            feedback = "No server reply; test discarded.";
        }
    }

    /** Called on the client thread after vanilla has applied the received statistics. */
    public static void onStatsReceived() {
        checkContext();
        if (pending == null) return;
        var mc = Minecraft.getInstance();
        var counts = new LinkedHashMap<String, Integer>();
        for (var crop : CROPS.entrySet()) counts.put(crop.getKey(), mc.player.getStats().getValue(Stats.BLOCK_MINED, crop.getValue()));
        String operation = pending;
        pending = null;
        long now = System.nanoTime();
        if (operation.equals("start")) {
            SAMPLE.start(counts, now);
            started = now;
            say("Server replied. Sample started with " + hoe + ". Harvest a known number of crops, then reopen Farming and press Stop crop test.");
            say("Diagnostic only: vanilla stats may omit hoe effects or include immature crops. Rune/ability messages are ignored.");
            feedback = "Harvest, then reopen Farming and press Stop.";
            return;
        }
        var result = SAMPLE.finish(counts, now);
        if (!result.valid()) { say("Statistics reset or became incomplete. Sample discarded; no rate calculated."); feedback = "Counters changed; restart the test."; return; }
        lastResult = result;
        say(String.format(Locale.ROOT, "%s: server reports %,d crop blocks in %.1fs (%,.0f/h). Unvalidated server statistic.",
            hoe, result.total(), result.elapsedNanos() / 1_000_000_000.0, result.hourly()));
        if (result.total() == 0) {
            say("No increase reported. If you harvested crops, this counter is unsuitable on this server.");
            feedback = "No gain reported; server may not track crops.";
        } else {
            result.changes().entrySet().stream().filter(e -> e.getValue() > 0).sorted(Map.Entry.comparingByKey())
                .forEach(e -> say(e.getKey() + ": " + e.getValue()));
            say("Compare against a known harvest before trusting this count, especially crops broken by hoe effects.");
            feedback = "Check against a known number of harvested crops.";
        }
    }

    private static void reset() { SAMPLE.reset(); pending = null; connection = null; hoe = ""; }

    private static void say(String message) {
        feedback = message;
        var player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(Component.literal("[Crop stats] " + message));
    }
}
