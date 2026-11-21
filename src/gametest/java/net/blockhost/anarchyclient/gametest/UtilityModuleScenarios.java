package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Scenarios for movement, inventory, and quality-of-life modules. */
final class UtilityModuleScenarios {

    private UtilityModuleScenarios() {}

    /** Auto Totem refills the offhand after a totem pops. */
    static void autoTotemRefillsAfterPop(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name() + " weapon.offhand with totem_of_undying");
            arena.run("item replace entity " + arena.name() + " inventory.4 with totem_of_undying");
            context.waitTicks(5);
            GameTestSupport.enable(context, "auto_totem", true);
            try {
                arena.run("damage " + arena.name() + " 40 minecraft:generic");
                arena.server()
                        .waitFor(
                                server -> arena.serverPlayer(server).isAlive()
                                        && arena.serverPlayer(server)
                                                .getOffhandItem()
                                                .is(Items.TOTEM_OF_UNDYING)
                                        && count(arena.serverPlayer(server).getInventory()) == 1,
                                60);
            } finally {
                GameTestSupport.enable(context, "auto_totem", false);
            }
        }
    }

    private static int count(final Inventory inventory) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(Items.TOTEM_OF_UNDYING)) {
                total += inventory.getItem(slot).getCount();
            }
        }
        return total;
    }

    /** Freecam moves only the camera; the server keeps the player where they stood. */
    static void freecam(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            Vec3 start = arena.server()
                    .computeOnServer(server -> arena.serverPlayer(server).position());
            GameTestSupport.enable(context, "freecam", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyUp, 30);
                Vec3 camera = context.computeOnClient(
                        client -> client.gameRenderer.mainCamera().position());
                Vec3 server = arena.server()
                        .computeOnServer(s -> arena.serverPlayer(s).position());
                GameTestSupport.check(camera.distanceTo(start) > 3.0, "Freecam did not move the camera");
                GameTestSupport.check(server.distanceTo(start) < 0.1, "Freecam moved the real player to " + server);
            } finally {
                GameTestSupport.enable(context, "freecam", false);
            }
            context.waitTicks(2);
            Vec3 player = context.computeOnClient(client -> client.player.position());
            GameTestSupport.check(player.distanceTo(start) < 0.1, "Freecam left the player at " + player);
        }
    }

    /** Velocity cancels explosion knockback with its default settings. */
    static void velocity(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("effect give " + arena.name() + " resistance infinite 4 true");
            // Blast-proof floor, so the player does not fall into a crater instead of being pushed.
            arena.run("fill " + GameTestSupport.at(arena.origin().offset(-3, -2, -3)) + " "
                    + GameTestSupport.at(arena.origin().offset(4, -1, 3)) + " obsidian");
            Vec3 start = context.computeOnClient(client -> client.player.position());
            GameTestSupport.enable(context, "velocity", true);
            try {
                arena.run("summon minecraft:tnt " + (arena.origin().getX() + 1.5) + " "
                        + arena.origin().getY() + " " + (arena.origin().getZ() + 0.5) + " {fuse:1}");
                context.waitTicks(20);
                double moved = context.computeOnClient(
                        client -> client.player.position().subtract(start).horizontalDistance());
                GameTestSupport.check(moved < 0.5, "Velocity let the explosion push the player " + moved + " blocks");
            } finally {
                GameTestSupport.enable(context, "velocity", false);
            }
        }
    }

    /** Step walks the player up a one-block ledge without jumping. */
    static void step(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            arena.run("fill " + GameTestSupport.at(origin.offset(2, 0, -1)) + " "
                    + GameTestSupport.at(origin.offset(5, 0, 1)) + " stone");
            context.waitTicks(5);
            GameTestSupport.enable(context, "step", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyUp, 30);
                double y = arena.server()
                        .computeOnServer(server -> arena.serverPlayer(server).getY());
                GameTestSupport.check(y >= origin.getY() + 1.0, "Step did not climb the ledge (y " + y + ")");
            } finally {
                GameTestSupport.enable(context, "step", false);
            }
        }
    }

    /** Speed moves the player faster than walking, as the server sees it. */
    static void speed(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            double startX = arena.server()
                    .computeOnServer(server -> arena.serverPlayer(server).getX());
            GameTestSupport.enable(context, "speed", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyUp, 20);
                double moved = arena.server()
                                .computeOnServer(
                                        server -> arena.serverPlayer(server).getX())
                        - startX;
                // Walking covers about 4.3 blocks a second.
                GameTestSupport.check(moved > 5.0, "Speed moved the player only " + moved + " blocks in a second");
            } finally {
                GameTestSupport.enable(context, "speed", false);
            }
        }
    }

    /** Flight lifts the player while jump is held, as the server sees it. */
    static void flight(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            double startY = arena.server()
                    .computeOnServer(server -> arena.serverPlayer(server).getY());
            GameTestSupport.enable(context, "flight", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyJump, 20);
                double risen = arena.server()
                                .computeOnServer(
                                        server -> arena.serverPlayer(server).getY())
                        - startY;
                GameTestSupport.check(risen > 3.0, "Flight rose only " + risen + " blocks");
                GameTestSupport.check(
                        context.computeOnClient(client -> client.level != null), "Flight got the player kicked");
            } finally {
                GameTestSupport.enable(context, "flight", false);
            }
        }
    }

    /** Auto Replenish tops up a low hotbar stack from the main inventory. */
    static void autoReplenish(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.hotbar(0, "cobblestone 3");
            arena.run("item replace entity " + arena.name() + " inventory.10 with cobblestone 64");
            context.waitTicks(5);
            GameTestSupport.enable(context, "auto_replenish", true);
            try {
                arena.server()
                        .waitFor(
                                server -> arena.serverPlayer(server)
                                                .getInventory()
                                                .getItem(0)
                                                .getCount()
                                        > 16,
                                60);
            } finally {
                GameTestSupport.enable(context, "auto_replenish", false);
            }
        }
    }

    /** Auto Respawn leaves the death screen by itself. */
    static void autoRespawn(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            GameTestSupport.enable(context, "auto_respawn", true);
            try {
                arena.run("kill " + arena.name());
                context.waitForScreen(DeathScreen.class);
                context.waitFor(
                        client -> client.player != null && client.player.isAlive() && client.gui.screen() == null, 80);
            } finally {
                GameTestSupport.enable(context, "auto_respawn", false);
            }
        }
    }

    /** Name Protect hides the player's name in incoming chat. */
    static void nameProtect(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            GameTestSupport.enable(context, "name_protect", true);
            try {
                arena.run("tellraw " + arena.name() + " \"hello " + arena.name() + " how are you\"");
                context.waitFor(client -> ClientChat.contains(client, "how are you"), 40);
                GameTestSupport.check(
                        context.computeOnClient(client -> ClientChat.contains(client, "hello You how are you")),
                        "Name Protect did not replace the name: " + context.computeOnClient(ClientChat::messages));
            } finally {
                GameTestSupport.enable(context, "name_protect", false);
            }
        }
    }

    /** Inventory Cleaner drops junk and keeps everything else. */
    static void inventoryCleaner(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name() + " inventory.2 with rotten_flesh 12");
            arena.run("item replace entity " + arena.name() + " inventory.3 with diamond 5");
            context.waitTicks(5);
            GameTestSupport.enable(context, "inventory_cleaner", true);
            try {
                arena.server()
                        .waitFor(
                                server -> !arena.serverPlayer(server)
                                        .getInventory()
                                        .contains(stack -> stack.is(Items.ROTTEN_FLESH)),
                                100);
                boolean keptDiamonds = arena.server()
                        .computeOnServer(server -> arena.serverPlayer(server)
                                .getInventory()
                                .getItem(12)
                                .is(Items.DIAMOND));
                GameTestSupport.check(keptDiamonds, "Inventory Cleaner dropped the diamonds too");
            } finally {
                GameTestSupport.enable(context, "inventory_cleaner", false);
            }
        }
    }

    /** Auto Walk keeps the player walking forward, and stops at a wall by default. */
    static void autoWalk(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            arena.run("fill " + GameTestSupport.at(origin.offset(6, 0, -2)) + " "
                    + GameTestSupport.at(origin.offset(6, 2, 2)) + " stone");
            context.waitTicks(5);
            GameTestSupport.enable(context, "auto_walk", true);
            try {
                context.waitFor(client -> client.player.getX() > origin.getX() + 5.0, 80);
                context.waitFor(
                        client -> !GameTestSupport.module("auto_walk").enabled()
                                || client.player.getDeltaMovement().horizontalDistance() < 0.01,
                        40);
                double x = context.computeOnClient(client -> client.player.getX());
                GameTestSupport.check(x < origin.getX() + 6.0, "Auto Walk went through the wall (x " + x + ")");
            } finally {
                GameTestSupport.enable(context, "auto_walk", false);
            }
        }
    }

    /** Auto Sprint makes the server see the player sprinting while walking forward. */
    static void autoSprint(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            GameTestSupport.enable(context, "auto_sprint", true);
            try {
                context.getInput().holdKey(options -> options.keyUp);
                arena.server().waitFor(server -> arena.serverPlayer(server).isSprinting(), 40);
            } finally {
                context.getInput().releaseKey(options -> options.keyUp);
                GameTestSupport.enable(context, "auto_sprint", false);
            }
        }
    }

    /** Stash Finder reports a cluster of chests in chat. */
    static void stashFinder(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            for (int index = 0; index < 10; index++) {
                arena.setBlock(origin.offset(-6 + index % 5 * 2, 0, 5 + index / 5 * 2), "chest");
            }
            context.waitTicks(5);
            GameTestSupport.enable(context, "stash_finder", true);
            try {
                context.waitFor(client -> ClientChat.contains(client, "Possible stash"), 200);
            } finally {
                GameTestSupport.enable(context, "stash_finder", false);
            }
        }
    }

    /** Elytra Fly starts gliding by itself and carries the player forward. */
    static void elytraFly(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name() + " armor.chest with elytra");
            arena.run("effect give " + arena.name() + " resistance infinite 4 true");
            arena.teleport(arena.origin().above(40), -90.0F);
            context.waitTicks(5);
            double startX = context.computeOnClient(client -> client.player.getX());
            GameTestSupport.enable(context, "elytra_fly", true);
            try {
                context.waitFor(client -> client.player.isFallFlying(), 40);
                context.getInput().holdKeyFor(options -> options.keyUp, 20);
                double moved = arena.server()
                                .computeOnServer(
                                        server -> arena.serverPlayer(server).getX())
                        - startX;
                GameTestSupport.check(moved > 10.0, "Elytra Fly moved the player only " + moved + " blocks");
            } finally {
                GameTestSupport.enable(context, "elytra_fly", false);
            }
        }
    }

    /** Surround still finishes while an enemy stands right next to the player. */
    static void surroundNextToEnemy(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos feet = arena.origin();
            arena.spawnTarget(feet.east(2));
            arena.give("obsidian 64");
            context.waitTicks(10);
            GameTestSupport.enable(context, "surround", true);
            try {
                arena.server()
                        .waitFor(
                                server -> {
                                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                                        if (!server.overworld()
                                                .getBlockState(feet.relative(direction))
                                                .is(Blocks.OBSIDIAN)) {
                                            return false;
                                        }
                                    }
                                    return true;
                                },
                                120);
            } finally {
                GameTestSupport.enable(context, "surround", false);
            }
        }
    }
}
