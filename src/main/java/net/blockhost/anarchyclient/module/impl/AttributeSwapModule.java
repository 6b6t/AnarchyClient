package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class AttributeSwapModule extends Module {

    private final BooleanSetting restoreSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("restore_slot")
            .name("Restore")
            .defaultValue(true)
            .build()));
    private int previousSlot = -1;

    public AttributeSwapModule() {
        super("attribute_swap", "Attribute Swap", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gui.screen() != null) {
            return;
        }
        if (client.options.keyAttack.isDown()) {
            int selected = player.getInventory().getSelectedSlot();
            int best = AutoWeaponModule.bestHotbarWeapon(player.getInventory());
            if (best >= 0 && best != selected) {
                if (this.previousSlot < 0) {
                    this.previousSlot = selected;
                }
                InventoryActions.selectHotbarSlot(player, best);
            }
            return;
        }
        if (this.restoreSlot.value() && this.previousSlot >= 0) {
            InventoryActions.selectHotbarSlot(player, this.previousSlot);
        }
        this.previousSlot = -1;
    }

    @Override
    protected void onDisable() {
        this.previousSlot = -1;
    }
}
