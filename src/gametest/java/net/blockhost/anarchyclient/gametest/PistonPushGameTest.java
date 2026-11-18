package net.blockhost.anarchyclient.gametest;

import net.blockhost.anarchyclient.module.DebugValueRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Piston Push places a piston at the target's head height and powers it toward the target. */
public final class PistonPushGameTest implements FabricClientGameTest {

    @Override
    public void runTest(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos target = arena.origin().offset(3, 0, 0);
            BlockPos head = target.above();
            arena.hole(target);
            arena.give("piston 16");
            arena.give("redstone_block 16");
            arena.teleport(target.offset(-2, 0, 2), 135.0F);
            arena.spawnTarget(target);
            context.waitTicks(10);

            GameTestSupport.enable(context, "piston_push", true);
            try {
                arena.server().waitFor(server -> {
                    for (Direction facing : Direction.Plane.HORIZONTAL) {
                        BlockState state = server.overworld().getBlockState(head.relative(facing.getOpposite()));
                        if (state.getBlock() instanceof PistonBaseBlock && state.getValue(PistonBaseBlock.FACING) == facing
                                && state.getValue(PistonBaseBlock.EXTENDED)) {
                            return true;
                        }
                    }
                    return false;
                }, 200);
            } catch (AssertionError timeout) {
                throw new AssertionError("Piston Push never fired a piston at the target; debug "
                        + DebugValueRegistry.snapshot().get("piston_push"), timeout);
            } finally {
                context.takeScreenshot("piston_push_result");
                GameTestSupport.enable(context, "piston_push", false);
            }
        }
    }
}
