package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.mixin.ClientInputAccessor;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;

import java.util.Set;

public final class AvoidHazardsModule extends Module {

    private static final Set<Block> HAZARDS = Set.of(
            Blocks.LAVA,
            Blocks.FIRE,
            Blocks.SOUL_FIRE,
            Blocks.CACTUS,
            Blocks.SWEET_BERRY_BUSH,
            Blocks.WITHER_ROSE,
            Blocks.POWDER_SNOW
    );

    private final BooleanSetting stopMovement = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("stop_movement")
            .name("Stop")
            .defaultValue(true)
            .build()));

    public AvoidHazardsModule() {
        super("avoid_hazards", "Avoid Hazards", ModuleCategory.MOVEMENT);
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        LocalPlayer player = client.player;
        if (!this.stopMovement.value() || player == null || client.level == null || input == null) {
            return;
        }
        BlockPos next = BlockPos.containing(player.position().add(player.getDeltaMovement().multiply(4.0, 0.0, 4.0)));
        if (isHazard(client.level.getBlockState(next).getBlock())
                || isHazard(client.level.getBlockState(next.below()).getBlock())) {
            input.keyPresses = new Input(false, false, false, false, input.keyPresses.jump(), input.keyPresses.shift(), false);
            ((ClientInputAccessor) input).anarchyclient$setMoveVector(Vec2.ZERO);
        }
    }

    static boolean isHazard(final Block block) {
        return HAZARDS.contains(block);
    }
}
