package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

public final class XrayModule extends Module {

    private final StringSetting blocks = this.setting(StringSetting.from(StringSetting.builder()
            .id("blocks")
            .name("Blocks")
            .defaultValue("diamond_ore,deepslate_diamond_ore,ancient_debris,emerald_ore,deepslate_emerald_ore,spawner")
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(48.0)
            .min(8.0)
            .max(96.0)
            .step(4.0)
            .build()));
    private final NumberSetting verticalRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical_range")
            .name("Vertical")
            .defaultValue(48.0)
            .min(8.0)
            .max(96.0)
            .step(4.0)
            .build()));
    private final NumberSetting maxBlocks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_blocks")
            .name("Max")
            .defaultValue(128.0)
            .min(16.0)
            .max(512.0)
            .step(16.0)
            .build()));
    private final SelectSetting renderMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("render_mode")
            .name("Render")
            .defaultValue("Filled")
            .addAllOptions(List.of("Filled", "Outline"))
            .build()));

    private List<BlockPos> cachedPositions = List.of();
    private String lastBlocks = "";
    private Set<Block> parsedBlocks = Set.of();
    private int cooldownTicks;

    public XrayModule() {
        super("xray", "Xray", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (!this.lastBlocks.equals(this.blocks.value())) {
            this.parsedBlocks = BlockScan.parseBlocks(this.blocks.value());
            this.lastBlocks = this.blocks.value();
            this.cooldownTicks = 0;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        this.cachedPositions = BlockScan.matchingBlocks(client, this.parsedBlocks,
                this.range.value().intValue(), this.verticalRange.value().intValue(), this.maxBlocks.value().intValue());
        this.cooldownTicks = 30;
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
        boolean filled = "Filled".equals(this.renderMode.value());
        for (BlockPos pos : this.cachedPositions) {
            AABB box = new AABB(pos).move(camera.scale(-1));
            if (filled) {
                WorldLineRenderer.fillNoDepth(matrices, submits, box, new WorldLineRenderer.Color(90, 255, 160, 35));
            }
            WorldLineRenderer.boxNoDepth(matrices, submits, box, new WorldLineRenderer.Color(90, 255, 160, 180));
        }
    }
}
