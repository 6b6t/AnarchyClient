package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class AutoToolModule extends Module {

    private final BooleanSetting requireAttack = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("require_attack")
            .name("Mining Only")
            .defaultValue(true)
            .build()));

    public AutoToolModule() {
        super("auto_tool", "Auto Tool", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.screen != null) {
            return;
        }
        if (this.requireAttack.value() && !client.options.keyAttack.isDown()) {
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult hitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockState state = client.level.getBlockState(hitResult.getBlockPos());
        int bestSlot = bestToolSlot(player.getInventory(), state);
        if (bestSlot >= 0) {
            InventoryActions.selectHotbarSlot(player, bestSlot);
        }
    }

    static int bestToolSlot(final Inventory inventory, final BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            float speed = inventory.getItem(slot).getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }
}
