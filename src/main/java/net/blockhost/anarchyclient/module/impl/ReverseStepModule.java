package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class ReverseStepModule extends Module {

    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.7)
            .min(0.1)
            .max(3.0)
            .step(0.1)
            .build()));

    public ReverseStepModule() {
        super("reverse_step", "Reverse Step", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || player.onGround() || player.isInWater() || player.isInLava() || client.options.keyJump.isDown()) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y < 0.0) {
            player.setDeltaMovement(velocity.x, velocity.y - this.speed.value(), velocity.z);
        }
    }
}
