package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class TrueSightModule extends Module {

    private final BooleanSetting players = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("players")
            .name("Players")
            .defaultValue(true)
            .build()));
    private final BooleanSetting mobs = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("mobs")
            .name("Mobs")
            .defaultValue(true)
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(96.0)
            .min(8.0)
            .max(256.0)
            .step(8.0)
            .build()));

    public TrueSightModule() {
        super("true_sight", "True Sight", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        double rangeSqr = this.range.value() * this.range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living
                    && entity != client.player
                    && entity.isInvisible()
                    && client.player.distanceToSqr(entity) <= rangeSqr
                    && this.shouldOutline(living)) {
                EspOutlineRegistry.set(entity.getId(), 0xAA66CCFF);
            }
        }
    }

    private boolean shouldOutline(final LivingEntity entity) {
        return entity instanceof Player ? this.players.value() : this.mobs.value();
    }
}
