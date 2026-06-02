package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

public final class BlockEspModule extends Module {

    private final StringSetting blocks = this.setting(StringSetting.from(StringSetting.builder()
            .id("blocks")
            .name("Blocks")
            .defaultValue("diamond_ore,deepslate_diamond_ore,ancient_debris,spawner")
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(32.0)
            .min(8.0)
            .max(96.0)
            .step(4.0)
            .build()));
    private final NumberSetting verticalRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical_range")
            .name("Vertical")
            .defaultValue(24.0)
            .min(8.0)
            .max(96.0)
            .step(4.0)
            .build()));
    private final NumberSetting maxBlocks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_blocks")
            .name("Max")
            .defaultValue(96.0)
            .min(8.0)
            .max(512.0)
            .step(8.0)
            .build()));
    private List<BlockPos> cachedPositions = List.of();
    private String lastBlocks = "";
    private Set<Block> parsedBlocks = Set.of();
    private int scanCooldownTicks;

    public BlockEspModule() {
        super("block_esp", "Block ESP", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (!this.lastBlocks.equals(this.blocks.value())) {
            this.parsedBlocks = BlockScan.parseBlocks(this.blocks.value());
            this.lastBlocks = this.blocks.value();
            this.scanCooldownTicks = 0;
        }
        if (this.scanCooldownTicks > 0) {
            this.scanCooldownTicks--;
            return;
        }
        this.cachedPositions = BlockScan.matchingBlocks(client, this.parsedBlocks, this.range.value().intValue(),
                this.verticalRange.value().intValue(), this.maxBlocks.value().intValue());
        this.scanCooldownTicks = 30;
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        MultiBufferSource consumers = context.bufferSource();
        if (client.level == null || matrices == null || consumers == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.getMainCamera().position();
        for (BlockPos pos : this.cachedPositions) {
            WorldLineRenderer.box(matrices, consumers, new AABB(pos).move(camera.scale(-1)), new WorldLineRenderer.Color(76, 228, 120, 190));
        }
    }
}
