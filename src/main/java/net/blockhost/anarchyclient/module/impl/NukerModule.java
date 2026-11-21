package net.blockhost.anarchyclient.module.impl;

import java.util.List;
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
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.state.BlockState;

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
                    BlockTargetScanner.SortMode.RANDOM.label()))
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

    private final SingleBlockMiner miner = new SingleBlockMiner(this);

    public NukerModule() {
        super(
                "nuker",
                "Nuker",
                ModuleCategory.WORLD,
                "Breaks blocks around the player in a sphere, cube, or floor shape, with allow and deny lists. "
                        + "Legit mines one block at a time; Packet only breaks blocks the server accepts in one tick.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            this.miner.stop(client);
            return;
        }
        boolean packet = "Packet".equals(this.mode.value());
        BlockPos center = player.blockPosition();
        if (packet) {
            this.miner.stop(client);
        } else if (this.miner.continueCurrent(
                client, pos -> this.accepts(player, pos, client.level.getBlockState(pos), center))) {
            return;
        }
        int actions = 0;
        for (BlockTargetScanner.BlockTarget target : BlockTargetScanner.scan(
                client,
                this.radius.value().intValue(),
                this.radius.value().intValue(),
                BlockTargetScanner.SortMode.fromSetting(this.sort.value()),
                this.blocksPerTick.value().intValue() * 5,
                candidate -> this.accepts(player, candidate.pos(), candidate.state(), center)
                        && (!packet || breaksInOnePacket(player, candidate.pos(), candidate.state())))) {
            if (packet) {
                if (packetBreak(client, target.pos())
                        && ++actions >= this.blocksPerTick.value().intValue()) {
                    break;
                }
                continue;
            }
            SingleBlockMiner.Step step = this.miner.start(client, target.pos(), Direction.UP);
            if (step == SingleBlockMiner.Step.MINING) {
                // Starting another block would abort this one.
                break;
            }
            if (step == SingleBlockMiner.Step.BROKEN
                    && ++actions >= this.blocksPerTick.value().intValue()) {
                break;
            }
        }
        this.debugValue("broken", actions);
    }

    @Override
    protected void onDisable() {
        this.miner.stop(Minecraft.getInstance());
    }

    private boolean accepts(
            final LocalPlayer player, final BlockPos pos, final BlockState state, final BlockPos center) {
        return !state.isAir()
                && SingleBlockMiner.canEverBreak(player, player.level(), pos, state)
                && CombatTargets.canReachBlock(player, pos)
                && passesShape(pos, center, this.radius.value().intValue(), this.shape.value())
                && (!this.excludeLiquids.value() || state.getFluidState().isEmpty())
                && (this.allowList.value().isEmpty() || this.allowList.value().contains(state.getBlock()))
                && !this.denyList.value().contains(state.getBlock())
                && (!this.requireTool.value() || player.getMainHandItem().isCorrectToolForDrops(state));
    }

    /** The server accepts a stop right after a start only when one tick of progress reaches 0.7. */
    private static boolean breaksInOnePacket(final LocalPlayer player, final BlockPos pos, final BlockState state) {
        return state.getDestroyProgress(player, player.level(), pos) >= SpeedMineModule.SERVER_BREAK_THRESHOLD;
    }

    private static boolean packetBreak(final Minecraft client, final BlockPos pos) {
        if (client.player == null || client.getConnection() == null) {
            return false;
        }
        client.getConnection()
                .send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
        client.getConnection()
                .send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        client.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
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
