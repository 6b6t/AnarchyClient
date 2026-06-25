package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;

public final class EntityOwnerModule extends Module {

    public EntityOwnerModule() {
        super("entity_owner", "Entity Owner", ModuleCategory.RENDER);
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
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof TamableAnimal animal && animal.getOwnerReference() != null) {
                WorldLineRenderer.boxNoDepth(matrices, submits, entity.getBoundingBox().inflate(0.05).move(camera.scale(-1)),
                        new WorldLineRenderer.Color(120, 230, 255, 210));
            }
        }
    }
}
