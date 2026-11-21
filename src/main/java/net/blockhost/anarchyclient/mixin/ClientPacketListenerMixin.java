package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.GameJoinedEvent;
import net.blockhost.anarchyclient.event.GameLeftEvent;
import net.blockhost.anarchyclient.event.SendChatEvent;
import net.blockhost.anarchyclient.event.SoundPacketEvent;
import net.blockhost.anarchyclient.module.impl.NoRotateSetModule;
import net.blockhost.anarchyclient.module.impl.TimeChangerModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(
            method = "handleSoundEvent",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                            shift = At.Shift.AFTER))
    private void anarchyclient$handleSoundEvent(final ClientboundSoundPacket packet, final CallbackInfo info) {
        AnarchyClient.MODULES.call(new SoundPacketEvent(Minecraft.getInstance(), packet));
    }

    // HEAD also runs on the network thread before vanilla moves the packet to the client thread, so only the
    // client-thread pass records the rotation.
    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void anarchyclient$beforeSetback(final ClientboundPlayerPositionPacket packet, final CallbackInfo info) {
        Minecraft client = Minecraft.getInstance();
        if (client.isSameThread()) {
            NoRotateSetModule.beforeSetback(client.player, packet);
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void anarchyclient$afterSetback(final ClientboundPlayerPositionPacket packet, final CallbackInfo info) {
        NoRotateSetModule.afterSetback(Minecraft.getInstance().player);
    }

    // Runs on the client thread only: off it, vanilla re-queues the packet before reaching TAIL.
    @Inject(method = "handleSetTime", at = @At("TAIL"))
    private void anarchyclient$afterSetTime(final ClientboundSetTimePacket packet, final CallbackInfo info) {
        TimeChangerModule.afterServerTime(Minecraft.getInstance().level);
    }

    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void anarchyclient$handleLogin(final ClientboundLoginPacket packet, final CallbackInfo info) {
        AnarchyClient.MODULES.call(new GameJoinedEvent(Minecraft.getInstance(), (ClientPacketListener) (Object) this));
    }

    @Inject(method = "clearLevel", at = @At("HEAD"))
    private void anarchyclient$clearLevel(final CallbackInfo info) {
        AnarchyClient.MODULES.call(new GameLeftEvent(Minecraft.getInstance(), (ClientPacketListener) (Object) this));
    }

    @ModifyVariable(method = "sendChat", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String anarchyclient$sendChat(final String message) {
        String changed = AnarchyClient.MODULES
                .call(new SendChatEvent(Minecraft.getInstance(), message, false))
                .message();
        // Modules may lengthen the message (Better Chat's suffix); past 256 characters the chat packet cannot be
        // encoded and the client disconnects.
        return changed == null ? message : StringUtil.trimChatMessage(changed);
    }

    @ModifyVariable(method = "sendCommand", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String anarchyclient$sendCommand(final String command) {
        return AnarchyClient.MODULES
                .call(new SendChatEvent(Minecraft.getInstance(), command, true))
                .message();
    }
}
