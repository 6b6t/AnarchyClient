package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.List;

public final class BlockInModule extends Module {

    private static final List<BlockPos> OFFSETS = List.of(
            new BlockPos(0, -1, 0),
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1),
            new BlockPos(1, 1, 0),
            new BlockPos(-1, 1, 0),
            new BlockPos(0, 1, 1),
            new BlockPos(0, 1, -1),
            new BlockPos(0, 2, 0)
    );

    private final BooleanSetting multiPlace = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("multi_place")
            .name("Multi")
            .defaultValue(false)
            .build()));
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
    private final NumberSetting maxTurnDegrees = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_turn_degrees")
            .name("Turn")
            .defaultValue(90.0)
            .min(15.0)
            .max(180.0)
            .step(5.0)
            .build()));
    private final BooleanSetting onlyOnGround = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("only_on_ground")
            .name("Ground")
            .defaultValue(true)
            .build()));
    private final BooleanSetting disableWhenDone = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("disable_when_done")
            .name("Done Off")
            .defaultValue(true)
            .build()));

    public BlockInModule() {
        super("block_in", "Block In", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || player.connection == null) {
            return;
        }
        if (this.onlyOnGround.value() && !player.onGround()) {
            return;
        }
        if (this.center.value()) {
            centerPlayer(player);
        }
        boolean complete = true;
        for (BlockPos target : targetPositions(BlockPos.containing(player.getX(), player.getY(), player.getZ()))) {
            BlockPlacement.PlacementResult result = BlockPlacement.place(
                    client,
                    this,
                    target,
                    this.rotate.value(),
                    this.maxTurnDegrees.value().floatValue()
            );
            if (result == BlockPlacement.PlacementResult.WAITING) {
                complete = false;
            }
            if (result == BlockPlacement.PlacementResult.PLACED && !this.multiPlace.value()) {
                return;
            }
        }
        if (complete && this.disableWhenDone.value()) {
            this.enabled(false);
        }
    }

    static List<BlockPos> targetPositions(final BlockPos base) {
        return OFFSETS.stream()
                .map(base::offset)
                .toList();
    }

    private static void centerPlayer(final LocalPlayer player) {
        double x = Math.floor(player.getX()) + 0.5;
        double z = Math.floor(player.getZ()) + 0.5;
        double dx = x - player.getX();
        double dz = z - player.getZ();
        if (dx * dx + dz * dz < 0.0001) {
            return;
        }
        player.setPos(x, player.getY(), z);
        player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x,
                player.getY(),
                z,
                player.onGround(),
                player.horizontalCollision
        ));
    }
}
