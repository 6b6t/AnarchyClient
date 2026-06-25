package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class BedAuraModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));

    public BedAuraModule() {
        super("bed_aura", "Bed Aura", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null || client.level.dimension() == Level.OVERWORLD) {
            return;
        }
        net.minecraft.world.entity.player.Player target = CombatTargets.nearestEnemy(client, this.range.value());
        if (target == null) {
            return;
        }
        BlockPos bed = findBed(client, target.blockPosition(), this.range.value().intValue());
        if (bed != null) {
            client.gameMode.useItemOn(client.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(bed), Direction.UP, bed, false));
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos ground = target.blockPosition().relative(direction).below();
            if (client.level.isEmptyBlock(ground.above()) && client.level.isEmptyBlock(ground.above().relative(direction))) {
                WorldInteraction.useOnBlock(client, this, ground, Direction.UP,
                        stack -> stack.getItem() instanceof BedItem, true);
                return;
            }
        }
    }

    static BlockPos findBed(final Minecraft client, final BlockPos center, final int radius) {
        if (client.level == null) {
            return null;
        }
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - 1; y <= center.getY() + 2; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.level.getBlockState(pos).getBlock() instanceof BedBlock) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
}
