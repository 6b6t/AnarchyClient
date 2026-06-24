package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.inventory.InventorySlotRef;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class AutoTotemModule extends Module {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Totem")
            .addAllOptions(List.of("Totem", "Crystal", "Golden Apple", "Shield"))
            .build()));
    private final NumberSetting healthThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health_threshold")
            .name("Health")
            .defaultValue(12.0)
            .min(1.0)
            .max(36.0)
            .step(0.5)
            .build()));
    private final BooleanSetting includeAbsorption = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("include_absorption")
            .name("Absorption")
            .defaultValue(true)
            .build()));
    private final BooleanSetting emergencyTotem = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("emergency_totem")
            .name("Emergency")
            .defaultValue(true)
            .build()));
    private final BooleanSetting missingArmorTotem = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("missing_armor_totem")
            .name("Armor Risk")
            .defaultValue(true)
            .build()));
    private final NumberSetting fallDistanceThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("fall_distance_threshold")
            .name("Fall")
            .defaultValue(12.0)
            .min(0.0)
            .max(80.0)
            .step(1.0)
            .build()));
    private final BooleanSetting fireTotem = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("fire_totem")
            .name("Fire")
            .defaultValue(true)
            .build()));
    private final BooleanSetting predictExplosions = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("predict_explosions")
            .name("Explosions")
            .defaultValue(true)
            .build()));
    private final NumberSetting explosionRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("explosion_range")
            .name("Blast Range")
            .defaultValue(6.0)
            .min(2.0)
            .max(12.0)
            .step(0.5)
            .build()));
    private final BooleanSetting switchBack = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("switch_back")
            .name("Switch Back")
            .defaultValue(true)
            .build()));
    private final NumberSetting switchBackDelayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("switch_back_delay_ticks")
            .name("Back Delay")
            .defaultValue(20.0)
            .min(0.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private int switchBackTicks;
    private boolean emergencyWasActive;

    public AutoTotemModule() {
        super("auto_totem", "Auto Totem", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }

        boolean needsEmergencyTotem = this.emergencyTotem.value() && this.needsTotem(player, client.level);
        Item desiredItem = this.desiredItem(needsEmergencyTotem);
        if (this.waitingForSwitchBack(player, desiredItem, needsEmergencyTotem)) {
            return;
        }
        if (player.getOffhandItem().is(desiredItem)) {
            return;
        }

        int inventorySlot = this.findItemSlot(player.getInventory(), desiredItem);
        if (inventorySlot < 0) {
            return;
        }
        InventorySlotRef source = InventorySlots.storageSlot(inventorySlot).orElse(null);
        if (source == null) {
            return;
        }

        InventoryAction action = InventoryAction.pickupSwap(
                source,
                InventorySlots.offhandSlot()
        );
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_LIFE,
                5,
                action
        ));
    }

    private Item desiredItem(final boolean needsEmergencyTotem) {
        if (needsEmergencyTotem) {
            return Items.TOTEM_OF_UNDYING;
        }
        return switch (this.mode.value()) {
            case "Crystal" -> Items.END_CRYSTAL;
            case "Golden Apple" -> Items.GOLDEN_APPLE;
            case "Shield" -> Items.SHIELD;
            default -> Items.TOTEM_OF_UNDYING;
        };
    }

    private boolean waitingForSwitchBack(final LocalPlayer player, final Item desiredItem,
                                         final boolean needsEmergencyTotem) {
        if (needsEmergencyTotem) {
            this.emergencyWasActive = true;
            this.switchBackTicks = this.switchBackDelayTicks.value().intValue();
            return false;
        }
        if (!this.switchBack.value() || desiredItem == Items.TOTEM_OF_UNDYING || !this.emergencyWasActive) {
            this.emergencyWasActive = false;
            return false;
        }
        if (!player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            this.emergencyWasActive = false;
            return false;
        }
        if (this.switchBackTicks > 0) {
            this.switchBackTicks--;
            return true;
        }
        this.emergencyWasActive = false;
        return false;
    }

    private boolean needsTotem(final LocalPlayer player, final ClientLevel level) {
        float health = effectiveHealth(player, this.includeAbsorption.value());
        if (health <= this.healthThreshold.value()) {
            return true;
        }
        if (this.missingArmorTotem.value() && missingArmor(player)) {
            return true;
        }
        if (player.fallDistance >= this.fallDistanceThreshold.value()) {
            return true;
        }
        if (this.fireTotem.value() && player.isOnFire()) {
            return true;
        }
        if (!this.predictExplosions.value() || level == null) {
            return false;
        }
        double predictedDamage = predictedExplosionDamage(level.entitiesForRendering(), player, this.explosionRange.value());
        return predictedDamage >= damageUntilThreshold(health, this.healthThreshold.value());
    }

    static float effectiveHealth(final LocalPlayer player, final boolean includeAbsorption) {
        float health = player.getHealth();
        if (includeAbsorption) {
            health += player.getAbsorptionAmount();
        }
        return health;
    }

    static double damageUntilThreshold(final double health, final double threshold) {
        return Math.max(0.0, health - threshold);
    }

    static boolean missingArmor(final LocalPlayer player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    static double predictedExplosionDamage(final Iterable<? extends Entity> entities, final LocalPlayer player,
                                           final double range) {
        double predictedDamage = 0.0;
        double rangeSqr = range * range;
        for (Entity entity : entities) {
            double maxDamage = maxExplosionDamage(entity);
            if (maxDamage <= 0.0) {
                continue;
            }
            double distanceSqr = player.distanceToSqr(entity);
            if (distanceSqr <= rangeSqr) {
                predictedDamage = Math.max(predictedDamage, estimateExplosionDamage(distanceSqr, range, maxDamage));
            }
        }
        return predictedDamage;
    }

    static double estimateExplosionDamage(final double distanceSqr, final double range, final double maxDamage) {
        if (range <= 0.0 || maxDamage <= 0.0) {
            return 0.0;
        }
        double distance = Math.sqrt(Math.max(0.0, distanceSqr));
        double exposure = Math.max(0.0, 1.0 - distance / range);
        return maxDamage * exposure * exposure;
    }

    private static double maxExplosionDamage(final Entity entity) {
        if (!entity.isAlive()) {
            return 0.0;
        }
        if (entity instanceof EndCrystal || entity instanceof PrimedTnt) {
            return 20.0;
        }
        if (entity instanceof Creeper) {
            return 18.0;
        }
        return 0.0;
    }

    private int findItemSlot(final Inventory inventory, final Item item) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                return slot;
            }
        }
        return -1;
    }

    static int toInventoryMenuSlot(final int inventorySlot) {
        return InventorySlots.toInventoryMenuSlot(inventorySlot);
    }
}
