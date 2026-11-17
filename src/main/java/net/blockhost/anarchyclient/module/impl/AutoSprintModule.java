package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

public final class AutoSprintModule extends Module {

    private static AutoSprintModule active;

    private final BooleanSetting keepSprint = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("keep_sprint")
            .name("Keep Sprint")
            .defaultValue(true)
            .build()));
    private final BooleanSetting omniSprint = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("omni_sprint")
            .name("Omni")
            .defaultValue(false)
            .build()));
    private final BooleanSetting allowUsingItem = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("allow_using_item")
            .name("Use Item")
            .defaultValue(false)
            .build()));

    public AutoSprintModule() {
        super("auto_sprint", "Auto Sprint", ModuleCategory.MOVEMENT, "Keeps sprinting active when movement conditions allow it.", List.of("sprint"));
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
    }

    /**
     * Whether vanilla should treat {@code player} as having forward impulse for sprinting. Vanilla stops
     * sprinting without forward input in {@code LocalPlayer.shouldStopRunSprinting}, so omni sprint needs a
     * mixin there that calls this.
     */
    public static boolean omniSprintImpulse(final LocalPlayer player) {
        AutoSprintModule module = active;
        return module != null && module.omniSprint.value() && player != null && player.input != null
                && InputStates.moving(player.input.keyPresses);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || player.input == null) {
            return;
        }
        boolean moving = this.omniSprint.value()
                ? InputStates.moving(player.input.keyPresses)
                : player.input.hasForwardImpulse();
        boolean blocked = player.isCrouching()
                || player.isInWater()
                || !this.allowUsingItem.value() && player.isUsingItem();
        if ((moving || this.keepSprint.value() && player.isSprinting()) && !blocked) {
            player.setSprinting(true);
        }
    }
}
