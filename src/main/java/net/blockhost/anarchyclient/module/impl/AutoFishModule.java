package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class AutoFishModule extends Module {

    private final NumberSetting minHookTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_hook_ticks")
            .name("Min Ticks")
            .defaultValue(25.0)
            .min(5.0)
            .max(100.0)
            .step(5.0)
            .build()));
    private final BooleanSetting autoCast = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("auto_cast")
            .name("Auto Cast")
            .defaultValue(true)
            .build()));
    private int recastCooldownTicks;

    public AutoFishModule() {
        super("auto_fish", "Auto Fish", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (!player.getMainHandItem().is(Items.FISHING_ROD) && !player.getOffhandItem().is(Items.FISHING_ROD)) {
            return;
        }
        InteractionHand hand = player.getMainHandItem().is(Items.FISHING_ROD) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (player.fishing == null) {
            if (this.autoCast.value() && this.isRecastReady()) {
                client.gameMode.useItem(player, hand);
                this.resetRecastCooldown();
            }
            return;
        }

        if (player.fishing.tickCount >= this.minHookTicks.value()
                && player.fishing.getDeltaMovement().y < -0.08
                && player.fishing.isInWater()) {
            client.gameMode.useItem(player, hand);
            this.resetRecastCooldown();
        }
    }

    boolean isRecastReady() {
        if (this.recastCooldownTicks > 0) {
            this.recastCooldownTicks--;
            return false;
        }
        return true;
    }

    void resetRecastCooldown() {
        this.recastCooldownTicks = 10;
    }
}
