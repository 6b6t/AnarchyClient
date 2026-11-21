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
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

public final class ScaffoldModule extends Module {

    private final NumberSetting blocksPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("blocks_per_tick")
            .name("Blocks")
            .defaultValue(1.0)
            .min(1.0)
            .max(6.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final BooleanSetting expand = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("expand")
            .name("Expand")
            .defaultValue(true)
            .build()));
    private final BooleanSetting tower = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("tower")
            .name("Tower")
            .defaultValue(false)
            .build()));
    private final BooleanSetting safeWalk = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("safe_walk")
            .name("Safe Walk")
            .defaultValue(true)
            .build()));
    private final BooleanSetting eagle = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("eagle")
            .name("Eagle")
            .defaultValue(false)
            .build()));
    private final BooleanSetting prediction = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("prediction")
            .name("Predict")
            .defaultValue(true)
            .build()));
    private final BooleanSetting sprint = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sprint")
            .name("Sprint")
            .defaultValue(false)
            .build()));
    private final SelectSetting technique = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("technique")
            .name("Technique")
            .defaultValue("Normal")
            .addAllOptions(List.of("Normal", "Expand", "Down", "Telly"))
            .build()));
    private final SelectSetting towerMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("tower_mode")
            .name("Tower Mode")
            .defaultValue("Motion")
            .addAllOptions(List.of("Motion", "Pulldown", "None"))
            .build()));
    private int startY = Integer.MIN_VALUE;

    public ScaffoldModule() {
        super(
                "scaffold",
                "Scaffold",
                ModuleCategory.WORLD,
                "Places blocks under the player while walking, with optional tower, sprint, and edge protection.");
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        LocalPlayer player = client.player;
        if (player == null || player.input != input || client.gui.screen() != null) {
            return;
        }
        if (this.sprint.value()) {
            input.keyPresses = InputStates.withSprint(input.keyPresses, true);
            player.setSprinting(true);
        }
        if (this.eagle.value()
                && client.level != null
                && client.level.getBlockState(player.blockPosition().below()).canBeReplaced()) {
            input.keyPresses = InputStates.withShift(input.keyPresses, true);
        }
        InputStates.refreshMoveVector(input);
    }

    @Override
    public boolean preventEdgeFall(final Minecraft client, final Player player) {
        return this.safeWalk.value() && client.player == player && client.gui.screen() == null;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        // Keep the bridge height while jumping, but follow the player once they land on a new level.
        if (this.startY == Integer.MIN_VALUE || player.onGround()) {
            this.startY = player.getBlockY();
        }
        if (this.tower.value() && player.input != null && player.input.keyPresses.jump()) {
            if ("Motion".equals(this.towerMode.value())) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.42, player.getDeltaMovement().z);
            } else if ("Pulldown".equals(this.towerMode.value()) && player.getDeltaMovement().y < 0.0) {
                player.setDeltaMovement(player.getDeltaMovement().x, -0.28, player.getDeltaMovement().z);
            }
        }
        List<BlockPos> targets = targets(player);
        // The batch stops at a target that is still rotating, so it is retried next tick.
        int placed = BlockPlacer.placeBatch(
                client,
                this.id(),
                targets.stream()
                        .map(pos -> new BlockPlacer.PlacementRequest(pos, stack -> true))
                        .toList(),
                this.blocksPerTick.value().intValue(),
                this.rotate.value(),
                80.0F);
        this.debugValue("placed", placed);
        this.debugValue("targets", targets.size());
    }

    @Override
    protected void onDisable() {
        this.startY = Integer.MIN_VALUE;
    }

    private List<BlockPos> targets(final LocalPlayer player) {
        BlockPos base = player.blockPosition().below();
        if ("Down".equals(this.technique.value())) {
            base = base.below();
        }
        if (this.startY != Integer.MIN_VALUE && base.getY() > this.startY - 1 && !this.tower.value()) {
            base = new BlockPos(base.getX(), this.startY - 1, base.getZ());
        }
        List<BlockPos> targets = new ArrayList<>();
        targets.add(base);
        if (this.expand.value() || "Expand".equals(this.technique.value()) || "Telly".equals(this.technique.value())) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                targets.add(base.relative(direction));
            }
        }
        if ("Telly".equals(this.technique.value())) {
            targets.add(base.relative(player.getDirection()));
            targets.add(base.relative(player.getDirection()).relative(player.getDirection()));
        }
        if (this.prediction.value()) {
            BlockPos predicted = BlockPos.containing(
                            player.getX() + player.getDeltaMovement().x,
                            player.getY(),
                            player.getZ() + player.getDeltaMovement().z)
                    .below();
            targets.add(predicted);
            if (this.expand.value()) {
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    targets.add(predicted.relative(direction));
                }
            }
        }
        return CombatPlacementPlanner.unique(targets);
    }
}
