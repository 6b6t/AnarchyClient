package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.impl.BetterChatModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component anarchyclient$modifyChatMessage(final Component message) {
        return AnarchyClient.MODULES.chatMessage(Minecraft.getInstance(), message);
    }

    @Inject(method = "clearMessages", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$preventChatClear(final boolean history, final CallbackInfo info) {
        if (BetterChatModule.shouldPreventClearMessages()) {
            info.cancel();
        }
    }
}
