package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class StrafeModule extends Module {

    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.32)
            .min(0.05)
            .max(2.0)
            .step(0.01)
            .build()));

    public StrafeModule() {
        super("strafe", "Strafe", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !MovementVelocity.moving(client) || player.onGround()) {
            return;
        }
        Vec3 horizontal = MovementVelocity.fromKeys(client, player.getYRot(), this.speed.value());
        if (horizontal != Vec3.ZERO) {
            player.setDeltaMovement(horizontal.x, player.getDeltaMovement().y, horizontal.z);
        }
    }
}
