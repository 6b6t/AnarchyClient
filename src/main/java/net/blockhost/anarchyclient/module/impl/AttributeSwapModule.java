package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class AttributeSwapModule extends Module {

    private final BooleanSetting restoreSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("restore_slot")
            .name("Restore")
            .defaultValue(true)
            .build()));
    private int previousSlot = -1;

    public AttributeSwapModule() {
        super("attribute_swap", "Attribute Swap", ModuleCategory.COMBAT,
                "Switches to the best hotbar weapon when you hit an entity and switches back on the next tick.");
    }

    @Override
    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        if (!(player instanceof LocalPlayer localPlayer) || client.gui.screen() != null) {
            return false;
        }
        int selected = localPlayer.getInventory().getSelectedSlot();
        int best = AutoWeaponModule.bestHotbarWeapon(localPlayer.getInventory());
        // The attack packet follows this hook and syncs the carried item first, so the hit uses the new slot.
        if (best >= 0 && best != selected) {
            if (this.previousSlot < 0) {
                this.previousSlot = selected;
            }
            InventoryActions.selectHotbarSlot(localPlayer, best);
        }
        return false;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || this.previousSlot < 0) {
            return;
        }
        if (this.restoreSlot.value()) {
            InventoryActions.selectHotbarSlot(player, this.previousSlot);
        }
        this.previousSlot = -1;
    }

    @Override
    protected void onDisable() {
        this.previousSlot = -1;
    }
}
