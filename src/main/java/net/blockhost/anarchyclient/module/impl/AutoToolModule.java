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
    private final BooleanSetting ignoreDurability = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ignore_durability")
            .name("Ignore Damage")
            .defaultValue(false)
            .build()));
    private final BooleanSetting pauseDuringCombat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_during_combat")
            .name("Combat Pause")
            .defaultValue(false)
            .build()));

    public AutoToolModule() {
        super("auto_tool", "Auto Tool", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gui.screen() != null) {
            return;
        }
        if (this.requireAttack.value() && !client.options.keyAttack.isDown()) {
            return;
        }
        if (this.pauseDuringCombat.value() && player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult hitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockState state = client.level.getBlockState(hitResult.getBlockPos());
        int bestSlot = bestToolSlot(player.getInventory(), state, this.ignoreDurability.value());
        if (bestSlot >= 0) {
            InventoryActions.selectHotbarSlot(player, bestSlot);
        }
    }

    static int bestToolSlot(final Inventory inventory, final BlockState state) {
        return bestToolSlot(inventory, state, false);
    }

    static int bestToolSlot(final Inventory inventory, final BlockState state, final boolean ignoreDurability) {
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.isEmpty()
                    || !ignoreDurability && stack.isDamageableItem() && stack.nextDamageWillBreak()
                    || state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state)) {
                continue;
            }
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }
}
