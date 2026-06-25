package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class ClickTpModule extends Module {

    private final BooleanSetting requireSneak = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("require_sneak")
            .name("Require Sneak")
            .defaultValue(true)
            .build()));

    public ClickTpModule() {
        super("click_tp", "Click TP", ModuleCategory.MOVEMENT);
    }

    @Override
    public boolean mouseClick(final Minecraft client, final MouseButtonInfo buttonInfo, final int action) {
        if (buttonInfo.button() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE || action != GLFW.GLFW_PRESS) {
            return false;
        }
        LocalPlayer player = client.player;
        if (player == null || client.getConnection() == null || client.hitResult == null
                || client.hitResult.getType() != HitResult.Type.BLOCK
                || this.requireSneak.value() && !client.options.keyShift.isDown()) {
            return false;
        }
        Vec3 target = ((BlockHitResult) client.hitResult).getLocation().add(0.0, 0.15, 0.0);
        player.setPos(target);
        client.getConnection().send(new ServerboundMovePlayerPacket.Pos(target, player.onGround(), player.horizontalCollision));
        return true;
    }
}
