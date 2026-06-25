package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class DamageParticlesModule extends Module {

    private final NumberSetting count = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("count")
            .name("Count")
            .defaultValue(4.0)
            .min(1.0)
            .max(24.0)
            .step(1.0)
            .build()));

    public DamageParticlesModule() {
        super("damage_particles", "Damage Particles", ModuleCategory.RENDER);
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.level == null || !(packet instanceof ClientboundDamageEventPacket damage)) {
            return false;
        }
        Entity entity = client.level.getEntity(damage.entityId());
        if (entity == null) {
            return false;
        }
        Vec3 center = entity.getBoundingBox().getCenter();
        RandomSource random = client.level.getRandom();
        for (int i = 0; i < this.count.value().intValue(); i++) {
            client.level.addParticle(ParticleTypes.DAMAGE_INDICATOR, center.x, center.y + 0.4, center.z,
                    (random.nextDouble() - 0.5) * 0.12,
                    random.nextDouble() * 0.18,
                    (random.nextDouble() - 0.5) * 0.12);
        }
        return false;
    }
}
