package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.projectile.ProjectileAim;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.Optional;

public final class ProjectileAimbotModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(48.0)
            .min(4.0)
            .max(128.0)
            .step(4.0)
            .build()));
    private final BooleanSetting hostiles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hostiles")
            .name("Hostiles")
            .defaultValue(false)
            .build()));

    public ProjectileAimbotModule() {
        super("projectile_aimbot", "Projectile Aimbot", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || !isProjectile(player.getMainHandItem())) {
            RotationManager.clear(this.id());
            return;
        }
        TargetPolicy policy = TargetPolicy.of(true, this.hostiles.value(), false, true, true, true, true);
        Optional<LivingEntity> target = TargetQuery.closest(client.level.entitiesForRendering(), player, policy,
                this.range.value(), Comparator.comparingDouble(player::distanceToSqr));
        target.ifPresentOrElse(entity -> RotationManager.request(new RotationRequest(
                this.id(),
                ProjectileAim.rotationToHit(player.getEyePosition(), entity, ProjectileAim.throwableVelocity(projectileType(player.getMainHandItem())),
                        ProjectileAim.gravity(projectileType(player.getMainHandItem()))),
                70,
                35.0F,
                2,
                2.0F,
                RotationTurnMode.STEPPED,
                true
        )), () -> RotationManager.clear(this.id()));
    }

    static boolean isProjectile(final ItemStack stack) {
        return stack.is(Items.ENDER_PEARL)
                || stack.is(Items.SNOWBALL)
                || stack.is(Items.EGG)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.TRIDENT);
    }

    private static String projectileType(final ItemStack stack) {
        if (stack.is(Items.TRIDENT)) {
            return "trident";
        }
        if (stack.is(Items.SPLASH_POTION)) {
            return "potion";
        }
        return "throwable";
    }
}
