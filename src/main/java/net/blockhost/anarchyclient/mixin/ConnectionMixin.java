package net.blockhost.anarchyclient.mixin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.PacketEventSilencer;
import net.blockhost.anarchyclient.event.PacketReceiveEvent;
import net.blockhost.anarchyclient.event.PacketSendEvent;
import net.blockhost.anarchyclient.event.PacketSentEvent;
import net.blockhost.anarchyclient.network.PositionPacketLimiter;
import net.blockhost.anarchyclient.rotation.ServerRotationTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires packet events for the client's own connections.
 *
 * <p>Receive events run on the Netty thread so listeners can cancel or replace packets before
 * vanilla handles them. Listeners must move other side effects to the client thread with
 * {@code MainThread.run}. Connections owned by the
 * integrated server (singleplayer and LAN) receive serverbound packets and are skipped.
 */
@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @Shadow
    public abstract PacketFlow getReceiving();

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void anarchyclient$receivePacket(
            final ChannelHandlerContext context, final Packet<?> packet, final CallbackInfo info) {
        if (!this.anarchyclient$isClientConnection()) {
            return;
        }
        PacketReceiveEvent event = AnarchyClient.MODULES.call(
                new PacketReceiveEvent(Minecraft.getInstance(), (Connection) (Object) this, packet));
        if (event.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"),
            cancellable = true)
    private void anarchyclient$sendPacket(
            final Packet<?> packet,
            final ChannelFutureListener listener,
            final boolean flush,
            final CallbackInfo info) {
        if (anarchyclient$resending.get() || !this.anarchyclient$isClientConnection()) {
            return;
        }
        Packet<?> outgoing = packet;
        if (!PacketEventSilencer.silent()) {
            PacketSendEvent event = AnarchyClient.MODULES.call(
                    new PacketSendEvent(Minecraft.getInstance(), (Connection) (Object) this, packet));
            if (event.isCancelled()) {
                info.cancel();
                return;
            }
            outgoing = event.packet();
        }
        // Applies to silent sends too, such as packets a lag module releases together.
        Packet<?> limited = PositionPacketLimiter.limit(outgoing);
        if (limited != packet) {
            info.cancel();
            anarchyclient$resending.set(true);
            try {
                PacketEventSilencer.runSilently(() -> ((Connection) (Object) this).send(limited, listener, flush));
            } finally {
                anarchyclient$resending.set(false);
            }
        }
    }

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("RETURN"))
    private void anarchyclient$sentPacket(
            final Packet<?> packet,
            final ChannelFutureListener listener,
            final boolean flush,
            final CallbackInfo info) {
        if (!this.anarchyclient$isClientConnection()) {
            return;
        }
        // Silent sends (replaced or delayed packets) also reach the server, so they count too.
        ServerRotationTracker.onSent(packet);
        if (!PacketEventSilencer.silent()) {
            AnarchyClient.MODULES.call(
                    new PacketSentEvent(Minecraft.getInstance(), (Connection) (Object) this, packet));
        }
    }

    /** Set while this mixin sends a replaced packet, so the replacement is not handled twice. */
    @Unique
    private static final ThreadLocal<Boolean> anarchyclient$resending = ThreadLocal.withInitial(() -> false);

    @Unique
    private boolean anarchyclient$isClientConnection() {
        return this.getReceiving() == PacketFlow.CLIENTBOUND;
    }
}
