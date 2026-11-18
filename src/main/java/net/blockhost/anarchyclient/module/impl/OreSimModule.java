package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.WorldLineRenderer;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shows where an ore generates by height. It does not simulate world generation, so it cannot
 * tell where individual ores are.
 */
public final class OreSimModule extends Module {

    private static final String DEBRIS = "Ancient Debris";

    private final SelectSetting target = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("target")
            .name("Target")
            .defaultValue("Diamond")
            .addAllOptions(List.of("Diamond", DEBRIS))
            .build()));
    private final NumberSetting radius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("radius")
            .name("Radius")
            .defaultValue(24.0)
            .min(4.0)
            .max(64.0)
            .step(2.0)
            .build()));

    public OreSimModule() {
        super("ore_sim", "Ore Sim", ModuleCategory.RENDER,
                "Outlines the height band where the selected ore generates in its dimension and marks its most common layer. "
                        + "It does not predict exact ore positions.");
    }

    @Override
    public void renderWorld(final Minecraft client, final LevelRenderContext context) {
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || matrices == null || submits == null) {
            return;
        }
        OreBand band = band(this.target.value());
        if (client.level.dimension() != band.dimension()) {
            return;
        }
        double size = this.radius.value();
        Vec3 offset = client.gameRenderer.mainCamera().position().scale(-1);
        Vec3 center = player.getPosition(client.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        WorldLineRenderer.Color color = DEBRIS.equals(this.target.value())
                ? new WorldLineRenderer.Color(190, 120, 85, 210)
                : new WorldLineRenderer.Color(105, 235, 255, 210);
        AABB bandBox = new AABB(center.x - size, band.minY(), center.z - size,
                center.x + size, band.maxY() + 1, center.z + size);
        WorldLineRenderer.boxNoDepth(matrices, submits, bandBox.move(offset), color);
        AABB peakLayer = new AABB(bandBox.minX, band.peakY(), bandBox.minZ, bandBox.maxX, band.peakY() + 1, bandBox.maxZ);
        WorldLineRenderer.fillNoDepth(matrices, submits, peakLayer.move(offset),
                new WorldLineRenderer.Color(color.red(), color.green(), color.blue(), 40));
    }

    /**
     * Vanilla generation heights. Diamonds generate in the Overworld and follow a triangle that
     * peaks below the world bottom, so the lowest layers above bedrock are the richest. Ancient
     * debris generates in the Nether: small veins anywhere from Y 8 to 119, and large veins in a
     * triangle from Y 8 to 24 that peaks at Y 16.
     */
    static OreBand band(final String target) {
        return DEBRIS.equals(target)
                ? new OreBand(Level.NETHER, 8, 16, 119)
                : new OreBand(Level.OVERWORLD, -64, -59, 16);
    }

    record OreBand(ResourceKey<Level> dimension, int minY, int peakY, int maxY) {
    }
}
