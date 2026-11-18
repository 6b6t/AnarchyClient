package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Scenarios for movement modules. */
final class MovementModuleScenarios {

    private MovementModuleScenarios() {
    }

    /** Block In centers the player and boxes them in without getting them kicked. */
    static void blockIn(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos feet = arena.origin();
            arena.give("obsidian 64");
            arena.run("tp " + arena.name() + " " + (feet.getX() + 0.8) + " " + feet.getY() + " " + (feet.getZ() + 0.2));
            context.waitTicks(5);
            GameTestSupport.enable(context, "block_in", true);
            try {
                context.waitFor(client -> !GameTestSupport.module("block_in").enabled(), 200);
                boolean boxed = arena.server().computeOnServer(server -> {
                    ServerLevel level = server.overworld();
                    return level.getBlockState(feet.above(2)).is(Blocks.OBSIDIAN)
                            && level.getBlockState(feet.east()).is(Blocks.OBSIDIAN)
                            && level.getBlockState(feet.above().west()).is(Blocks.OBSIDIAN);
                });
                GameTestSupport.check(boxed, "Block In did not finish the box on the server");
            } finally {
                if (context.computeOnClient(client -> client.level != null)) {
                    GameTestSupport.enable(context, "block_in", false);
                }
            }
        }
    }

    /** Burrow fills the player's own cell, and its rubberband packet must not get the player kicked. */
    static void burrow(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos feet = arena.origin();
            arena.give("obsidian 16");
            context.waitTicks(5);
            GameTestSupport.enable(context, "burrow", true);
            try {
                context.waitFor(client -> !GameTestSupport.module("burrow").enabled(), 100);
                context.waitTicks(10);
                boolean connected = context.computeOnClient(client -> client.level != null && client.getConnection() != null);
                GameTestSupport.check(connected, "Burrow got the player kicked");
                boolean filled = arena.server().computeOnServer(server -> server.overworld().getBlockState(feet).is(Blocks.OBSIDIAN));
                GameTestSupport.check(filled, "Burrow did not place a block in the player's cell");
            } finally {
                if (context.computeOnClient(client -> client.level != null)) {
                    GameTestSupport.enable(context, "burrow", false);
                }
            }
        }
    }
}
