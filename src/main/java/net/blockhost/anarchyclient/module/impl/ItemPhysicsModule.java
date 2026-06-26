package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;

public final class ItemPhysicsModule extends Module {

    private static PhysicsTransform activeTransform = PhysicsTransform.identity();

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Flat")
            .addAllOptions(List.of("Flat", "Spin", "Float"))
            .build()));
    private final BooleanSetting floatItems = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("float_items")
            .name("Float")
            .defaultValue(false)
            .build()));
    private final NumberSetting spinSpeed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("spin_speed")
            .name("Spin")
            .defaultValue(2.0)
            .min(0.0)
            .max(12.0)
            .step(0.5)
            .build()));
    private final NumberSetting scale = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("scale")
            .name("Scale")
            .defaultValue(1.0)
            .min(0.25)
            .max(2.0)
            .step(0.05)
            .build()));

    public ItemPhysicsModule() {
        super("item_physics", "Item Physics", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        activeTransform = new PhysicsTransform(this.mode.value(), this.spinSpeed.value(), this.scale.value());
        if (client.level == null) {
            return;
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity item) {
                item.setNoGravity(this.floatItems.value());
            }
        }
    }

    @Override
    protected void onDisable() {
        activeTransform = PhysicsTransform.identity();
    }

    public static PhysicsTransform activeTransform() {
        return activeTransform;
    }

    public record PhysicsTransform(String mode, double spinSpeed, double scale) {

        static PhysicsTransform identity() {
            return new PhysicsTransform("Vanilla", 0.0, 1.0);
        }

        public boolean identityTransform() {
            return "Vanilla".equals(this.mode) && this.scale == 1.0;
        }
    }
}
