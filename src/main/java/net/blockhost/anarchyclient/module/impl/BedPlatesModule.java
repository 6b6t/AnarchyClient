package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.AABB;

public final class BedPlatesModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(20.0)
            .min(4.0)
            .max(64.0)
            .step(4.0)
            .build()));

    public BedPlatesModule() {
        super("bed_plates", "Bed Plates", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        BlockPos center = client.player.blockPosition();
        int radius = this.range.value().intValue();
        BlockPos.betweenClosed(center.offset(-radius, -8, -radius), center.offset(radius, 8, radius)).forEach(pos -> {
            if (client.level.getBlockState(pos).getBlock() instanceof BedBlock) {
                MarkerManager.put(new CuboidMarker("bed_plates:" + pos.asLong(), new AABB(pos), MarkerStyle.CYAN, 12));
            }
        });
    }
}
