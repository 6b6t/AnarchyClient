package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.util.MainThread;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class VelocityModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Scale")
            .addAllOptions(List.of("Scale", "Cancel", "Reverse", "Jump Reset"))
            .build()));
    private final NumberSetting horizontal = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("horizontal")
            .name("Horizontal")
            .defaultValue(0.0)
            .min(0.0)
            .max(100.0)
            .step(5.0)
            .build()));
    private final NumberSetting vertical = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical")
            .name("Vertical")
            .defaultValue(0.0)
            .min(0.0)
            .max(100.0)
            .step(5.0)
            .build()));
    private final BooleanSetting allEntities = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("all_entities")
            .name("All Entities")
            .defaultValue(false)
            .build()));
    private final NumberSetting chance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("chance")
            .name("Chance")
            .defaultValue(100.0)
            .min(0.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private final BooleanSetting onlyGround = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("only_ground")
            .name("Ground Only")
            .defaultValue(false)
            .build()));
    private int jumpResetTicks;

    public VelocityModule() {
        super("velocity", "Velocity", ModuleCategory.MOVEMENT,
                "Reduces, cancels, or reverses the knockback you take from hits and explosions, with an optional jump reset mode.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.jumpResetTicks <= 0 || client.player == null) {
            return;
        }
        this.jumpResetTicks--;
        if (client.player.onGround()) {
            Vec3 velocity = client.player.getDeltaMovement();
            client.player.setDeltaMovement(velocity.x, Math.max(velocity.y, 0.42), velocity.z);
            this.jumpResetTicks = 0;
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        LocalPlayer player = client.player;
        if (player == null) {
            return false;
        }
        if (packet instanceof ClientboundSetEntityMotionPacket motion) {
            boolean self = motion.id() == player.getId();
            if (!self && !this.allEntities.value() || !this.applies(player, self)) {
                return false;
            }
            Vec3 movement = this.adjust(motion.movement(), self);
            if (movement != null) {
                handleReplacement(connection, new ClientboundSetEntityMotionPacket(motion.id(), movement));
            }
            return true;
        }
        if (packet instanceof ClientboundExplodePacket explosion && explosion.playerKnockback().isPresent()) {
            if (!this.applies(player, true)) {
                return false;
            }
            Vec3 knockback = this.adjust(explosion.playerKnockback().get(), true);
            handleReplacement(connection, new ClientboundExplodePacket(explosion.center(), explosion.radius(),
                    explosion.blockCount(), Optional.ofNullable(knockback), explosion.explosionParticle(),
                    explosion.explosionSound(), explosion.blockParticles(), explosion.playSound()));
            return true;
        }
        return false;
    }

    private boolean applies(final LocalPlayer player, final boolean self) {
        if (this.onlyGround.value() && self && !player.onGround()) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble(100.0) < this.chance.value();
    }

    /** Returns the motion to apply instead of the server's, or {@code null} to drop it. */
    private Vec3 adjust(final Vec3 movement, final boolean self) {
        switch (this.mode.value()) {
            case "Cancel" -> {
                return null;
            }
            case "Reverse" -> {
                return scaleMotion(movement.reverse(), this.horizontal.value(), this.vertical.value());
            }
            case "Jump Reset" -> {
                if (self) {
                    MainThread.run(() -> this.jumpResetTicks = 3);
                }
                return scaleMotion(movement, this.horizontal.value(), this.vertical.value());
            }
            default -> {
                return scaleMotion(movement, this.horizontal.value(), this.vertical.value());
            }
        }
    }

    /**
     * Hands a replacement packet to vanilla the same way {@code Connection.channelRead0} does, so it
     * keeps its place in the client's packet queue.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void handleReplacement(final Connection connection, final Packet packet) {
        PacketListener listener = connection.getPacketListener();
        if (listener == null) {
            return;
        }
        try {
            packet.handle(listener);
        } catch (RunningOnDifferentThreadException ignored) {
            // Vanilla handlers queue themselves on the client thread.
        }
    }

    static Vec3 scaleMotion(final Vec3 movement, final double horizontalPercent, final double verticalPercent) {
        double horizontalScale = horizontalPercent / 100.0;
        double verticalScale = verticalPercent / 100.0;
        return new Vec3(movement.x * horizontalScale, movement.y * verticalScale, movement.z * horizontalScale);
    }
}
