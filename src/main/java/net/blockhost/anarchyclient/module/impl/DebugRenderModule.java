package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DebugRenderModule extends Module {

    private final BooleanSetting playerBox = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("player_box")
            .name("Player Box")
            .defaultValue(true)
            .build()));
    private final BooleanSetting chunk = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("chunk")
            .name("Chunk")
            .defaultValue(true)
            .build()));

    public DebugRenderModule() {
        super("debug_render", "Debug Render", ModuleCategory.RENDER);
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.player == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        if (this.playerBox.value()) {
            WorldLineRenderer.boxNoDepth(
                    matrices,
                    submits,
                    client.player.getBoundingBox().move(camera.scale(-1)),
                    new WorldLineRenderer.Color(80, 220, 255, 220)
            );
        }
        if (this.chunk.value()) {
            renderChunk(matrices, submits, client.player.blockPosition(), camera);
        }
    }

    private static void renderChunk(final PoseStack matrices, final SubmitNodeCollector submits, final BlockPos playerPos,
                                    final Vec3 camera) {
        int minX = Math.floorDiv(playerPos.getX(), 16) * 16;
        int minZ = Math.floorDiv(playerPos.getZ(), 16) * 16;
        int minY = playerPos.getY() - 8;
        int maxY = playerPos.getY() + 8;
        AABB box = new AABB(minX, minY, minZ, minX + 16, maxY, minZ + 16).move(camera.scale(-1));
        WorldLineRenderer.boxNoDepth(matrices, submits, box, new WorldLineRenderer.Color(255, 220, 90, 210));
    }
}
