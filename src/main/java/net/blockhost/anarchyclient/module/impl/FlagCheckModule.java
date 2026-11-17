package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.util.MainThread;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class FlagCheckModule extends Module {

    private final BooleanSetting chat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("chat")
            .name("Chat")
            .defaultValue(true)
            .build()));
    private final BooleanSetting invalidAttributes = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("invalid_attributes")
            .name("Invalid")
            .defaultValue(false)
            .build()));
    private final NumberSetting forceRotateAngle = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("force_rotate_angle")
            .name("Rotate")
            .defaultValue(90.0)
            .min(10.0)
            .max(180.0)
            .step(5.0)
            .build()));
    private int lastInvalidAlertTick;

    public FlagCheckModule() {
        super("flag_check", "Flag Check", ModuleCategory.MISC,
                "Detects server lagbacks and forced rotations, records them, and alerts you in chat.");
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        LocalPlayer player = client.player;
        if (player == null || player.tickCount <= 25 || !(packet instanceof ClientboundPlayerPositionPacket position)) {
            return false;
        }
        // Vanilla applies the packet before queued tasks run, so read the current rotation now.
        Set<Relative> relatives = position.relatives();
        float yawDelta = rotationDelta(position.change().yRot(), player.getYRot(), relatives.contains(Relative.Y_ROT));
        float pitchDelta = rotationDelta(position.change().xRot(), player.getXRot(), relatives.contains(Relative.X_ROT));
        Vec3 current = player.position();
        Vec3 change = position.change().position();
        Vec3 target = new Vec3(
                relatives.contains(Relative.X) ? current.x + change.x : change.x,
                relatives.contains(Relative.Y) ? current.y + change.y : change.y,
                relatives.contains(Relative.Z) ? current.z + change.z : change.z
        );
        float yaw = relatives.contains(Relative.Y_ROT) ? player.getYRot() + position.change().yRot() : position.change().yRot();
        float pitch = relatives.contains(Relative.X_ROT) ? player.getXRot() + position.change().xRot() : position.change().xRot();
        boolean forceRotate = yawDelta >= this.forceRotateAngle.value() || pitchDelta >= this.forceRotateAngle.value();
        ServerObserver.FlagReason reason = forceRotate ? ServerObserver.FlagReason.FORCE_ROTATE : ServerObserver.FlagReason.LAGBACK;
        String detail = forceRotate ? Math.round(yawDelta) + " yaw / " + Math.round(pitchDelta) + " pitch" : "";
        MainThread.run(() -> {
            ServerObserver.recordFlag(reason, target, yaw, pitch, detail);
            this.notify(client, reason, detail);
        });
        return false;
    }

    /** Angle the server turns the player by. A relative rotation is already the turn amount. */
    static float rotationDelta(final float change, final float current, final boolean relative) {
        return relative ? Math.abs(change) : angleDelta(change, current);
    }

    @Override
    public void tick(final Minecraft client) {
        if (!this.invalidAttributes.value() || client.player == null || client.player.tickCount - this.lastInvalidAlertTick < 20) {
            return;
        }
        boolean invalidHealth = client.player.getHealth() <= 0.0F && client.player.isAlive();
        boolean invalidHunger = client.player.getFoodData().getFoodLevel() <= 0;
        if (!invalidHealth && !invalidHunger) {
            return;
        }
        this.lastInvalidAlertTick = client.player.tickCount;
        String detail = (invalidHealth ? "health" : "") + (invalidHealth && invalidHunger ? ", " : "") + (invalidHunger ? "hunger" : "");
        ServerObserver.recordFlag(ServerObserver.FlagReason.INVALID_ATTRIBUTES, client.player.position(),
                client.player.getYRot(), client.player.getXRot(), detail);
        this.notify(client, ServerObserver.FlagReason.INVALID_ATTRIBUTES, detail);
    }

    private void notify(final Minecraft client, final ServerObserver.FlagReason reason, final String detail) {
        if (!this.chat.value() || client.player == null) {
            return;
        }
        String suffix = detail == null || detail.isBlank() ? "" : " (" + detail + ")";
        client.player.sendSystemMessage(Component.literal("Flag detected: " + label(reason) + suffix + "."));
    }

    private static String label(final ServerObserver.FlagReason reason) {
        return switch (reason) {
            case LAGBACK -> "lagback";
            case FORCE_ROTATE -> "force rotate";
            case INVALID_ATTRIBUTES -> "invalid attributes";
        };
    }

    static float angleDelta(final float newer, final float older) {
        float delta = newer - older;
        if (delta > 180.0F) {
            delta -= 360.0F;
        }
        if (delta < -180.0F) {
            delta += 360.0F;
        }
        return Math.abs(delta);
    }
}
