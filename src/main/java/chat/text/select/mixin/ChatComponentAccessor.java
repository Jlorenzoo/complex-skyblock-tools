/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.components.ChatComponent
 *  net.minecraft.client.multiplayer.chat.GuiMessage$Line
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package chat.text.select.mixin;

import java.util.List;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={ChatComponent.class})
public interface ChatComponentAccessor {
    @Accessor(value="trimmedMessages")
    public List<GuiMessage.Line> chattextselect$getTrimmedMessages();

    @Accessor(value="chatScrollbarPos")
    public int chattextselect$getChatScrollbarPos();
}

