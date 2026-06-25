package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public final class ItemPhysicsModule extends Module {

    private final BooleanSetting floatItems = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("float_items")
            .name("Float")
            .defaultValue(false)
            .build()));

    public ItemPhysicsModule() {
        super("item_physics", "Item Physics", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.level == null) {
            return;
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item) {
                item.setNoGravity(this.floatItems.value());
            }
        }
    }
}
