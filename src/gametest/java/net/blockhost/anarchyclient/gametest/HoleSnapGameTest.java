package net.blockhost.anarchyclient.gametest;

import com.google.gson.JsonPrimitive;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;

/** Hole Snap pulls a player standing on a surround wall down into the hole next to it. */
public final class HoleSnapGameTest implements GameScenario {

    @Override
    public void run(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos hole = arena.origin().offset(3, 0, 0);
            arena.hole(hole);
            arena.teleport(hole.west().above(), -90.0F);
            context.waitTicks(10);

            GameTestSupport.setting(context, "hole_snap", "directional", new JsonPrimitive(false));
            GameTestSupport.enable(context, "hole_snap", true);
            try {
                context.waitFor(client -> client.player.blockPosition().equals(hole) && client.player.onGround(), 100);
                arena.server().waitFor(server -> arena.serverPlayer(server).blockPosition().equals(hole), 40);
            } finally {
                GameTestSupport.enable(context, "hole_snap", false);
                GameTestSupport.setting(context, "hole_snap", "directional", new JsonPrimitive(true));
            }
        }
    }
}
