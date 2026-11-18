package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.world.HoleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class HoleSnapModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(3.0)
            .min(1.0)
            .max(6.0)
            .step(1.0)
            .build()));
    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.25)
            .min(0.05)
            .max(1.0)
            .step(0.05)
            .description("Blocks per tick you are pulled toward the hole.")
            .build()));
    private final BooleanSetting directional = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("directional")
            .name("Directional")
            .defaultValue(true)
            .description("Only snaps to holes ahead of where you are moving.")
            .build()));
    private final BooleanSetting doubleHoles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("double_holes")
            .name("Double Holes")
            .defaultValue(true)
            .build()));
    private final BooleanSetting pauseSneaking = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_sneaking")
            .name("Pause Sneaking")
            .defaultValue(true)
            .build()));
    private final BooleanSetting autoDisable = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("auto_disable")
            .name("Auto Disable")
            .defaultValue(false)
            .description("Turns off once you are in a hole.")
            .build()));

    public HoleSnapModule() {
        super("hole_snap", "Hole Snap", ModuleCategory.MOVEMENT,
                "Pulls you sideways into a nearby obsidian or bedrock hole as you move past it.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || player.isFallFlying() || player.getAbilities().flying
                || player.isPassenger() || this.pauseSneaking.value() && player.isShiftKeyDown()) {
            return;
        }
        BlockPos feet = player.blockPosition();
        if (this.isHoleCell(client.level, feet)) {
            if (this.autoDisable.value() && player.onGround()) {
                this.enabled(false);
            }
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        Optional<BlockPos> hole = this.findHole(client.level, player.position(), feet,
                this.directional.value() ? motion : null);
        if (hole.isEmpty()) {
            return;
        }
        Vec3 pull = pull(player.position(), Vec3.atBottomCenterOf(hole.orElseThrow()), this.speed.value());
        player.setDeltaMovement(pull.x, motion.y, pull.z);
        this.debugValue("hole", hole.orElseThrow().toShortString());
    }

    private boolean isHoleCell(final BlockGetter level, final BlockPos pos) {
        return HoleManager.isHole(level, pos) || this.doubleHoles.value() && HoleManager.isDoubleHoleCell(level, pos);
    }

    private Optional<BlockPos> findHole(final BlockGetter level, final Vec3 position, final BlockPos feet,
                                        final Vec3 motion) {
        int radius = this.range.value().intValue();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 0; dy++) {
                    BlockPos pos = feet.offset(dx, dy, dz);
                    if (!this.isHoleCell(level, pos) || !HoleManager.isOpen(level.getBlockState(pos.above()))) {
                        continue;
                    }
                    Vec3 center = Vec3.atBottomCenterOf(pos);
                    if (motion != null && !isAhead(position, center, motion)) {
                        continue;
                    }
                    double distance = center.distanceToSqr(position);
                    if (distance < bestDistance) {
                        best = pos;
                        bestDistance = distance;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** Returns whether the hole lies in the direction of horizontal movement. */
    static boolean isAhead(final Vec3 position, final Vec3 hole, final Vec3 motion) {
        double horizontal = motion.x * motion.x + motion.z * motion.z;
        if (horizontal < 1.0E-4) {
            return false;
        }
        return (hole.x - position.x) * motion.x + (hole.z - position.z) * motion.z > 0.0;
    }

    /** Returns the horizontal velocity that moves toward the hole center without overshooting it. */
    static Vec3 pull(final Vec3 position, final Vec3 hole, final double speed) {
        double dx = hole.x - position.x;
        double dz = hole.z - position.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1.0E-4) {
            return Vec3.ZERO;
        }
        double step = Math.min(speed, distance);
        return new Vec3(dx / distance * step, 0.0, dz / distance * step);
    }
}
