package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class FreeLookModule extends Module {

    private static FreeLookModule active;

    private final BooleanSetting lockPlayer = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("lock_player")
            .name("Lock Player")
            .defaultValue(true)
            .build()));
    private float cameraYaw;
    private float cameraPitch;

    public FreeLookModule() {
        super("free_look", "Free Look", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            this.cameraYaw = client.player.getYRot();
            this.cameraPitch = client.player.getXRot();
        }
        active = this;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public CameraTransform cameraTransform(final Minecraft client, final Vec3 position, final float yaw,
                                           final float pitch) {
        return new CameraTransform(position, this.cameraYaw, this.cameraPitch);
    }

    public static boolean handleTurn(final Minecraft client, final LocalPlayer player,
                                     final double xOffset, final double yOffset) {
        FreeLookModule module = active;
        if (module == null || !module.lockPlayer.value() || client.gui.screen() != null || player == null) {
            return false;
        }
        module.cameraYaw += (float) xOffset * 0.15F;
        module.cameraPitch = Mth.clamp(module.cameraPitch + (float) yOffset * 0.15F, -90.0F, 90.0F);
        return true;
    }

    static float clampedPitch(final float pitch) {
        return Mth.clamp(pitch, -90.0F, 90.0F);
    }
}
