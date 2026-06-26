package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public final class SurroundModule extends Module {

    private final NumberSetting blocksPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("blocks_per_tick")
            .name("Blocks")
            .defaultValue(2.0)
            .min(1.0)
            .max(8.0)
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
    private final BooleanSetting center = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("center")
            .name("Center")
            .defaultValue(true)
            .build()));
    private final BooleanSetting extend = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("extend")
            .name("Extend")
            .defaultValue(true)
            .build()));
    private final BooleanSetting floor = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("floor")
            .name("Floor")
            .defaultValue(true)
            .build()));
    private final BooleanSetting doubleHeight = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("double_height")
            .name("Double")
            .defaultValue(false)
            .build()));
    private final BooleanSetting disableOnMove = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("disable_on_move")
            .name("Move Disable")
            .defaultValue(false)
            .build()));
    private BlockPos startBlock;

    public SurroundModule() {
        super("surround", "Surround", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.startBlock == null) {
            this.startBlock = player.blockPosition();
        }
        if (this.disableOnMove.value() && !player.blockPosition().equals(this.startBlock)) {
            this.enabled(false);
            return;
        }
        if (this.center.value()) {
            centerPlayer(player);
        }
        List<BlockPos> targets = targetPositions(player, this.extend.value(), this.floor.value(), this.doubleHeight.value());
        int placed = CombatPlacementPlanner.placeBatch(client, this, targets, CombatPlacementPlanner.HARD_BLOCKS,
                CombatPlacementPlanner.Options.of(this.blocksPerTick.value().intValue(), this.rotate.value(), 5.5)
                        .withSupport(this.support.value()));
        this.debugValue("positions", targets.size());
        this.debugValue("placed", placed);
    }

    static List<BlockPos> targetPositions(final BlockPos base) {
        return targetPositions(base, false, true, false);
    }

    static List<BlockPos> targetPositions(final LocalPlayer player, final boolean extend, final boolean floor,
                                          final boolean doubleHeight) {
        return targetPositions(player.blockPosition(), extend, floor, doubleHeight);
    }

    static List<BlockPos> targetPositions(final BlockPos base, final boolean extend, final boolean floor,
                                          final boolean doubleHeight) {
        List<BlockPos> positions = new ArrayList<>();
        if (floor) {
            positions.add(base.below());
        }
        addRing(positions, base);
        if (doubleHeight) {
            addRing(positions, base.above());
        }
        if (extend) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos extended = base.relative(direction);
                addRing(positions, extended);
                if (floor) {
                    positions.add(extended.below());
                }
            }
        }
        return CombatPlacementPlanner.unique(positions);
    }

    private static void addRing(final List<BlockPos> positions, final BlockPos base) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            positions.add(base.relative(direction));
        }
    }

    private static void centerPlayer(final LocalPlayer player) {
        double centerX = player.blockPosition().getX() + 0.5;
        double centerZ = player.blockPosition().getZ() + 0.5;
        double dx = centerX - player.getX();
        double dz = centerZ - player.getZ();
        if (dx * dx + dz * dz > 0.0009) {
            player.setDeltaMovement(dx * 0.35, player.getDeltaMovement().y, dz * 0.35);
        }
    }

    @Override
    protected void onDisable() {
        this.startBlock = null;
    }
}
