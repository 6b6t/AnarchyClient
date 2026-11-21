package net.blockhost.anarchyclient.module.impl;

import java.util.OptionalInt;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AutoExtinguishModule extends Module {

    private final BooleanSetting extinguishBlocks = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("extinguish_blocks")
            .name("Blocks")
            .defaultValue(true)
            .build()));
    private final BooleanSetting extinguishSelf = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("extinguish_self")
            .name("Self")
            .defaultValue(false)
            .build()));
    private final BooleanSetting restoreSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("restore_slot")
            .name("Restore")
            .defaultValue(true)
            .build()));
    private final NumberSetting horizontalRadius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("horizontal_radius")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final NumberSetting verticalRadius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical_radius")
            .name("Vertical")
            .defaultValue(2.0)
            .min(1.0)
            .max(6.0)
            .step(1.0)
            .build()));
    private final NumberSetting maxBlocksPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_blocks_per_tick")
            .name("Max")
            .defaultValue(4.0)
            .min(1.0)
            .max(24.0)
            .step(1.0)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(4.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;

    public AutoExtinguishModule() {
        super(
                "auto_extinguish",
                "Auto Extinguish",
                ModuleCategory.PLAYER,
                "Puts out fire blocks within reach and can pour a water bucket at your feet when you are burning.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (this.extinguishSelf.value()
                && player.getRemainingFireTicks() > 0
                && !player.isInWater()
                && this.pourWaterAtFeet(client, player)) {
            return;
        }
        if (this.extinguishBlocks.value()) {
            int extinguished = this.extinguishNearbyFire(client, player);
            if (extinguished > 0) {
                this.cooldownTicks = this.delay.value().intValue();
            }
        }
    }

    /**
     * Looks down at the block under the player and pours the bucket once the server has that
     * rotation. Returns whether this tick was spent on it.
     */
    private boolean pourWaterAtFeet(final Minecraft client, final LocalPlayer player) {
        OptionalInt slot =
                InventoryActions.findHotbarSlot(player.getInventory(), stack -> stack.is(Items.WATER_BUCKET));
        BlockPos ground = player.blockPosition().below();
        if (slot.isEmpty() || client.level.getBlockState(ground).isAir()) {
            return false;
        }
        RotationManager.request(new RotationRequest(
                this.id(),
                Rotation.lookingAt(Vec3.atCenterOf(ground).add(0.0, 0.5, 0.0), player.getEyePosition()),
                InventoryActionScheduler.PRIORITY_LIFE,
                90.0F,
                2,
                2.0F,
                RotationTurnMode.STEPPED,
                false));
        RotationManager.apply(player);
        if (!RotationManager.serverLookHits(player, new AABB(ground), player.blockInteractionRange())) {
            return true;
        }
        int previous = player.getInventory().getSelectedSlot();
        InventoryActions.selectHotbarSlot(player, slot.getAsInt());
        InteractionResult result = client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            this.cooldownTicks = Math.max(5, this.delay.value().intValue());
        }
        if (this.restoreSlot.value()) {
            InventoryActions.selectHotbarSlot(player, previous);
        }
        return true;
    }

    private int extinguishNearbyFire(final Minecraft client, final LocalPlayer player) {
        BlockPos center = player.blockPosition();
        int horizontal = this.horizontalRadius.value().intValue();
        int vertical = this.verticalRadius.value().intValue();
        int max = this.maxBlocksPerTick.value().intValue();
        int count = 0;
        for (BlockPos cursor : BlockPos.betweenClosed(
                center.offset(-horizontal, -vertical, -horizontal), center.offset(horizontal, vertical, horizontal))) {
            BlockPos pos = cursor.immutable();
            if (count >= max) {
                break;
            }
            if (!client.level.isLoaded(pos)
                    || !isFireBlock(client.level.getBlockState(pos).getBlock())
                    || !CombatTargets.canReachBlock(player, pos)) {
                continue;
            }
            // Fire breaks instantly, so a real break packet puts it out on the server too.
            if (WorldInteraction.breakBlockWithHeldItem(client, this, pos, Direction.UP)
                    && !isFireBlock(client.level.getBlockState(pos).getBlock())) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected void onDisable() {
        RotationManager.clear(this.id());
        WorldInteraction.stopBreaking(Minecraft.getInstance(), this);
        this.cooldownTicks = 0;
    }

    private static boolean isFireBlock(final Block block) {
        return block == Blocks.FIRE || block == Blocks.SOUL_FIRE;
    }
}
