package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.command.ClientCommands;
import net.blockhost.anarchyclient.command.CommandPrefix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs a chat line typed with the configurable {@link CommandPrefix} through the client's own
 * {@link ClientCommands} dispatcher and cancels the send, so it never reaches the server or the vanilla
 * {@code /} command path.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$runPrefixCommand(final String message, final boolean addToRecent, final CallbackInfo info) {
        char prefix = CommandPrefix.first();
        if (prefix == '/' || message.isEmpty() || message.charAt(0) != prefix) {
            return;
        }
        ClientCommands.execute(Minecraft.getInstance().getConnection(), message.substring(1).trim());
        info.cancel();
    }
}
