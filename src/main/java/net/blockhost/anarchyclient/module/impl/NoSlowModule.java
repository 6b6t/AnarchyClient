package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.mixin.ClientInputAccessor;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

public final class NoSlowModule extends Module {

    private final BooleanSetting consume = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("consume")
            .name("Consume")
            .defaultValue(true)
            .build()));
    private final BooleanSetting bows = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("bows")
            .name("Bows")
            .defaultValue(true)
            .build()));
    private final BooleanSetting shields = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("shields")
            .name("Shields")
            .defaultValue(true)
            .build()));

    public NoSlowModule() {
        super("no_slow", "No Slow", ModuleCategory.MOVEMENT);
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        LocalPlayer player = client.player;
        if (player == null || input == null || !player.isUsingItem() || !this.shouldBypass(player.getUseItem())) {
            return;
        }
        ((ClientInputAccessor) input).anarchyclient$setMoveVector(InputStates.moveVector(input.keyPresses));
    }

    private boolean shouldBypass(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (this.consume.value() && stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
            return true;
        }
        if (this.bows.value() && (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem)) {
            return true;
        }
        return this.shields.value() && stack.getItem() instanceof ShieldItem;
    }
}
