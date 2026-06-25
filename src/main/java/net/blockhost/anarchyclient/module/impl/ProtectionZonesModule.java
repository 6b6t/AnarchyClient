package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ProtectionZonesModule extends Module {

    private final StringSetting zones = this.setting(StringSetting.from(StringSetting.builder()
            .id("zones")
            .name("Zones")
            .defaultValue("")
            .description("Centers as x,y,z entries separated by semicolons.")
            .build()));
    private final NumberSetting radius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("radius")
            .name("Radius")
            .defaultValue(16.0)
            .min(1.0)
            .max(128.0)
            .step(1.0)
            .build()));

    public ProtectionZonesModule() {
        super("protection_zones", "Protection Zones", ModuleCategory.RENDER);
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
        for (BlockPos center : parseZones(this.zones.value())) {
            double radiusValue = this.radius.value();
            AABB box = new AABB(
                    center.getX() - radiusValue, center.getY() - radiusValue, center.getZ() - radiusValue,
                    center.getX() + radiusValue, center.getY() + radiusValue, center.getZ() + radiusValue
            ).move(camera.scale(-1));
            WorldLineRenderer.boxNoDepth(matrices, submits, box, new WorldLineRenderer.Color(80, 255, 160, 190));
        }
    }

    static List<BlockPos> parseZones(final String value) {
        List<BlockPos> positions = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return positions;
        }
        for (String entry : value.split("[;|]")) {
            String[] parts = entry.trim().split(",");
            if (parts.length != 3) {
                continue;
            }
            try {
                positions.add(new BlockPos(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim())
                ));
            } catch (NumberFormatException ignored) {
                // Invalid user entry, skip it and keep the rest of the list usable.
            }
        }
        return positions;
    }
}
