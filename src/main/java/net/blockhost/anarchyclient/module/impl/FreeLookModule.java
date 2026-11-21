package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class FreeLookModule extends Module {

    private static FreeLookModule active;

    private final BooleanSetting lockPlayer = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("lock_player")
            .name("Lock Player")
            .defaultValue(true)
            .description(
                    "When on, the mouse turns only the camera. When off, the mouse turns both, but the camera ignores rotations from other modules.")
            .build()));
    private float cameraYaw;
    private float cameraPitch;
    private boolean initialized;

    public FreeLookModule() {
        super(
                "free_look",
                "Free Look",
                ModuleCategory.RENDER,
                "Lets you rotate the camera independently of the player's facing direction.");
    }

    @Override
    protected void onEnable() {
        // The player may not exist yet (enabled from config at startup), so take its facing on first use.
        this.initialized = false;
        active = this;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
    }

    /** Returns the yaw the camera follows: the free-look yaw for the local player, else {@code vanillaYaw}. */
    public static float viewYaw(final Entity entity, final float vanillaYaw) {
        FreeLookModule module = active;
        if (module == null || !(entity instanceof LocalPlayer player)) {
            return vanillaYaw;
        }
        module.initialize(player);
        return module.cameraYaw;
    }

    /** Returns the pitch the camera follows: the free-look pitch for the local player, else {@code vanillaPitch}. */
    public static float viewPitch(final Entity entity, final float vanillaPitch) {
        FreeLookModule module = active;
        if (module == null || !(entity instanceof LocalPlayer player)) {
            return vanillaPitch;
        }
        module.initialize(player);
        return module.cameraPitch;
    }

    private void initialize(final LocalPlayer player) {
        if (!this.initialized) {
            this.cameraYaw = player.getYRot();
            this.cameraPitch = player.getXRot();
            this.initialized = true;
        }
    }

    public static boolean handleTurn(
            final Minecraft client, final LocalPlayer player, final double xOffset, final double yOffset) {
        FreeLookModule module = active;
        if (module == null || client.gui.screen() != null || player == null) {
            return false;
        }
        // The camera always follows the mouse; only a locked player is kept from turning with it.
        module.initialize(player);
        module.cameraYaw += (float) xOffset * 0.15F;
        module.cameraPitch = clampedPitch(module.cameraPitch + (float) yOffset * 0.15F);
        return module.lockPlayer.value();
    }

    static float clampedPitch(final float pitch) {
        return Mth.clamp(pitch, -90.0F, 90.0F);
    }
}
