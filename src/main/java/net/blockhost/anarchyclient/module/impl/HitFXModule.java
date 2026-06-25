package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HitFXModule extends Module {

    private final NumberSetting count = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("count")
            .name("Count")
            .defaultValue(8.0)
            .min(1.0)
            .max(40.0)
            .step(1.0)
            .build()));

    public HitFXModule() {
        super("hit_fx", "Hit FX", ModuleCategory.RENDER);
    }

    @Override
    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        if (client.level != null && target != null) {
            Vec3 center = target.getBoundingBox().getCenter();
            RandomSource random = client.level.getRandom();
            for (int i = 0; i < this.count.value().intValue(); i++) {
                client.level.addParticle(ParticleTypes.CRIT, center.x, center.y, center.z,
                        (random.nextDouble() - 0.5) * 0.2,
                        random.nextDouble() * 0.2,
                        (random.nextDouble() - 0.5) * 0.2);
            }
        }
        return false;
    }
}
