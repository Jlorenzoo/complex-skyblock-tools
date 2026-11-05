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
            PassiveIncomeCapture.observe(overlay ? "action-bar" : "system-chat", message);
            if (!overlay) {
                BoosterManager.onChatMessage(message.getString());
                com.skyblocktoolkit.GoldShardTracker.chat(message.getString());
            }
        });

        // 4. Tick event for scoreboard checks, rate tracker ticks, and delayed screen opening
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            com.skyblocktoolkit.ItemStatsTracker.tick(client);
            if (client.level != previousLevel) {
                BoosterManager.reset(); BoosterStatsManager.resetConnection(); RateTracker.disconnect();
                PassiveIncomeCapture.reset();
                com.skyblocktoolkit.CropHarvestTracker.reset();
                com.skyblocktoolkit.GoldShardTracker.reset();
                com.skyblocktoolkit.VisualAlertOverlay.reset();
                previousLevel = client.level;
            }
            BoosterManager.onClientTick(client);
            BoosterStatsManager.tick();
            XpTracker.tick(client);
            com.skyblocktoolkit.CropHarvestTracker.tick();
            long now = System.currentTimeMillis();
            boolean farming = client.player != null && client.gui.screen() == null
                    && (com.skyblocktoolkit.CropHarvestTracker.isActive(now)
                        || (client.player.getMainHandItem().getItem() instanceof net.minecraft.world.item.HoeItem
                            && client.options.keyAttack.isDown()));
            if (farming) {
                BoosterStatsManager.farmingActivity(now);
            }
            com.skyblocktoolkit.GoldShardTracker.tick(client);
            RateTracker.onClientTick();
            PassiveIncomeCapture.tick(client, farming);
            com.skyblocktoolkit.VisualAlertOverlay.tick(System.currentTimeMillis());

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
