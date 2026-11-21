package net.blockhost.anarchyclient.module.impl;

import java.util.Arrays;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Smart Eat's rules: when to eat, which food is best, and where in the inventory to find it. */
final class FoodSelection {

    private FoodSelection() {}

    /** Whether vanilla lets the player start eating {@code stack}, given whether they are hungry at all. */
    static boolean canEatNow(final boolean hungry, final ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food != null && (hungry || food.canAlwaysEat());
    }

    static boolean shouldEat(
            final int hunger, final float health, final double hungerThreshold, final double healthThreshold) {
        return shouldEat(hunger, health, hungerThreshold, healthThreshold, ThresholdMode.ANY);
    }

    static boolean shouldEat(
            final int hunger,
            final float health,
            final double hungerThreshold,
            final double healthThreshold,
            final ThresholdMode mode) {
        boolean hungerLow = hunger <= hungerThreshold;
        boolean healthLow = health <= healthThreshold;
        return mode.test(healthLow, hungerLow);
    }

    static OptionalInt bestGappleSlot(final Inventory inventory, final boolean preferEnchanted) {
        return bestGappleSlot(inventory, 0, Inventory.getSelectionSize() - 1, preferEnchanted);
    }

    static OptionalInt bestGappleSlot(
            final Inventory inventory, final int start, final int end, final boolean preferEnchanted) {
        OptionalInt enchanted = findSlot(inventory, start, end, stack -> stack.is(Items.ENCHANTED_GOLDEN_APPLE));
        OptionalInt golden = findSlot(inventory, start, end, stack -> stack.is(Items.GOLDEN_APPLE));
        if (preferEnchanted && enchanted.isPresent()) {
            return enchanted;
        }
        return golden.isPresent() ? golden : enchanted;
    }

    static OptionalInt findSoupSlot(final Inventory inventory, final int start, final int end) {
        return findSlot(inventory, start, end, FoodSelection::isSoup);
    }

    static InteractionHand soupHand(final ItemStack mainHand, final ItemStack offHand) {
        return consumableHand(mainHand, offHand, FoodSelection::isSoup);
    }

    static boolean isSoup(final ItemStack stack) {
        return !stack.isEmpty()
                && isSoupItemId(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
    }

    static boolean isSoupItemId(final String itemId) {
        return "mushroom_stew".equals(itemId)
                || "rabbit_stew".equals(itemId)
                || "beetroot_soup".equals(itemId)
                || "suspicious_stew".equals(itemId);
    }

    static boolean isClickable(final HitResult hitResult, final Level level) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            return entity instanceof Villager || entity instanceof TamableAnimal;
        }
        if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHitResult.getBlockPos();
            Block block = level.getBlockState(pos).getBlock();
            return block instanceof BaseEntityBlock || block instanceof CraftingTableBlock;
        }
        return false;
    }

    static double foodScore(final ItemStack stack, final FoodPriority priority) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) {
            return 0.0;
        }
        return foodScore(food, priority);
    }

    static double foodScore(final FoodProperties food, final FoodPriority priority) {
        return switch (priority) {
            case HUNGER -> food.nutrition();
            case SATURATION -> food.saturation();
            case COMBINED -> food.nutrition() + food.saturation();
        };
    }

    static Set<String> parseBlacklist(final String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split("[,;|\\s]+"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> token.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    static boolean isBlacklisted(final ItemStack stack, final Set<String> blacklist) {
        return isBlacklisted(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), blacklist);
    }

    static boolean isBlacklisted(final String itemId, final Set<String> blacklist) {
        String id = itemId.toLowerCase(Locale.ROOT);
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return blacklist.contains(path) || blacklist.contains(id);
    }

    static OptionalInt findBestFoodSlot(
            final Inventory inventory,
            final int start,
            final int end,
            final Predicate<ItemStack> predicate,
            final FoodPriority priority) {
        int bestSlot = -1;
        double bestScore = -1;
        for (int slot = start; slot <= end; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!predicate.test(stack)) {
                continue;
            }
            double score = foodScore(stack, priority);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot < 0 ? OptionalInt.empty() : OptionalInt.of(bestSlot);
    }

    static OptionalInt findSlot(
            final Inventory inventory, final int start, final int end, final Predicate<ItemStack> predicate) {
        for (int slot = start; slot <= end; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    static OptionalInt findEmptyHotbarSlot(final Inventory inventory) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    static InteractionHand consumableHand(
            final ItemStack mainHand, final ItemStack offHand, final Predicate<ItemStack> predicate) {
        if (predicate.test(mainHand)) {
            return InteractionHand.MAIN_HAND;
        }
        if (predicate.test(offHand)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    static boolean isFood(final ItemStack stack, final boolean allowGoldenApples, final Set<String> blacklist) {
        if (!stack.has(DataComponents.FOOD)) {
            return false;
        }
        if (!allowGoldenApples && isGapple(stack)) {
            return false;
        }
        return !isBlacklisted(stack, blacklist);
    }

    static boolean isGapple(final ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    enum ThresholdMode {
        HEALTH,
        HUNGER,
        ANY,
        BOTH;

        boolean test(final boolean healthLow, final boolean hungerLow) {
            return switch (this) {
                case HEALTH -> healthLow;
                case HUNGER -> hungerLow;
                case BOTH -> healthLow && hungerLow;
                case ANY -> healthLow || hungerLow;
            };
        }

        static ThresholdMode fromSetting(final String value) {
            return switch (value) {
                case "Health" -> HEALTH;
                case "Hunger" -> HUNGER;
                case "Both" -> BOTH;
                default -> ANY;
            };
        }
    }

    enum FoodPriority {
        SATURATION,
        HUNGER,
        COMBINED;

        static FoodPriority fromSetting(final String value) {
            return switch (value) {
                case "Hunger" -> HUNGER;
                case "Combined" -> COMBINED;
                default -> SATURATION;
            };
        }
    }
}
