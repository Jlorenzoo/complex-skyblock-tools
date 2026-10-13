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
                previousLevel = client.level;
            }
            BoosterManager.onClientTick(client);
            BoosterStatsManager.tick();
            RateTracker.onClientTick();

            if (openMenuRequested) {
                openMenuRequested = false;
                client.setScreenAndShow(new com.skyblocktoolkit.ToolkitScreen(null,1));
            }

            if (menuKeyMapping != null && menuKeyMapping.consumeClick()) {
                client.setScreenAndShow(new com.skyblocktoolkit.ToolkitScreen(null,1));
            }
        });

        // 5. Register Optional Keybind for menu (unbound by default so it doesn't conflict)
        // 6. Register Client Command: /boosterhud opens the menu directly!
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("boostermenu").executes(ctx->{requestOpenMenu();return 1;}));
            dispatcher.register(ClientCommands.literal("boosterhud")
                    .executes(ctx -> {
                        requestOpenMenu();
                        return 1;
                    })
                    .then(ClientCommands.literal("menu").executes(ctx -> {
                        requestOpenMenu();
                        return 1;
                    }))
                    .then(ClientCommands.literal("stats").executes(ctx -> {
                        requestOpenMenu();
                        return 1;
                    }))
                    .then(ClientCommands.literal("reset").executes(ctx -> {
                        BoosterManager.reset();
                        return 1;
                    }))
            );
        });
    }
}
