package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.command.ClientCommands;
import net.blockhost.anarchyclient.command.CommandPrefix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs a chat line typed with the configurable {@link CommandPrefix} through the client's own
 * {@link ClientCommands} dispatcher and cancels the send, so it never reaches the server or the vanilla
 * {@code /} command path.
 *
 * <p>Closing the chat screen is the caller's job (it runs after {@code handleChatInput} either way), so
 * cancelling here only skips the send itself and the recent-message history — the latter is replayed
 * below so prefix commands stay recallable with the up arrow like any other line.</p>
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    public abstract String normalizeChatMessage(String message);

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$runPrefixCommand(final String message, final boolean addToRecent, final CallbackInfo info) {
        char prefix = CommandPrefix.first();
        String normalized = normalizeChatMessage(message);
        if (prefix == '/' || normalized.isEmpty() || normalized.charAt(0) != prefix) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (addToRecent) {
            client.gui.hud.getChat().addRecentChat(normalized);
        }
        ClientCommands.execute(client.getConnection(), normalized.substring(1).trim());
        info.cancel();
    }
}
