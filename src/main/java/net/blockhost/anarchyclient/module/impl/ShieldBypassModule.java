package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class ShieldBypassModule extends Module {

    private final BooleanSetting ignoreAxes = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ignore_axes")
            .name("Ignore Axes")
            .defaultValue(true)
            .description("Do not bypass when holding an axe.")
            .build()));
    private final BooleanSetting pauseForKillAura = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_for_kill_aura")
            .name("Pause Kill Aura")
            .defaultValue(true)
            .build()));
    private final NumberSetting distance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("distance")
            .name("Distance")
            .defaultValue(0.6)
            .min(0.1)
            .max(2.0)
            .step(0.1)
            .build()));

    public ShieldBypassModule() {
        super(
                "shield_bypass",
                "Shield Bypass",
                ModuleCategory.COMBAT,
                "Hits a shielding target from behind by briefly sending a position past its shield. This can flag anti-cheats.");
    }

    @Override
    public boolean mouseClick(final Minecraft client, final MouseButtonInfo buttonInfo, final int action) {
        ClientPacketListener connection = client.getConnection();
        if (buttonInfo.button() != InputConstants.MOUSE_BUTTON_LEFT || action != InputConstants.PRESS) {
            return false;
        }
        LocalPlayer player = client.player;
        if (player == null
                || client.level == null
                || client.gameMode == null
                || connection == null
                || client.gui.screen() != null
                || player.isUsingItem()) {
            return false;
        }
        if (this.pauseForKillAura.value()
                && AnarchyClient.MODULES.find("kill_aura").map(Module::enabled).orElse(false)) {
            return false;
        }
        if (this.ignoreAxes.value() && player.getMainHandItem().is(ItemTags.AXES)) {
            return false;
        }
        if (!(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)
                || !target.isBlocking()) {
            return false;
        }
        Vec3 original = player.position();
        Vec3 bypass = behindPosition(target.position(), target.getLookAngle(), original.y, this.distance.value());
        if (!client.level.noCollision(player, player.getBoundingBox().move(bypass.subtract(original)))) {
            return false;
        }
        connection.send(new ServerboundMovePlayerPacket.Pos(bypass, player.onGround(), player.horizontalCollision));
        client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        player.resetAttackStrengthTicker();
        connection.send(new ServerboundMovePlayerPacket.Pos(original, player.onGround(), player.horizontalCollision));
        return true;
    }

    static Vec3 behindPosition(
            final Vec3 targetPosition, final Vec3 targetLook, final double y, final double distance) {
        Vec3 horizontalLook = targetLook.multiply(1.0, 0.0, 1.0);
        if (horizontalLook.lengthSqr() < 1.0E-8) {
            return new Vec3(targetPosition.x, y, targetPosition.z);
        }
        Vec3 offset = horizontalLook.normalize().scale(-Math.max(0.05, distance));
        return new Vec3(targetPosition.x + offset.x, y, targetPosition.z + offset.z);
    }
}
