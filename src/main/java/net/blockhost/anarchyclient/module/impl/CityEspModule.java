package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Set;

public final class CityEspModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(16.0)
            .min(4.0)
            .max(64.0)
            .step(2.0)
            .build()));
    private final Set<BlockPos> positions = new LinkedHashSet<>();
    private int cooldownTicks;

    public CityEspModule() {
        super("city_esp", "City ESP", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            this.positions.clear();
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        this.cooldownTicks = 10;
        this.positions.clear();
        double rangeSqr = this.range.value() * this.range.value();
        for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player target) || target == player || target.distanceToSqr(player) > rangeSqr) {
                continue;
            }
            BlockPos base = target.blockPosition();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos pos = base.relative(direction);
                if (isCityBlock(client, pos)) {
                    this.positions.add(pos.immutable());
                }
            }
        }
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.level == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        for (BlockPos pos : this.positions) {
            WorldLineRenderer.boxNoDepth(matrices, submits, new AABB(pos).move(camera.scale(-1)),
                    new WorldLineRenderer.Color(255, 95, 90, 190));
        }
    }

    private static boolean isCityBlock(final Minecraft client, final BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        return state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.RESPAWN_ANCHOR)
                || state.is(Blocks.ENDER_CHEST);
    }
}
