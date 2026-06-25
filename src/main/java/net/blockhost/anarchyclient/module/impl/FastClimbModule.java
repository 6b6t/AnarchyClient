package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class FastClimbModule extends Module {

    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.35)
            .min(0.1)
            .max(2.0)
            .step(0.05)
            .build()));

    public FastClimbModule() {
        super("fast_climb", "Fast Climb", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.onClimbable() || !client.options.keyUp.isDown()) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, this.speed.value(), velocity.z);
    }
}
