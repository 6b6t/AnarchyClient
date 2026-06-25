package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class AirJumpModule extends Module {

    private final NumberSetting velocity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("velocity")
            .name("Velocity")
            .defaultValue(0.42)
            .min(0.1)
            .max(1.2)
            .step(0.01)
            .build()));
    private final BooleanSetting resetFallDistance = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("reset_fall_distance")
            .name("Reset Fall")
            .defaultValue(true)
            .build()));
    private boolean jumpDown;

    public AirJumpModule() {
        super("air_jump", "Air Jump", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        boolean pressed = player != null && client.options.keyJump.isDown();
        if (player != null && pressed && !this.jumpDown && !player.onGround() && !player.isFallFlying()) {
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x, this.velocity.value(), movement.z);
            if (this.resetFallDistance.value()) {
                player.resetFallDistance();
            }
        }
        this.jumpDown = pressed;
    }
}
