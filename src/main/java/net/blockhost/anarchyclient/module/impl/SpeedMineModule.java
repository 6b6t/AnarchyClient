package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class SpeedMineModule extends Module {

    private final NumberSetting extraAttempts = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("extra_attempts")
            .name("Attempts")
            .defaultValue(1.0)
            .min(1.0)
            .max(5.0)
            .step(1.0)
            .build()));

    public SpeedMineModule() {
        super("speed_mine", "Speed Mine", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult hit = (BlockHitResult) client.hitResult;
        Direction direction = hit.getDirection();
        for (int attempt = 0; attempt < this.extraAttempts.value().intValue(); attempt++) {
            WorldInteraction.breakBlock(client, hit.getBlockPos(), direction, stack -> true);
        }
    }
}
