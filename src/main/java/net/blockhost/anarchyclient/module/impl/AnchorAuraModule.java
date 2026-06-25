package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class AnchorAuraModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));

    public AnchorAuraModule() {
        super("anchor_aura", "Anchor Aura", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        net.minecraft.world.entity.player.Player target = CombatTargets.nearestEnemy(client, this.range.value());
        if (target == null) {
            return;
        }
        BlockPos anchor = findAnchor(client, target.blockPosition(), this.range.value().intValue());
        if (anchor != null) {
            if (client.level.dimension() != Level.NETHER) {
                client.gameMode.useItemOn(client.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(anchor), Direction.UP, anchor, false));
            } else {
                WorldInteraction.useOnBlock(client, this, anchor, Direction.UP, stack -> stack.is(Items.GLOWSTONE), true);
            }
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = target.blockPosition().relative(direction);
            if (client.level.isEmptyBlock(pos)) {
                BlockPlacement.place(client, this, pos, true, 70.0F, stack -> stack.is(Items.RESPAWN_ANCHOR));
                return;
            }
        }
    }

    static BlockPos findAnchor(final Minecraft client, final BlockPos center, final int radius) {
        if (client.level == null) {
            return null;
        }
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - 1; y <= center.getY() + 2; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.level.getBlockState(pos).getBlock() instanceof RespawnAnchorBlock) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
}
