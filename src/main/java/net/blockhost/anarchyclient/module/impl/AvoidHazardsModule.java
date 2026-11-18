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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
    private final BooleanSetting voidEdges = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("void")
            .name("Void")
            .defaultValue(true)
            .description("Also stops you from walking off an edge with nothing below down to the bottom of the world.")
            .build()));
    private final BooleanSetting unloadedChunks = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("unloaded_chunks")
            .name("Unloaded Chunks")
            .defaultValue(false)
            .description("Also stops you from walking into chunks that have not loaded yet.")
            .build()));

    public AvoidHazardsModule() {
        super("avoid_hazards", "Avoid Hazards", ModuleCategory.MOVEMENT,
                "Stops your movement when you are about to walk into lava, fire, cactus, berry bushes, wither roses, "
                        + "powder snow, or the void, and optionally into unloaded chunks. You can still walk out once inside one.");
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        LocalPlayer player = client.player;
        if (!this.stopMovement.value() || player == null || client.level == null || input == null) {
            return;
        }
        BlockPos current = player.blockPosition();
        if (isHazard(client.level.getBlockState(current).getBlock())
                || isHazard(client.level.getBlockState(current.below()).getBlock())) {
            // Already in a hazard: let the player walk out instead of freezing them in place.
            return;
        }
        BlockPos next = BlockPos.containing(player.position().add(player.getDeltaMovement().multiply(4.0, 0.0, 4.0)));
        boolean grounded = player.onGround() && !player.getAbilities().flying;
        if (isHazard(client.level.getBlockState(next).getBlock())
                || isHazard(client.level.getBlockState(next.below()).getBlock())
                || this.unloadedChunks.value() && !client.level.hasChunk(next.getX() >> 4, next.getZ() >> 4)
                // An unloaded chunk reads as void air, so only loaded columns count as void; the Unloaded Chunks
                // setting decides about the others.
                || this.voidEdges.value() && grounded && !next.equals(current)
                && client.level.hasChunk(next.getX() >> 4, next.getZ() >> 4) && isVoidColumn(client.level, next)) {
            input.keyPresses = new Input(false, false, false, false, input.keyPresses.jump(), input.keyPresses.shift(), false);
            ((ClientInputAccessor) input).anarchyclient$setMoveVector(Vec2.ZERO);
        }
    }

    /** Returns whether nothing below {@code pos} down to the bottom of the world can stop a fall. */
    static boolean isVoidColumn(final BlockGetter level, final BlockPos pos) {
        for (BlockPos below = pos.below(); below.getY() >= level.getMinY(); below = below.below()) {
            BlockState state = level.getBlockState(below);
            if (!state.getCollisionShape(level, below).isEmpty() || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static boolean isHazard(final Block block) {
        return HAZARDS.contains(block);
    }
}
