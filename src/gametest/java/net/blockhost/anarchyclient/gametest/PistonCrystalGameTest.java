package net.blockhost.anarchyclient.gametest;

import net.blockhost.anarchyclient.module.DebugValueRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Piston Crystal against a player in an obsidian hole.
 *
 * <p>Layout along the x axis: the target stands at T in the hole, the crystal goes on the surround
 * block west of T at head height, the piston one block further west on an obsidian support, and
 * the player stands two blocks south of the piston, within reach of the pushed crystal.
 */
public final class PistonCrystalGameTest implements GameScenario {

    private static final Logger LOGGER = LoggerFactory.getLogger(PistonCrystalGameTest.class);

    @Override
    public void run(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos target = arena.origin().offset(3, 0, 0);
            arena.hole(target);
            arena.setBlock(target.offset(-2, 0, 0), "obsidian");
            arena.run("effect give " + arena.name() + " resistance infinite 4 true");
            arena.give("piston 16");
            arena.give("end_crystal 16");
            arena.give("redstone_block 16");
            arena.give("diamond_pickaxe[enchantments={efficiency:5}]");
            arena.teleport(target.offset(-2, 0, 2), 135.0F);
            arena.spawnTarget(target);
            context.waitTicks(10);
            context.takeScreenshot("piston_crystal_setup");

            GameTestSupport.enable(context, "piston_crystal", true);
            try {
                arena.server().waitFor(server -> GameTestSupport.targetHits(server) > 0, 300);
            } catch (AssertionError timeout) {
                throw new AssertionError("Piston Crystal never damaged the target; debug "
                        + DebugValueRegistry.snapshot().get("piston_crystal"), timeout);
            } finally {
                context.takeScreenshot("piston_crystal_result");
                GameTestSupport.enable(context, "piston_crystal", false);
            }
            LOGGER.info("Piston Crystal hit the target {} time(s)", arena.targetHits());
        }
    }
}
