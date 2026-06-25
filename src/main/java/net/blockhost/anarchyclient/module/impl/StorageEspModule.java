package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StorageEspModule extends Module {

    enum Category { REGULAR, SHULKER, ENDER }

    record MergedBox(AABB box, Category category) {
    }

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Box")
            .addAllOptions(List.of("Box", "Filled"))
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(96.0)
            .min(16.0)
            .max(192.0)
            .step(8.0)
            .build()));
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(180.0)
            .min(40.0)
            .max(255.0)
            .step(5.0)
            .build()));
    private final BooleanSetting shulkers = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("shulkers")
            .name("Shulkers")
            .defaultValue(true)
            .build()));
    private List<MergedBox> cachedBoxes = List.of();
    private int scanCooldownTicks;

    public StorageEspModule() {
        super("storage_esp", "Storage ESP", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.scanCooldownTicks > 0) {
            this.scanCooldownTicks--;
            return;
        }
        this.cachedBoxes = this.scan(client);
        this.scanCooldownTicks = 20;
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
        int alpha = this.opacity.value().intValue();
        boolean filled = "Filled".equals(this.mode.value());
        for (MergedBox box : this.cachedBoxes) {
            AABB moved = box.box().move(camera.scale(-1));
            WorldLineRenderer.Color color = color(box.category(), alpha);
            if (filled) {
                WorldLineRenderer.fillNoDepth(matrices, submits, moved,
                        new WorldLineRenderer.Color(color.red(), color.green(), color.blue(), Math.max(40, alpha / 3)));
            }
            WorldLineRenderer.boxNoDepth(matrices, submits, moved, color);
        }
    }

    private List<MergedBox> scan(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return List.of();
        }
        int chunkRadius = Math.max(1, (int) Math.ceil(this.range.value() / 16.0));
        double rangeSqr = this.range.value() * this.range.value();
        ChunkPos center = player.chunkPosition();
        List<BlockPos> regular = new ArrayList<>();
        List<BlockPos> shulker = new ArrayList<>();
        List<BlockPos> ender = new ArrayList<>();
        for (int chunkX = center.x() - chunkRadius; chunkX <= center.x() + chunkRadius; chunkX++) {
            for (int chunkZ = center.z() - chunkRadius; chunkZ <= center.z() + chunkRadius; chunkZ++) {
                if (!client.level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                LevelChunk chunk = client.level.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (Vec3.atCenterOf(blockEntity.getBlockPos()).distanceToSqr(player.position()) > rangeSqr) {
                        continue;
                    }
                    if (blockEntity instanceof ShulkerBoxBlockEntity) {
                        if (this.shulkers.value()) {
                            shulker.add(blockEntity.getBlockPos().immutable());
                        }
                    } else if (blockEntity instanceof EnderChestBlockEntity) {
                        ender.add(blockEntity.getBlockPos().immutable());
                    } else if (isRegularStorage(blockEntity)) {
                        regular.add(blockEntity.getBlockPos().immutable());
                    }
                }
            }
        }
        List<MergedBox> boxes = new ArrayList<>();
        merge(regular, Category.REGULAR, boxes);
        merge(shulker, Category.SHULKER, boxes);
        merge(ender, Category.ENDER, boxes);
        return List.copyOf(boxes);
    }

    private static boolean isRegularStorage(final BlockEntity blockEntity) {
        return blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof BaseContainerBlockEntity;
    }

    static List<AABB> mergePositions(final List<BlockPos> positions) {
        List<MergedBox> boxes = new ArrayList<>();
        merge(positions, Category.REGULAR, boxes);
        return boxes.stream().map(MergedBox::box).toList();
    }

    private static void merge(final List<BlockPos> positions, final Category category, final List<MergedBox> out) {
        if (positions.isEmpty()) {
            return;
        }
        Set<Long> remaining = new HashSet<>(positions.size());
        for (BlockPos position : positions) {
            remaining.add(position.asLong());
        }
        List<BlockPos> sorted = new ArrayList<>(positions);
        sorted.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getZ)
                .thenComparingInt(BlockPos::getX));
        for (BlockPos start : sorted) {
            if (!remaining.contains(start.asLong())) {
                continue;
            }
            int x0 = start.getX();
            int y0 = start.getY();
            int z0 = start.getZ();
            int x1 = x0;
            while (remaining.contains(BlockPos.asLong(x1 + 1, y0, z0))) {
                x1++;
            }
            int z1 = z0;
            while (rowPresent(remaining, x0, x1, y0, z1 + 1)) {
                z1++;
            }
            int y1 = y0;
            while (planePresent(remaining, x0, x1, y1 + 1, z0, z1)) {
                y1++;
            }

            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    for (int x = x0; x <= x1; x++) {
                        remaining.remove(BlockPos.asLong(x, y, z));
                    }
                }
            }
            out.add(new MergedBox(new AABB(x0, y0, z0, x1 + 1, y1 + 1, z1 + 1), category));
        }
    }

    private static boolean rowPresent(final Set<Long> positions, final int x0, final int x1, final int y, final int z) {
        for (int x = x0; x <= x1; x++) {
            if (!positions.contains(BlockPos.asLong(x, y, z))) {
                return false;
            }
        }
        return true;
    }

    private static boolean planePresent(final Set<Long> positions, final int x0, final int x1, final int y, final int z0,
                                        final int z1) {
        for (int z = z0; z <= z1; z++) {
            if (!rowPresent(positions, x0, x1, y, z)) {
                return false;
            }
        }
        return true;
    }

    private static WorldLineRenderer.Color color(final Category category, final int alpha) {
        return switch (category) {
            case SHULKER -> new WorldLineRenderer.Color(190, 116, 255, alpha);
            case ENDER -> new WorldLineRenderer.Color(88, 255, 180, alpha);
            case REGULAR -> new WorldLineRenderer.Color(255, 196, 76, alpha);
        };
    }
}
