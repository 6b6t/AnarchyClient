package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class EChestFarmerModule extends Module {

    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(5.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private int cooldown;

    public EChestFarmerModule() {
        super("echest_farmer", "EChest Farmer", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null || this.cooldown-- > 0) {
            return;
        }
        BlockPos target = client.player.blockPosition().below();
        if (client.level.getBlockState(target).is(Blocks.ENDER_CHEST)) {
            WorldInteraction.breakBlock(client, target, Direction.UP, stack -> true);
            this.cooldown = this.delayTicks.value().intValue();
            return;
        }
        BlockPlacement.place(client, this, target, true, 70.0F, stack -> stack.is(Items.ENDER_CHEST));
        this.cooldown = this.delayTicks.value().intValue();
    }
}
