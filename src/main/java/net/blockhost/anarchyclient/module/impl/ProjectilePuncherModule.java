package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

public final class ProjectilePuncherModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(0.25)
            .build()));
    private int cooldown;

    public ProjectilePuncherModule() {
        super("projectile_puncher", "Projectile Puncher", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        double rangeSqr = this.range.value() * this.range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Projectile
                    && entity.distanceToSqr(client.player) <= rangeSqr
                    && ArrowDodgeModule.approaching(entity.position(), entity.getDeltaMovement(), client.player.position())) {
                client.gameMode.attack(client.player, entity);
                client.player.swing(InteractionHand.MAIN_HAND);
                this.cooldown = 5;
                return;
            }
        }
    }
}
