package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.world.HoleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PistonPushModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(2.0)
            .max(6.0)
            .step(0.5)
            .build()));
    private final BooleanSetting onlyHoles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("only_holes")
            .name("Only Holes")
            .defaultValue(true)
            .description("Only pushes targets that stand in a safe hole.")
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .description("Turns toward the redstone block spot before placing it.")
            .build()));
    private final NumberSetting cooldown = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cooldown")
            .name("Cooldown")
            .defaultValue(10.0)
            .min(0.0)
            .max(60.0)
            .step(1.0)
            .description("Ticks to wait after a push before planning the next one.")
            .build()));
    private final BooleanSetting autoDisable = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("auto_disable")
            .name("Auto Disable")
            .defaultValue(false)
            .description("Turns the module off after one push.")
            .build()));
    private int cooldownTicks;

    public PistonPushModule() {
        super("piston_push", "Piston Push", ModuleCategory.COMBAT,
                "Places a piston at the nearest enemy's head height and powers it with a redstone block, so the piston "
                        + "head forces them out of their hole or down into a crawl. Needs pistons and redstone blocks in the hotbar.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        double reach = Math.min(this.range.value(), player.blockInteractionRange());
        Player target = CombatTargets.nearestEnemy(client, this.range.value() + 2.0);
        if (target == null || this.onlyHoles.value() && !inHole(level, target.blockPosition())) {
            this.debugValue("state", "no target");
            return;
        }
        BlockPos feet = target.blockPosition();
        for (Direction facing : orderedFacings(level, feet, player.getEyePosition())) {
            BlockPos piston = pistonPosition(feet, facing);
            if (!CombatTargets.canReachBlock(player, piston, reach)) {
                continue;
            }
            if (PistonPlacement.isExtended(level.getBlockState(piston))) {
                this.finishPush();
                return;
            }
            PistonPlacement.Result pistonResult = PistonPlacement.placePiston(client, this, piston, facing, reach);
            this.debugValue("piston", pistonResult);
            switch (pistonResult) {
                case WAITING, PLACED -> {
                    return;
                }
                case MISSING_ITEM -> {
                    this.debugValue("state", "no pistons");
                    return;
                }
                case BLOCKED -> {
                    continue;
                }
                case PRESENT -> {
                    // Power it below.
                }
            }
            List<BlockPos> power = PistonPlacement.powerPositions(piston, facing, List.of(feet, feet.above()));
            PistonPlacement.Result powerResult = PistonPlacement.placePower(client, this, power, this.rotate.value());
            this.debugValue("power", powerResult);
            if (powerResult == PistonPlacement.Result.PLACED || powerResult == PistonPlacement.Result.PRESENT) {
                this.finishPush();
            }
            return;
        }
        this.debugValue("state", "no spot");
    }

    private void finishPush() {
        this.cooldownTicks = this.cooldown.value().intValue();
        RotationManager.clear(this.id());
        if (this.autoDisable.value()) {
            this.enabled(false);
        }
    }

    static boolean inHole(final BlockGetter level, final BlockPos feet) {
        return HoleManager.isHole(level, feet) || HoleManager.isDoubleHoleCell(level, feet);
    }

    /** The piston sits beside the target's head and faces it. */
    static BlockPos pistonPosition(final BlockPos feet, final Direction facing) {
        return feet.above().relative(facing.getOpposite());
    }

    /**
     * Returns the piston facings that can push the target at {@code feet}: the target's head cell
     * must be open for the piston head, and the piston cell must be open or already hold a piston
     * that faces the target. Nearest piston spots come first.
     */
    static List<Direction> orderedFacings(final BlockGetter level, final BlockPos feet, final Vec3 eye) {
        List<Direction> facings = new ArrayList<>();
        if (!PistonPlacement.isOpen(level, feet.above())) {
            return facings;
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos piston = pistonPosition(feet, facing);
            if (PistonPlacement.isOpen(level, piston)
                    || PistonPlacement.isPistonFacing(level.getBlockState(piston), facing)) {
                facings.add(facing);
            }
        }
        facings.sort(Comparator.comparingDouble(facing -> Vec3.atCenterOf(pistonPosition(feet, facing)).distanceToSqr(eye)));
        return facings;
    }

    @Override
    protected void onDisable() {
        this.cooldownTicks = 0;
        RotationManager.clear(this.id());
    }
}
