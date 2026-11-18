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

    private PlayerModuleScenarios() {
    }

    static void autoArmor(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name() + " inventory.2 with diamond_chestplate");
            arena.run("item replace entity " + arena.name() + " inventory.3 with diamond_helmet");
            context.waitTicks(5);
            GameTestSupport.enable(context, "auto_armor", true);
            try {
                arena.server().waitFor(server -> arena.serverPlayer(server).getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE)
                        && arena.serverPlayer(server).getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET), 100);
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
                context.waitFor(client -> client.player.onGround() && client.player.getY() < arena.origin().getY() + 1, 200);
                context.waitTicks(10);
                float health = arena.server().computeOnServer(server -> arena.serverPlayer(server).getHealth());
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
            arena.server().runOnServer(server -> arena.serverPlayer(server).getFoodData().setFoodLevel(6));
            context.waitTicks(10);
            GameTestSupport.enable(context, "smart_eat", true);
            try {
                arena.server().waitFor(server -> arena.serverPlayer(server).getFoodData().getFoodLevel() > 6, 200);
            } finally {
                GameTestSupport.enable(context, "smart_eat", false);
            }
        }
    }

    static void chestStealer(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos chest = arena.origin().offset(2, 0, 0);
            arena.setBlock(chest, "chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:5},{Slot:4b,id:\"minecraft:emerald\",count:3}]}");
            context.waitTicks(5);
            GameTestSupport.enable(context, "chest_stealer", true);
            try {
                context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(chest).relative(Direction.UP, 0.5), Direction.UP, chest, false)));
                arena.server().waitFor(server -> arena.serverPlayer(server).getInventory().contains(stack -> stack.is(Items.DIAMOND))
                        && arena.serverPlayer(server).getInventory().contains(stack -> stack.is(Items.EMERALD)), 100);
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
                arena.server().waitFor(server -> server.overworld().getBlockState(block).isAir(), 60);
                int selected = context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
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
            arena.run("fill " + GameTestSupport.at(origin.offset(1, -2, -1)) + " " + GameTestSupport.at(origin.offset(6, -1, 1)) + " air");
            arena.hotbar(0, "dirt 64");
            context.waitTicks(5);
            GameTestSupport.enable(context, "scaffold", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyUp, 30);
                context.waitTicks(5);
                int placed = arena.server().computeOnServer(server -> {
                    int count = 0;
                    for (int dx = 1; dx <= 6; dx++) {
                        if (!server.overworld().getBlockState(origin.offset(dx, -1, 0)).isAir()) {
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
            arena.run("fill " + GameTestSupport.at(origin.offset(1, -3, -2)) + " " + GameTestSupport.at(origin.offset(8, -1, 2)) + " water");
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
            arena.run("fill " + GameTestSupport.at(origin.offset(1, 0, -1)) + " " + GameTestSupport.at(origin.offset(3, 1, 1)) + " dirt");
            arena.hotbar(0, "diamond_shovel[enchantments={efficiency:5}]");
            context.waitTicks(5);
            GameTestSupport.enable(context, "nuker", true);
            try {
                arena.server().waitFor(server -> {
                    int left = 0;
                    for (int dx = 1; dx <= 3; dx++) {
                        for (int dy = 0; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (!server.overworld().getBlockState(origin.offset(dx, dy, dz)).isAir()) {
                                    left++;
                                }
                            }
                        }
                    }
                    return left <= 9;
                }, 200);
            } finally {
                GameTestSupport.enable(context, "nuker", false);
            }
        }
    }
}
