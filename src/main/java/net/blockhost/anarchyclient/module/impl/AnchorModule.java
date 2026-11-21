package net.blockhost.anarchyclient.module.impl;

import java.util.Optional;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.world.HoleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

public final class AnchorModule extends Module {

    private static final int MAX_DEPTH = 5;

    private final NumberSetting pullSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("pull_speed")
            .name("Pull")
            .defaultValue(0.45)
            .min(0.05)
            .max(2.0)
            .step(0.05)
            .build()));

    public AnchorModule() {
        super(
                "anchor",
                "Anchor",
                ModuleCategory.MOVEMENT,
                "Pulls you down and centers you while you fall over a safe obsidian or bedrock hole, helping you drop into it.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null
                || client.level == null
                || player.onGround()
                || player.isFallFlying()
                || player.getAbilities().flying) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        if (movement.y > 0.0) {
            return;
        }
        Optional<BlockPos> hole = holeBelow(client.level, player.blockPosition(), MAX_DEPTH);
        if (hole.isEmpty()) {
            return;
        }
        Vec3 center = Vec3.atBottomCenterOf(hole.get());
        double pullX = Math.min(0.2, Math.max(-0.2, center.x - player.getX()));
        double pullZ = Math.min(0.2, Math.max(-0.2, center.z - player.getZ()));
        player.setDeltaMovement(pullX, -this.pullSpeed.value(), pullZ);
    }

    /** Finds a safe hole straight below {@code start}, looking only through open blocks. */
    static Optional<BlockPos> holeBelow(final BlockGetter level, final BlockPos start, final int maxDepth) {
        BlockPos pos = start;
        for (int depth = 0; depth <= maxDepth; depth++) {
            if (HoleManager.isHole(level, pos)) {
                return Optional.of(pos);
            }
            if (!HoleManager.isOpen(level.getBlockState(pos))) {
                return Optional.empty();
            }
            pos = pos.below();
        }
        return Optional.empty();
    }
}
