package com.moneybooster;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class MoneyBoosterMod implements ClientModInitializer {
    public static final String MOD_ID = "moneyboosterhud";
    private static KeyMapping menuKeyMapping;
    private static net.minecraft.client.multiplayer.ClientLevel previousLevel;
    private static boolean openMenuRequested = false;

    public static void requestOpenMenu() {
        openMenuRequested = true;
    }

    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide()) BoosterSources.capture(player.getItemInHand(hand));
            return net.minecraft.world.InteractionResult.PASS;
        });
        // 1. Register HUD element
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "hud_overlay"),
                new BoosterHudOverlay()
        );

        // 2. Register Game Chat Message listeners (server/system messages)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                BoosterManager.onChatMessage(message.getString());
            }
        });

        // 4. Tick event for scoreboard checks, rate tracker ticks, and delayed screen opening
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != previousLevel) {
                BoosterManager.reset(); BoosterStatsManager.resetConnection(); RateTracker.disconnect();
                com.skyblocktoolkit.CropHarvestTracker.reset();
                previousLevel = client.level;
            }
            BoosterManager.onClientTick(client);
            BoosterStatsManager.tick();
            XpTracker.tick(client);
            com.skyblocktoolkit.CropHarvestTracker.tick();
            RateTracker.onClientTick();

            if (openMenuRequested) {
                openMenuRequested = false;
                client.setScreenAndShow(new com.skyblocktoolkit.ToolkitScreen(null,1));
            }

            if (menuKeyMapping != null && menuKeyMapping.consumeClick()) {
                client.setScreenAndShow(new com.skyblocktoolkit.ToolkitScreen(null,1));
            }
        });

    }
}
