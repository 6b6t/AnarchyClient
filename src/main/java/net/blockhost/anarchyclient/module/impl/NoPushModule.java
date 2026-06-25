package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class NoPushModule extends Module {

    private static boolean active;
    private static boolean activeEntities;

    private final BooleanSetting entities = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("entities")
            .name("Entities")
            .defaultValue(true)
            .build()));
    private final BooleanSetting fluids = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("fluids")
            .name("Fluids")
            .defaultValue(false)
            .build()));

    public NoPushModule() {
        super("no_push", "No Push", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        active = true;
        activeEntities = this.entities.value();
    }

    @Override
    public void tick(final Minecraft client) {
        activeEntities = this.entities.value();
    }

    @Override
    protected void onDisable() {
        active = false;
        activeEntities = false;
    }

    public static boolean shouldCancelPush(final Minecraft client, final Entity entity) {
        return active && activeEntities && client != null && client.player != null && entity == client.player;
    }

    boolean cancelEntities() {
        return this.entities.value();
    }

    boolean cancelFluids() {
        return this.fluids.value();
    }
}
