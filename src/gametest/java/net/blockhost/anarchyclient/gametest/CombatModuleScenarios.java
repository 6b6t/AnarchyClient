package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Scenarios for established combat modules against a server-side target. */
final class CombatModuleScenarios {

    private CombatModuleScenarios() {
    }

    /** Places the target on an obsidian floor three blocks east and makes the player explosion-proof. */
    private static BlockPos targetOnObsidian(final GameTestSupport.Arena arena) {
        BlockPos origin = arena.origin();
        arena.run("fill " + GameTestSupport.at(origin.offset(-1, -1, -3)) + " " + GameTestSupport.at(origin.offset(6, -1, 3))
                + " obsidian");
        arena.run("effect give " + arena.name() + " resistance infinite 4 true");
        BlockPos target = origin.offset(3, 0, 0);
        arena.spawnTarget(target);
        return target;
    }

    private static void expectTargetDamaged(final ClientGameTestContext context, final GameTestSupport.Arena arena,
                                            final String moduleId, final int timeoutTicks) {
        float start = arena.targetHealth();
        GameTestSupport.enable(context, moduleId, true);
        try {
            arena.server().waitFor(server -> GameTestSupport.targetHealth(server) < start, timeoutTicks);
        } finally {
            GameTestSupport.enable(context, moduleId, false);
        }
    }

    static void crystalAura(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            targetOnObsidian(arena);
            arena.give("end_crystal 32");
            context.waitTicks(10);
            expectTargetDamaged(context, arena, "crystal_aura", 200);
        }
    }

    static void anchorAura(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            targetOnObsidian(arena);
            arena.give("respawn_anchor 8");
            arena.give("glowstone 32");
            context.waitTicks(10);
            expectTargetDamaged(context, arena, "anchor_aura", 200);
        }
    }

    static void killAura(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.spawnTarget(arena.origin().offset(2, 0, 0));
            arena.hotbar(0, "diamond_sword");
            context.waitTicks(10);
            expectTargetDamaged(context, arena, "kill_aura", 100);
        }
    }

    static void autoTotem(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name() + " inventory.4 with totem_of_undying");
            context.waitTicks(5);
            GameTestSupport.enable(context, "auto_totem", true);
            try {
                arena.run("damage " + arena.name() + " 15");
                arena.server().waitFor(server -> arena.serverPlayer(server).getOffhandItem().is(Items.TOTEM_OF_UNDYING), 60);
            } finally {
                GameTestSupport.enable(context, "auto_totem", false);
            }
        }
    }

    static void surround(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos feet = arena.origin();
            arena.give("obsidian 64");
            context.waitTicks(5);
            GameTestSupport.enable(context, "surround", true);
            try {
                arena.server().waitFor(server -> {
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        if (!is(server, feet.relative(direction), Blocks.OBSIDIAN)) {
                            return false;
                        }
                    }
                    return true;
                }, 100);
            } finally {
                GameTestSupport.enable(context, "surround", false);
            }
        }
    }

    static void autoTrap(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos target = arena.origin().offset(3, 0, 0);
            arena.spawnTarget(target);
            arena.give("obsidian 64");
            context.waitTicks(10);
            GameTestSupport.enable(context, "auto_trap", true);
            try {
                arena.server().waitFor(server -> {
                    int placed = 0;
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        if (is(server, target.above().relative(direction), Blocks.OBSIDIAN)) {
                            placed++;
                        }
                    }
                    return placed >= 3;
                }, 200);
            } finally {
                GameTestSupport.enable(context, "auto_trap", false);
            }
        }
    }

    static void autoWeb(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos target = arena.origin().offset(3, 0, 0);
            arena.spawnTarget(target);
            arena.give("cobweb 16");
            context.waitTicks(10);
            GameTestSupport.enable(context, "auto_web", true);
            try {
                arena.server().waitFor(server -> is(server, target, Blocks.COBWEB) || is(server, target.above(), Blocks.COBWEB), 100);
            } finally {
                GameTestSupport.enable(context, "auto_web", false);
            }
        }
    }

    static void holeFiller(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos hole = arena.origin().offset(3, 0, 0);
            arena.hole(hole);
            arena.spawnTarget(arena.origin().offset(4, 0, 2));
            arena.give("obsidian 64");
            context.waitTicks(10);
            GameTestSupport.enable(context, "hole_filler", true);
            try {
                arena.server().waitFor(server -> !server.overworld().getBlockState(hole).isAir(), 200);
            } finally {
                GameTestSupport.enable(context, "hole_filler", false);
            }
        }
    }

    static void autoCity(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos target = arena.origin().offset(3, 0, 0);
            arena.hole(target);
            arena.spawnTarget(target);
            arena.hotbar(0, "diamond_pickaxe[enchantments={efficiency:5}]");
            context.waitTicks(10);
            GameTestSupport.enable(context, "auto_city", true);
            try {
                arena.server().waitFor(server -> {
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        if (server.overworld().getBlockState(target.relative(direction)).isAir()) {
                            return true;
                        }
                    }
                    return false;
                }, 400);
            } finally {
                GameTestSupport.enable(context, "auto_city", false);
            }
        }
    }

    private static boolean is(final MinecraftServer server, final BlockPos pos, final Block block) {
        return server.overworld().getBlockState(pos).is(block);
    }
}
