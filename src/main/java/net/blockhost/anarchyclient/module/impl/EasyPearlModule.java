package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class EasyPearlModule extends Module {

    private final NumberSetting pitch = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("pitch")
            .name("Pitch")
            .defaultValue(80.0)
            .min(-90.0)
            .max(90.0)
            .step(1.0)
            .build()));
    private boolean pending;

    public EasyPearlModule() {
        super("easy_pearl", "Easy Pearl", ModuleCategory.PLAYER);
    }

    @Override
    protected void onEnable() {
        this.pending = true;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (!this.pending || player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (SilentHotbar.usableHand(player, this.id(), stack -> stack.is(Items.ENDER_PEARL),
                SilentHotbar.PRIORITY_COMBAT, 4, true).isPresent()) {
            player.setXRot(this.pitch.value().floatValue());
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            this.pending = false;
            this.enabled(false);
        }
    }
}
