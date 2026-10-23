package com.skyblocktoolkit;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** 1.21.11 uses one method for both chat and action-bar feedback. */
public final class ClientMessages {
    private ClientMessages() {}
    public static void system(LocalPlayer player, Component message) { player.displayClientMessage(message, false); }
    public static void overlay(LocalPlayer player, Component message) { player.displayClientMessage(message, true); }
}
