package net.blockhost.anarchyclient.module.impl;

import java.util.Optional;
import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class AutoFireworkModule extends Module {

    private final NumberSetting minSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_speed")
            .name("Min Speed")
            .defaultValue(20.0)
            .min(0.0)
            .max(60.0)
            .step(1.0)
            .description("Fires a rocket when your gliding speed drops below this many blocks per second.")
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(30.0)
            .min(5.0)
            .max(200.0)
            .step(5.0)
            .description("Minimum ticks between two rockets.")
            .build()));
    private final BooleanSetting climbOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("climb_only")
            .name("Climb Only")
            .defaultValue(false)
            .description("Only fires while you look upward.")
            .build()));
    private final BooleanSetting keepDurability = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("keep_durability")
            .name("Keep Elytra")
            .defaultValue(true)
            .description("Stops firing when your chest item has 10 or less durability left.")
            .build()));
    private int cooldown;

    public AutoFireworkModule() {
        super(
                "auto_firework",
                "Auto Firework",
                ModuleCategory.MOVEMENT,
                "Uses a firework rocket from your hotbar or offhand while you glide with an elytra and slow down, "
                        + "so you keep flying without clicking.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (player == null
                || client.gameMode == null
                || !player.isFallFlying()
                || this.cooldown > 0
                || client.gui.screen() != null
                || player.isUsingItem()) {
            return;
        }
        if (this.climbOnly.value() && player.getXRot() > -5.0F) {
            return;
        }
        if (this.keepDurability.value() && nearlyBroken(player.getItemBySlot(EquipmentSlot.CHEST))) {
            return;
        }
        if (!shouldFire(blocksPerSecond(player.getDeltaMovement()), this.minSpeed.value())) {
            return;
        }
        Optional<InteractionHand> hand = SilentHotbar.usableHand(
                player, this.id(), stack -> stack.is(Items.FIREWORK_ROCKET), SilentHotbar.PRIORITY_NORMAL, 2, true);
        if (hand.isEmpty()) {
            this.debugValue("state", "no rockets");
            return;
        }
        client.gameMode.useItem(player, hand.orElseThrow());
        this.cooldown = this.delay.value().intValue();
    }

    static boolean nearlyBroken(final ItemStack stack) {
        return stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= 10;
    }

    static double blocksPerSecond(final Vec3 motion) {
        return motion.length() * 20.0;
    }

    static boolean shouldFire(final double blocksPerSecond, final double minSpeed) {
        return blocksPerSecond < minSpeed;
    }

    @Override
    protected void onDisable() {
        this.cooldown = 0;
    }
}
