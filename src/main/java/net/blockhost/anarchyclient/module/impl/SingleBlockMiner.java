package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.ToolSelector;
import net.blockhost.anarchyclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Keeps a module on one block until it breaks. Starting a second break aborts the first, so a
 * module may break several instant blocks in a tick but must stay on a non-instant block.
 */
final class SingleBlockMiner {

    private final Module owner;
    private @Nullable BlockPos current;
    private Direction face = Direction.UP;

    SingleBlockMiner(final Module owner) {
        this.owner = owner;
    }

    /**
     * Continues the block in progress while {@code stillWanted} accepts it. Returns whether this
     * tick was spent on it; when it returns {@code false} the module may pick a new block.
     */
    boolean continueCurrent(final Minecraft client, final Predicate<BlockPos> stillWanted,
                            final Predicate<ItemStack> tools) {
        BlockPos pos = this.current;
        if (pos == null) {
            return false;
        }
        if (client.level == null || client.level.getBlockState(pos).isAir() || !stillWanted.test(pos)
                || !WorldInteraction.breakBlock(client, this.owner, pos, this.face, tools)) {
            this.stop(client);
            return false;
        }
        if (client.level.getBlockState(pos).isAir()) {
            this.current = null;
        }
        return true;
    }

    boolean continueCurrent(final Minecraft client, final Predicate<BlockPos> stillWanted) {
        return this.continueCurrent(client, stillWanted, ToolSelector.ANY);
    }

    /**
     * Starts breaking {@code pos}. Returns {@link Step#BROKEN} when it broke at once, so the
     * caller may try another block this tick, {@link Step#MINING} when the break takes more
     * ticks (the caller must stop for this tick), or {@link Step#FAILED}. Blocks the player can
     * never break, such as bedrock or barriers outside creative mode, fail at once.
     */
    Step start(final Minecraft client, final BlockPos pos, final Direction face, final Predicate<ItemStack> tools) {
        if (client.level == null || client.player == null) {
            return Step.FAILED;
        }
        BlockState before = client.level.getBlockState(pos);
        if (!canEverBreak(client.player, client.level, pos, before)
                || !WorldInteraction.breakBlock(client, this.owner, pos, face, tools)) {
            return Step.FAILED;
        }
        if (client.level.getBlockState(pos) != before) {
            this.current = null;
            return Step.BROKEN;
        }
        this.current = pos.immutable();
        this.face = face;
        return Step.MINING;
    }

    Step start(final Minecraft client, final BlockPos pos, final Direction face) {
        return this.start(client, pos, face, ToolSelector.ANY);
    }

    /** Vanilla never finishes a survival break whose destroy progress per tick is zero. */
    static boolean canEverBreak(final Player player, final BlockGetter level, final BlockPos pos, final BlockState state) {
        return player.getAbilities().instabuild || state.getDestroyProgress(player, level, pos) > 0.0F;
    }

    @Nullable BlockPos current() {
        return this.current;
    }

    /** Forgets the block and aborts its break. */
    void stop(final @Nullable Minecraft client) {
        this.current = null;
        if (client != null) {
            WorldInteraction.stopBreaking(client, this.owner);
        }
    }

    enum Step {
        BROKEN,
        MINING,
        FAILED
    }
}
