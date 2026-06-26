package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class ModuleImplementationUpgradeTest {

    @Test
    void noInteractModesMatchExpectedTargets() {
        assertTrue(NoInteractModule.matchesHand("Both", InteractionHand.OFF_HAND));
        assertTrue(NoInteractModule.matchesHand("Main Hand", InteractionHand.MAIN_HAND));
        assertFalse(NoInteractModule.matchesHand("Off Hand", InteractionHand.MAIN_HAND));

        assertTrue(NoInteractModule.matchesList("All", List.of(), Blocks.STONE));
        assertTrue(NoInteractModule.matchesList("Listed", List.of(Blocks.CHEST), Blocks.CHEST));
        assertFalse(NoInteractModule.matchesList("Listed", List.of(Blocks.CHEST), Blocks.STONE));
        assertTrue(NoInteractModule.matchesList("Unlisted", List.of(Blocks.CHEST), Blocks.STONE));
        assertFalse(NoInteractModule.matchesList("Unlisted", List.of(Blocks.CHEST), Blocks.CHEST));

        assertTrue(NoInteractModule.matchesInteractMode("Both", "Hit"));
        assertTrue(NoInteractModule.matchesInteractMode("Interact", "Interact"));
        assertFalse(NoInteractModule.matchesInteractMode("None", "Hit"));
    }

    @Test
    void fastUseFiltersSomeModeByItemAndBlock() {
        assertTrue(FastUseModule.shouldApplyItem(Items.BOW, "All", List.of(), false));
        assertTrue(FastUseModule.shouldApplyItem(Items.EXPERIENCE_BOTTLE, "Some",
                List.of(Items.EXPERIENCE_BOTTLE), false));
        assertTrue(FastUseModule.shouldApplyItem(Items.STONE, "Some", List.of(), true));
        assertFalse(FastUseModule.shouldApplyItem(Items.BOW, "Some", List.of(), false));
        assertFalse(FastUseModule.shouldApplyItem(null, "All", List.of(), true));
    }

    @Test
    void serverSpoofChannelMatchingIsCaseInsensitive() {
        assertTrue(ServerSpoofModule.matchesChannel("minecraft:register", List.of("REGISTER")));
        assertTrue(ServerSpoofModule.matchesChannel("fabric:registry/sync", List.of("fabric")));
        assertFalse(ServerSpoofModule.matchesChannel("minecraft:brand", List.of("fabric")));
        assertFalse(ServerSpoofModule.matchesChannel("minecraft:brand", List.of(" ")));
    }

    @Test
    void airPlaceClassifiesCommonPlaceableItems() {
        assertTrue(AirPlaceModule.matchesHand("Both", InteractionHand.OFF_HAND));
        assertTrue(AirPlaceModule.placeableItem(Items.STONE));
        assertTrue(AirPlaceModule.placeableItem(Items.ARMOR_STAND));
        assertTrue(AirPlaceModule.placeableItem(Items.CREEPER_SPAWN_EGG));
        assertTrue(AirPlaceModule.placeableItem(Items.FIREWORK_ROCKET));
        assertFalse(AirPlaceModule.placeableItem(Items.DIAMOND_SWORD));
    }

    @Test
    void noSlowDefaultStuckBlockFiltersAreScopedToEnabledModule() {
        NoSlowModule module = new NoSlowModule();
        assertFalse(NoSlowModule.shouldIgnoreStuckBlock(Blocks.HONEY_BLOCK.defaultBlockState()));

        module.enabled(true);
        assertTrue(NoSlowModule.shouldIgnoreStuckBlock(Blocks.HONEY_BLOCK.defaultBlockState()));
        assertFalse(NoSlowModule.shouldIgnoreStuckBlock(Blocks.COBWEB.defaultBlockState()));

        module.enabled(false);
        assertFalse(NoSlowModule.shouldIgnoreStuckBlock(Blocks.HONEY_BLOCK.defaultBlockState()));
    }
}
