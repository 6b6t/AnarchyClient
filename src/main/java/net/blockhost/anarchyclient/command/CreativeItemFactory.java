package net.blockhost.anarchyclient.command;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TypedEntityData;

import java.util.List;

public final class CreativeItemFactory {

    private static final List<Item> RANDOM_ITEMS = List.of(
            Items.COMMAND_BLOCK,
            Items.REPEATING_COMMAND_BLOCK,
            Items.CHAIN_COMMAND_BLOCK,
            Items.STRUCTURE_VOID,
            Items.PLAYER_HEAD,
            Items.ARMOR_STAND,
            Items.WITHER_SPAWN_EGG,
            Items.ENDER_DRAGON_SPAWN_EGG,
            Items.BARRIER,
            Items.DEBUG_STICK
    );

    private CreativeItemFactory() {
    }

    public static ItemStack playerHead(final String playerName) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        String name = sanitizeName(playerName, "Player");
        stack.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(name));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name + "'s Head"));
        return stack;
    }

    public static ItemStack hologramArmorStand(final String text) {
        ItemStack stack = named(new ItemStack(Items.ARMOR_STAND), text);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Invisible", true);
        tag.putBoolean("NoGravity", true);
        tag.putBoolean("Marker", true);
        tag.putBoolean("CustomNameVisible", true);
        stack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(EntityTypes.ARMOR_STAND, tag));
        return stack;
    }

    public static ItemStack bossbarEgg(final String text) {
        ItemStack stack = named(new ItemStack(Items.WITHER_SPAWN_EGG), text);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("NoAI", true);
        tag.putBoolean("Silent", true);
        tag.putBoolean("Invulnerable", true);
        tag.putBoolean("CustomNameVisible", true);
        stack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(EntityTypes.WITHER, tag));
        return stack;
    }

    public static ItemStack randomItem(final int index) {
        Item item = RANDOM_ITEMS.get(Math.floorMod(index, RANDOM_ITEMS.size()));
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Anarchy Item " + Math.floorMod(index, 10_000)));
        if (item == Items.PLAYER_HEAD) {
            stack.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved("AnarchyClient"));
        }
        return stack;
    }

    public static ItemStack named(final ItemStack stack, final String text) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(sanitizeName(text, "AnarchyClient")));
        return stack;
    }

    static String sanitizeName(final String value, final String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.length() > 128 ? trimmed.substring(0, 128) : trimmed;
    }
}
