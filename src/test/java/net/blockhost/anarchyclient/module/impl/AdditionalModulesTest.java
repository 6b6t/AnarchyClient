package net.blockhost.anarchyclient.module.impl;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdditionalModulesTest {

    @Test
    void emptyItemParserInputDoesNotTouchRegistry() {
        assertTrue(ItemScan.parseItems("  ").isEmpty());
    }

    @Test
    void parsesAutoLoginCommandMap() {
        assertEquals(Map.of(
                        "example.org", "/login hunter2",
                        "*", "/register hunter2 hunter2"
                ),
                AutoLoginModule.parseCommands("example.org=/login hunter2; *=/register hunter2 hunter2"));
    }

    @Test
    void classifiesOversizedVelocityPacketsAsSuspicious() {
        assertEquals(
                AntiCrashModule.SuspiciousPacket.ENTITY_MOTION,
                AntiCrashModule.suspiciousPacket(
                        new ClientboundSetEntityMotionPacket(1, new Vec3(0, 2000, 0)),
                        30_000_000,
                        1000,
                        100_000,
                        128,
                        4096
                )
        );
    }

    @Test
    void acceptsFiniteVectorsInsideLimit() {
        assertFalse(AntiCrashModule.outsideLimit(new Vec3(10, 20, -30), 100));
        assertTrue(AntiCrashModule.outsideLimit(new Vec3(Double.NaN, 20, -30), 100));
        assertTrue(AntiCrashModule.outsideLimit(new Vec3(10, 101, -30), 100));
    }

    @Test
    void nullFluidStatesAreNotFlowingFluidSignals() {
        assertFalse(NewChunksModule.isFlowingFluid(null));
    }

    @Test
    void hitChanceCanForceAttacks() {
        assertTrue(KillAuraModule.shouldAttack(new java.util.Random(0), 100));
    }

    @Test
    void antiSpawnpointCancelsOnlySpawnChangingUses() {
        assertTrue(AntiSpawnpointModule.shouldCancelUse(true, false, true, false));
        assertTrue(AntiSpawnpointModule.shouldCancelUse(false, true, false, true));
        assertFalse(AntiSpawnpointModule.shouldCancelUse(false, true, true, false));
        assertFalse(AntiSpawnpointModule.shouldCancelUse(true, false, false, true));
    }

    @Test
    void coordLoggerFormatsKnownEventsAndPositions() {
        assertEquals("Wither spawned", CoordLoggerModule.worldEventName(1023));
        assertEquals("End portal opened", CoordLoggerModule.worldEventName(1038));
        assertEquals("Global event 9", CoordLoggerModule.worldEventName(9));
        assertEquals("1.0, 65.0, -3.0", CoordLoggerModule.formatPosition(new BlockPos(1, 65, -3)));
    }

    @Test
    void autoSoupRecognizesSoupStacksAndHands() {
        assertTrue(AutoSoupModule.isSoupItemId("mushroom_stew"));
        assertTrue(AutoSoupModule.isSoupItemId("rabbit_stew"));
        assertTrue(AutoSoupModule.isSoupItemId("beetroot_soup"));
        assertFalse(AutoSoupModule.isSoupItemId("bowl"));
    }

    @Test
    void blockInTargetsExpectedShellAroundPlayer() {
        BlockPos base = new BlockPos(10, 64, -5);
        assertIterableEquals(List.of(
                new BlockPos(10, 63, -5),
                new BlockPos(11, 64, -5),
                new BlockPos(9, 64, -5),
                new BlockPos(10, 64, -4),
                new BlockPos(10, 64, -6),
                new BlockPos(11, 65, -5),
                new BlockPos(9, 65, -5),
                new BlockPos(10, 65, -4),
                new BlockPos(10, 65, -6),
                new BlockPos(10, 66, -5)
        ), BlockInModule.targetPositions(base));
    }

    @Test
    void boostUsesHorizontalLookDirectionOnly() {
        assertEquals(new Vec3(0.0, 0.0, 2.0), BoostModule.boostVector(new Vec3(0.0, -0.8, 1.0), 2.0));
        assertEquals(Vec3.ZERO, BoostModule.boostVector(new Vec3(0.0, 1.0, 0.0), 2.0));
    }

    @Test
    void glideCapsOnlyExcessiveDownwardVelocity() {
        assertEquals(-0.125, GlideModule.cappedFallVelocity(-0.5, 0.125));
        assertEquals(-0.05, GlideModule.cappedFallVelocity(-0.05, 0.125));
    }

    @Test
    void antiVanishIgnoresNormalLeaveMessages() {
        UUID vanished = UUID.randomUUID();
        Map<UUID, String> previous = new HashMap<>();
        previous.put(vanished, "AdminName");

        assertTrue(AntiVanishModule.vanishedPlayers(previous, Map.of(), List.of("AdminName left the game"), true).isEmpty());
        assertEquals(List.of("AdminName"), AntiVanishModule.vanishedPlayers(previous, Map.of(), List.of(), true));
    }

    @Test
    void autoPotUsesHealingOnlyAtOrBelowThreshold() {
        assertTrue(AutoPotModule.shouldUseHealing(10.0F, 10.0));
        assertTrue(AutoPotModule.shouldUseHealing(9.5F, 10.0));
        assertFalse(AutoPotModule.shouldUseHealing(10.5F, 10.0));
    }

    @Test
    void inputStatesComputeUpdatedMoveVectors() {
        Input forwardRight = new Input(true, false, false, true, false, false, false);
        Vec2 vector = InputStates.moveVector(forwardRight);

        assertEquals(-0.70710677F, vector.x, 1.0E-6F);
        assertEquals(0.70710677F, vector.y, 1.0E-6F);
        Vec2 stopped = InputStates.moveVector(new Input(true, true, false, false, false, false, false));
        assertEquals(0.0F, stopped.x, 1.0E-6F);
        assertEquals(0.0F, stopped.y, 1.0E-6F);
    }

    @Test
    void autoJumpRequiresGroundMovementAndDryBlocksWhenConfigured() {
        assertTrue(AutoJumpModule.shouldJump(true, true, false, true, true));
        assertTrue(AutoJumpModule.shouldJump(true, false, false, false, true));
        assertFalse(AutoJumpModule.shouldJump(false, true, false, true, true));
        assertFalse(AutoJumpModule.shouldJump(true, false, false, true, true));
        assertFalse(AutoJumpModule.shouldJump(true, true, true, true, true));
    }

    @Test
    void soundBlockerMatchesListedSoundsAndSafeEmptyLists() {
        Identifier step = Identifier.withDefaultNamespace("block.stone.step");
        Identifier splash = Identifier.withDefaultNamespace("entity.generic.splash");

        assertTrue(SoundBlockerModule.shouldBlock(step, java.util.Set.of(step), false));
        assertFalse(SoundBlockerModule.shouldBlock(splash, java.util.Set.of(step), false));
        assertFalse(SoundBlockerModule.shouldBlock(step, java.util.Set.of(step), true));
        assertTrue(SoundBlockerModule.shouldBlock(splash, java.util.Set.of(step), true));
        assertFalse(SoundBlockerModule.shouldBlock(step, java.util.Set.of(), true));
    }

    @Test
    void expThrowerScoresDurabilityPercent() {
        assertEquals(50.0, EXPThrowerModule.durabilityPercent(400, 200));
        assertEquals(100.0, EXPThrowerModule.durabilityPercent(400, 0));
        assertEquals(100.0, EXPThrowerModule.durabilityPercent(0, 10));
    }

    @Test
    void packetLoggerFiltersCaseInsensitively() {
        assertTrue(PacketLoggerModule.matchesFilter("clientbound/minecraft:sound", "SOUND"));
        assertTrue(PacketLoggerModule.matchesFilter("ClientboundSoundPacket", ""));
        assertFalse(PacketLoggerModule.matchesFilter("ClientboundSoundPacket", "chat"));
    }

    @Test
    void breadcrumbsAgeTrailPointsImmutably() {
        BreadcrumbsModule.TrailPoint point = new BreadcrumbsModule.TrailPoint(new Vec3(1.0, 2.0, 3.0), 4);
        BreadcrumbsModule.TrailPoint ticked = BreadcrumbsModule.tick(point);

        assertEquals(new Vec3(1.0, 2.0, 3.0), ticked.position());
        assertEquals(5, ticked.age());
        assertEquals(4, point.age());
    }

    @Test
    void velocityScalesHorizontalAndVerticalMotionIndependently() {
        Vec3 scaled = VelocityModule.scaleMotion(new Vec3(2.0, 4.0, -4.0), 50.0, 25.0);
        assertEquals(1.0, scaled.x, 1.0E-9);
        assertEquals(1.0, scaled.y, 1.0E-9);
        assertEquals(-2.0, scaled.z, 1.0E-9);

        Vec3 stopped = VelocityModule.scaleMotion(new Vec3(2.0, 4.0, -4.0), 0.0, 0.0);
        assertEquals(0.0, stopped.x, 1.0E-9);
        assertEquals(0.0, stopped.y, 1.0E-9);
        assertEquals(0.0, stopped.z, 1.0E-9);
    }

    @Test
    void packetGuardModulesClassifyTheirPackets() {
        assertTrue(NoRotateSetModule.shouldCancel(new ClientboundPlayerRotationPacket(90.0F, false, 30.0F, false), false));
        assertTrue(NoSlotSetModule.shouldCancel(new ClientboundSetHeldSlotPacket(2)));
        assertTrue(NoSwingModule.shouldCancel(new ServerboundSwingPacket(InteractionHand.MAIN_HAND), true));
        assertFalse(NoSwingModule.shouldCancel(new ServerboundSwingPacket(InteractionHand.MAIN_HAND), false));
    }

    @Test
    void autoClickerDelayUsesFasterConfiguredRate() {
        assertEquals(2, AutoClickerModule.delayTicks(8.0, 12.0));
        assertEquals(20, AutoClickerModule.delayTicks(1.0, 1.0));
    }

    @Test
    void chestStealerIgnoresEmptySlots() {
        List<ItemStack> stacks = List.of(
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY
        );

        assertEquals(-1, ChestStealerModule.firstStealableSlot(stacks, 3, true, Set.of()));
    }

    @Test
    void inventoryCleanerSkipsEmptyAndSelectedSlots() {
        List<ItemStack> stacks = List.of(
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY
        );
        InventoryCleanerModule.CleanerOptions options = new InventoryCleanerModule.CleanerOptions(
                true,
                Set.of(),
                true,
                true,
                true,
                true,
                true,
                true,
                true
        );

        assertEquals(-1, InventoryCleanerModule.findDropSlot(stacks, 1, options));
    }

    @Test
    void tntTimerFormatsFuseSeconds() {
        assertEquals("4.0s", TntTimerModule.formatFuse(80));
        assertEquals("0.0s", TntTimerModule.formatFuse(-5));
    }

    @Test
    void zoomFovUsesConfiguredFactorWithFloor() {
        assertEquals(30, ZoomModule.zoomedFov(90, 3.0));
        assertEquals(10, ZoomModule.zoomedFov(30, 10.0));
    }

    @Test
    void autoDisableParsesModuleIds() {
        assertIterableEquals(List.of("kill_aura", "velocity", "zoom"),
                AutoDisableModule.parseModuleIds("kill_aura, velocity; zoom").stream().toList());
    }

    @Test
    void flagCheckComputesWrappedAngleDeltas() {
        assertEquals(20.0F, FlagCheckModule.angleDelta(-170.0F, 170.0F), 1.0E-6F);
        assertEquals(45.0F, FlagCheckModule.angleDelta(45.0F, 90.0F), 1.0E-6F);
    }

    @Test
    void staffAlertParsesNamesCaseInsensitively() {
        assertEquals(Set.of("admin", "moderator", "helper"),
                StaffAlertModule.parseNames("Admin, Moderator; HELPER"));
    }

    @Test
    void holeEspRequiresTwoAirBlocksAndSafeShell() {
        assertTrue(HoleEspModule.isHole(true, true, true, true, true, true, true));
        assertFalse(HoleEspModule.isHole(false, true, true, true, true, true, true));
        assertFalse(HoleEspModule.isHole(true, true, true, true, false, true, true));
    }

    @Test
    void blockTargetScannerSortsByDistance() {
        List<BlockTargetScanner.BlockTarget> targets = new java.util.ArrayList<>(List.of(
                new BlockTargetScanner.BlockTarget(new BlockPos(0, 0, 3), null, 9.0),
                new BlockTargetScanner.BlockTarget(new BlockPos(0, 0, 1), null, 1.0),
                new BlockTargetScanner.BlockTarget(new BlockPos(0, 0, 2), null, 4.0)
        ));

        BlockTargetScanner.sort(targets, BlockTargetScanner.SortMode.CLOSEST);

        assertIterableEquals(List.of(
                new BlockPos(0, 0, 1),
                new BlockPos(0, 0, 2),
                new BlockPos(0, 0, 3)
        ), targets.stream().map(BlockTargetScanner.BlockTarget::pos).toList());
    }

    @Test
    void colorSignsConvertsAmpersandFormattingCodes() {
        assertEquals("\u00a7aGreen \u00a7lBold", ColorSignsModule.colorize("&aGreen &lBold"));
        assertEquals("plain text", ColorSignsModule.colorize("plain text"));
    }

    @Test
    void shieldBypassPositionsBehindTargetLookDirection() {
        Vec3 position = ShieldBypassModule.behindPosition(new Vec3(10.0, 64.0, 5.0), new Vec3(0.0, 0.0, 1.0),
                70.0, 0.6);

        assertEquals(new Vec3(10.0, 70.0, 4.4), position);
    }

    @Test
    void autoLogUsesEffectiveHealthAndTotemPopThresholds() {
        assertTrue(AutoLogModule.shouldDisconnectForHealth(9.0F, 2.0F, 11.0, true));
        assertFalse(AutoLogModule.shouldDisconnectForHealth(9.0F, 2.0F, 8.0, false));
        assertTrue(AutoLogModule.shouldDisconnectForTotemPops(2, 2));
        assertFalse(AutoLogModule.shouldDisconnectForTotemPops(2, 0));
    }

    @Test
    void notifierFormatsTotemPopCounts() {
        assertEquals("Steve popped a totem.", NotifierModule.totemPopMessage("Steve", 1));
        assertEquals("Steve popped a totem (x3).", NotifierModule.totemPopMessage("Steve", 3));
    }

    @Test
    void logoutSpotsFormatLabelsAndExpireByAge() {
        assertEquals("Steve 17.5 HP", LogoutSpotsModule.formatLabel("Steve", 17.5F));
        assertFalse(LogoutSpotsModule.expired(20, 20));
        assertTrue(LogoutSpotsModule.expired(21, 20));
    }

    @Test
    void lightOverlayRequiresSpawnableLowLightBlocks() {
        assertTrue(LightOverlayModule.shouldRenderSpawnMarker(true, true, true, 0, 0));
        assertTrue(LightOverlayModule.shouldRenderSpawnMarker(true, true, true, 4, 7));
        assertFalse(LightOverlayModule.shouldRenderSpawnMarker(false, true, true, 0, 0));
        assertFalse(LightOverlayModule.shouldRenderSpawnMarker(true, true, false, 0, 0));
        assertFalse(LightOverlayModule.shouldRenderSpawnMarker(true, true, true, 8, 7));
    }

    @Test
    void chestSwapTargetsOppositeChestSlotState() {
        assertEquals(ChestSwapModule.SwapTarget.CHESTPLATE, ChestSwapModule.swapTarget(true));
        assertEquals(ChestSwapModule.SwapTarget.GLIDER, ChestSwapModule.swapTarget(false));
    }

    @Test
    void autoMendUsesDurabilityAndTotemSafetyThresholds() {
        assertTrue(AutoMendModule.needsMending(400, 200, 75.0));
        assertFalse(AutoMendModule.needsMending(400, 40, 75.0));
        assertTrue(AutoMendModule.protectedTotem(8.0F, 2.0F, true, 12.0));
        assertFalse(AutoMendModule.protectedTotem(8.0F, 6.0F, true, 12.0));
        assertFalse(AutoMendModule.protectedTotem(8.0F, 2.0F, false, 12.0));
    }
}
