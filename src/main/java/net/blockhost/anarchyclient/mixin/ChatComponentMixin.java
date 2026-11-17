package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.impl.BetterChatModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Shadow
    protected abstract void refreshTrimmedMessages();

    @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component anarchyclient$modifyChatMessage(final Component message) {
        return AnarchyClient.MODULES.chatMessage(Minecraft.getInstance(), message);
    }

    @Inject(
            method = "addMessage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V"
            )
    )
    private void anarchyclient$replaceDuplicateMessage(final Component contents, final @Nullable MessageSignature signature,
                                                       final GuiMessageSource source, final @Nullable GuiMessageTag tag,
                                                       final CallbackInfo info) {
        Component replaced = BetterChatModule.takeReplacedMessage();
        if (replaced != null && !this.allMessages.isEmpty() && this.allMessages.getFirst().content() == replaced) {
            this.allMessages.removeFirst();
            this.refreshTrimmedMessages();
        }
    }

    // Runs after vanilla flushes the delayed chat queue and the deletion queue, so only the
    // visible messages and history survive.
    @Inject(
            method = "clearMessages",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;trimmedMessages:Ljava/util/List;"
            ),
            cancellable = true
    )
    private void anarchyclient$preventChatClear(final boolean history, final CallbackInfo info) {
        if (BetterChatModule.shouldPreventClearMessages()) {
            info.cancel();
        }
    }
}
