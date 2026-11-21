package net.blockhost.anarchyclient.module.impl;

import java.util.List;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public final class BurrowModule extends Module {

    private final BooleanSetting center = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("center")
            .name("Center")
            .defaultValue(true)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final BooleanSetting support = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("support")
            .name("Support")
            .defaultValue(true)
            .build()));
    private final BooleanSetting jump = this.setting(BooleanSetting.from(
            BooleanSetting.builder().id("jump").name("Jump").defaultValue(true).build()));
    private final BooleanSetting rubberband = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rubberband")
            .name("Rubberband")
            .defaultValue(true)
            .build()));
    private final NumberSetting centerTolerance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("center_tolerance")
            .name("Tolerance")
            .defaultValue(0.08)
            .min(0.01)
            .max(0.3)
            .step(0.01)
            .build()));
    private final NumberSetting rubberbandHeight = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("rubberband_height")
            .name("Rubberband Y")
            .defaultValue(1.2)
            .min(0.0)
            .max(8.0)
            .step(0.1)
            .build()));
    /** Ticks to keep trying to place before giving up, so a failed burrow does not leave the module on. */
    private static final int PLACE_TIMEOUT_TICKS = 20;

    private Phase phase = Phase.CENTERING;
    private BlockPos target;
    private int placingTicks;

    public BurrowModule() {
        super(
                "burrow",
                "Burrow",
                ModuleCategory.COMBAT,
                "Centers you, jumps, and places a block in the space you stood in, then sends a raised position to lag you back into it.");
    }

    @Override
    protected void onEnable() {
        this.phase = Phase.CENTERING;
        this.target = null;
        this.placingTicks = 0;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }
        if (this.target == null) {
            this.target = player.blockPosition();
        }
        if (this.center.value()
                && this.phase == Phase.CENTERING
                && !centerPlayer(player, this.centerTolerance.value())) {
            this.debugValue("phase", "centering");
            return;
        }
        if (this.jump.value() && this.phase != Phase.PLACING && player.getY() < this.target.getY() + 1.05) {
            if (player.onGround()) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.42, player.getDeltaMovement().z);
            }
            this.phase = Phase.JUMPING;
            this.debugValue("phase", "jumping");
            return;
        }
        this.phase = Phase.PLACING;
        int placed = CombatPlacementPlanner.placeBatch(
                client,
                this,
                List.of(this.target),
                CombatPlacementPlanner.COMBAT_BLOCKS,
                CombatPlacementPlanner.Options.of(1, this.rotate.value(), 5.5)
                        .withSupport(this.support.value())
                        .withAvoidSelf(false)
                        .withAvoidEntities(false));
        this.debugValue("phase", "placing");
        this.debugValue("placed", placed);
        if (CombatPlacementPlanner.isReplaceable(client.level, this.target)
                && ++this.placingTicks > PLACE_TIMEOUT_TICKS) {
            // No blocks, or the cell is still occupied by the player (Jump off): it will never work.
            player.sendSystemMessage(Translations.component(
                    "anarchyclient.message.burrow.burrow_failed_could_not_place_a",
                    "Burrow failed: could not place a block in your cell."));
            this.enabled(false);
            return;
        }
        if (!CombatPlacementPlanner.isReplaceable(client.level, this.target)) {
            if (this.rubberband.value() && client.getConnection() != null) {
                Vec3 position = player.position();
                client.getConnection()
                        .send(new ServerboundMovePlayerPacket.Pos(
                                position.x,
                                position.y + this.rubberbandHeight.value(),
                                position.z,
                                false,
                                player.horizontalCollision));
            }
            this.enabled(false);
        }
    }

    private static boolean centerPlayer(final LocalPlayer player, final double tolerance) {
        double centerX = player.blockPosition().getX() + 0.5;
        double centerZ = player.blockPosition().getZ() + 0.5;
        double dx = centerX - player.getX();
        double dz = centerZ - player.getZ();
        if (dx * dx + dz * dz <= tolerance * tolerance) {
            return true;
        }
        player.setDeltaMovement(dx * 0.45, player.getDeltaMovement().y, dz * 0.45);
        return false;
    }

    private enum Phase {
        CENTERING,
        JUMPING,
        PLACING
    }
}
