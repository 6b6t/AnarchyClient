package net.blockhost.anarchyclient.mixin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.PacketEventSilencer;
import net.blockhost.anarchyclient.event.PacketReceiveEvent;
import net.blockhost.anarchyclient.event.PacketSendEvent;
import net.blockhost.anarchyclient.event.PacketSentEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void anarchyclient$receivePacket(final ChannelHandlerContext context, final Packet<?> packet,
                                             final CallbackInfo info) {
        PacketReceiveEvent event = AnarchyClient.MODULES.call(new PacketReceiveEvent(
                Minecraft.getInstance(),
                (Connection) (Object) this,
                packet
        ));
        if (event.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void anarchyclient$sendPacket(final Packet<?> packet, final ChannelFutureListener listener,
                                          final boolean flush, final CallbackInfo info) {
        if (PacketEventSilencer.silent()) {
            return;
        }
        PacketSendEvent event = AnarchyClient.MODULES.call(new PacketSendEvent(
                Minecraft.getInstance(),
                (Connection) (Object) this,
                packet
        ));
        if (event.isCancelled()) {
            info.cancel();
            return;
        }
        if (event.packet() != packet) {
            info.cancel();
            PacketEventSilencer.runSilently(() -> ((Connection) (Object) this).send(event.packet(), listener, flush));
        }
    }

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("RETURN")
    )
    private void anarchyclient$sentPacket(final Packet<?> packet, final ChannelFutureListener listener,
                                          final boolean flush, final CallbackInfo info) {
        if (!PacketEventSilencer.silent()) {
            AnarchyClient.MODULES.call(new PacketSentEvent(Minecraft.getInstance(), (Connection) (Object) this, packet));
        }
    }
}
