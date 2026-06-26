package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.event.AttackEntityEvent;
import net.blockhost.anarchyclient.event.BlockAttackEvent;
import net.blockhost.anarchyclient.event.BlockBreakingProgressEvent;
import net.blockhost.anarchyclient.event.BlockInteractEvent;
import net.blockhost.anarchyclient.event.CameraTransformEvent;
import net.blockhost.anarchyclient.event.ChatMessageEvent;
import net.blockhost.anarchyclient.event.ClientInputEvent;
import net.blockhost.anarchyclient.event.ClientTickEvent;
import net.blockhost.anarchyclient.event.EntityAddedEvent;
import net.blockhost.anarchyclient.event.EntityInteractEvent;
import net.blockhost.anarchyclient.event.EntityRemovedEvent;
import net.blockhost.anarchyclient.event.FovEvent;
import net.blockhost.anarchyclient.event.GameJoinedEvent;
import net.blockhost.anarchyclient.event.GameLeftEvent;
import net.blockhost.anarchyclient.event.HudRenderEvent;
import net.blockhost.anarchyclient.event.ItemTooltipEvent;
import net.blockhost.anarchyclient.event.ItemStopUseEvent;
import net.blockhost.anarchyclient.event.ItemUseEvent;
import net.blockhost.anarchyclient.event.MouseClickEvent;
import net.blockhost.anarchyclient.event.MouseScrollInputEvent;
import net.blockhost.anarchyclient.event.OpenScreenEvent;
import net.blockhost.anarchyclient.event.PacketReceiveEvent;
import net.blockhost.anarchyclient.event.PacketSendEvent;
import net.blockhost.anarchyclient.event.PacketSentEvent;
import net.blockhost.anarchyclient.event.ParticleEvent;
import net.blockhost.anarchyclient.event.PreventEdgeFallEvent;
import net.blockhost.anarchyclient.event.SendChatEvent;
import net.blockhost.anarchyclient.event.SoundPacketEvent;
import net.blockhost.anarchyclient.event.TabPlayerNameEvent;
import net.blockhost.anarchyclient.event.WorldRenderEvent;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

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
    public void handleMouseClick(final MouseClickEvent event) {
        if (this.module.mouseClick(event.client(), event.buttonInfo(), event.action())) {
            event.cancel();
        }
    }

    @EventHandler
    public void handleAttackEntity(final AttackEntityEvent event) {
        if (this.module.attackEntity(event.client(), event.player(), event.target())) {
            event.cancel();
        }
    }

    @EventHandler
    public void handleBlockAttack(final BlockAttackEvent event) {
        if (this.module.attackBlock(event.client(), event.pos(), event.direction())) {
            event.cancel();
        }
    }

    @EventHandler
    public void handleChatMessage(final ChatMessageEvent event) {
        Component message = this.module.chatMessage(event.client(), event.message());
        if (message != null) {
            event.message(message);
        }
    }

    @EventHandler
    public void handleSendChat(final SendChatEvent event) {
        String message = this.module.sendChatMessage(event.client(), event.message(), event.command());
        if (message != null) {
            event.message(message);
        }
    }

    @EventHandler
    public void handleTabPlayerName(final TabPlayerNameEvent event) {
        Component name = this.module.tabPlayerName(event.client(), event.playerInfo(), event.name());
        if (name != null) {
            event.name(name);
        }
    }

    @EventHandler
    public void handleItemTooltip(final ItemTooltipEvent event) {
        this.module.itemTooltip(event.client(), event.stack(), event.lines());
    }

    @EventHandler
    public void handleEntityAdded(final EntityAddedEvent event) {
        this.module.entityAdded(event.client(), event.entity());
    }

    @EventHandler
    public void handleEntityRemoved(final EntityRemovedEvent event) {
        this.module.entityRemoved(event.client(), event.entity(), event.reason());
    }

    @EventHandler
    public void handleBlockBreakingProgress(final BlockBreakingProgressEvent event) {
        this.module.blockBreakingProgress(event.client(), event.breakerId(), event.pos(), event.progress());
    }

    @EventHandler
    public void handleBlockInteract(final BlockInteractEvent event) {
        if (this.module.blockInteract(event.client(), event.hand(), event.hitResult())) {
            event.cancel();
        }
    }

    @EventHandler
    public void handleEntityInteract(final EntityInteractEvent event) {
        if (this.module.entityInteract(event.client(), event.player(), event.entity(), event.hitResult(), event.hand())) {
            event.cancel();
        }
    }

    @EventHandler
    public void handleItemUse(final ItemUseEvent event) {
        if (this.module.itemUse(event.client(), event.hand())) {
            event.cancel();
        }
    }

    @EventHandler
    public void handleItemStopUse(final ItemStopUseEvent event) {
        this.module.itemStopUse(event.client(), event.hand(), event.stack(), event.remainingTicks());
    }

    @EventHandler
    public void handleOpenScreen(final OpenScreenEvent event) {
        if (this.module.openScreen(event.client(), event.screen())) {
            event.cancel();
        }
    }

    @EventHandler
    public void handleMouseScroll(final MouseScrollInputEvent event) {
        if (this.module.mouseScroll(event.client(), event.xOffset(), event.yOffset())) {
            event.cancel();
        }
    }

    @EventHandler
    public void handleParticle(final ParticleEvent event) {
        if (this.module.particle(event.client(), event.particle(), event.alwaysShow())) {
            event.cancel();
        }
    }

    @EventHandler
    public void handleFov(final FovEvent event) {
        event.fov(this.module.fov(event.client(), event.fov()));
    }

    @EventHandler
    public void handleCameraTransform(final CameraTransformEvent event) {
        Module.CameraTransform transform = this.module.cameraTransform(
                event.client(),
                event.position(),
                event.yaw(),
                event.pitch()
        );
        if (transform != null) {
            event.position(transform.position());
            event.yaw(transform.yaw());
            event.pitch(transform.pitch());
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
        Packet<?> replacement = this.module.replaceSendPacket(event.client(), event.connection(), event.packet());
        if (replacement != event.packet()) {
            event.packet(replacement);
        }
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
