package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OreSimModule extends Module {

    private final StringSetting seed = this.setting(StringSetting.from(StringSetting.builder()
            .id("seed")
            .name("Seed")
            .defaultValue("")
            .build()));
    private final SelectSetting target = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("target")
            .name("Target")
            .defaultValue("Diamond")
            .addAllOptions(List.of("Diamond", "Ancient Debris"))
            .build()));
    private final NumberSetting radius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("radius")
            .name("Radius")
            .defaultValue(24.0)
            .min(4.0)
            .max(64.0)
            .step(2.0)
            .build()));
    private final NumberSetting maxMarkers = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_markers")
            .name("Max")
            .defaultValue(96.0)
            .min(8.0)
            .max(512.0)
            .step(8.0)
            .build()));
    private final List<BlockPos> markers = new ArrayList<>();
    private int cooldown;

    public OreSimModule() {
        super("ore_sim", "Ore Sim", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            this.markers.clear();
            return;
        }
        if (this.cooldown-- > 0) {
            return;
        }
        this.cooldown = 40;
        this.markers.clear();
        this.markers.addAll(scan(player.blockPosition(), this.seed.value(), this.target.value(),
                this.radius.value().intValue(), this.maxMarkers.value().intValue()));
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.player == null || matrices == null || submits == null || this.markers.isEmpty()) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        WorldLineRenderer.Color color = "Ancient Debris".equals(this.target.value())
                ? new WorldLineRenderer.Color(190, 120, 85, 210)
                : new WorldLineRenderer.Color(105, 235, 255, 210);
        for (BlockPos marker : this.markers) {
            WorldLineRenderer.boxNoDepth(matrices, submits,
                    new AABB(marker).inflate(0.01).move(camera.scale(-1)), color);
        }
    }

    static List<BlockPos> scan(final BlockPos center, final String seed, final String target, final int radius,
                               final int maxMarkers) {
        if (seed == null || seed.isBlank() || maxMarkers <= 0) {
            return List.of();
        }
        int minY = "Ancient Debris".equals(target) ? 8 : -64;
        int maxY = "Ancient Debris".equals(target) ? 22 : 16;
        int spacing = "Ancient Debris".equals(target) ? 4 : 3;
        List<BlockPos> result = new ArrayList<>();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x += spacing) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z += spacing) {
                int y = minY + Math.floorMod(hash(seed, x, z), maxY - minY + 1);
                BlockPos pos = new BlockPos(x, y, z);
                if (hash(seed, pos.getX(), pos.getY(), pos.getZ()) % 11 == 0) {
                    result.add(pos);
                }
            }
        }
        result.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return result.size() <= maxMarkers ? List.copyOf(result) : List.copyOf(result.subList(0, maxMarkers));
    }

    private static int hash(final String seed, final int... values) {
        int hash = seed.hashCode();
        for (int value : values) {
            hash = 31 * hash + value;
            hash ^= hash >>> 16;
        }
        return hash & 0x7FFFFFFF;
    }
}
