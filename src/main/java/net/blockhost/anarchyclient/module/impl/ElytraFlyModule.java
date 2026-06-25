package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class ElytraFlyModule extends Module {

    private final NumberSetting horizontalSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("horizontal_speed")
            .name("Horizontal")
            .defaultValue(1.2)
            .min(0.1)
            .max(4.0)
            .step(0.1)
            .build()));
    private final NumberSetting verticalSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical_speed")
            .name("Vertical")
            .defaultValue(0.6)
            .min(0.05)
            .max(2.0)
            .step(0.05)
            .build()));

    public ElytraFlyModule() {
        super("elytra_fly", "Elytra Fly", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.isFallFlying()) {
            return;
        }
        Vec3 horizontal = MovementVelocity.fromKeys(client, player.getYRot(), this.horizontalSpeed.value());
        double y = 0.0;
        if (client.options.keyJump.isDown()) {
            y += this.verticalSpeed.value();
        }
        if (client.options.keyShift.isDown()) {
            y -= this.verticalSpeed.value();
        }
        player.setDeltaMovement(horizontal.x, y, horizontal.z);
    }
}
