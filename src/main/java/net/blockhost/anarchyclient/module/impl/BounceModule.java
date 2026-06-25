package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class BounceModule extends Module {

    private final NumberSetting velocity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("velocity")
            .name("Velocity")
            .defaultValue(0.42)
            .min(0.1)
            .max(1.5)
            .step(0.01)
            .build()));

    public BounceModule() {
        super("bounce", "Bounce", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.onGround() || client.options.keyShift.isDown()) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, this.velocity.value(), movement.z);
    }
}
