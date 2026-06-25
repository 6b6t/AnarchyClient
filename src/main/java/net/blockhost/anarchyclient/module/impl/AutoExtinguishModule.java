package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionConstraints;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;

import java.util.OptionalInt;

public final class AutoExtinguishModule extends Module {

    private final BooleanSetting extinguishBlocks = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("extinguish_blocks")
            .name("Blocks")
            .defaultValue(true)
            .build()));
    private final BooleanSetting extinguishSelf = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("extinguish_self")
            .name("Self")
            .defaultValue(false)
            .build()));
    private final BooleanSetting restoreSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("restore_slot")
            .name("Restore")
            .defaultValue(true)
            .build()));
    private final NumberSetting horizontalRadius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("horizontal_radius")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final NumberSetting verticalRadius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical_radius")
            .name("Vertical")
            .defaultValue(2.0)
            .min(1.0)
            .max(6.0)
            .step(1.0)
            .build()));
    private final NumberSetting maxBlocksPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_blocks_per_tick")
            .name("Max")
            .defaultValue(4.0)
            .min(1.0)
            .max(24.0)
            .step(1.0)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(4.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;

    public AutoExtinguishModule() {
        super("auto_extinguish", "Auto Extinguish", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (this.extinguishSelf.value() && player.getRemainingFireTicks() > 0) {
            this.queueWaterBucket(player);
        }
        if (this.extinguishBlocks.value()) {
            int extinguished = this.extinguishNearbyFire(client, player);
            if (extinguished > 0) {
                this.cooldownTicks = this.delay.value().intValue();
            }
        }
    }

    private void queueWaterBucket(final LocalPlayer player) {
        OptionalInt slot = InventoryActions.findHotbarSlot(player.getInventory(), stack -> stack.is(Items.WATER_BUCKET));
        if (slot.isEmpty()) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_LIFE,
                Math.max(5, this.delay.value().intValue()),
                InventoryActionConstraints.cautiousPlayerInventory(),
                InventoryAction.useHotbarItem(slot.orElseThrow(), Items.WATER_BUCKET, this.restoreSlot.value())
        ));
    }

    private int extinguishNearbyFire(final Minecraft client, final LocalPlayer player) {
        BlockPos center = player.blockPosition();
        int horizontal = this.horizontalRadius.value().intValue();
        int vertical = this.verticalRadius.value().intValue();
        int max = this.maxBlocksPerTick.value().intValue();
        int count = 0;
        for (int y = center.getY() - vertical; y <= center.getY() + vertical && count < max; y++) {
            for (int x = center.getX() - horizontal; x <= center.getX() + horizontal && count < max; x++) {
                for (int z = center.getZ() - horizontal; z <= center.getZ() + horizontal && count < max; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!client.level.isLoaded(pos) || !isFireBlock(client.level.getBlockState(pos).getBlock())) {
                        continue;
                    }
                    if (client.gameMode.destroyBlock(pos)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static boolean isFireBlock(final Block block) {
        return block == Blocks.FIRE || block == Blocks.SOUL_FIRE;
    }
}
