package net.blockhost.anarchyclient.module.impl;

import java.util.ArrayList;
import java.util.List;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.placement.BlockPlacer;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class AutoAnvilModule extends Module {

    private final SelectSetting targetMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("target")
            .name("Target")
            .defaultValue("Enemy")
            .addAllOptions(List.of("Enemy", "Self"))
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));
    private final NumberSetting dropHeight = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("drop_height")
            .name("Height")
            .defaultValue(3.0)
            .min(2.0)
            .max(6.0)
            .step(1.0)
            .build()));
    private final NumberSetting blocksPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("blocks_per_tick")
            .name("Blocks")
            .defaultValue(2.0)
            .min(1.0)
            .max(6.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final BooleanSetting support = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("support")
            .name("Support")
            .defaultValue(true)
            .build()));
    private final BooleanSetting faceTrap = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("face_trap")
            .name("Trap")
            .defaultValue(true)
            .build()));

    public AutoAnvilModule() {
        super(
                "auto_anvil",
                "Auto Anvil",
                ModuleCategory.COMBAT,
                "Places an anvil above the nearest enemy or yourself so it falls on them, with an optional side support block and face trap.");
        this.range.visibleWhen(() -> "Enemy".equals(this.targetMode.value()));
    }

    @Override
    public void tick(final Minecraft client) {
        Player target = "Self".equals(this.targetMode.value())
                ? client.player
                : CombatTargets.nearestEnemy(client, this.range.value());
        if (target == null || client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        BlockPos base = target.blockPosition();
        Direction side = supportSide(base, client.player.position());
        List<BlockPos> supports =
                supportPositions(base, this.dropHeight.value().intValue(), this.faceTrap.value(), side);
        int placed = 0;
        if (this.support.value()) {
            placed += CombatPlacementPlanner.placeBatch(
                    client,
                    this,
                    supports,
                    CombatPlacementPlanner.HARD_BLOCKS,
                    CombatPlacementPlanner.Options.of(this.blocksPerTick.value().intValue(), this.rotate.value(), 6.0)
                            .withSupport(true)
                            .withAvoidSelf(true));
        }
        if (placed < this.blocksPerTick.value().intValue()) {
            BlockPos anvil = base.above(this.dropHeight.value().intValue());
            BlockPlacer.PlacementResult result = BlockPlacer.place(
                    client,
                    this.id(),
                    anvil,
                    this.rotate.value(),
                    70.0F,
                    stack -> stack.is(Items.ANVIL),
                    BlockPlacer.PlacementOptions.ANY_BLOCK_ITEM);
            if (result == BlockPlacer.PlacementResult.PLACED) {
                placed++;
            }
        }
        this.debugValue("target", target.getScoreboardName());
        this.debugValue("placed", placed);
    }

    static List<BlockPos> supportPositions(
            final BlockPos base, final int dropHeight, final boolean faceTrap, final Direction side) {
        List<BlockPos> positions = new ArrayList<>();
        // Support the anvil from the side so the column below it stays open and it can fall.
        positions.add(base.above(dropHeight).relative(side));
        if (faceTrap) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                positions.add(base.above().relative(direction));
            }
        }
        return CombatPlacementPlanner.unique(positions);
    }

    static Direction supportSide(final BlockPos base, final Vec3 placer) {
        return Direction.getApproximateNearest(placer.x - (base.getX() + 0.5), 0.0, placer.z - (base.getZ() + 0.5));
    }
}
