package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public final class ItemChamsModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(32.0)
            .min(4.0)
            .max(128.0)
            .step(4.0)
            .build()));

    public ItemChamsModule() {
        super("item_chams", "Item Chams", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        double rangeSqr = this.range.value() * this.range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity && entity.distanceToSqr(client.player) <= rangeSqr) {
                MarkerManager.put(new CuboidMarker("item_chams:" + entity.getId(), entity.getBoundingBox().inflate(0.05),
                        MarkerStyle.CYAN, 4));
            }
        }
    }
}
