package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public final class FastExpModule extends Module {

    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    );

    private final NumberSetting minDurabilityPercent = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_durability_percent")
            .name("Min %")
            .defaultValue(80.0)
            .min(1.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(1.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final NumberSetting maxThrows = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_throws")
            .name("Throws")
            .defaultValue(32.0)
            .min(1.0)
            .max(256.0)
            .step(1.0)
            .build()));
    private final BooleanSetting requireMending = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("require_mending")
            .name("Mending")
            .defaultValue(true)
            .build()));
    private final BooleanSetting pauseInCombat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_combat")
            .name("Combat")
            .defaultValue(true)
            .build()));
    private int cooldownTicks;
    private int throwsThisRun;

    public FastExpModule() {
        super("fast_exp", "Fast EXP", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.cooldownTicks-- > 0) {
            return;
        }
        if (this.pauseInCombat.value() && player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        if (!needsRepair(player, this.minDurabilityPercent.value(), this.requireMending.value())
                || this.throwsThisRun >= this.maxThrows.value().intValue()) {
            this.throwsThisRun = 0;
            return;
        }
        if (SilentHotbar.selectMatching(player, this.id(), stack -> stack.is(Items.EXPERIENCE_BOTTLE),
                SilentHotbar.PRIORITY_LIFE, 3, true).isEmpty()) {
            return;
        }
        RotationManager.request(new RotationRequest(
                this.id(),
                new Rotation(player.getYRot(), 90.0F),
                80,
                90.0F,
                2,
                2.0F,
                RotationTurnMode.STEPPED,
                true
        ));
        RotationManager.apply(player);
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        this.throwsThisRun++;
        this.cooldownTicks = this.delay.value().intValue();
    }

    @Override
    protected void onDisable() {
        this.cooldownTicks = 0;
        this.throwsThisRun = 0;
        SilentHotbar.clear(this.id());
        RotationManager.clear(this.id());
    }

    static boolean needsRepair(final LocalPlayer player, final double minDurabilityPercent, final boolean requireMending) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isDamageableItem()
                    && EXPThrowerModule.durabilityPercent(stack) <= minDurabilityPercent
                    && (!requireMending || EquipmentScorer.hasEnchantment(stack, Enchantments.MENDING))) {
                return true;
            }
        }
        return false;
    }
}
