package net.blockhost.anarchyclient.module.impl;

import java.util.List;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class HitboxModule extends Module {

    private static double activeExpansion;

    private final NumberSetting expansion = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("expansion")
            .name("Expansion")
            .defaultValue(0.4)
            .min(0.0)
            .max(3.0)
            .step(0.05)
            .build()));

    public HitboxModule() {
        super(
                "hitbox",
                "Hitbox",
                ModuleCategory.COMBAT,
                "Enlarges living entity hitboxes for your crosshair and adds the same extra range to client targeting.",
                List.of("hitboxes"));
    }

    @Override
    public void tick(final Minecraft client) {
        activeExpansion = this.expansion.value();
    }

    @Override
    protected void onDisable() {
        activeExpansion = 0.0;
    }

    public static double rangeBonus() {
        return activeExpansion;
    }

    public static float pickRadiusBonus(final Entity entity) {
        return pickRadiusBonus(entity instanceof LivingEntity && entity.level().isClientSide(), activeExpansion);
    }

    // Only client-side living entities grow, so the integrated server's projectile hits stay vanilla.
    static float pickRadiusBonus(final boolean clientLivingEntity, final double expansion) {
        return clientLivingEntity && expansion > 0.0 ? (float) expansion : 0.0F;
    }
}
