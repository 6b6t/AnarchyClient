package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

public final class AutoCityModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));

    public AutoCityModule() {
        super("auto_city", "Auto City", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        Player target = CombatTargets.nearestEnemy(client, this.range.value());
        if (target == null || client.level == null) {
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = target.blockPosition().relative(direction);
            if (client.level.getBlockState(pos).is(Blocks.OBSIDIAN)
                    || client.level.getBlockState(pos).is(Blocks.ENDER_CHEST)) {
                WorldInteraction.breakBlock(client, pos, direction.getOpposite(), stack -> true);
                return;
            }
        }
    }
}
