package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.component.SwingAnimation;

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
        super("projectile_puncher", "Projectile Puncher", ModuleCategory.COMBAT,
                "Hits fireballs, wind charges, and shulker bullets that fly toward you within range to deflect or destroy them.");
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
        for (Entity entity : client.level.entitiesForRendering()) {
            if (isPunchable(entity)
                    && CombatTargets.canReachEntity(client.player, entity, this.range.value())
                    && ArrowDodgeModule.approaching(entity.position(), entity.getDeltaMovement(), client.player.position())) {
                client.gameMode.attack(client.player, entity);
                client.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
                this.cooldown = 5;
                return;
            }
        }
    }

    /**
     * Returns whether a hit affects this projectile. Vanilla deflects redirectable projectiles
     * (fireballs, wind charges) and destroys shulker bullets; the server kicks for attacking
     * non-attackable arrows and tridents, and other projectiles ignore hits.
     */
    static boolean isPunchable(final Entity entity) {
        return entity instanceof Projectile
                && entity.isAttackable()
                && (entity.is(EntityTypeTags.REDIRECTABLE_PROJECTILE) || entity instanceof ShulkerBullet);
    }
}
