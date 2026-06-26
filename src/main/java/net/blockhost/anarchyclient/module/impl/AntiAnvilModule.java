package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AnvilBlock;

import java.util.List;

public final class AntiAnvilModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Both")
            .addAllOptions(List.of("Break", "Place", "Both"))
            .build()));
    private final NumberSetting scanHeight = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("scan_height")
            .name("Height")
            .defaultValue(6.0)
            .min(2.0)
            .max(12.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));

    public AntiAnvilModule() {
        super("anti_anvil", "Anti Anvil", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        for (int y = 1; y <= this.scanHeight.value().intValue(); y++) {
            BlockPos pos = client.player.blockPosition().above(y);
            if (client.level.getBlockState(pos).getBlock() instanceof AnvilBlock) {
                if (this.shouldPlace() && y > 1) {
                    BlockPos blocker = pos.below();
                    if (client.level.getBlockState(blocker).canBeReplaced()
                            && BlockPlacement.place(client, this, blocker, this.rotate.value(), 60.0F,
                            stack -> stack.is(Items.OBSIDIAN)) == BlockPlacement.PlacementResult.PLACED) {
                        return;
                    }
                }
                if (this.shouldBreak()) {
                    WorldInteraction.breakBlock(client, pos, Direction.UP, stack -> true);
                }
                return;
            }
        }
    }

    private boolean shouldBreak() {
        return "Break".equals(this.mode.value()) || "Both".equals(this.mode.value());
    }

    private boolean shouldPlace() {
        return "Place".equals(this.mode.value()) || "Both".equals(this.mode.value());
    }
}
