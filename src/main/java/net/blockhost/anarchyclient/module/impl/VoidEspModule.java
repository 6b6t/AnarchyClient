package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class VoidEspModule extends Module {

    private final NumberSetting minY = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_y")
            .name("Min Y")
            .defaultValue(0.0)
            .min(-128.0)
            .max(128.0)
            .step(1.0)
            .build()));
    private final NumberSetting radius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("radius")
            .name("Radius")
            .defaultValue(24.0)
            .min(4.0)
            .max(96.0)
            .step(4.0)
            .build()));
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(70.0)
            .min(10.0)
            .max(180.0)
            .step(5.0)
            .build()));

    public VoidEspModule() {
        super("void_esp", "Void ESP", ModuleCategory.RENDER);
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (player == null || matrices == null || submits == null) {
            return;
        }
        double radiusValue = this.radius.value();
        double y = this.minY.value();
        Vec3 camera = client.gameRenderer.mainCamera().position();
        AABB plane = new AABB(
                player.getX() - radiusValue,
                y,
                player.getZ() - radiusValue,
                player.getX() + radiusValue,
                y + 0.05,
                player.getZ() + radiusValue
        ).move(camera.scale(-1));
        int alpha = this.opacity.value().intValue();
        WorldLineRenderer.fillNoDepth(matrices, submits, plane, new WorldLineRenderer.Color(255, 50, 70, alpha));
        WorldLineRenderer.boxNoDepth(matrices, submits, plane, new WorldLineRenderer.Color(255, 80, 95, Math.min(255, alpha + 80)));
    }
}
