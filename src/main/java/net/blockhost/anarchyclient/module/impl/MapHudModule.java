package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.util.List;

public final class MapHudModule extends HudElementModule {

    public MapHudModule() {
        super("map_hud", "Map HUD", "Top Left");
    }

    @Override
    protected void renderHudElement(final Minecraft client, final GuiGraphicsExtractor graphics) {
        int size = 104;
        int footer = 20;
        HudPosition position = this.position(graphics, size, size + footer);
        int x = position.x();
        int y = position.y();
        int center = size / 2;
        double range = 64.0;
        double scale = (size - 12) / (range * 2.0);
        BlockPos playerPos = client.player.blockPosition();

        graphics.fill(x, y, x + size, y + size + footer, 0x66000000);
        graphics.outline(x, y, size, size + footer, 0x55FFFFFF);
        graphics.fill(x + center - 1, y + 4, x + center + 1, y + size - 4, 0x33FFFFFF);
        graphics.fill(x + 4, y + center - 1, x + size - 4, y + center + 1, 0x33FFFFFF);

        int chunkX = Math.floorDiv(playerPos.getX(), 16) * 16;
        int chunkZ = Math.floorDiv(playerPos.getZ(), 16) * 16;
        for (int gx = chunkX - 64; gx <= chunkX + 64; gx += 16) {
            int sx = x + center + (int) Math.round((gx - playerPos.getX()) * scale);
            if (sx >= x + 4 && sx <= x + size - 4) {
                graphics.fill(sx, y + 4, sx + 1, y + size - 4, 0x225B7088);
            }
        }
        for (int gz = chunkZ - 64; gz <= chunkZ + 64; gz += 16) {
            int sy = y + center + (int) Math.round((gz - playerPos.getZ()) * scale);
            if (sy >= y + 4 && sy <= y + size - 4) {
                graphics.fill(x + 4, sy, x + size - 4, sy + 1, 0x225B7088);
            }
        }

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == client.player || entity.distanceToSqr(client.player) > range * range) {
                continue;
            }
            int sx = x + center + (int) Math.round((entity.getX() - client.player.getX()) * scale);
            int sy = y + center + (int) Math.round((entity.getZ() - client.player.getZ()) * scale);
            if (sx < x + 4 || sx > x + size - 4 || sy < y + 4 || sy > y + size - 4) {
                continue;
            }
            int color = entity instanceof net.minecraft.world.entity.player.Player ? 0xFF7DD3FC : 0xFFFF7A7A;
            graphics.fill(sx - 1, sy - 1, sx + 2, sy + 2, color);
        }

        graphics.fill(x + center - 2, y + center - 2, x + center + 3, y + center + 3, 0xFFFFFFFF);
        double yaw = Math.toRadians(client.player.getYRot());
        int arrowX = x + center - (int) Math.round(Math.sin(yaw) * 8.0);
        int arrowY = y + center + (int) Math.round(Math.cos(yaw) * 8.0);
        WorldLineRenderer2D.line(graphics, x + center, y + center, arrowX, arrowY, 0xFFECE8E0);

        String chunk = "Chunk " + Math.floorDiv(playerPos.getX(), 16) + ", " + Math.floorDiv(playerPos.getZ(), 16);
        String biome = client.level.getBiome(playerPos).unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("unknown");
        graphics.text(client.font, chunk, x + 5, y + size + 4, this.color(), true);
        graphics.text(client.font, biome, x + 5, y + size + 13, 0xFF9FB0C3, true);
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        BlockPos pos = client.player.blockPosition();
        return List.of(
                "Chunk " + Math.floorDiv(pos.getX(), 16) + ", " + Math.floorDiv(pos.getZ(), 16),
                "Region " + Math.floorDiv(pos.getX(), 512) + ", " + Math.floorDiv(pos.getZ(), 512),
                "Biome " + client.level.getBiome(pos).unwrapKey().map(key -> key.identifier().toString()).orElse("unknown")
        );
    }
}
