package net.blockhost.anarchyclient.module.impl;

import java.util.Set;
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
import net.minecraft.world.phys.Vec3;

public final class AvoidHazardsModule extends Module {

    private static final Set<Block> HAZARDS = Set.of(
            Blocks.LAVA,
            Blocks.FIRE,
            Blocks.SOUL_FIRE,
            Blocks.CACTUS,
            Blocks.SWEET_BERRY_BUSH,
            Blocks.WITHER_ROSE,
            Blocks.POWDER_SNOW);

    /** Blocks ahead of the player to check, before adding the distance their speed carries them. */
    private static final double LOOK_AHEAD = 0.6;

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
        super(
                "avoid_hazards",
                "Avoid Hazards",
                ModuleCategory.MOVEMENT,
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
        // Look ahead where the player is trying to walk. Looking ahead by velocity alone lets a stopped
        // player creep forward: their speed drops, the look-ahead falls back into the current block, and
        // the stop releases until they are already over the edge.
        Vec3 wish = wishDirection(input.getMoveVector(), player.getYRot());
        if (wish.lengthSqr() < 1.0E-4) {
            return;
        }
        double reach = LOOK_AHEAD + player.getDeltaMovement().horizontalDistance() * 3.0;
        BlockPos next = BlockPos.containing(player.position().add(wish.scale(reach)));
        boolean grounded = player.onGround() && !player.getAbilities().flying;
        if (isHazard(client.level.getBlockState(next).getBlock())
                || isHazard(client.level.getBlockState(next.below()).getBlock())
                || (this.unloadedChunks.value() && !client.level.hasChunk(next.getX() >> 4, next.getZ() >> 4))
                // An unloaded chunk reads as void air, so only loaded columns count as void; the Unloaded Chunks
                // setting decides about the others.
                || (this.voidEdges.value()
                        && grounded
                        && client.level.hasChunk(next.getX() >> 4, next.getZ() >> 4)
                        && isVoidColumn(client.level, next))) {
            input.keyPresses =
                    new Input(false, false, false, false, input.keyPresses.jump(), input.keyPresses.shift(), false);
            ((ClientInputAccessor) input).anarchyclient$setMoveVector(Vec2.ZERO);
        }
    }

    /**
     * Returns the horizontal unit direction the movement keys push toward, using vanilla's rotation
     * of the input vector by the player's yaw, or zero when no key is held.
     */
    static Vec3 wishDirection(final Vec2 move, final float yaw) {
        double length = Math.sqrt(move.x * move.x + move.y * move.y);
        if (length < 1.0E-4) {
            return Vec3.ZERO;
        }
        double strafe = move.x / length;
        double forward = move.y / length;
        double radians = Math.toRadians(yaw);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new Vec3(strafe * cos - forward * sin, 0.0, forward * cos + strafe * sin);
    }

    /** Returns whether nothing below {@code pos} down to the bottom of the world can stop a fall. */
    static boolean isVoidColumn(final BlockGetter level, final BlockPos pos) {
        for (BlockPos below = pos.below(); below.getY() >= level.getMinY(); below = below.below()) {
            BlockState state = level.getBlockState(below);
            if (!state.getCollisionShape(level, below).isEmpty()
                    || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static boolean isHazard(final Block block) {
        return HAZARDS.contains(block);
    }
}
