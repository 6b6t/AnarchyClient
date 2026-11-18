package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/** Scenarios for inventory, trading, and other world interaction modules. */
final class WorldModuleScenarios {

    private WorldModuleScenarios() {
    }

    static void autoTame(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos spot = arena.origin().offset(2, 0, 0);
            arena.run("summon minecraft:wolf " + GameTestSupport.at(spot) + " {NoAI:1b}");
            arena.give("bone 64");
            context.waitTicks(10);
            GameTestSupport.enable(context, "auto_tame", true);
            try {
                arena.server().waitFor(server -> wolves(server, spot).stream().anyMatch(Wolf::isTame), 400);
            } finally {
                GameTestSupport.enable(context, "auto_tame", false);
            }
        }
    }

    private static List<Wolf> wolves(final MinecraftServer server, final BlockPos spot) {
        return server.overworld().getEntitiesOfClass(Wolf.class, new AABB(spot).inflate(4.0));
    }

    static void autoTrade(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos spot = arena.origin().offset(2, 0, 0);
            arena.run("summon minecraft:villager " + GameTestSupport.at(spot) + " {NoAI:1b,"
                    + "VillagerData:{profession:\"minecraft:librarian\",level:2,type:\"minecraft:plains\"},"
                    + "Offers:{Recipes:[{buy:{id:\"minecraft:paper\",count:24},sell:{id:\"minecraft:emerald\",count:1},"
                    + "maxUses:3,rewardExp:0b}]}}");
            arena.give("paper 64");
            context.waitTicks(10);
            context.runOnClient(client -> {
                Entity villager = StreamSupport.stream(client.level.entitiesForRendering().spliterator(), false)
                        .filter(entity -> entity instanceof Villager)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("The villager did not reach the client"));
                client.gameMode.interact(client.player, villager, new EntityHitResult(villager), InteractionHand.MAIN_HAND);
            });
            context.waitForScreen(MerchantScreen.class);
            GameTestSupport.enable(context, "auto_trade", true);
            try {
                arena.server().waitFor(server -> count(arena.serverPlayer(server).getInventory(), Items.EMERALD) >= 2, 200);
            } finally {
                GameTestSupport.enable(context, "auto_trade", false);
                context.setScreen(() -> null);
            }
        }
    }

    static void inventorySorter(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name() + " inventory.0 with stone 10");
            arena.run("item replace entity " + arena.name() + " inventory.1 with apple 3");
            arena.run("item replace entity " + arena.name() + " inventory.3 with stone 5");
            arena.run("item replace entity " + arena.name() + " inventory.6 with apple 1");
            arena.run("item replace entity " + arena.name() + " inventory.8 with dirt 2");
            context.waitTicks(10);
            GameTestSupport.enable(context, "inventory_sorter", true);
            try {
                context.waitFor(client -> !GameTestSupport.module("inventory_sorter").enabled(), 200);
                context.waitTicks(5);
                List<String> order = arena.server().computeOnServer(server -> {
                    Inventory inventory = arena.serverPlayer(server).getInventory();
                    return IntStream.range(9, 15)
                            .mapToObj(slot -> inventory.getItem(slot).isEmpty() ? "empty"
                                    : BuiltInRegistries.ITEM.getKey(inventory.getItem(slot).getItem()).toString())
                            .toList();
                });
                GameTestSupport.check(order.equals(List.of("minecraft:apple", "minecraft:apple", "minecraft:dirt",
                        "minecraft:stone", "minecraft:stone", "empty")), "Unexpected sorted order " + order);
            } finally {
                GameTestSupport.enable(context, "inventory_sorter", false);
            }
        }
    }

    static void itemSaver(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos block = arena.origin().offset(2, 1, 0);
            arena.setBlock(block, "stone");
            arena.hotbar(0, "diamond_pickaxe[damage=1559]");
            arena.hotbar(1, "iron_pickaxe");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            GameTestSupport.enable(context, "item_saver", true);
            try {
                context.getInput().lookAt(block);
                context.waitTicks(2);
                context.getInput().holdKeyFor(options -> options.keyAttack, 5);
                context.waitTicks(2);
                int selected = context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
                GameTestSupport.check(selected == 1, "Item Saver kept the nearly broken pickaxe selected (slot " + selected + ")");
                int damage = arena.server().computeOnServer(server ->
                        arena.serverPlayer(server).getInventory().getItem(0).getDamageValue());
                GameTestSupport.check(damage == 1559, "The protected pickaxe lost durability: " + damage);
            } finally {
                GameTestSupport.enable(context, "item_saver", false);
            }
        }
    }

    static void arrowSwap(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.hotbar(0, "bow");
            arena.hotbar(1, "arrow 16");
            arena.run("item replace entity " + arena.name()
                    + " inventory.5 with tipped_arrow[potion_contents={potion:\"minecraft:strength\"}] 4");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            context.waitTicks(5);
            GameTestSupport.enable(context, "arrow_swap", true);
            try {
                arena.server().waitFor(server -> arena.serverPlayer(server).getInventory().getItem(1)
                        .is(Items.TIPPED_ARROW), 60);
            } finally {
                GameTestSupport.enable(context, "arrow_swap", false);
            }
        }
    }

    static void avoidHazardsVoid(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            int bottom = context.computeOnClient(client -> client.level.getMinY());
            arena.run("fill " + GameTestSupport.at(new BlockPos(origin.getX() + 1, bottom, origin.getZ() - 2)) + " "
                    + GameTestSupport.at(origin.offset(4, -1, 2)) + " air");
            context.waitTicks(10);
            GameTestSupport.enable(context, "avoid_hazards", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyUp, 40);
                context.waitTicks(10);
                double y = context.computeOnClient(client -> client.player.getY());
                GameTestSupport.check(y >= origin.getY() - 0.01, "Avoid Hazards let the player walk into the void (y " + y + ")");
            } finally {
                GameTestSupport.enable(context, "avoid_hazards", false);
            }
        }
    }

    static void avoidHazardsLava(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            arena.run("fill " + GameTestSupport.at(origin.offset(2, -1, -2)) + " " + GameTestSupport.at(origin.offset(4, -1, 2))
                    + " lava");
            context.waitTicks(10);
            GameTestSupport.enable(context, "avoid_hazards", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyUp, 60);
                context.waitTicks(10);
                boolean safe = context.computeOnClient(client -> !client.player.isInLava() && !client.player.isOnFire()
                        && client.player.getX() < origin.getX() + 2.0);
                GameTestSupport.check(safe, "Avoid Hazards let the player walk into lava");
            } finally {
                GameTestSupport.enable(context, "avoid_hazards", false);
            }
        }
    }

    private static int count(final Inventory inventory, final Item item) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
