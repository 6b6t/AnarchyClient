package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Scenarios for movement modules. */
final class MovementModuleScenarios {

    private MovementModuleScenarios() {}

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
                boolean connected =
                        context.computeOnClient(client -> client.level != null && client.getConnection() != null);
                GameTestSupport.check(connected, "Burrow got the player kicked");
                boolean filled = arena.server()
                        .computeOnServer(
                                server -> server.overworld().getBlockState(feet).is(Blocks.OBSIDIAN));
                GameTestSupport.check(filled, "Burrow did not place a block in the player's cell");
            } finally {
                if (context.computeOnClient(client -> client.level != null)) {
                    GameTestSupport.enable(context, "burrow", false);
                }
            }
        }
    }

    /** Spider climbs a wall the player walks into. */
    static void spider(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            arena.run("fill " + GameTestSupport.at(origin.offset(1, 0, -1)) + " "
                    + GameTestSupport.at(origin.offset(1, 8, 1)) + " stone");
            context.waitTicks(5);
            GameTestSupport.enable(context, "spider", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyUp, 30);
                double y = arena.server()
                        .computeOnServer(server -> arena.serverPlayer(server).getY());
                GameTestSupport.check(y >= origin.getY() + 3.0, "Spider climbed only to y " + y);
            } finally {
                GameTestSupport.enable(context, "spider", false);
            }
        }
    }

    /** High Jump lifts a jump well above the vanilla height of about 1.25 blocks. */
    static void highJump(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            double startY = arena.server()
                    .computeOnServer(server -> arena.serverPlayer(server).getY());
            GameTestSupport.enable(context, "high_jump", true);
            try {
                // High Jump needs one tick on the ground to know that the next jump leaves it.
                context.waitTicks(2);
                context.getInput().holdKeyFor(options -> options.keyJump, 2);
                double peak = startY;
                for (int tick = 0; tick < 20; tick++) {
                    context.waitTick();
                    peak = Math.max(
                            peak,
                            arena.server()
                                    .computeOnServer(
                                            server -> arena.serverPlayer(server).getY()));
                }
                double height = peak - startY;
                GameTestSupport.check(height > 2.0, "High Jump reached only " + height + " blocks");
            } finally {
                GameTestSupport.enable(context, "high_jump", false);
            }
        }
    }

    /** Reverse Step pulls the player down a four-block drop within a few ticks. */
    static void reverseStep(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            ledge(arena);
            GameTestSupport.enable(context, "reverse_step", true);
            try {
                context.getInput().holdKey(options -> options.keyUp);
                context.waitFor(client -> !client.player.onGround(), 40);
                context.waitTicks(5);
                context.getInput().releaseKey(options -> options.keyUp);
                double y = context.computeOnClient(client -> client.player.getY());
                // Vanilla falls only about one block in five ticks.
                GameTestSupport.check(
                        y < origin.getY() + 0.5,
                        "Reverse Step left the player at y " + y + " five ticks after the ledge");
            } finally {
                context.getInput().releaseKey(options -> options.keyUp);
                GameTestSupport.enable(context, "reverse_step", false);
            }
        }
    }

    /** Safe Walk stops the player at the edge of a ledge. */
    static void safeWalk(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            ledge(arena);
            GameTestSupport.enable(context, "safe_walk", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyUp, 30);
                context.waitTicks(5);
                double y = arena.server()
                        .computeOnServer(server -> arena.serverPlayer(server).getY());
                GameTestSupport.check(y >= origin.getY() + 4.0 - 0.01, "Safe Walk let the player fall to y " + y);
            } finally {
                GameTestSupport.enable(context, "safe_walk", false);
            }
        }
    }

    /** Puts the player, facing east, on a stone platform four blocks up whose east edge is one block ahead. */
    private static void ledge(final GameTestSupport.Arena arena) {
        BlockPos origin = arena.origin();
        arena.run("fill " + GameTestSupport.at(origin.offset(-2, 3, -1)) + " "
                + GameTestSupport.at(origin.offset(0, 3, 1)) + " stone");
        arena.teleport(origin.above(4), -90.0F);
        arena.context().waitFor(client -> client.player.onGround(), 40);
        arena.context().waitTicks(5);
    }

    /** Anti Void catches the player below the bottom of the End and warns in chat. */
    static void antiVoid(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            GameTestSupport.enable(context, "anti_void", true);
            try {
                // Open void between the main island and the outer islands.
                arena.run("execute in minecraft:the_end run tp " + arena.name() + " 300.5 30 300.5");
                context.waitFor(client -> client.level.dimension() == Level.END, 200);
                context.waitFor(client -> client.player.getY() < 1.0, 100);
                context.waitTicks(40);
                double y = context.computeOnClient(client -> client.player.getY());
                GameTestSupport.check(y > -4.0, "Anti Void let the player fall to y " + y);
                GameTestSupport.check(
                        context.computeOnClient(client -> client.player.isAlive()), "The player died in the void");
                GameTestSupport.check(
                        context.computeOnClient(client -> ClientChat.contains(client, "Anti Void stopped a fall")),
                        "Anti Void did not warn in chat: " + context.computeOnClient(ClientChat::messages));
            } finally {
                GameTestSupport.enable(context, "anti_void", false);
            }
        }
    }

    /** No Web walks through cobwebs at close to normal speed. */
    static void noWeb(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            arena.run("fill " + GameTestSupport.at(origin.offset(1, 0, -1)) + " "
                    + GameTestSupport.at(origin.offset(8, 1, 1)) + " cobweb");
            context.waitTicks(5);
            double startX = arena.server()
                    .computeOnServer(server -> arena.serverPlayer(server).getX());
            GameTestSupport.enable(context, "no_web", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyUp, 20);
                double moved = arena.server()
                                .computeOnServer(
                                        server -> arena.serverPlayer(server).getX())
                        - startX;
                // Cobwebs cut walking to a quarter, about one block a second.
                GameTestSupport.check(moved > 3.0, "No Web moved the player only " + moved + " blocks through cobwebs");
            } finally {
                GameTestSupport.enable(context, "no_web", false);
            }
        }
    }

    /** No Slow keeps walking speed while blocking with a shield. */
    static void noSlow(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.hotbar(0, "shield");
            context.waitTicks(5);
            double startX = arena.server()
                    .computeOnServer(server -> arena.serverPlayer(server).getX());
            GameTestSupport.enable(context, "no_slow", true);
            try {
                context.getInput().holdKey(options -> options.keyUse);
                context.getInput().holdKeyFor(options -> options.keyUp, 20);
                GameTestSupport.check(
                        context.computeOnClient(client -> client.player.isUsingItem()),
                        "The player was not blocking with the shield");
                double moved = arena.server()
                                .computeOnServer(
                                        server -> arena.serverPlayer(server).getX())
                        - startX;
                // Using an item cuts walking to a fifth, under one block a second.
                GameTestSupport.check(moved > 3.0, "No Slow moved the player only " + moved + " blocks while blocking");
            } finally {
                context.getInput().releaseKey(options -> options.keyUse);
                GameTestSupport.enable(context, "no_slow", false);
            }
        }
    }
}
