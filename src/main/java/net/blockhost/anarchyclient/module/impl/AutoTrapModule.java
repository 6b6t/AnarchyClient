package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class AutoTrapModule extends Module {

    private final SelectSetting targetMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("target")
            .name("Target")
            .defaultValue("Enemy")
            .addAllOptions(List.of("Enemy", "Self"))
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.5)
            .min(1.0)
            .max(6.0)
            .step(0.5)
            .build()));
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
    private final BooleanSetting includeFeet = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("include_feet")
            .name("Feet")
            .defaultValue(false)
            .build()));
    private final BooleanSetting face = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("face")
            .name("Face")
            .defaultValue(true)
            .build()));
    private final BooleanSetting top = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("top")
            .name("Top")
            .defaultValue(true)
            .build()));
    private final BooleanSetting antiStep = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("anti_step")
            .name("Anti Step")
            .defaultValue(false)
            .build()));
    private final BooleanSetting selfTrap = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("avoid_self")
            .name("Avoid Self")
            .defaultValue(true)
            .build()));

    public AutoTrapModule() {
        super("auto_trap", "Auto Trap", ModuleCategory.COMBAT);
        this.range.visibleWhen(() -> "Enemy".equals(this.targetMode.value()));
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        Player target = "Self".equals(this.targetMode.value()) ? player : CombatTargets.nearestEnemy(client, this.range.value());
        if (target == null) {
            return;
        }
        List<BlockPos> targets = trapPositions(target.blockPosition(), this.includeFeet.value(), this.face.value(),
                this.top.value(), this.antiStep.value());
        int placed = CombatPlacementPlanner.placeBatch(client, this, targets, CombatPlacementPlanner.HARD_BLOCKS,
                CombatPlacementPlanner.Options.of(this.blocksPerTick.value().intValue(), this.rotate.value(), 5.5)
                        .withSupport(this.support.value())
                        .withAvoidSelf(this.selfTrap.value()));
        this.debugValue("target", target.getScoreboardName());
        this.debugValue("positions", targets.size());
        this.debugValue("placed", placed);
    }

    static List<BlockPos> trapPositions(final BlockPos base) {
        return trapPositions(base, false);
    }

    static List<BlockPos> trapPositions(final BlockPos base, final boolean includeFeet) {
        return trapPositions(base, includeFeet, true, true, false);
    }

    static List<BlockPos> trapPositions(final BlockPos base, final boolean includeFeet, final boolean face,
                                        final boolean top, final boolean antiStep) {
        List<BlockPos> positions = new ArrayList<>();
        if (top) {
            positions.add(base.above(2));
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (face) {
                positions.add(base.above().relative(direction));
            }
            if (includeFeet) {
                positions.add(base.relative(direction));
            }
            if (antiStep) {
                positions.add(base.above(2).relative(direction));
            }
        }
        return CombatPlacementPlanner.unique(positions);
    }

}
