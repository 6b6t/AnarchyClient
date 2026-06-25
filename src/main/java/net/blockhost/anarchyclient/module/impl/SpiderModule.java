package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class SpiderModule extends Module {

    private final NumberSetting climbSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("climb_speed")
            .name("Climb Speed")
            .defaultValue(0.28)
            .min(0.05)
            .max(1.0)
            .step(0.05)
            .build()));

    public SpiderModule() {
        super("spider", "Spider", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.horizontalCollision || player.isInWater() || player.isInLava()) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, this.climbSpeed.value(), velocity.z);
    }
}
