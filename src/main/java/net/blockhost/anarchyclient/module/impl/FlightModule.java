package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class FlightModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Velocity")
            .addAllOptions(List.of("Velocity", "Creative", "Glide", "Hover", "Damage"))
            .build()));
    private final NumberSetting horizontal = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("horizontal")
            .name("Horizontal")
            .defaultValue(0.45)
            .min(0.05)
            .max(2.5)
            .step(0.05)
            .build()));
    private final NumberSetting vertical = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical")
            .name("Vertical")
            .defaultValue(0.35)
            .min(0.05)
            .max(2.5)
            .step(0.05)
            .build()));
    private boolean changedAbilities;

    public FlightModule() {
        super("flight", "Flight", ModuleCategory.MOVEMENT, List.of("full_flight"));
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        if ("Creative".equals(this.mode.value())) {
            Abilities abilities = player.getAbilities();
            if (!abilities.mayfly || !abilities.flying) {
                this.changedAbilities = !abilities.instabuild;
                abilities.mayfly = true;
                abilities.flying = true;
                player.onUpdateAbilities();
            }
            return;
        }
        this.clearAbilities(player);
        Vec3 horizontalVelocity = MovementVelocity.fromKeys(client, player.getYRot(), this.horizontal.value());
        double y = switch (this.mode.value()) {
            case "Glide" -> -0.03;
            case "Hover" -> player.getDeltaMovement().y * 0.2;
            case "Damage" -> player.hurtTime > 0 ? this.vertical.value() : -0.02;
            default -> 0.0;
        };
        if (client.options.keyJump.isDown()) {
            y += this.vertical.value();
        }
        if (client.options.keyShift.isDown()) {
            y -= this.vertical.value();
        }
        player.setDeltaMovement(horizontalVelocity.x, y, horizontalVelocity.z);
        player.resetFallDistance();
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            this.clearAbilities(client.player);
        }
    }

    private void clearAbilities(final LocalPlayer player) {
        if (!this.changedAbilities) {
            return;
        }
        Abilities abilities = player.getAbilities();
        abilities.mayfly = false;
        abilities.flying = false;
        player.onUpdateAbilities();
        this.changedAbilities = false;
    }
}
