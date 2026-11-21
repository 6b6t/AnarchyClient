package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Scenarios for established player, movement, and world modules. */
final class PlayerModuleScenarios {

    private PlayerModuleScenarios() {}

    static void autoArmor(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name() + " inventory.2 with diamond_chestplate");
            arena.run("item replace entity " + arena.name() + " inventory.3 with diamond_helmet");
            context.waitTicks(5);
            GameTestSupport.enable(context, "auto_armor", true);
            try {
                arena.server()
                        .waitFor(
                                server -> arena.serverPlayer(server)
                                                .getItemBySlot(EquipmentSlot.CHEST)
                                                .is(Items.DIAMOND_CHESTPLATE)
                                        && arena.serverPlayer(server)
                                                .getItemBySlot(EquipmentSlot.HEAD)
                                                .is(Items.DIAMOND_HELMET),
                                100);
            } finally {
                GameTestSupport.enable(context, "auto_armor", false);
            }
        }
    }

    static void noFall(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            GameTestSupport.enable(context, "no_fall", true);
            try {
                arena.teleport(arena.origin().above(25), -90.0F);
                context.waitFor(
                        client -> client.player.onGround()
                                && client.player.getY() < arena.origin().getY() + 1,
                        200);
                context.waitTicks(10);
                float health = arena.server()
                        .computeOnServer(server -> arena.serverPlayer(server).getHealth());
                GameTestSupport.check(health >= 20.0F, "No Fall let the player take fall damage: health " + health);
            } finally {
                GameTestSupport.enable(context, "no_fall", false);
            }
        }
    }

    static void smartEat(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.hotbar(1, "cooked_beef 8");
            arena.server()
                    .runOnServer(
                            server -> arena.serverPlayer(server).getFoodData().setFoodLevel(6));
            context.waitTicks(10);
            GameTestSupport.enable(context, "smart_eat", true);
            try {
                arena.server()
                        .waitFor(
                                server ->
                                        arena.serverPlayer(server).getFoodData().getFoodLevel() > 6,
                                200);
            } finally {
                GameTestSupport.enable(context, "smart_eat", false);
            }
        }
    }

    static void chestStealer(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos chest = arena.origin().offset(2, 0, 0);
            arena.setBlock(
                    chest,
                    "chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:5},{Slot:4b,id:\"minecraft:emerald\",count:3}]}");
            context.waitTicks(5);
            GameTestSupport.enable(context, "chest_stealer", true);
            try {
                context.runOnClient(client -> client.gameMode.useItemOn(
                        client.player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(
                                Vec3.atCenterOf(chest).relative(Direction.UP, 0.5), Direction.UP, chest, false)));
                arena.server()
                        .waitFor(
                                server -> arena.serverPlayer(server)
                                                .getInventory()
                                                .contains(stack -> stack.is(Items.DIAMOND))
                                        && arena.serverPlayer(server)
                                                .getInventory()
                                                .contains(stack -> stack.is(Items.EMERALD)),
                                100);
            } finally {
                GameTestSupport.enable(context, "chest_stealer", false);
                context.setScreen(() -> null);
            }
        }
    }

    static void autoTool(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos block = arena.origin().offset(2, 1, 0);
            arena.setBlock(block, "stone");
            arena.hotbar(0, "diamond_sword");
            arena.hotbar(3, "iron_pickaxe");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            GameTestSupport.enable(context, "auto_tool", true);
            try {
                context.getInput().lookAt(block);
                context.waitTicks(2);
                context.getInput().holdKeyFor(options -> options.keyAttack, 40);
                arena.server()
                        .waitFor(
                                server ->
                                        server.overworld().getBlockState(block).isAir(),
                                60);
                int selected = context.computeOnClient(
                        client -> client.player.getInventory().getSelectedSlot());
                GameTestSupport.check(selected == 3, "Auto Tool did not switch to the pickaxe (slot " + selected + ")");
            } finally {
                GameTestSupport.enable(context, "auto_tool", false);
            }
        }
    }

    static void scaffold(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            arena.run("fill " + GameTestSupport.at(origin.offset(1, -2, -1)) + " "
                    + GameTestSupport.at(origin.offset(6, -1, 1)) + " air");
            arena.hotbar(0, "dirt 64");
            context.waitTicks(5);
            GameTestSupport.enable(context, "scaffold", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyUp, 30);
                context.waitTicks(5);
                int placed = arena.server().computeOnServer(server -> {
                    int count = 0;
                    for (int dx = 1; dx <= 6; dx++) {
                        if (!server.overworld()
                                .getBlockState(origin.offset(dx, -1, 0))
                                .isAir()) {
                            count++;
                        }
                    }
                    return count;
                });
                double y = context.computeOnClient(client -> client.player.getY());
                GameTestSupport.check(placed >= 2, "Scaffold placed only " + placed + " blocks under the player");
                GameTestSupport.check(y >= origin.getY() - 0.01, "The player fell while Scaffold was on (y " + y + ")");
            } finally {
                GameTestSupport.enable(context, "scaffold", false);
            }
        }
    }

    static void jesus(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            arena.run("fill " + GameTestSupport.at(origin.offset(1, -3, -2)) + " "
                    + GameTestSupport.at(origin.offset(8, -1, 2)) + " water");
            context.waitTicks(5);
            GameTestSupport.enable(context, "jesus", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyUp, 40);
                double y = context.computeOnClient(client -> client.player.getY());
                double x = context.computeOnClient(client -> client.player.getX());
                GameTestSupport.check(x > origin.getX() + 3, "The player did not move over the water (x " + x + ")");
                GameTestSupport.check(y >= origin.getY() - 0.2, "Jesus let the player sink (y " + y + ")");
            } finally {
                GameTestSupport.enable(context, "jesus", false);
            }
        }
    }

    static void nuker(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            arena.run("fill " + GameTestSupport.at(origin.offset(1, 0, -1)) + " "
                    + GameTestSupport.at(origin.offset(3, 1, 1)) + " dirt");
            arena.hotbar(0, "diamond_shovel[enchantments={efficiency:5}]");
            context.waitTicks(5);
            GameTestSupport.enable(context, "nuker", true);
            try {
                arena.server()
                        .waitFor(
                                server -> {
                                    int left = 0;
                                    for (int dx = 1; dx <= 3; dx++) {
                                        for (int dy = 0; dy <= 1; dy++) {
                                            for (int dz = -1; dz <= 1; dz++) {
                                                if (!server.overworld()
                                                        .getBlockState(origin.offset(dx, dy, dz))
                                                        .isAir()) {
                                                    left++;
                                                }
                                            }
                                        }
                                    }
                                    return left <= 9;
                                },
                                200);
            } finally {
                GameTestSupport.enable(context, "nuker", false);
            }
        }
    }

    /** Reach hits a target that stands just outside the vanilla attack range. */
    static void reach(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            // The target's near side is 3.7 blocks away: past vanilla's 3, inside Reach's default 4.
            arena.spawnTarget(arena.origin().offset(4, 0, 0));
            arena.hotbar(0, "diamond_sword");
            context.waitTicks(10);
            context.getInput().pressKey(options -> options.keyAttack);
            context.waitTicks(5);
            GameTestSupport.check(
                    arena.targetHits() == 0, "The target was in vanilla reach, so the test proves nothing");
            GameTestSupport.enable(context, "reach", true);
            try {
                context.waitTicks(25);
                context.getInput().pressKey(options -> options.keyAttack);
                arena.server().waitFor(server -> GameTestSupport.targetHits(server) > 0, 20);
            } finally {
                GameTestSupport.enable(context, "reach", false);
            }
        }
    }

    /** EXP Thrower throws XP bottles while worn armor is below the durability threshold. */
    static void expThrower(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name() + " armor.chest with diamond_chestplate[damage=400]");
            arena.hotbar(2, "experience_bottle 16");
            context.waitTicks(5);
            GameTestSupport.enable(context, "exp_thrower", true);
            try {
                arena.server()
                        .waitFor(
                                server -> arena.serverPlayer(server)
                                                .getInventory()
                                                .getItem(2)
                                                .getCount()
                                        < 16,
                                100);
            } finally {
                GameTestSupport.enable(context, "exp_thrower", false);
            }
        }
    }

    /** Auto Mend moves a damaged mending item into the offhand. */
    static void autoMend(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name()
                    + " inventory.5 with diamond_pickaxe[damage=1200,enchantments={mending:1}]");
            context.waitTicks(5);
            GameTestSupport.enable(context, "auto_mend", true);
            try {
                arena.server()
                        .waitFor(
                                server -> arena.serverPlayer(server)
                                        .getOffhandItem()
                                        .is(Items.DIAMOND_PICKAXE),
                                100);
            } finally {
                GameTestSupport.enable(context, "auto_mend", false);
            }
        }
    }

    /** Chest Swap puts on the elytra in place of the chestplate, then turns itself off. */
    static void chestSwap(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name() + " armor.chest with diamond_chestplate");
            arena.run("item replace entity " + arena.name() + " inventory.7 with elytra");
            context.waitTicks(5);
            GameTestSupport.enable(context, "chest_swap", true);
            try {
                arena.server()
                        .waitFor(
                                server -> arena.serverPlayer(server)
                                                .getItemBySlot(EquipmentSlot.CHEST)
                                                .is(Items.ELYTRA)
                                        && arena.serverPlayer(server)
                                                .getInventory()
                                                .contains(stack -> stack.is(Items.DIAMOND_CHESTPLATE)),
                                100);
                context.waitFor(client -> !GameTestSupport.module("chest_swap").enabled(), 20);
            } finally {
                GameTestSupport.enable(context, "chest_swap", false);
            }
        }
    }

    /** Elytra Swap puts on the elytra once the player starts falling. */
    static void elytraSwap(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name() + " armor.chest with iron_chestplate");
            arena.run("item replace entity " + arena.name() + " inventory.7 with elytra");
            arena.run("effect give " + arena.name() + " resistance infinite 4 true");
            context.waitTicks(5);
            GameTestSupport.enable(context, "elytra_swap", true);
            try {
                context.waitTicks(10);
                boolean early = arena.server()
                        .computeOnServer(server -> arena.serverPlayer(server)
                                .getItemBySlot(EquipmentSlot.CHEST)
                                .is(Items.ELYTRA));
                GameTestSupport.check(!early, "Elytra Swap equipped the elytra on the ground");
                arena.teleport(arena.origin().above(30), -90.0F);
                arena.server()
                        .waitFor(
                                server -> arena.serverPlayer(server)
                                        .getItemBySlot(EquipmentSlot.CHEST)
                                        .is(Items.ELYTRA),
                                60);
            } finally {
                GameTestSupport.enable(context, "elytra_swap", false);
            }
        }
    }
}
