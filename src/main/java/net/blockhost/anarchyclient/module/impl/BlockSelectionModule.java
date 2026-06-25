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

public final class BlockSelectionModule extends Module {

    private final BooleanSetting fill = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("fill")
            .name("Fill")
            .defaultValue(false)
            .build()));

    public BlockSelectionModule() {
        super("block_selection", "Block Selection", ModuleCategory.RENDER);
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (!(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || matrices == null
                || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        AABB box = new AABB(hit.getBlockPos()).inflate(0.002).move(camera.scale(-1));
        if (this.fill.value()) {
            WorldLineRenderer.fillNoDepth(matrices, submits, box, new WorldLineRenderer.Color(120, 230, 255, 35));
        }
        WorldLineRenderer.boxNoDepth(matrices, submits, box, new WorldLineRenderer.Color(120, 230, 255, 220));
    }
}
