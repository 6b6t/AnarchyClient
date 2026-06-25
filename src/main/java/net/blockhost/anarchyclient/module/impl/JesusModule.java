package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class JesusModule extends Module {

    private final NumberSetting floatSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("float_speed")
            .name("Float Speed")
            .defaultValue(0.1)
            .min(0.02)
            .max(0.5)
            .step(0.02)
            .build()));

    public JesusModule() {
        super("jesus", "Jesus", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || (!player.isInWater() && !player.isInLava()) || client.options.keyShift.isDown()) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, Math.max(velocity.y, this.floatSpeed.value()), velocity.z);
    }
}
