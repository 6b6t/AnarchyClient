package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;

/**
 * Crystal Blocker fills the crystal spots around a player standing on an obsidian floor while an
 * enemy is near, and never builds inside the player.
 */
public final class CrystalBlockerGameTest implements GameScenario {

    private static final int REQUIRED_BLOCKS = 4;

    @Override
    public void run(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos feet = arena.origin();
            arena.run("fill " + GameTestSupport.at(feet.offset(-2, -1, -2)) + " " + GameTestSupport.at(feet.offset(2, -1, 2))
                    + " obsidian");
            arena.give("obsidian 64");
            arena.spawnTarget(feet.offset(6, 0, 0));
            context.waitTicks(10);

            GameTestSupport.enable(context, "crystal_blocker", true);
            try {
                arena.server().waitFor(server -> filledSpots(server, feet) >= REQUIRED_BLOCKS, 120);
                boolean selfClear = arena.server().computeOnServer(server ->
                        server.overworld().getBlockState(feet).isAir() && server.overworld().getBlockState(feet.above()).isAir());
                GameTestSupport.check(selfClear, "Crystal Blocker placed a block inside the player");
            } finally {
                context.takeScreenshot("crystal_blocker_result");
                GameTestSupport.enable(context, "crystal_blocker", false);
            }
        }
    }

    private static int filledSpots(final MinecraftServer server, final BlockPos feet) {
        int count = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (server.overworld().getBlockState(feet.offset(dx, 0, dz)).is(Blocks.OBSIDIAN)) {
                    count++;
                }
            }
        }
        return count;
    }
}
