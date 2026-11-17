package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

public final class BedPlatesModule extends Module {

    private static final int VERTICAL_RANGE = 8;
    private static final int SCAN_INTERVAL_TICKS = 10;
    // Outlives one scan interval so markers stay visible until the next scan refreshes them.
    private static final int MARKER_LIFETIME_TICKS = SCAN_INTERVAL_TICKS + 2;

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(20.0)
            .min(4.0)
            .max(64.0)
            .step(4.0)
            .build()));
    private int scanCooldown;

    public BedPlatesModule() {
        super("bed_plates", "Bed Plates", ModuleCategory.RENDER, "Outlines beds, including straw beds, within range with cyan boxes.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            this.scanCooldown = 0;
            return;
        }
        if (this.scanCooldown-- > 0) {
            return;
        }
        this.scanCooldown = SCAN_INTERVAL_TICKS - 1;
        ClientLevel level = client.level;
        BlockPos center = client.player.blockPosition();
        int radius = this.range.value().intValue();
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minY = Math.max(level.getMinY(), center.getY() - VERTICAL_RANGE);
        int maxY = Math.min(level.getMaxY(), center.getY() + VERTICAL_RANGE);
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        for (int chunkX = SectionPos.blockToSectionCoord(minX); chunkX <= SectionPos.blockToSectionCoord(maxX); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(minZ); chunkZ <= SectionPos.blockToSectionCoord(maxZ); chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (int sectionY = SectionPos.blockToSectionCoord(minY); sectionY <= SectionPos.blockToSectionCoord(maxY); sectionY++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
                    // The palette check skips the vast majority of sections, which contain no bed at all.
                    if (section.hasOnlyAir() || !section.maybeHas(BedPlatesModule::isBed)) {
                        continue;
                    }
                    markBeds(section,
                            Math.max(minX, SectionPos.sectionToBlockCoord(chunkX)),
                            Math.max(minY, SectionPos.sectionToBlockCoord(sectionY)),
                            Math.max(minZ, SectionPos.sectionToBlockCoord(chunkZ)),
                            Math.min(maxX, SectionPos.sectionToBlockCoord(chunkX, 15)),
                            Math.min(maxY, SectionPos.sectionToBlockCoord(sectionY, 15)),
                            Math.min(maxZ, SectionPos.sectionToBlockCoord(chunkZ, 15)));
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        this.scanCooldown = 0;
    }

    static boolean isBed(final BlockState state) {
        return state.getBlock() instanceof AbstractBedBlock;
    }

    private static void markBeds(final LevelChunkSection section, final int minX, final int minY, final int minZ,
                                 final int maxX, final int maxY, final int maxZ) {
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isBed(section.getBlockState(x & 15, y & 15, z & 15))) {
                        BlockPos pos = new BlockPos(x, y, z);
                        MarkerManager.put(new CuboidMarker("bed_plates:" + pos.asLong(), new AABB(pos), MarkerStyle.CYAN,
                                MARKER_LIFETIME_TICKS));
                    }
                }
            }
        }
    }
}
