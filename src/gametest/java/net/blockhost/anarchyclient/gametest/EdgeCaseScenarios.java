package net.blockhost.anarchyclient.gametest;

import com.google.gson.JsonPrimitive;
import net.blockhost.anarchyclient.module.DebugValueRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Edge cases and interactions between modules. */
final class EdgeCaseScenarios {

    private EdgeCaseScenarios() {
    }

    /**
     * Piston Crystal must keep cycling: the blast destroys its piston, so it has to rebuild the setup
     * for a second hit. The target is healed after each hit so it survives.
     */
    static void pistonCrystalRepeats(final ClientGameTestContext context) {
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
            AtomicInteger hits = new AtomicInteger();
            GameTestSupport.enable(context, "piston_crystal", true);
            try {
                arena.server().waitFor(server -> {
                    ServerPlayer victim = target(server);
                    if (victim != null && victim.getHealth() < victim.getMaxHealth()) {
                        hits.incrementAndGet();
                        victim.setHealth(victim.getMaxHealth());
                        GameTestSupport.resetInvulnerability(victim);
                    }
                    return hits.get() >= 2;
                }, 600);
            } catch (AssertionError timeout) {
                throw new AssertionError("Piston Crystal hit the target only " + hits.get() + " time(s); debug "
                        + DebugValueRegistry.snapshot().get("piston_crystal"), timeout);
            } finally {
                GameTestSupport.enable(context, "piston_crystal", false);
            }
        }
    }

    /** Anti Aim must give way to modules that need to rotate, so Surround still finishes. */
    static void antiAimWithSurround(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos feet = arena.origin();
            arena.give("obsidian 64");
            context.waitTicks(5);
            GameTestSupport.enable(context, "anti_aim", true);
            GameTestSupport.enable(context, "surround", true);
            try {
                arena.server().waitFor(server -> {
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        if (!server.overworld().getBlockState(feet.relative(direction)).is(Blocks.OBSIDIAN)) {
                            return false;
                        }
                    }
                    return true;
                }, 120);
            } finally {
                GameTestSupport.enable(context, "surround", false);
                GameTestSupport.enable(context, "anti_aim", false);
            }
        }
    }

    /** Anti Aim must also let Kill Aura aim, so the target still takes damage. */
    static void antiAimWithKillAura(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.spawnTarget(arena.origin().offset(2, 0, 0));
            arena.hotbar(0, "diamond_sword");
            context.waitTicks(10);
            float start = arena.targetHealth();
            GameTestSupport.enable(context, "anti_aim", true);
            GameTestSupport.enable(context, "kill_aura", true);
            try {
                arena.server().waitFor(server -> GameTestSupport.targetHealth(server) < start, 100);
            } finally {
                GameTestSupport.enable(context, "kill_aura", false);
                GameTestSupport.enable(context, "anti_aim", false);
            }
        }
    }

    /** With Strength already active, Arrow Swap moves on to the next effect in its list. */
    static void arrowSwapSkipsActiveEffects(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.hotbar(0, "bow");
            arena.hotbar(1, "arrow 16");
            arena.run("item replace entity " + arena.name()
                    + " inventory.5 with tipped_arrow[potion_contents={potion:\"minecraft:strength\"}] 4");
            arena.run("item replace entity " + arena.name()
                    + " inventory.6 with tipped_arrow[potion_contents={potion:\"minecraft:swiftness\"}] 4");
            arena.run("effect give " + arena.name() + " strength 600 0 true");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            context.waitTicks(5);
            GameTestSupport.enable(context, "arrow_swap", true);
            try {
                arena.server().waitFor(server -> {
                    PotionContents potion = arena.serverPlayer(server).getInventory().getItem(1)
                            .get(DataComponents.POTION_CONTENTS);
                    return potion != null && potion.potion().map(holder -> holder.is(Potions.SWIFTNESS)).orElse(false);
                }, 60);
            } finally {
                GameTestSupport.enable(context, "arrow_swap", false);
            }
        }
    }

    /** Item Saver also stops attacks with a weapon that is about to break. */
    static void itemSaverBlocksAttacks(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.spawnTarget(arena.origin().offset(2, 0, 0));
            arena.hotbar(0, "diamond_sword[damage=1559]");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            context.waitTicks(10);
            float start = arena.targetHealth();
            GameTestSupport.enable(context, "item_saver", true);
            try {
                context.getInput().lookAt(arena.origin().offset(2, 1, 0));
                context.waitTicks(2);
                context.getInput().pressKey(options -> options.keyAttack);
                context.waitTicks(10);
                float health = arena.targetHealth();
                int damage = arena.server().computeOnServer(server ->
                        arena.serverPlayer(server).getInventory().getItem(0).getDamageValue());
                GameTestSupport.check(health == start, "Item Saver let a nearly broken sword hit the target");
                GameTestSupport.check(damage == 1559, "The protected sword lost durability: " + damage);
            } finally {
                GameTestSupport.enable(context, "item_saver", false);
            }
        }
    }

    /** Crawl Trap roofs a crawling target so it cannot stand up. */
    static void crawlTrap(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos target = arena.origin().offset(3, 0, 0);
            arena.spawnTarget(target);
            arena.server().runOnServer(server -> target(server).setPose(Pose.SWIMMING));
            arena.give("obsidian 32");
            context.waitFor(client -> client.level.players().stream()
                    .anyMatch(player -> GameTestSupport.TARGET.equals(player.getScoreboardName()) && player.getPose() == Pose.SWIMMING), 40);
            GameTestSupport.enable(context, "crawl_trap", true);
            try {
                arena.server().waitFor(server -> server.overworld().getBlockState(target.above()).is(Blocks.OBSIDIAN), 100);
            } finally {
                GameTestSupport.enable(context, "crawl_trap", false);
            }
        }
    }

    /** Crystal Aura with ID prediction still hurts the target and does not get the player kicked. */
    static void crystalAuraIdPredict(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            arena.run("fill " + GameTestSupport.at(origin.offset(-1, -1, -3)) + " " + GameTestSupport.at(origin.offset(6, -1, 3))
                    + " obsidian");
            arena.run("effect give " + arena.name() + " resistance infinite 4 true");
            arena.spawnTarget(origin.offset(3, 0, 0));
            arena.give("end_crystal 32");
            context.waitTicks(10);
            GameTestSupport.setting(context, "crystal_aura", "id_predict", new JsonPrimitive(true));
            GameTestSupport.setting(context, "crystal_aura", "set_dead", new JsonPrimitive(true));
            float start = arena.targetHealth();
            GameTestSupport.enable(context, "crystal_aura", true);
            try {
                arena.server().waitFor(server -> GameTestSupport.targetHealth(server) < start, 200);
                context.waitTicks(40);
                GameTestSupport.check(context.computeOnClient(client -> client.level != null), "Crystal Aura got the player kicked");
            } finally {
                if (context.computeOnClient(client -> client.level != null)) {
                    GameTestSupport.enable(context, "crystal_aura", false);
                }
                GameTestSupport.setting(context, "crystal_aura", "id_predict", new JsonPrimitive(false));
                GameTestSupport.setting(context, "crystal_aura", "set_dead", new JsonPrimitive(false));
            }
        }
    }

    /** Skin Flicker changes the skin layers while on and restores the player's own choice afterwards. */
    static void skinFlickerRestores(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.arena(context, world);
            context.runOnClient(client -> {
                client.options.setModelPart(PlayerModelPart.HAT, false);
                client.options.setModelPart(PlayerModelPart.JACKET, true);
            });
            Set<PlayerModelPart> before = parts(context);
            GameTestSupport.enable(context, "skin_flicker", true);
            boolean changed = false;
            for (int tick = 0; tick < 30 && !changed; tick++) {
                context.waitTick();
                changed = !parts(context).equals(before);
            }
            GameTestSupport.enable(context, "skin_flicker", false);
            GameTestSupport.check(changed, "Skin Flicker never changed the skin layers");
            GameTestSupport.check(parts(context).equals(before), "Skin Flicker did not restore the skin layers: "
                    + parts(context) + " instead of " + before);
        }
    }

    private static Set<PlayerModelPart> parts(final ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            Set<PlayerModelPart> enabled = EnumSet.noneOf(PlayerModelPart.class);
            for (PlayerModelPart part : PlayerModelPart.values()) {
                if (client.options.isModelPartEnabled(part)) {
                    enabled.add(part);
                }
            }
            return enabled;
        });
    }

    /** Hole Snap in its default directional mode pulls a player who walks toward a hole into it. */
    static void holeSnapDirectional(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos hole = arena.origin().offset(3, 0, 0);
            arena.hole(hole);
            arena.teleport(hole.west().above(), -90.0F);
            context.waitTicks(10);
            GameTestSupport.enable(context, "hole_snap", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyUp, 3);
                context.waitFor(client -> client.player.blockPosition().equals(hole) && client.player.onGround(), 100);
            } finally {
                GameTestSupport.enable(context, "hole_snap", false);
            }
        }
    }

    /** Announcer reports placed blocks in chat at its interval. */
    static void announcer(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            arena.hotbar(0, "dirt 16");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            GameTestSupport.setting(context, "announcer", "interval", new JsonPrimitive(10.0));
            GameTestSupport.enable(context, "announcer", true);
            try {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos ground = origin.offset(2, -1, dz);
                    context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(ground).relative(Direction.UP, 0.5), Direction.UP, ground, false)));
                    context.waitTicks(5);
                }
                context.waitFor(client -> ClientChat.contains(client, "I just placed 3 blocks."), 260);
            } finally {
                GameTestSupport.enable(context, "announcer", false);
                GameTestSupport.setting(context, "announcer", "interval", new JsonPrimitive(60.0));
            }
        }
    }

    /** Spiral starts by walking east, its first leg. */
    static void spiral(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            double startX = context.computeOnClient(client -> client.player.getX());
            double startZ = context.computeOnClient(client -> client.player.getZ());
            GameTestSupport.enable(context, "spiral", true);
            try {
                context.waitTicks(40);
                double dx = context.computeOnClient(client -> client.player.getX()) - startX;
                double dz = context.computeOnClient(client -> client.player.getZ()) - startZ;
                GameTestSupport.check(dx > 4.0 && Math.abs(dz) < 1.0, "Spiral did not walk east: moved " + dx + ", " + dz);
                double serverDx = arena.server().computeOnServer(server -> arena.serverPlayer(server).getX()) - startX;
                GameTestSupport.check(serverDx > 3.0, "The server did not see the Spiral walk: moved " + serverDx);
            } finally {
                GameTestSupport.enable(context, "spiral", false);
            }
        }
    }

    /** Auto Firework fires rockets from the hotbar while gliding slowly. */
    static void autoFirework(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name() + " armor.chest with elytra");
            arena.hotbar(0, "firework_rocket 16");
            arena.run("effect give " + arena.name() + " resistance infinite 4 true");
            arena.teleport(arena.origin().above(60), -90.0F);
            context.waitTicks(10);
            context.getInput().pressKey(options -> options.keyJump);
            context.waitTicks(2);
            context.getInput().pressKey(options -> options.keyJump);
            context.waitFor(client -> client.player.isFallFlying(), 40);
            GameTestSupport.enable(context, "auto_firework", true);
            try {
                arena.server().waitFor(server -> arena.serverPlayer(server).getInventory().getItem(0).getCount() < 16, 100);
            } finally {
                GameTestSupport.enable(context, "auto_firework", false);
            }
        }
    }

    private static ServerPlayer target(final MinecraftServer server) {
        return server.overworld().players().stream()
                .filter(player -> GameTestSupport.TARGET.equals(player.getScoreboardName()))
                .findFirst()
                .orElse(null);
    }
}
