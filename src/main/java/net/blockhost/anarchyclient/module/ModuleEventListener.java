package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.event.ClientInputEvent;
import net.blockhost.anarchyclient.event.ClientTickEvent;
import net.blockhost.anarchyclient.event.GameJoinedEvent;
import net.blockhost.anarchyclient.event.GameLeftEvent;
import net.blockhost.anarchyclient.event.HudRenderEvent;
import net.blockhost.anarchyclient.event.PacketReceiveEvent;
import net.blockhost.anarchyclient.event.PacketSendEvent;
import net.blockhost.anarchyclient.event.PacketSentEvent;
import net.blockhost.anarchyclient.event.PreventEdgeFallEvent;
import net.blockhost.anarchyclient.event.SoundPacketEvent;
import net.blockhost.anarchyclient.event.WorldRenderEvent;
import net.lenni0451.lambdaevents.EventHandler;

public final class ModuleEventListener {

    private final Module module;

    public ModuleEventListener(final Module module) {
        this.module = module;
    }

    @EventHandler
    public void handleClientTick(final ClientTickEvent event) {
        this.module.tick(event.client());
    }

    @EventHandler
    public void handleClientInput(final ClientInputEvent event) {
        this.module.updateInput(event.client(), event.input());
    }

    @EventHandler
    public void handlePreventEdgeFall(final PreventEdgeFallEvent event) {
        if (this.module.preventEdgeFall(event.client(), event.player())) {
            event.prevent();
        }
    }

    @EventHandler
    public void handleWorldRender(final WorldRenderEvent event) {
        this.module.renderWorld(event.context());
    }

    @EventHandler
    public void handleHudRender(final HudRenderEvent event) {
        this.module.renderHud(event.client(), event.graphics());
    }

    @EventHandler
    public void handleSoundPacket(final SoundPacketEvent event) {
        this.module.soundPacket(event.client(), event.packet());
    }

    @EventHandler
    public void handlePacketReceive(final PacketReceiveEvent event) {
        if (this.module.receivePacket(event.client(), event.connection(), event.packet())) {
            event.cancel();
        }
    }

    @EventHandler
    public void handlePacketSend(final PacketSendEvent event) {
        if (this.module.sendPacket(event.client(), event.connection(), event.packet())) {
            event.cancel();
        }
    }

    @EventHandler
    public void handlePacketSent(final PacketSentEvent event) {
        this.module.sentPacket(event.client(), event.connection(), event.packet());
    }

    @EventHandler
    public void handleGameJoined(final GameJoinedEvent event) {
        this.module.gameJoined(event.client(), event.listener());
    }

    @EventHandler
    public void handleGameLeft(final GameLeftEvent event) {
        this.module.gameLeft(event.client(), event.listener());
    }
}
