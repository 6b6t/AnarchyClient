package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
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
        GameTestSupport.enable(context, moduleId, true);
        try {
            arena.server().waitFor(server -> GameTestSupport.targetHits(server) > 0, timeoutTicks);
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

    /** Bed Aura places a bed next to the target in the Nether and detonates it. */
    static void bedAura(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("execute in minecraft:the_nether run forceload add -8 -8 8 8");
            arena.run("execute in minecraft:the_nether run fill -6 100 -6 6 106 6 air");
            arena.run("execute in minecraft:the_nether run fill -6 99 -6 6 99 6 obsidian");
            arena.run("effect give " + arena.name() + " resistance infinite 4 true");
            arena.give("red_bed 16");
            arena.run("execute in minecraft:the_nether run tp " + arena.name() + " 0.5 100 0.5 -90 0");
            context.waitFor(client -> client.level.dimension() == Level.NETHER, 200);
            arena.spawnTarget(Level.NETHER, new BlockPos(3, 100, 0));
            context.waitTicks(10);
            expectTargetDamaged(context, arena, "bed_aura", 200);
        }
    }

    /** Auto Anvil drops an anvil on the target's head. */
    static void autoAnvil(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.spawnTarget(arena.origin().offset(3, 0, 0));
            arena.give("anvil 8");
            arena.give("obsidian 64");
            context.waitTicks(10);
            expectTargetDamaged(context, arena, "auto_anvil", 200);
        }
    }

    /** Auto Weapon switches to the strongest hotbar weapon while attack is held. */
    static void autoWeapon(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.hotbar(0, "stone");
            arena.hotbar(2, "wooden_sword");
            arena.hotbar(4, "diamond_sword");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            context.waitTicks(5);
            GameTestSupport.enable(context, "auto_weapon", true);
            try {
                context.waitTicks(5);
                int idle = context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
                GameTestSupport.check(idle == 0, "Auto Weapon switched slots outside combat (slot " + idle + ")");
                context.getInput().holdKeyFor(options -> options.keyAttack, 5);
                int selected = context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
                GameTestSupport.check(selected == 4, "Auto Weapon did not pick the diamond sword (slot " + selected + ")");
            } finally {
                GameTestSupport.enable(context, "auto_weapon", false);
            }
        }
    }

    /** Auto Clicker keeps attacking while attack is held. Vanilla attacks an entity once per click. */
    static void autoClicker(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.spawnTarget(arena.origin().offset(2, 0, 0));
            arena.hotbar(0, "diamond_sword");
            context.waitTicks(10);
            GameTestSupport.enable(context, "auto_clicker", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyAttack, 40);
                int hits = arena.targetHits();
                GameTestSupport.check(hits >= 5, "Auto Clicker hit the target only " + hits + " time(s) in two seconds");
            } finally {
                GameTestSupport.enable(context, "auto_clicker", false);
            }
        }
    }

    /** Auto Pot throws a healing potion when health drops below the threshold. */
    static void autoPot(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.hotbar(3, "splash_potion[potion_contents={potion:\"minecraft:healing\"}]");
            context.waitTicks(5);
            GameTestSupport.enable(context, "auto_pot", true);
            try {
                context.waitTicks(10);
                boolean kept = arena.server().computeOnServer(server -> hasSplashPotion(arena.serverPlayer(server).getInventory()));
                GameTestSupport.check(kept, "Auto Pot threw a potion at full health");
                arena.run("damage " + arena.name() + " 12 minecraft:generic");
                arena.server().waitFor(server -> !hasSplashPotion(arena.serverPlayer(server).getInventory()), 60);
            } finally {
                GameTestSupport.enable(context, "auto_pot", false);
            }
        }
    }

    private static boolean hasSplashPotion(final Inventory inventory) {
        return inventory.contains(stack -> stack.is(Items.SPLASH_POTION));
    }

    private static boolean is(final MinecraftServer server, final BlockPos pos, final Block block) {
        return server.overworld().getBlockState(pos).is(block);
    }
}
