package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ClickTpModule extends Module {

    private final BooleanSetting requireSneak = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("require_sneak")
            .name("Require Sneak")
            .defaultValue(true)
            .build()));

    public ClickTpModule() {
        super(
                "click_tp",
                "Click TP",
                ModuleCategory.MOVEMENT,
                "Teleports on top of the targeted block when you middle-click, optionally only while sneaking. This can flag anti-cheats.");
    }

    @Override
    public boolean mouseClick(final Minecraft client, final MouseButtonInfo buttonInfo, final int action) {
        if (buttonInfo.button() != InputConstants.MOUSE_BUTTON_MIDDLE || action != InputConstants.PRESS) {
            return false;
        }
        LocalPlayer player = client.player;
        if (player == null
                || client.getConnection() == null
                || client.hitResult == null
                || client.hitResult.getType() != HitResult.Type.BLOCK
                || (this.requireSneak.value() && !client.options.keyShift.isDown())) {
            return false;
        }
        // Stand on top of the clicked block; the raw hit point on a side face is inside the wall.
        Vec3 target = Vec3.atBottomCenterOf(
                ((BlockHitResult) client.hitResult).getBlockPos().above());
        // Move locally only: vanilla sends the position in its next tick, and a second position packet in the
        // same tick gets the player disconnected on 26.3.
        player.setPos(target);
        return true;
    }
}
