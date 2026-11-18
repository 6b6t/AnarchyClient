package net.blockhost.anarchyclient.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.blockhost.anarchyclient.i18n.Translations;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static net.blockhost.anarchyclient.command.CommandSupport.SUCCESS;
import static net.blockhost.anarchyclient.command.CommandSupport.requireCreative;
import static net.blockhost.anarchyclient.command.CommandSupport.requireInGame;
import static net.blockhost.anarchyclient.command.CommandSupport.suggestOnlinePlayers;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Registers and handles the {@code give}, {@code heads}, {@code drop}, {@code inventory}, {@code peek}, and {@code nbt} subcommands.
 */
final class ItemCommands {

    private ItemCommands() {
    }

    static void register(final LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root
                .then(literal("give")
                        .then(literal("head")
                                .then(argument("player", StringArgumentType.word())
                                        .suggests(CommandSupport::suggestOnlinePlayers)
                                        .executes(ItemCommands::giveHead)))
                        .then(literal("hologram")
                                .then(argument("text", StringArgumentType.greedyString())
                                        .executes(ItemCommands::giveHologram)))
                        .then(literal("bossbar")
                                .then(argument("text", StringArgumentType.greedyString())
                                        .executes(ItemCommands::giveBossbar)))
                        .then(literal("random")
                                .executes(context -> giveRandom(context, 1))
                                .then(argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> giveRandom(context, IntegerArgumentType.getInteger(context, "count"))))))
                .then(literal("heads")
                        .then(argument("player", StringArgumentType.word())
                                .suggests(CommandSupport::suggestOnlinePlayers)
                                .executes(ItemCommands::giveHead)))
                .then(literal("drop")
                        .executes(context -> dropSelected(context, true))
                        .then(literal("hand")
                                .executes(context -> dropSelected(context, true)))
                        .then(literal("one")
                                .executes(context -> dropSelected(context, false))))
                .then(literal("inventory")
                        .executes(ItemCommands::inventorySummary)
                        .then(literal("hotbar")
                                .executes(context -> inventorySummary(context, InventoryView.HOTBAR)))
                        .then(literal("armor")
                                .executes(context -> inventorySummary(context, InventoryView.ARMOR))))
                .then(literal("peek")
                        .executes(ItemCommands::peekHeldItem))
                .then(literal("nbt")
                        .executes(ItemCommands::heldItemNbt));
    }

    private static int giveHead(final CommandContext<FabricClientCommandSource> context) {
        return giveCreativeStack(context, CreativeItemFactory.playerHead(StringArgumentType.getString(context, "player")));
    }

    private static int giveHologram(final CommandContext<FabricClientCommandSource> context) {
        return giveCreativeStack(context, CreativeItemFactory.hologramArmorStand(StringArgumentType.getString(context, "text")));
    }

    private static int giveBossbar(final CommandContext<FabricClientCommandSource> context) {
        return giveCreativeStack(context, CreativeItemFactory.bossbarEgg(StringArgumentType.getString(context, "text")));
    }

    private static int giveRandom(final CommandContext<FabricClientCommandSource> context, final int count) {
        Minecraft client = requireCreative(context);
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        int given = 0;
        for (int index = 0; index < count; index++) {
            // Each item goes to its own slot; once the inventory is full, stop instead of overwriting.
            int slot = index == 0 ? targetInventorySlot(client.player.getInventory()) : client.player.getInventory().getFreeSlot();
            if (slot < 0) {
                break;
            }
            giveToSlot(client, slot, CreativeItemFactory.randomItem(index));
            given++;
        }
        context.getSource().sendFeedback(Translations.component("anarchyclient.message.item.gave_random_item", "Gave %s random item%s.", given, (given == 1 ? "" : "s")));
        return SUCCESS;
    }

    private static int giveCreativeStack(final CommandContext<FabricClientCommandSource> context, final ItemStack stack) {
        Minecraft client = requireCreative(context);
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        giveToSlot(client, targetInventorySlot(client.player.getInventory()), stack);
        context.getSource().sendFeedback(Translations.component("anarchyclient.message.item.gave_creative_item", "Gave creative item: ")
                .append(stack.getHoverName()));
        return SUCCESS;
    }

    /**
     * Picks the selected hotbar slot when it is empty, then the first free slot, and falls back to
     * replacing the selected item when the inventory is full.
     */
    static int targetInventorySlot(final Inventory inventory) {
        int selected = inventory.getSelectedSlot();
        if (inventory.getItem(selected).isEmpty()) {
            return selected;
        }
        int free = inventory.getFreeSlot();
        return free >= 0 ? free : selected;
    }

    /** Converts an inventory index to the inventory menu slot that the creative slot packet expects. */
    static int creativeMenuSlot(final int inventorySlot) {
        return Inventory.isHotbarSlot(inventorySlot) ? InventoryMenu.USE_ROW_SLOT_START + inventorySlot : inventorySlot;
    }

    /** Updates the client inventory too, so the item shows up without waiting for a server resync. */
    private static void giveToSlot(final Minecraft client, final int inventorySlot, final ItemStack stack) {
        client.player.getInventory().setItem(inventorySlot, stack.copy());
        client.getConnection().send(new ServerboundSetCreativeModeSlotPacket(creativeMenuSlot(inventorySlot), stack));
    }

    private static int dropSelected(final CommandContext<FabricClientCommandSource> context, final boolean all) {
        Minecraft client = requireInGame(context, "drop the selected item");
        if (client == null) {
            return 0;
        }
        if (client.gameMode != null && !client.player.getMainHandItem().isEmpty()) {
            client.gameMode.dropItem(client.player, all);
            context.getSource().sendFeedback(Translations.component("anarchyclient.message.item.dropped_selected", "Dropped selected %s.", (all ? "stack" : "item")));
            return SUCCESS;
        }
        context.getSource().sendError(Translations.component("anarchyclient.message.item.no_selected_item_to_drop", "No selected item to drop."));
        return 0;
    }

    private static int inventorySummary(final CommandContext<FabricClientCommandSource> context) {
        return inventorySummary(context, InventoryView.SELECTED);
    }

    private static int inventorySummary(final CommandContext<FabricClientCommandSource> context, final InventoryView view) {
        Minecraft client = requireInGame(context, "inspect inventory");
        if (client == null) {
            return 0;
        }
        List<String> lines = switch (view) {
            case SELECTED -> List.of("Selected: " + stackSummary(client.player.getInventory().getSelectedItem()));
            case HOTBAR -> hotbarSummary(client.player);
            case ARMOR -> armorSummary(client.player);
        };
        for (String line : lines) {
            context.getSource().sendFeedback(Component.literal(line));
        }
        return SUCCESS;
    }

    private static int peekHeldItem(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context, "peek at the held item");
        if (client == null) {
            return 0;
        }
        ItemStack stack = client.player.getMainHandItem();
        if (stack.isEmpty()) {
            context.getSource().sendError(Translations.component("anarchyclient.message.item.you_are_not_holding_an_item", "You are not holding an item."));
            return 0;
        }
        context.getSource().sendFeedback(Translations.component("anarchyclient.message.item.components", "%s | components: %s", stackSummary(stack), stack.getComponents().size()));
        return SUCCESS;
    }

    private static int heldItemNbt(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context, "inspect held item data");
        if (client == null) {
            return 0;
        }
        ItemStack stack = client.player.getMainHandItem();
        if (stack.isEmpty()) {
            context.getSource().sendError(Translations.component("anarchyclient.message.item.you_are_not_holding_an_item", "You are not holding an item."));
            return 0;
        }
        String data = stack.getComponents().toString();
        context.getSource().sendFeedback(Translations.component("anarchyclient.message.item.held_item_components", "Held item components: ")
                .append(CommandFeedback.copyable("[copy]", data)));
        return SUCCESS;
    }

    private static List<String> hotbarSummary(final Player player) {
        List<String> lines = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            lines.add((slot + 1) + ": " + stackSummary(player.getInventory().getItem(slot)));
        }
        return List.copyOf(lines);
    }

    private static List<String> armorSummary(final Player player) {
        return List.of(
                "Head: " + stackSummary(player.getItemBySlot(EquipmentSlot.HEAD)),
                "Chest: " + stackSummary(player.getItemBySlot(EquipmentSlot.CHEST)),
                "Legs: " + stackSummary(player.getItemBySlot(EquipmentSlot.LEGS)),
                "Feet: " + stackSummary(player.getItemBySlot(EquipmentSlot.FEET)),
                "Offhand: " + stackSummary(player.getItemBySlot(EquipmentSlot.OFFHAND))
        );
    }

    private static String stackSummary(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return stack.getCount() + "x " + stack.getHoverName().getString();
    }

    private enum InventoryView {
        SELECTED,
        HOTBAR,
        ARMOR
    }
}
