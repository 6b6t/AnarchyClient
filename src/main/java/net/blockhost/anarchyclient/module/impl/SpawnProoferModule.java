package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class SpawnProoferModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final NumberSetting lightThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("light")
            .name("Light")
            .defaultValue(7.0)
            .min(0.0)
            .max(15.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(5.0)
            .min(1.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;

    public SpawnProoferModule() {
        super("spawn_proofer", "Spawn Proofer", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        BlockPos target = findTarget(client, player.blockPosition(), this.range.value().intValue(),
                this.lightThreshold.value().intValue());
        if (target == null) {
            return;
        }
        WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(
                client,
                this,
                target,
                Direction.UP,
                stack -> stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH),
                this.rotate.value()
        );
        if (result == WorldInteraction.ActionResult.DONE) {
            this.cooldownTicks = this.delay.value().intValue();
        }
    }

    static BlockPos findTarget(final Minecraft client, final BlockPos center, final int radius, final int lightThreshold) {
        for (int y = center.getY() - 2; y <= center.getY() + 2; y++) {
            for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockPos above = pos.above();
                    if (!client.level.isLoaded(pos)
                            || client.level.getBlockState(pos).is(Blocks.TORCH)
                            || client.level.getBlockState(pos).is(Blocks.SOUL_TORCH)
                            || !client.level.getBlockState(pos).isFaceSturdy(client.level, pos, Direction.UP)
                            || !client.level.getBlockState(above).canBeReplaced()
                            || client.level.getMaxLocalRawBrightness(above) > lightThreshold) {
                        continue;
                    }
                    return pos.immutable();
                }
            }
        }
        return null;
    }
}
