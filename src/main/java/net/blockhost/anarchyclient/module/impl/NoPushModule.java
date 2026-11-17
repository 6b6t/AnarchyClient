package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class NoPushModule extends Module {

    private static NoPushModule active;

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
        super("no_push", "No Push", ModuleCategory.MOVEMENT,
                "Stops other entities, and optionally water and lava currents, from pushing the player around.");
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
    }

    public static boolean shouldCancelPush(final Minecraft client, final Entity entity) {
        NoPushModule module = active;
        return module != null && module.entities.value() && isLocalPlayer(client, entity);
    }

    public static boolean shouldCancelFluidPush(final Minecraft client, final Entity entity) {
        NoPushModule module = active;
        return module != null && module.fluids.value() && isLocalPlayer(client, entity);
    }

    private static boolean isLocalPlayer(final Minecraft client, final Entity entity) {
        return client != null && client.player != null && entity == client.player;
    }
}
