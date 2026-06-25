package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.placement.BlockPlacer;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;

public final class AutoWitherModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final NumberSetting verticalRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical_range")
            .name("Vertical")
            .defaultValue(3.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final SelectSetting priority = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("priority")
            .name("Priority")
            .defaultValue(BlockTargetScanner.SortMode.CLOSEST.label())
            .addAllOptions(List.of(
                    BlockTargetScanner.SortMode.CLOSEST.label(),
                    BlockTargetScanner.SortMode.FARTHEST.label(),
                    BlockTargetScanner.SortMode.RANDOM.label()
            ))
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(2.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final BooleanSetting turnOff = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("turn_off")
            .name("Turn Off")
            .defaultValue(true)
            .build()));
    private final BooleanSetting preview = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("preview")
            .name("Preview")
            .defaultValue(true)
            .build()));
    private WitherBuild currentBuild;
    private int stage;
    private int cooldownTicks;
    private List<BlockPos> previewPositions = List.of();

    public AutoWitherModule() {
        super("auto_wither", "Auto Wither", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            this.previewPositions = List.of();
            return;
        }
        if (this.currentBuild == null || !canContinue(client.level, this.currentBuild, this.stage)) {
            this.currentBuild = findBuild(client);
            this.stage = 0;
        }
        this.previewPositions = this.currentBuild == null ? List.of() : this.currentBuild.positions();
        if (this.currentBuild == null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }

        advanceCompletedStages(client.level);
        if (this.currentBuild == null) {
            return;
        }
        WitherStep step = this.currentBuild.steps().get(this.stage);
        if (!BlockPlacer.needsPlacement(client.level, step.pos()) && !step.matches(client.level.getBlockState(step.pos()))) {
            resetBuild();
            return;
        }
        BlockPlacer.PlacementResult result = BlockPlacer.place(
                client,
                this.id(),
                step.pos(),
                this.rotate.value(),
                70.0F,
                step::matchesItem,
                step.options()
        );
        if (result == BlockPlacer.PlacementResult.PLACED || result == BlockPlacer.PlacementResult.FILLED) {
            this.stage++;
            this.cooldownTicks = this.delay.value().intValue();
            finishIfComplete();
        }
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        if (!this.preview.value() || this.previewPositions.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.player == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        WorldLineRenderer.Color outline = new WorldLineRenderer.Color(180, 110, 255, 210);
        WorldLineRenderer.Color fill = new WorldLineRenderer.Color(120, 80, 180, 45);
        for (BlockPos pos : this.previewPositions) {
            AABB box = new AABB(pos).move(camera.scale(-1));
            WorldLineRenderer.fillNoDepth(matrices, submits, box, fill);
            WorldLineRenderer.boxNoDepth(matrices, submits, box, outline);
        }
    }

    @Override
    protected void onDisable() {
        resetBuild();
        this.cooldownTicks = 0;
        this.previewPositions = List.of();
    }

    private WitherBuild findBuild(final Minecraft client) {
        int horizontal = this.range.value().intValue();
        int vertical = this.verticalRange.value().intValue();
        for (BlockTargetScanner.BlockTarget target : BlockTargetScanner.scan(
                client,
                horizontal,
                vertical,
                BlockTargetScanner.SortMode.fromSetting(this.priority.value()),
                AutoTntModule.scanLimit(horizontal, vertical),
                candidate -> candidate.state().canBeReplaced()
        )) {
            for (Direction.Axis axis : List.of(Direction.Axis.X, Direction.Axis.Z)) {
                WitherBuild build = new WitherBuild(planSteps(target.pos(), axis));
                if (hasRoom(client.level, build)) {
                    return build;
                }
            }
        }
        return null;
    }

    private void advanceCompletedStages(final ClientLevel level) {
        while (this.currentBuild != null && this.stage < this.currentBuild.steps().size()) {
            WitherStep step = this.currentBuild.steps().get(this.stage);
            if (!step.matches(level.getBlockState(step.pos()))) {
                break;
            }
            this.stage++;
        }
        finishIfComplete();
    }

    private void finishIfComplete() {
        if (this.currentBuild == null || this.stage < this.currentBuild.steps().size()) {
            return;
        }
        resetBuild();
        if (this.turnOff.value()) {
            this.enabled(false);
        }
    }

    private void resetBuild() {
        this.currentBuild = null;
        this.stage = 0;
    }

    private static boolean canContinue(final ClientLevel level, final WitherBuild build, final int stage) {
        for (int index = 0; index < build.steps().size(); index++) {
            WitherStep step = build.steps().get(index);
            BlockState state = level.getBlockState(step.pos());
            if (index < stage && !step.matches(state)) {
                return false;
            }
            if (index >= stage && !BlockPlacer.needsPlacement(level, step.pos()) && !step.matches(state)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasRoom(final ClientLevel level, final WitherBuild build) {
        for (WitherStep step : build.steps()) {
            if (!level.isLoaded(step.pos())
                    || !BlockPlacer.needsPlacement(level, step.pos())
                    || !level.isUnobstructed(step.previewState(), step.pos(), CollisionContext.empty())) {
                return false;
            }
        }
        return true;
    }

    static List<BlockPos> planPositions(final BlockPos foot, final Direction.Axis axis) {
        return planSteps(foot, axis).stream().map(WitherStep::pos).toList();
    }

    static List<WitherStep> planSteps(final BlockPos foot, final Direction.Axis axis) {
        Direction negative = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        Direction positive = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockPos stem = foot.above();
        BlockPos skullCenter = foot.above(2);
        return List.of(
                new WitherStep(foot, WitherMaterial.SOUL),
                new WitherStep(stem, WitherMaterial.SOUL),
                new WitherStep(stem.relative(negative), WitherMaterial.SOUL),
                new WitherStep(stem.relative(positive), WitherMaterial.SOUL),
                new WitherStep(skullCenter.relative(negative), WitherMaterial.SKULL),
                new WitherStep(skullCenter, WitherMaterial.SKULL),
                new WitherStep(skullCenter.relative(positive), WitherMaterial.SKULL)
        );
    }

    enum WitherMaterial {
        SOUL,
        SKULL
    }

    record WitherStep(BlockPos pos, WitherMaterial material) {

        boolean matches(final BlockState state) {
            return switch (this.material) {
                case SOUL -> state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL);
                case SKULL -> state.is(Blocks.WITHER_SKELETON_SKULL) || state.is(Blocks.WITHER_SKELETON_WALL_SKULL);
            };
        }

        boolean matchesItem(final ItemStack stack) {
            return switch (this.material) {
                case SOUL -> stack.is(Items.SOUL_SAND) || stack.is(Items.SOUL_SOIL);
                case SKULL -> stack.is(Items.WITHER_SKELETON_SKULL);
            };
        }

        BlockPlacer.PlacementOptions options() {
            return this.material == WitherMaterial.SKULL
                    ? BlockPlacer.PlacementOptions.NON_FULL
                    : BlockPlacer.PlacementOptions.DEFAULT;
        }

        BlockState previewState() {
            return this.material == WitherMaterial.SKULL
                    ? Blocks.WITHER_SKELETON_SKULL.defaultBlockState()
                    : Blocks.SOUL_SAND.defaultBlockState();
        }
    }

    private record WitherBuild(List<WitherStep> steps) {

        private List<BlockPos> positions() {
            return this.steps.stream().map(WitherStep::pos).toList();
        }
    }
}
