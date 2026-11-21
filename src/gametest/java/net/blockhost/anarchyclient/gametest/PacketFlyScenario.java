package net.blockhost.anarchyclient.gametest;

import com.google.gson.JsonPrimitive;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Packet Fly must lift the player on the server while jump is held, and must not get the player
 * kicked: a 26.3 server disconnects clients that send two position packets in one tick.
 */
final class PacketFlyScenario {

    private PacketFlyScenario() {}

    static void run(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            try {
                // Normal mode's follow-up packet only works where the server rejects it. Singleplayer skips
                // movement checks and accepts it as a real move, so check the rise in Setback mode.
                GameTestSupport.setting(context, "packet_fly", "mode", new JsonPrimitive("Setback"));
                double startY = arena.server()
                        .computeOnServer(server -> arena.serverPlayer(server).getY());
                GameTestSupport.enable(context, "packet_fly", true);
                context.getInput().holdKeyFor(options -> options.keyJump, 30);
                double risen = arena.server()
                                .computeOnServer(
                                        server -> arena.serverPlayer(server).getY())
                        - startY;
                GameTestSupport.check(
                        risen > 1.0, "Packet Fly did not carry the player up on the server (rose " + risen + ")");
                GameTestSupport.enable(context, "packet_fly", false);

                GameTestSupport.setting(context, "packet_fly", "mode", new JsonPrimitive("Normal"));
                GameTestSupport.enable(context, "packet_fly", true);
                for (int tick = 0; tick < 40; tick++) {
                    context.waitTick();
                    boolean connected =
                            context.computeOnClient(client -> client.level != null && client.getConnection() != null);
                    GameTestSupport.check(connected, "Packet Fly got the player kicked after " + tick + " ticks");
                }
            } finally {
                if (context.computeOnClient(client -> client.level != null)) {
                    GameTestSupport.enable(context, "packet_fly", false);
                }
                GameTestSupport.setting(context, "packet_fly", "mode", new JsonPrimitive("Normal"));
            }
        }
    }
}
