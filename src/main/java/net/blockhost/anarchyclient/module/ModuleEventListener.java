package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.event.ClientInputEvent;
import net.blockhost.anarchyclient.event.ClientTickEvent;
import net.blockhost.anarchyclient.event.HudRenderEvent;
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
}
