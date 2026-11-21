package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enderman;

public final class EndermanLookModule extends Module {

    /** Endermen look for staring players within their 64 block follow range. */
    private static final double STARE_RANGE = 64.0;
    /** Cone size vanilla passes to {@code isLookingAtMe} in {@code Enderman.isBeingStaredBy}. */
    private static final double STARE_CONE = 0.025;

    private final NumberSetting pitch = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("pitch")
            .name("Pitch")
            .defaultValue(45.0)
            .min(-90.0)
            .max(90.0)
            .step(1.0)
            .build()));

    public EndermanLookModule() {
        super(
                "enderman_look",
                "Enderman Look",
                ModuleCategory.PLAYER,
                "Tilts your view to a set pitch when you look at an enderman within 64 blocks so it does not turn hostile.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || !LivingEntity.PLAYER_NOT_WEARING_DISGUISE_ITEM.test(player)) {
            return;
        }
        boolean staring =
                client
                        .level
                        .getEntitiesOfClass(
                                Enderman.class, player.getBoundingBox().inflate(STARE_RANGE))
                        .stream()
                        .anyMatch(enderman -> enderman.isAlive()
                                && enderman.distanceToSqr(player) <= STARE_RANGE * STARE_RANGE
                                && enderman.isLookingAtMe(player, STARE_CONE, true, false, enderman.getEyeY()));
        if (staring) {
            player.setXRot(this.pitch.value().floatValue());
        }
    }
}
