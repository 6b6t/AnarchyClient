package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MapBlockGetter;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.test.TestInventories;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class CombatSupportModulesTest {

    private static final BlockPos FEET = new BlockPos(0, 64, 0);

    @Test
    void crawlTrapRoofsACrawlingTargetJustAboveThem() {
        List<BlockPos> crawling = CrawlTrapModule.trapPositions(FEET, true, false);
        List<BlockPos> standing = CrawlTrapModule.trapPositions(FEET, false, true);

        assertEquals(List.of(FEET.above()), crawling);
        assertEquals(FEET.above(2), standing.getFirst());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            assertTrue(standing.contains(FEET.relative(direction)));
        }
    }

    @Test
    void crystalBlockerListsDangerousSpotsMostDamagingFirst() {
        MapBlockGetter level = new MapBlockGetter()
                .set(FEET.east().below(), Blocks.OBSIDIAN)
                .set(FEET.east(2).below(), Blocks.OBSIDIAN)
                .set(FEET.below(), Blocks.OBSIDIAN)
                .set(FEET.north().below(), Blocks.STONE);

        List<BlockPos> spots = CrystalBlockerModule.dangerousSpots(level, FEET,
                spot -> 20.0 - spot.distSqr(FEET), 15.0);

        // The player's own cell rests on obsidian too, but is never a target.
        assertEquals(List.of(FEET.east(), FEET.east(2)), spots);
    }

    @Test
    void crystalBlockerIgnoresSpotsBelowTheDamageLimitOrWithoutRoom() {
        MapBlockGetter level = new MapBlockGetter()
                .set(FEET.east().below(), Blocks.OBSIDIAN)
                .set(FEET.east().above(), Blocks.STONE)
                .set(FEET.west().below(), Blocks.BEDROCK);

        assertFalse(CrystalBlockerModule.isCrystalSpot(level, FEET.east()));
        assertTrue(CrystalBlockerModule.isCrystalSpot(level, FEET.west()));
        assertTrue(CrystalBlockerModule.dangerousSpots(level, FEET, spot -> 1.0, 2.0).isEmpty());
    }

    @Test
    void mainhandTotemTakesTotemsOnlyFromStorageSlots() {
        Inventory inventory = TestInventories.withSlots(new ItemStack(Items.TOTEM_OF_UNDYING));
        inventory.setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));

        assertEquals(OptionalInt.empty(), MainhandTotemModule.storageTotemSlot(inventory));

        inventory.setItem(20, new ItemStack(Items.TOTEM_OF_UNDYING));
        assertEquals(OptionalInt.of(20), MainhandTotemModule.storageTotemSlot(inventory));
    }

    @Test
    void arrowSwapRecognizesTippedArrowEffects() {
        ItemStack strength = new ItemStack(Items.TIPPED_ARROW);
        strength.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.STRENGTH));

        assertTrue(ArrowSwapModule.hasArrowEffect(strength, MobEffects.STRENGTH.value()));
        assertFalse(ArrowSwapModule.hasArrowEffect(strength, MobEffects.SPEED.value()));
        assertFalse(ArrowSwapModule.hasArrowEffect(new ItemStack(Items.ARROW), MobEffects.STRENGTH.value()));
    }

    @Test
    void arrowSwapFindsAmmoInVanillaPickOrder() {
        Inventory inventory = TestInventories.withSlots(new ItemStack(Items.STONE), new ItemStack(Items.ARROW));
        inventory.setItem(12, new ItemStack(Items.SPECTRAL_ARROW));

        assertEquals(OptionalInt.of(1), ArrowSwapModule.firstMatching(inventory, stack -> stack.is(Items.ARROW)
                || stack.is(Items.SPECTRAL_ARROW)));
        assertEquals(OptionalInt.of(12), ArrowSwapModule.firstMatching(inventory, stack -> stack.is(Items.SPECTRAL_ARROW)));
    }

    @Test
    void crystalAuraPredictsTheNextEntityIds() {
        assertArrayEquals(new int[] {101}, CrystalAuraModule.predictedIds(100, 1));
        assertArrayEquals(new int[] {101, 102, 103}, CrystalAuraModule.predictedIds(100, 3));
        assertArrayEquals(new int[0], CrystalAuraModule.predictedIds(100, 0));
    }

    @Test
    void illegalDisconnectSendsCoordinatesTheServerRejects() {
        var packet = IllegalDisconnect.packet();

        assertTrue(packet.hasPosition());
        assertTrue(Double.isNaN(packet.getX(0.0)));
        assertTrue(Double.isNaN(packet.getY(0.0)));
        assertTrue(Double.isNaN(packet.getZ(0.0)));
    }
}
