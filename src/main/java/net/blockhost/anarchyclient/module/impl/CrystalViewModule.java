package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

public final class CrystalViewModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(24.0)
            .min(4.0)
            .max(96.0)
            .step(4.0)
            .build()));

    public CrystalViewModule() {
        super("crystal_view", "Crystal View", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        double rangeSqr = this.range.value() * this.range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal && entity.distanceToSqr(client.player) <= rangeSqr) {
                MarkerManager.put(new CuboidMarker("crystal_view:" + entity.getId(), entity.getBoundingBox(),
                        MarkerStyle.CYAN, 4));
            }
        }
    }
}
