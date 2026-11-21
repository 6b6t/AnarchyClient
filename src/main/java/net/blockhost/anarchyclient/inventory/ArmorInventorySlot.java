package net.blockhost.anarchyclient.inventory;

import java.util.OptionalInt;
import net.minecraft.world.entity.EquipmentSlot;

public record ArmorInventorySlot(EquipmentSlot equipmentSlot) implements InventorySlotRef {

    public ArmorInventorySlot {
        if (InventorySlots.armorMenuSlot(equipmentSlot) < 0) {
            throw new IllegalArgumentException("Invalid armor slot: " + equipmentSlot);
        }
    }

    @Override
    public int menuSlot() {
        return InventorySlots.armorMenuSlot(this.equipmentSlot);
    }

    @Override
    public OptionalInt inventorySlot() {
        return OptionalInt.empty();
    }
}
