package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

public final class SwordBlockModule extends Module {

    private final BooleanSetting preferShield = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("prefer_shield")
            .name("Shield")
            .defaultValue(true)
            .build()));
    private boolean usingShield;

    public SwordBlockModule() {
        super("sword_block", "Sword Block", ModuleCategory.COMBAT);
    }

    @Override
    public boolean itemUse(final Minecraft client, final InteractionHand hand) {
        if (this.usingShield || !this.preferShield.value() || client.player == null || client.gameMode == null) {
            return false;
        }
        Optional<InteractionHand> shield = SilentHotbar.usableHand(client.player, this.id(),
                stack -> stack.is(Items.SHIELD), SilentHotbar.PRIORITY_COMBAT, 4, true);
        shield.ifPresent(value -> {
            this.usingShield = true;
            try {
                client.gameMode.useItem(client.player, value);
            } finally {
                this.usingShield = false;
            }
        });
        return false;
    }

    static boolean canBlock(final ItemStack stack) {
        return !stack.isEmpty() && (AutoClickerModule.isWeapon(stack) || stack.is(Items.SHIELD));
    }
}
