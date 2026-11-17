package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ItemPhysicsModule extends Module {

    private static PhysicsTransform activeTransform = PhysicsTransform.identity();

    private Set<Integer> floatedItems = new HashSet<>();

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
        super("item_physics", "Item Physics", ModuleCategory.RENDER,
                "Renders dropped items flat, spinning, or bobbing, and can stop them falling on your client until turned off.");
    }

    @Override
    public void tick(final Minecraft client) {
        activeTransform = new PhysicsTransform(this.mode.value(), this.spinSpeed.value(), this.scale.value());
        if (client.level == null) {
            this.floatedItems.clear();
            return;
        }
        if (!this.floatItems.value()) {
            this.restoreGravity(client.level);
            return;
        }
        // Rebuild the set each tick so items that despawned or were picked up drop out of it.
        Set<Integer> floating = new HashSet<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            // Only take over items that fall, so items the server already made weightless keep that state on restore.
            if (entity instanceof ItemEntity item && (this.floatedItems.contains(item.getId()) || !item.isNoGravity())) {
                item.setNoGravity(true);
                floating.add(item.getId());
            }
        }
        this.floatedItems = floating;
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.floatedItems.clear();
    }

    @Override
    protected void onDisable() {
        activeTransform = PhysicsTransform.identity();
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.level != null) {
            this.restoreGravity(client.level);
        } else {
            this.floatedItems.clear();
        }
    }

    private void restoreGravity(final ClientLevel level) {
        for (int id : this.floatedItems) {
            if (level.getEntity(id) instanceof ItemEntity item) {
                item.setNoGravity(false);
            }
        }
        this.floatedItems.clear();
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
