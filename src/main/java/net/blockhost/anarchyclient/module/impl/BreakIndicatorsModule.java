package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class BreakIndicatorsModule extends Module {

    private final NumberSetting lifetime = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("lifetime")
            .name("Lifetime")
            .defaultValue(40.0)
            .min(5.0)
            .max(200.0)
            .step(5.0)
            .build()));
    private final Map<BlockPos, BreakMarker> markers = new HashMap<>();

    public BreakIndicatorsModule() {
        super("break_indicators", "Break Indicators", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        Iterator<Map.Entry<BlockPos, BreakMarker>> iterator = this.markers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, BreakMarker> entry = iterator.next();
            BreakMarker ticked = entry.getValue().tick();
            if (ticked.age() > this.lifetime.value().intValue()) {
                iterator.remove();
            } else {
                entry.setValue(ticked);
            }
        }
    }

    @Override
    public void blockBreakingProgress(final Minecraft client, final int breakerId, final BlockPos pos,
                                      final int progress) {
        if (progress < 0 || progress >= 10) {
            this.markers.remove(pos);
        } else {
            this.markers.put(pos.immutable(), new BreakMarker(progress, 0));
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
        for (Map.Entry<BlockPos, BreakMarker> entry : this.markers.entrySet()) {
            double shrink = (10 - entry.getValue().progress()) * 0.035;
            AABB box = new AABB(entry.getKey()).inflate(-shrink).move(camera.scale(-1));
            WorldLineRenderer.fillNoDepth(matrices, submits, box, new WorldLineRenderer.Color(255, 75, 70, 45));
            WorldLineRenderer.boxNoDepth(matrices, submits, box, new WorldLineRenderer.Color(255, 95, 90, 190));
        }
    }

    record BreakMarker(int progress, int age) {

        BreakMarker tick() {
            return new BreakMarker(this.progress, this.age + 1);
        }
    }
}
