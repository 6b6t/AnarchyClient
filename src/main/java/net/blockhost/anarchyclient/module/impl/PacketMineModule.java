package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.inventory.ToolSelector;
import net.blockhost.anarchyclient.mixin.MultiPlayerGameModeAccessor;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.OptionalInt;

public final class PacketMineModule extends Module {

    private static final String INSTANT = "Instant";
    private static final String PROGRESS = "Progress";

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue(INSTANT)
            .addAllOptions(List.of(INSTANT, PROGRESS))
            .description("Instant sends start and finish packets for the block under the crosshair. Progress mines "
                    + "the block you click in the background and finishes it with the best tool once the server accepts it.")
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(5.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final BooleanSetting swing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("swing")
            .name("Swing")
            .defaultValue(true)
            .build()));
    private final BooleanSetting switchTool = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("switch_tool")
            .name("Switch Tool")
            .defaultValue(true)
            .description("Switches to the best hotbar tool only for the finish packet, then back.")
            .build()));
    private final BooleanSetting render = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("render")
            .name("Render")
            .defaultValue(true)
            .build()));
    private int cooldownTicks;
    private BlockPos lastTarget;
    private Target target;
    private int lastSelectedSlot = -1;

    public PacketMineModule() {
        super("packet_mine", "Packet Mine", ModuleCategory.WORLD,
                "Breaks blocks with start and finish mining packets. Instant mode hits the block under the crosshair after "
                        + "a set delay; Progress mode keeps mining a clicked block in the background, so you can look away. "
                        + "This can flag anti-cheats.");
        this.delay.visibleWhen(() -> INSTANT.equals(this.mode.value()));
        this.switchTool.visibleWhen(() -> PROGRESS.equals(this.mode.value()));
        this.render.visibleWhen(() -> PROGRESS.equals(this.mode.value()));
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.getConnection() == null) {
            return;
        }
        if (PROGRESS.equals(this.mode.value())) {
            this.tickProgress(client, player);
            return;
        }
        this.tickInstant(client, player);
    }

    private void tickInstant(final Minecraft client, final LocalPlayer player) {
        if (client.hitResult == null || this.cooldownTicks-- > 0 || client.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult hit = (BlockHitResult) client.hitResult;
        BlockPos pos = hit.getBlockPos();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > this.range.value() * this.range.value()) {
            return;
        }
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        Direction direction = hit.getDirection();
        if (this.rotate.value()) {
            this.requestRotation(player, hit.getLocation());
            RotationManager.apply(player);
        }
        send(client, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction);
        send(client, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, direction);
        this.swing(player);
        this.lastTarget = pos;
        this.cooldownTicks = this.delay.value().intValue();
    }

    private void tickProgress(final Minecraft client, final LocalPlayer player) {
        try {
            this.tickProgressTarget(client, player);
        } finally {
            // What the server saw held at the end of this tick, for the next tick's finish check.
            this.lastSelectedSlot = player.getInventory().getSelectedSlot();
        }
    }

    private void tickProgressTarget(final Minecraft client, final LocalPlayer player) {
        Target current = this.target;
        if (current == null) {
            return;
        }
        BlockState state = client.level.getBlockState(current.pos());
        if (state.isAir() || !state.getFluidState().isEmpty() && state.canBeReplaced()) {
            this.target = null;
            return;
        }
        if (!CombatTargets.canReachBlock(player, current.pos(), this.range.value())) {
            send(client, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, current.pos(), current.face());
            this.target = null;
            return;
        }
        int ticksMined = current.ticks();
        this.target = current.ticked();
        OptionalInt toolSlot = this.switchTool.value()
                ? ToolSelector.bestHotbarTool(player.getInventory(), state, ToolSelector.ANY)
                : OptionalInt.empty();
        ItemStack tool = toolSlot.isPresent() ? player.getInventory().getItem(toolSlot.getAsInt()) : player.getMainHandItem();
        float perTick = MiningProgress.perTick(player, client.level, current.pos(), state, tool);
        float shown = Math.min(1.0F, perTick * (ticksMined + 1) / SpeedMineModule.SERVER_BREAK_THRESHOLD);
        this.debugValue("progress", Math.round(shown * 100.0F) + "%");
        this.mark(current.pos(), shown);
        if (!MiningProgress.serverAccepts(perTick, ticksMined)) {
            return;
        }
        if (this.rotate.value()) {
            this.requestRotation(player, Vec3.atCenterOf(current.pos()));
            RotationManager.apply(player);
            if (!RotationManager.serverLookHits(player, new AABB(current.pos()), this.range.value() + 1.0)) {
                return;
            }
        }
        if (toolSlot.isPresent()) {
            boolean heldSinceLastTick = toolSlot.getAsInt() == this.lastSelectedSlot
                    && player.getInventory().getSelectedSlot() == toolSlot.getAsInt();
            if (!SilentHotbar.select(player, this.id(), toolSlot.getAsInt(), SilentHotbar.PRIORITY_NORMAL, 3, true)) {
                return;
            }
            ((MultiPlayerGameModeAccessor) client.gameMode).anarchyclient$ensureHasSentCarriedItem();
            if (!heldSinceLastTick) {
                // The server applies a tool's enchantments (Efficiency) in its next player tick, not when the slot
                // changes. Finishing in the same tick would be checked without them and refused.
                return;
            }
        }
        send(client, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, current.pos(), current.face());
        this.swing(player);
        this.lastTarget = current.pos();
        this.target = null;
    }

    @Override
    public boolean attackBlock(final Minecraft client, final BlockPos pos, final Direction direction) {
        LocalPlayer player = client.player;
        if (!PROGRESS.equals(this.mode.value()) || player == null || client.level == null
                || client.getConnection() == null || player.getAbilities().instabuild) {
            return false;
        }
        if (this.target != null && this.target.pos().equals(pos)) {
            // Already mining it in the background; keep vanilla from restarting the break.
            return true;
        }
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(client.level, pos) < 0.0F) {
            return false;
        }
        if (this.target != null) {
            send(client, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.target.pos(), this.target.face());
        }
        send(client, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction);
        this.swing(player);
        this.target = new Target(pos.immutable(), direction, 0);
        return true;
    }

    private void requestRotation(final LocalPlayer player, final Vec3 point) {
        RotationManager.request(new RotationRequest(
                this.id(),
                Rotation.lookingAt(point, player.getEyePosition()),
                70,
                60.0F,
                2,
                2.0F,
                RotationTurnMode.STEPPED,
                true
        ));
    }

    private void swing(final LocalPlayer player) {
        if (this.swing.value()) {
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        }
    }

    private void mark(final BlockPos pos, final float progress) {
        if (!this.render.value()) {
            return;
        }
        double inset = 0.5 * (1.0 - progress);
        MarkerManager.put(new CuboidMarker(this.id() + ":target", new AABB(pos).deflate(inset), MarkerStyle.CYAN, 2));
    }

    private static void send(final Minecraft client, final ServerboundPlayerActionPacket.Action action, final BlockPos pos,
                             final Direction direction) {
        ClientPacketListener connection = client.getConnection();
        if (connection != null) {
            connection.send(new ServerboundPlayerActionPacket(action, pos, direction));
        }
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.target = null;
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (this.target != null) {
            send(client, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.target.pos(), this.target.face());
        }
        this.cooldownTicks = 0;
        this.lastTarget = null;
        this.target = null;
        RotationManager.clear(this.id());
        SilentHotbar.clear(this.id());
    }

    record Target(BlockPos pos, Direction face, int ticks) {

        Target ticked() {
            return new Target(this.pos, this.face, this.ticks + 1);
        }
    }
}
