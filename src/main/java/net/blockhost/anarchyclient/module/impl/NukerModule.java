package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BlockListSetting;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;

import java.util.List;

public final class NukerModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Legit")
            .addAllOptions(List.of("Legit", "Packet"))
            .build()));
    private final SelectSetting shape = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("shape")
            .name("Shape")
            .defaultValue("Sphere")
            .addAllOptions(List.of("Sphere", "Cube", "Floor"))
            .build()));
    private final SelectSetting sort = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("sort")
            .name("Sort")
            .defaultValue(BlockTargetScanner.SortMode.CLOSEST.label())
            .addAllOptions(List.of(
                    BlockTargetScanner.SortMode.CLOSEST.label(),
                    BlockTargetScanner.SortMode.FARTHEST.label(),
                    BlockTargetScanner.SortMode.RANDOM.label()
            ))
            .build()));
    private final NumberSetting radius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("radius")
            .name("Radius")
            .defaultValue(3.0)
            .min(1.0)
            .max(6.0)
            .step(1.0)
            .build()));
    private final NumberSetting blocksPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("blocks_per_tick")
            .name("Blocks")
            .defaultValue(2.0)
            .min(1.0)
            .max(12.0)
            .step(1.0)
            .build()));
    private final BooleanSetting excludeLiquids = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("exclude_liquids")
            .name("No Liquids")
            .defaultValue(true)
            .build()));
    private final BooleanSetting requireTool = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("require_tool")
            .name("Require Tool")
            .defaultValue(false)
            .build()));
    private final BlockListSetting allowList = this.setting(BlockListSetting.from(BlockListSetting.builder()
            .id("allow")
            .name("Allow")
            .addAllDefaultValue(List.of())
            .build()));
    private final BlockListSetting denyList = this.setting(BlockListSetting.from(BlockListSetting.builder()
            .id("deny")
            .name("Deny")
            .addAllDefaultValue(List.of())
            .build()));

    public NukerModule() {
        super("nuker", "Nuker", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        int actions = 0;
        BlockPos center = player.blockPosition();
        for (BlockTargetScanner.BlockTarget target : BlockTargetScanner.scan(
                client,
                this.radius.value().intValue(),
                this.radius.value().intValue(),
                BlockTargetScanner.SortMode.fromSetting(this.sort.value()),
                this.blocksPerTick.value().intValue() * 5,
                candidate -> !candidate.state().isAir()
                        && passesShape(candidate.pos(), center, this.radius.value().intValue(), this.shape.value())
                        && (!this.excludeLiquids.value() || candidate.state().getFluidState().isEmpty())
                        && (this.allowList.value().isEmpty() || this.allowList.value().contains(candidate.state().getBlock()))
                        && !this.denyList.value().contains(candidate.state().getBlock())
                        && (!this.requireTool.value() || player.getMainHandItem().isCorrectToolForDrops(candidate.state()))
        )) {
            if (breakTarget(client, target.pos(), "Packet".equals(this.mode.value()))
                    && ++actions >= this.blocksPerTick.value().intValue()) {
                this.debugValue("broken", actions);
                return;
            }
        }
    }

    private static boolean breakTarget(final Minecraft client, final BlockPos pos, final boolean packet) {
        if (!packet) {
            return WorldInteraction.breakBlock(client, pos, Direction.UP, stack -> true);
        }
        if (client.player == null || client.getConnection() == null) {
            return false;
        }
        client.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pos,
                Direction.UP
        ));
        client.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                pos,
                Direction.UP
        ));
        client.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    static boolean passesShape(final BlockPos pos, final BlockPos center, final int radius, final String shape) {
        int dx = pos.getX() - center.getX();
        int dy = pos.getY() - center.getY();
        int dz = pos.getZ() - center.getZ();
        return switch (shape) {
            case "Cube" -> Math.abs(dx) <= radius && Math.abs(dy) <= radius && Math.abs(dz) <= radius;
            case "Floor" -> dy <= 0 && dy >= -radius && dx * dx + dz * dz <= radius * radius;
            default -> dx * dx + dy * dy + dz * dz <= radius * radius;
        };
    }
}
