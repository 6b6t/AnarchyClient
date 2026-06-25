package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BlockOutlineModule extends Module {

    private final BooleanSetting throughWalls = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("through_walls")
            .name("Through Walls")
            .defaultValue(true)
            .build()));
    private final BooleanSetting fill = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("fill")
            .name("Fill")
            .defaultValue(true)
            .build()));

    public BlockOutlineModule() {
        super("block_outline", "Block Outline", ModuleCategory.RENDER);
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (!(client.hitResult instanceof BlockHitResult hit)
                || client.hitResult.getType() != HitResult.Type.BLOCK
                || matrices == null
                || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        AABB box = new AABB(hit.getBlockPos()).inflate(0.003).move(camera.scale(-1));
        WorldLineRenderer.Color outline = this.throughWalls.value()
                ? new WorldLineRenderer.Color(95, 205, 255, 235)
                : new WorldLineRenderer.Color(255, 255, 255, 220);
        if (this.fill.value()) {
            WorldLineRenderer.fillNoDepth(matrices, submits, box, new WorldLineRenderer.Color(95, 205, 255, 35));
        }
        WorldLineRenderer.boxNoDepth(matrices, submits, box, outline);
    }
}
