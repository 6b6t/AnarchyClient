package net.blockhost.anarchyclient.module.impl;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.placement.BlockPlacer;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PistonCrystalModule extends Module {

    private static final int STAGE_TIMEOUT_TICKS = 40;

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(2.0)
            .max(6.0)
            .step(0.5)
            .build()));
    private final NumberSetting maxSelfDamage = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_self_damage")
            .name("Self Damage")
            .defaultValue(8.0)
            .min(0.0)
            .max(36.0)
            .step(1.0)
            .description("Skips a setup when the crystal could deal more than this much damage to you, in HP.")
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final BooleanSetting reset = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("reset")
            .name("Reset")
            .defaultValue(true)
            .description("Mines the redstone block after each blast so the piston can fire again.")
            .build()));
    /** Ticks a direction that failed is skipped, so the next setup gets a turn instead of the same one forever. */
    private static final int FAILED_DIRECTION_TICKS = 40;

    private PistonCrystalPlan plan;
    private Stage stage = Stage.PISTON;
    private int stageTicks;
    private final Map<Direction, Integer> failedDirections = new EnumMap<>(Direction.class);

    public PistonCrystalModule() {
        super(
                "piston_crystal",
                "Piston Crystal",
                ModuleCategory.COMBAT,
                "Places an end crystal beside the nearest enemy's head, pushes it into them with a powered piston, "
                        + "and breaks it. Works on targets in holes. Needs pistons, end crystals, and redstone blocks in the hotbar.");
    }

    @Override
    public void tick(final Minecraft client) {
        this.failedDirections.replaceAll((direction, ticks) -> ticks - 1);
        this.failedDirections.values().removeIf(ticks -> ticks <= 0);
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        Player target = CombatTargets.nearestEnemy(client, this.range.value() + 2.0);
        if (target == null) {
            this.resetPlan();
            this.debugValue("stage", "no target");
            return;
        }
        if (this.plan == null
                || !this.plan.feet().equals(target.blockPosition())
                || ++this.stageTicks > STAGE_TIMEOUT_TICKS) {
            this.plan = this.choosePlan(level, player, target).orElse(null);
            this.stage = Stage.PISTON;
            this.stageTicks = 0;
            if (this.plan == null) {
                this.debugValue("stage", "no setup");
                return;
            }
        }
        this.debugValue("stage", this.stage);
        switch (this.stage) {
            case PISTON -> this.placePiston(client);
            case CRYSTAL -> this.placeCrystal(client, level);
            case POWER -> this.power(client);
            case BREAK -> this.breakCrystal(client, level, player);
            case RESET -> this.resetPower(client, level);
        }
    }

    private void placePiston(final Minecraft client) {
        double reach = Math.min(this.range.value(), client.player.blockInteractionRange());
        switch (PistonPlacement.placePiston(client, this, this.plan.piston(), this.plan.pistonFacing(), reach)) {
            case PRESENT -> this.advance(Stage.CRYSTAL);
            case BLOCKED -> this.failPlan("piston blocked");
            case MISSING_ITEM -> this.failPlan("no pistons");
            default -> {}
        }
    }

    /** Drops the current setup and skips its direction for a while. */
    private void failPlan(final String reason) {
        this.debugValue("failed", this.stage + ": " + reason);
        if (this.plan != null) {
            this.failedDirections.put(this.plan.outward(), FAILED_DIRECTION_TICKS);
        }
        this.plan = null;
    }

    private void placeCrystal(final Minecraft client, final ClientLevel level) {
        if (crystalNear(level, this.plan.crystal()).isPresent()) {
            this.advance(Stage.POWER);
            return;
        }
        WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(
                client,
                this,
                this.plan.crystal().below(),
                Direction.UP,
                stack -> stack.is(Items.END_CRYSTAL),
                this.rotate.value());
        if (result == WorldInteraction.ActionResult.MISSING_ITEM || result == WorldInteraction.ActionResult.REFUSED) {
            this.failPlan("crystal " + result);
        }
    }

    private void power(final Minecraft client) {
        PistonPlacement.Result result =
                PistonPlacement.placePower(client, this, this.plan.power(), this.rotate.value());
        this.debugValue("power", result);
        switch (result) {
            case PLACED, PRESENT -> this.advance(Stage.BREAK);
            case BLOCKED, MISSING_ITEM -> this.failPlan("power " + result);
            default -> {}
        }
    }

    private void breakCrystal(final Minecraft client, final ClientLevel level, final LocalPlayer player) {
        // The crystal may still be moving with the piston head, so look around both cells.
        Optional<EndCrystal> crystal =
                crystalNear(level, this.plan.head()).or(() -> crystalNear(level, this.plan.crystal()));
        if (crystal.isEmpty()) {
            if (this.stageTicks > 6) {
                this.advance(this.reset.value() ? Stage.RESET : Stage.PISTON);
            }
            return;
        }
        EndCrystal entity = crystal.orElseThrow();
        // The crystal's hitbox already overlaps the head cell before the push. Wait until the piston has extended
        // or the crystal itself stands in the head cell; give up if the push never happens.
        if (!this.plan.pushed(level, entity)) {
            if (this.stageTicks > PUSH_TIMEOUT_TICKS) {
                this.failPlan("crystal not pushed");
            }
            return;
        }
        if (!CombatTargets.canReachEntity(player, entity)) {
            return;
        }
        client.gameMode.attack(player, entity);
        player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        this.advance(this.reset.value() ? Stage.RESET : Stage.PISTON);
    }

    private void resetPower(final Minecraft client, final ClientLevel level) {
        Optional<BlockPos> power = PistonPlacement.existingPower(level, this.plan.power());
        if (power.isEmpty()) {
            WorldInteraction.stopBreaking(client, this);
            this.advance(Stage.PISTON);
            return;
        }
        BlockPos pos = power.orElseThrow();
        WorldInteraction.breakBlock(client, this, pos, nearestFace(client.player.getEyePosition(), pos));
    }

    /** How long to wait for the piston to push the crystal before trying another setup. */
    private static final int PUSH_TIMEOUT_TICKS = 10;

    private void advance(final Stage next) {
        this.stage = next;
        this.stageTicks = 0;
    }

    private Optional<PistonCrystalPlan> choosePlan(
            final ClientLevel level, final LocalPlayer player, final Player target) {
        double reach = Math.min(this.range.value(), player.blockInteractionRange());
        double health = player.getHealth() + player.getAbsorptionAmount();
        BlockPos feet = target.blockPosition();
        Vec3 eye = player.getEyePosition();
        for (Direction outward : PistonCrystalPlan.candidates(level, feet, eye)) {
            if (this.failedDirections.containsKey(outward)) {
                continue;
            }
            PistonCrystalPlan candidate = PistonCrystalPlan.of(feet, outward);
            if (!CombatTargets.canReachBlock(player, candidate.piston(), reach)
                    || !CombatTargets.canReachBlock(player, candidate.crystal().below(), reach)
                    || !canBuild(level, player, candidate)) {
                continue;
            }
            double selfDamage = Math.max(
                    DamageEstimator.explosionDamage(
                            player, DamageEstimator.crystalCenter(candidate.crystal()), DamageEstimator.CRYSTAL_POWER),
                    DamageEstimator.explosionDamage(
                            player, DamageEstimator.crystalCenter(candidate.head()), DamageEstimator.CRYSTAL_POWER));
            if (selfDamage > this.maxSelfDamage.value() || !CrystalAuraModule.isSafe(selfDamage, health)) {
                continue;
            }
            return Optional.of(candidate);
        }
        return Optional.empty();
    }

    /**
     * Returns whether the piston can be placed (or already stands) and at least one power spot
     * is free or already holds a redstone block.
     */
    private static boolean canBuild(final ClientLevel level, final LocalPlayer player, final PistonCrystalPlan plan) {
        boolean pistonReady = PistonPlacement.isPistonFacing(level.getBlockState(plan.piston()), plan.pistonFacing())
                || BlockPlacer.findPlacementTarget(level, plan.piston()).isPresent();
        boolean powerReady = plan.power().stream()
                .anyMatch(pos -> level.getBlockState(pos).is(Blocks.REDSTONE_BLOCK)
                        || (PistonPlacement.isOpen(level, pos) && !CombatPlacementPlanner.playerInside(player, pos)));
        return pistonReady && powerReady;
    }

    private static Optional<EndCrystal> crystalNear(final ClientLevel level, final BlockPos cell) {
        AABB box = new AABB(cell).inflate(0.5, 0.0, 0.5);
        for (Entity entity : level.getEntities(null, box)) {
            if (entity instanceof EndCrystal crystal && crystal.isAlive()) {
                return Optional.of(crystal);
            }
        }
        return Optional.empty();
    }

    private static Direction nearestFace(final Vec3 eye, final BlockPos pos) {
        return Direction.getApproximateNearest(eye.subtract(Vec3.atCenterOf(pos)));
    }

    private void resetPlan() {
        if (this.plan != null) {
            WorldInteraction.stopBreaking(Minecraft.getInstance(), this);
        }
        this.plan = null;
        this.stage = Stage.PISTON;
        this.stageTicks = 0;
    }

    @Override
    protected void onDisable() {
        this.resetPlan();
        this.failedDirections.clear();
        RotationManager.clear(this.id());
    }

    private enum Stage {
        PISTON,
        CRYSTAL,
        POWER,
        BREAK,
        RESET
    }
}
