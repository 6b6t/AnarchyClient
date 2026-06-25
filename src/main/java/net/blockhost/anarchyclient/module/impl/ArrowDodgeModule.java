package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;

public final class ArrowDodgeModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(8.0)
            .min(2.0)
            .max(24.0)
            .step(1.0)
            .build()));
    private final NumberSetting strength = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("strength")
            .name("Strength")
            .defaultValue(0.45)
            .min(0.1)
            .max(1.5)
            .step(0.05)
            .build()));

    public ArrowDodgeModule() {
        super("arrow_dodge", "Arrow Dodge", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        double rangeSqr = this.range.value() * this.range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof AbstractArrow arrow
                    && arrow.distanceToSqr(client.player) <= rangeSqr
                    && approaching(arrow.position(), arrow.getDeltaMovement(), client.player.position())) {
                Vec3 side = arrow.getDeltaMovement().cross(new Vec3(0.0, 1.0, 0.0)).normalize().scale(this.strength.value());
                client.player.setDeltaMovement(client.player.getDeltaMovement().add(side.x, 0.0, side.z));
                return;
            }
        }
    }

    static boolean approaching(final Vec3 arrowPos, final Vec3 arrowVelocity, final Vec3 playerPos) {
        Vec3 toPlayer = playerPos.subtract(arrowPos);
        return arrowVelocity.lengthSqr() > 1.0E-5 && arrowVelocity.normalize().dot(toPlayer.normalize()) > 0.75;
    }
}
