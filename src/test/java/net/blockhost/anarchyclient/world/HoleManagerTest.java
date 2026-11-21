package net.blockhost.anarchyclient.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class HoleManagerTest {

    private static final BlockPos HOLE = new BlockPos(0, 64, 0);

    @Test
    void acceptsAnAirHoleWithObsidianWalls() {
        assertTrue(HoleManager.isHole(walledHole(Blocks.AIR.defaultBlockState()), HOLE));
    }

    @Test
    void rejectsLavaAndWaterPits() {
        assertFalse(HoleManager.isHole(walledHole(Blocks.LAVA.defaultBlockState()), HOLE));
        assertFalse(HoleManager.isHole(walledHole(Blocks.WATER.defaultBlockState()), HOLE));
    }

    @Test
    void findsBothCellsOfADoubleHole() {
        BlockPos second = HOLE.east();
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (BlockPos cell : List.of(HOLE, second)) {
            blocks.put(cell.below(), Blocks.OBSIDIAN.defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                blocks.putIfAbsent(cell.relative(direction), Blocks.BEDROCK.defaultBlockState());
            }
        }
        blocks.remove(HOLE);
        blocks.remove(second);
        BlockGetter level = new MapBlockGetter(blocks);

        assertFalse(HoleManager.isHole(level, HOLE));
        assertEquals(List.of(HOLE, second), HoleManager.nearbyDoubleHoleCells(level, HOLE, 2));
    }

    @Test
    void rejectsADoubleHoleWithAGapInTheWall() {
        BlockPos second = HOLE.east();
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (BlockPos cell : List.of(HOLE, second)) {
            blocks.put(cell.below(), Blocks.OBSIDIAN.defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                blocks.putIfAbsent(cell.relative(direction), Blocks.BEDROCK.defaultBlockState());
            }
        }
        blocks.remove(HOLE);
        blocks.remove(second);
        blocks.remove(second.north());

        assertTrue(HoleManager.nearbyDoubleHoleCells(new MapBlockGetter(blocks), HOLE, 2)
                .isEmpty());
    }

    private static BlockGetter walledHole(final BlockState inside) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(HOLE, inside);
        blocks.put(HOLE.below(), Blocks.OBSIDIAN.defaultBlockState());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            blocks.put(HOLE.relative(direction), Blocks.BEDROCK.defaultBlockState());
        }
        return new MapBlockGetter(blocks);
    }

    private record MapBlockGetter(Map<BlockPos, BlockState> blocks) implements BlockGetter {

        @Override
        public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(final BlockPos pos) {
            return this.blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(final BlockPos pos) {
            return this.getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinY() {
            return -64;
        }
    }
}
