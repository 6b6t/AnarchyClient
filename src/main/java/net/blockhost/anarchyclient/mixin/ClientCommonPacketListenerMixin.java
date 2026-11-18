package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.AutoReconnectModule;
import net.blockhost.anarchyclient.module.impl.SilentDisconnectModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

/**
 * Silent Disconnect replaces the disconnect screen with the screen vanilla would return to.
 * Vanilla still tears down the connection and the level, and transfers are never touched.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerMixin {

    @Unique
    private static final SystemToast.SystemToastId ANARCHYCLIENT$DISCONNECT_TOAST = new SystemToast.SystemToastId(10000L);

    @Shadow
    @Final
    protected @Nullable ServerData serverData;

    @Shadow
    @Final
    protected @Nullable Screen postDisconnectScreen;

    @Shadow
    protected boolean isTransferring;

    @Unique
    private @Nullable Component anarchyclient$silentReason;

    @Inject(method = "createDisconnectScreen", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$silentDisconnect(final DisconnectionDetails details, final CallbackInfoReturnable<Screen> info) {
        if (this.isTransferring || !SilentDisconnectModule.shouldSuppress(Minecraft.getInstance())) {
            return;
        }
        this.anarchyclient$silentReason = details.reason();
        AutoReconnectModule.noteSilentDisconnect();
        info.setReturnValue(Objects.requireNonNullElseGet(
                this.postDisconnectScreen,
                () -> this.serverData != null ? new JoinMultiplayerScreen(new TitleScreen()) : new TitleScreen()
        ));
    }

    // Vanilla clears toasts and chat while disconnecting, so the reason is shown afterwards.
    @Inject(method = "onDisconnect", at = @At("TAIL"))
    private void anarchyclient$showSilentReason(final DisconnectionDetails details, final CallbackInfo info) {
        Component reason = this.anarchyclient$silentReason;
        if (reason == null) {
            return;
        }
        this.anarchyclient$silentReason = null;
        SystemToast.addOrUpdate(
                Minecraft.getInstance().gui.toastManager(),
                ANARCHYCLIENT$DISCONNECT_TOAST,
                Component.literal("Disconnected"),
                reason
        );
    }
}
