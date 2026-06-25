package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class AnchorModule extends Module {

    private final NumberSetting pullSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("pull_speed")
            .name("Pull")
            .defaultValue(0.45)
            .min(0.05)
            .max(2.0)
            .step(0.05)
            .build()));

    public AnchorModule() {
        super("anchor", "Anchor", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || player.onGround() || player.isFallFlying()) {
            return;
        }
        BlockPos below = player.blockPosition().below();
        if (!client.level.getBlockState(below).getCollisionShape(client.level, below).isEmpty()) {
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x * 0.4, -this.pullSpeed.value(), movement.z * 0.4);
        }
    }
}
