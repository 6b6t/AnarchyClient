package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.WorldLineRenderer;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

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

    /** Chunk columns scanned per tick, so a large range is spread over several ticks. */
    private static final int COLUMNS_PER_TICK = 8;

    private static final int PASS_COOLDOWN_TICKS = 20;

    private List<BlockPos> cachedPositions = List.of();
    private String lastBlocks = "";
    private Set<Block> parsedBlocks = Set.of();
    private @Nullable ScanPass pass;
    private int cooldownTicks;
    /** The level the cached positions belong to; a dimension change or rejoin replaces it. */
    private @Nullable Level scannedLevel;

    public XrayModule() {
        super(
                "xray",
                "Xray",
                ModuleCategory.RENDER,
                "Highlights the selected blocks through terrain, nearest first, up to the configured count.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            this.reset();
            return;
        }
        if (this.scannedLevel != client.level) {
            this.reset();
            this.scannedLevel = client.level;
        }
        if (!this.lastBlocks.equals(this.blocks.value())) {
            this.parsedBlocks = BlockScan.parseBlocks(this.blocks.value());
            this.lastBlocks = this.blocks.value();
            this.pass = null;
            this.cooldownTicks = 0;
        }
        if (this.pass == null) {
            if (this.cooldownTicks > 0) {
                this.cooldownTicks--;
                return;
            }
            this.pass = new ScanPass(
                    player.blockPosition(),
                    this.range.value().intValue(),
                    this.verticalRange.value().intValue(),
                    Set.copyOf(this.parsedBlocks));
        }
        if (this.pass.step(client.level, COLUMNS_PER_TICK)) {
            this.cachedPositions = this.pass.nearest(this.maxBlocks.value().intValue());
            this.pass = null;
            this.cooldownTicks = PASS_COOLDOWN_TICKS;
        }
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    private void reset() {
        this.cachedPositions = List.of();
        this.pass = null;
        this.cooldownTicks = 0;
        this.scannedLevel = null;
    }

    @Override
    public void renderWorld(final Minecraft client, final LevelRenderContext context) {
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

    /**
     * One scan over the chunk columns in range. Sections whose palette has none of the blocks are
     * skipped without reading their blocks.
     */
    static final class ScanPass {

        private final BlockPos center;
        private final int horizontal;
        private final int vertical;
        private final Predicate<BlockState> wanted;
        private final int minChunkX;
        private final int maxChunkX;
        private final int maxChunkZ;
        private final List<BlockPos> found = new ArrayList<>();
        private int nextChunkX;
        private int nextChunkZ;

        ScanPass(final BlockPos center, final int horizontal, final int vertical, final Set<Block> blocks) {
            this.center = center.immutable();
            this.horizontal = Math.max(0, horizontal);
            this.vertical = Math.max(0, vertical);
            this.wanted = state -> blocks.contains(state.getBlock());
            this.minChunkX = SectionPos.blockToSectionCoord(center.getX() - this.horizontal);
            this.maxChunkX = SectionPos.blockToSectionCoord(center.getX() + this.horizontal);
            int minChunkZ = SectionPos.blockToSectionCoord(center.getZ() - this.horizontal);
            this.maxChunkZ = SectionPos.blockToSectionCoord(center.getZ() + this.horizontal);
            this.nextChunkX = this.minChunkX;
            this.nextChunkZ = blocks.isEmpty() ? this.maxChunkZ + 1 : minChunkZ;
        }

        /** Scans up to {@code columns} chunk columns. Returns whether the pass is complete. */
        boolean step(final Level level, final int columns) {
            for (int scanned = 0; scanned < columns && this.nextChunkZ <= this.maxChunkZ; scanned++) {
                this.scanColumn(level, this.nextChunkX, this.nextChunkZ);
                if (++this.nextChunkX > this.maxChunkX) {
                    this.nextChunkX = this.minChunkX;
                    this.nextChunkZ++;
                }
            }
            return this.nextChunkZ > this.maxChunkZ;
        }

        List<BlockPos> nearest(final int max) {
            this.found.sort(Comparator.comparingDouble(pos -> pos.distSqr(this.center)));
            return List.copyOf(this.found.subList(0, Math.min(Math.max(0, max), this.found.size())));
        }

        private void scanColumn(final Level level, final int chunkX, final int chunkZ) {
            if (!level.hasChunk(chunkX, chunkZ)) {
                return;
            }
            LevelChunk chunk = level.getChunk(chunkX, chunkZ);
            int minSection = Math.max(
                    level.getMinSectionY(), SectionPos.blockToSectionCoord(this.center.getY() - this.vertical));
            int maxSection = Math.min(
                    level.getMaxSectionY(), SectionPos.blockToSectionCoord(this.center.getY() + this.vertical));
            for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
                LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sectionY));
                if (section.hasOnlyAir() || !section.maybeHas(this.wanted)) {
                    continue;
                }
                this.scanSection(section, chunkX, sectionY, chunkZ);
            }
        }

        private void scanSection(
                final LevelChunkSection section, final int chunkX, final int sectionY, final int chunkZ) {
            int baseX = SectionPos.sectionToBlockCoord(chunkX);
            int baseY = SectionPos.sectionToBlockCoord(sectionY);
            int baseZ = SectionPos.sectionToBlockCoord(chunkZ);
            for (int y = 0; y < 16; y++) {
                if (Math.abs(baseY + y - this.center.getY()) > this.vertical) {
                    continue;
                }
                for (int z = 0; z < 16; z++) {
                    if (Math.abs(baseZ + z - this.center.getZ()) > this.horizontal) {
                        continue;
                    }
                    for (int x = 0; x < 16; x++) {
                        if (Math.abs(baseX + x - this.center.getX()) <= this.horizontal
                                && this.wanted.test(section.getBlockState(x, y, z))) {
                            this.found.add(new BlockPos(baseX + x, baseY + y, baseZ + z));
                        }
                    }
                }
            }
        }
    }
}
