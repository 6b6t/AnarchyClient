package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GlideModule extends Module {

    private final NumberSetting fallSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("fall_speed")
            .name("Fall Speed")
            .defaultValue(0.125)
            .min(0.005)
            .max(0.5)
            .step(0.005)
            .build()));
    private final NumberSetting minHeight = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_height")
            .name("Min Height")
            .defaultValue(0.0)
            .min(0.0)
            .max(5.0)
            .step(0.25)
            .build()));

    public GlideModule() {
        super("glide", "Glide", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || player.onGround() || player.isInWater() || player.isInLava()
                || player.onClimbable() || player.isFallFlying()) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y >= 0.0 || !this.highEnoughToGlide(player)) {
            return;
        }
        player.setDeltaMovement(velocity.x, cappedFallVelocity(velocity.y, this.fallSpeed.value()), velocity.z);
    }

    private boolean highEnoughToGlide(final LocalPlayer player) {
        if (this.minHeight.value() <= 0.0) {
            return true;
        }
        AABB box = player.getBoundingBox();
        AABB swept = box.minmax(box.move(0.0, -this.minHeight.value(), 0.0));
        return player.level().noCollision(player, swept);
    }

    static double cappedFallVelocity(final double currentVelocity, final double maxFallSpeed) {
        return Math.max(currentVelocity, -maxFallSpeed);
    }
}
