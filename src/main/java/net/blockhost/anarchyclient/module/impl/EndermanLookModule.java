package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.EntityHitResult;

public final class EndermanLookModule extends Module {

    private final NumberSetting pitch = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("pitch")
            .name("Pitch")
            .defaultValue(45.0)
            .min(-90.0)
            .max(90.0)
            .step(1.0)
            .build()));

    public EndermanLookModule() {
        super("enderman_look", "Enderman Look", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof EnderMan)) {
            return;
        }
        player.setXRot(this.pitch.value().floatValue());
    }
}
