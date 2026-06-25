package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

public final class AntiCrashModule extends Module {

    private final BooleanSetting log = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("log")
            .name("Log")
            .defaultValue(false)
            .build()));
    private final NumberSetting maxCoordinate = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_coordinate")
            .name("Coords")
            .defaultValue(30_000_000.0)
            .min(1024.0)
            .max(30_000_000.0)
            .step(1024.0)
            .build()));
    private final NumberSetting maxVelocity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_velocity")
            .name("Velocity")
            .defaultValue(1000.0)
            .min(16.0)
            .max(30_000_000.0)
            .step(16.0)
            .build()));
    private final NumberSetting maxParticles = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_particles")
            .name("Particles")
            .defaultValue(100_000.0)
            .min(1000.0)
            .max(1_000_000.0)
            .step(1000.0)
            .build()));
    private final NumberSetting maxExplosionRadius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_explosion_radius")
            .name("Explosion")
            .defaultValue(128.0)
            .min(16.0)
            .max(1024.0)
            .step(8.0)
            .build()));
    private final NumberSetting maxExplosionBlocks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_explosion_blocks")
            .name("Blocks")
            .defaultValue(4096.0)
            .min(256.0)
            .max(65_536.0)
            .step(256.0)
            .build()));
    private int logCooldownTicks;

    public AntiCrashModule() {
        super("anti_crash", "Anti Crash", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.logCooldownTicks > 0) {
            this.logCooldownTicks--;
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        SuspiciousPacket reason = suspiciousPacket(
                packet,
                this.maxCoordinate.value(),
                this.maxVelocity.value(),
                this.maxParticles.value().intValue(),
                this.maxExplosionRadius.value().floatValue(),
                this.maxExplosionBlocks.value().intValue()
        );
        if (reason == SuspiciousPacket.NONE) {
            return false;
        }
        if (this.log.value() && this.logCooldownTicks == 0 && client.player != null) {
            client.player.sendSystemMessage(Component.literal("Anti Crash blocked " + reason.displayName() + "."));
            this.logCooldownTicks = 40;
        }
        return true;
    }

    static SuspiciousPacket suspiciousPacket(final Packet<?> packet, final double maxCoordinate, final double maxVelocity,
                                            final int maxParticles, final float maxExplosionRadius,
                                            final int maxExplosionBlocks) {
        if (packet instanceof ClientboundExplodePacket explosion) {
            if (outsideLimit(explosion.center(), maxCoordinate)) {
                return SuspiciousPacket.EXPLOSION_POSITION;
            }
            if (explosion.playerKnockback().map(knockback -> outsideLimit(knockback, maxVelocity)).orElse(false)) {
                return SuspiciousPacket.EXPLOSION_KNOCKBACK;
            }
            if (!Float.isFinite(explosion.radius()) || explosion.radius() > maxExplosionRadius) {
                return SuspiciousPacket.EXPLOSION_RADIUS;
            }
            if (explosion.blockCount() > maxExplosionBlocks) {
                return SuspiciousPacket.EXPLOSION_BLOCKS;
            }
        } else if (packet instanceof ClientboundLevelParticlesPacket particles) {
            if (particles.getCount() > maxParticles
                    || outsideLimit(new Vec3(particles.getX(), particles.getY(), particles.getZ()), maxCoordinate)) {
                return SuspiciousPacket.PARTICLES;
            }
        } else if (packet instanceof ClientboundPlayerPositionPacket position) {
            if (outsideLimit(position.change().position(), maxCoordinate)
                    || outsideLimit(position.change().deltaMovement(), maxVelocity)) {
                return SuspiciousPacket.PLAYER_POSITION;
            }
        } else if (packet instanceof ClientboundSetEntityMotionPacket motion) {
            if (outsideLimit(motion.movement(), maxVelocity)) {
                return SuspiciousPacket.ENTITY_MOTION;
            }
        }
        return SuspiciousPacket.NONE;
    }

    static boolean outsideLimit(final Vec3 vector, final double limit) {
        return vector == null
                || !Double.isFinite(vector.x)
                || !Double.isFinite(vector.y)
                || !Double.isFinite(vector.z)
                || Math.abs(vector.x) > limit
                || Math.abs(vector.y) > limit
                || Math.abs(vector.z) > limit;
    }

    enum SuspiciousPacket {
        NONE("nothing"),
        EXPLOSION_POSITION("an explosion position packet"),
        EXPLOSION_KNOCKBACK("an explosion knockback packet"),
        EXPLOSION_RADIUS("an oversized explosion packet"),
        EXPLOSION_BLOCKS("an oversized explosion block packet"),
        PARTICLES("a particle packet"),
        PLAYER_POSITION("a player position packet"),
        ENTITY_MOTION("an entity motion packet");

        private final String displayName;

        SuspiciousPacket(final String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return this.displayName;
        }
    }
}
