package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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

    /** Speed Mine breaks a block that vanilla mining cannot finish in the same time. */
    static void speedMine(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos block = arena.origin().offset(2, 1, 0);
            arena.setBlock(block, "deepslate");
            arena.hotbar(0, "wooden_pickaxe");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            context.getInput().lookAt(block);
            context.waitTicks(5);
            // A wooden pickaxe needs 45 ticks for deepslate.
            context.getInput().holdKeyFor(options -> options.keyAttack, 40);
            context.waitTicks(10);
            GameTestSupport.check(!arena.blockAt(block).isAir(), "Vanilla mining broke the block, so the test proves nothing");
            GameTestSupport.enable(context, "speed_mine", true);
            try {
                context.getInput().holdKeyFor(options -> options.keyAttack, 40);
                arena.server().waitFor(server -> server.overworld().getBlockState(block).isAir(), 5);
            } finally {
                GameTestSupport.enable(context, "speed_mine", false);
            }
        }
    }

    /** Fast Place places more blocks than vanilla while use is held for the same time. */
    static void fastPlace(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            arena.setBlock(origin.offset(4, 1, 0), "stone");
            arena.hotbar(0, "dirt 64");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            context.waitTicks(5);
            int vanilla = placeTowardsPlayer(context, arena);
            arena.run("fill " + GameTestSupport.at(origin.offset(1, 1, 0)) + " " + GameTestSupport.at(origin.offset(3, 1, 0)) + " air");
            context.waitTicks(10);
            GameTestSupport.enable(context, "fast_place", true);
            try {
                int fast = placeTowardsPlayer(context, arena);
                GameTestSupport.check(fast > vanilla, "Fast Place placed " + fast + " block(s), vanilla " + vanilla);
            } finally {
                GameTestSupport.enable(context, "fast_place", false);
            }
        }
    }

    /** Holds use for three ticks against the wall and counts the blocks placed between it and the player. */
    private static int placeTowardsPlayer(final ClientGameTestContext context, final GameTestSupport.Arena arena) {
        context.getInput().holdKeyFor(options -> options.keyUse, 3);
        context.waitTicks(5);
        return arena.server().computeOnServer(server -> {
            int placed = 0;
            for (int dx = 1; dx <= 3; dx++) {
                if (server.overworld().getBlockState(arena.origin().offset(dx, 1, 0)).is(Blocks.DIRT)) {
                    placed++;
                }
            }
            return placed;
        });
    }

    /** Vein Miner breaks the connected blocks of the type the player just mined. */
    static void veinMiner(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            BlockPos from = origin.offset(2, 0, -1);
            BlockPos to = origin.offset(3, 1, 0);
            arena.run("fill " + GameTestSupport.at(from) + " " + GameTestSupport.at(to) + " dirt");
            // Breaks dirt instantly, which Vein Miner needs for the extra blocks.
            arena.hotbar(0, "diamond_shovel[enchantments={efficiency:5}]");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            context.getInput().lookAt(origin.offset(2, 1, 0));
            context.waitTicks(5);
            GameTestSupport.enable(context, "vein_miner", true);
            try {
                context.getInput().pressKey(options -> options.keyAttack);
                arena.server().waitFor(server -> BlockPos.betweenClosedStream(from, to)
                        .allMatch(pos -> server.overworld().getBlockState(pos).isAir()), 20);
            } finally {
                GameTestSupport.enable(context, "vein_miner", false);
            }
        }
    }

    /** Auto Shearer shears a sheep in range. */
    static void autoShearer(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos spot = arena.origin().offset(2, 0, 0);
            arena.run("summon minecraft:sheep " + GameTestSupport.at(spot) + " {NoAI:1b}");
            arena.hotbar(4, "shears");
            context.waitTicks(10);
            GameTestSupport.enable(context, "auto_shearer", true);
            try {
                arena.server().waitFor(server -> server.overworld().getEntitiesOfClass(Sheep.class, new AABB(spot).inflate(3.0))
                        .stream().anyMatch(Sheep::isSheared), 100);
            } finally {
                GameTestSupport.enable(context, "auto_shearer", false);
            }
        }
    }

    /** Auto Breed feeds both cows, so they fall in love. */
    static void autoBreed(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos origin = arena.origin();
            arena.run("summon minecraft:cow " + GameTestSupport.at(origin.offset(2, 0, 1)) + " {NoAI:1b}");
            arena.run("summon minecraft:cow " + GameTestSupport.at(origin.offset(2, 0, -1)) + " {NoAI:1b}");
            arena.hotbar(3, "wheat 8");
            context.waitTicks(10);
            GameTestSupport.enable(context, "auto_breed", true);
            try {
                arena.server().waitFor(server -> server.overworld().getEntitiesOfClass(Cow.class, new AABB(origin).inflate(5.0))
                        .stream().filter(Animal::isInLove).count() == 2, 100);
            } finally {
                GameTestSupport.enable(context, "auto_breed", false);
            }
        }
    }

    /** Auto Farm harvests ripe wheat and plants new seeds in its place. */
    static void autoFarm(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos crop = arena.origin().offset(2, 0, 0);
            arena.setBlock(crop.below(), "farmland[moisture=7]");
            arena.setBlock(crop.below().east(), "water");
            arena.setBlock(crop, "wheat[age=7]");
            arena.hotbar(0, "wheat_seeds 8");
            context.waitTicks(5);
            GameTestSupport.enable(context, "auto_farm", true);
            try {
                arena.server().waitFor(server -> {
                    BlockState state = server.overworld().getBlockState(crop);
                    return state.is(Blocks.WHEAT) && state.getValue(BlockStateProperties.AGE_7) == 0;
                }, 100);
            } finally {
                GameTestSupport.enable(context, "auto_farm", false);
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
