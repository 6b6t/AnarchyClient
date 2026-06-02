package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class AutoWeaponModule extends Module {

    private final BooleanSetting onlyInCombat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("only_in_combat")
            .name("Combat Only")
            .defaultValue(true)
            .build()));

    public AutoWeaponModule() {
        super("auto_weapon", "Auto Weapon", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.screen != null) {
            return;
        }
        if (this.onlyInCombat.value() && !client.options.keyAttack.isDown()) {
            return;
        }

        Inventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        int bestSlot = bestHotbarWeapon(inventory);
        if (bestSlot != selected && bestSlot >= 0) {
            InventoryActions.selectHotbarSlot(player, bestSlot);
        }
    }

    static int bestHotbarWeapon(final Inventory inventory) {
        int bestSlot = -1;
        double bestScore = 0.0;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            double score = EquipmentScorer.weaponScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }
}
