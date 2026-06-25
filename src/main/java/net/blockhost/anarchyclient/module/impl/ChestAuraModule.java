package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ChestAuraModule extends Module {

    private final StringSetting blocks = this.setting(StringSetting.from(StringSetting.builder()
            .id("blocks")
            .name("Blocks")
            .defaultValue("chest,trapped_chest,barrel,ender_chest,shulker_box")
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(6.0)
            .step(0.5)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(10.0)
            .min(1.0)
            .max(60.0)
            .step(1.0)
            .build()));
    private final NumberSetting forgetAfter = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("forget_after")
            .name("Forget")
            .defaultValue(0.0)
            .min(0.0)
            .max(2400.0)
            .step(20.0)
            .build()));
    private final SelectSetting closeMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("close_mode")
            .name("Close")
            .defaultValue("If Empty")
            .addAllOptions(List.of("Never", "Always", "If Empty"))
            .build()));
    private final Map<BlockPos, Integer> openedBlocks = new HashMap<>();
    private String lastBlocks = "";
    private Set<Block> parsedBlocks = Set.of();
    private int delayTicks;

    public ChestAuraModule() {
        super("chest_aura", "Chest Aura", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }
        this.tickOpenedBlocks();
        this.closeContainerIfNeeded(player);
        if (this.delayTicks > 0) {
            this.delayTicks--;
            return;
        }
        if (client.gui.screen() != null) {
            return;
        }
        if (!this.lastBlocks.equals(this.blocks.value())) {
            this.parsedBlocks = BlockScan.parseBlocks(this.blocks.value());
            this.lastBlocks = this.blocks.value();
        }
        BlockPos target = this.findTarget(client, player);
        if (target == null) {
            return;
        }
        this.openBlock(client, player, target);
        this.markOpened(client.level.getBlockState(target), target);
        this.delayTicks = this.delay.value().intValue();
    }

    @Override
    protected void onDisable() {
        this.openedBlocks.clear();
        this.delayTicks = 0;
    }

    private void tickOpenedBlocks() {
        int forget = this.forgetAfter.value().intValue();
        if (forget <= 0 || this.openedBlocks.isEmpty()) {
            return;
        }
        this.openedBlocks.replaceAll((pos, age) -> age + 1);
        this.openedBlocks.entrySet().removeIf(entry -> entry.getValue() > forget);
    }

    private void closeContainerIfNeeded(final LocalPlayer player) {
        if (!(player.containerMenu instanceof ChestMenu chestMenu)) {
            return;
        }
        switch (this.closeMode.value()) {
            case "Always" -> player.closeContainer();
            case "If Empty" -> {
                if (isContainerEmpty(chestMenu)) {
                    player.closeContainer();
                }
            }
            default -> {
            }
        }
    }

    private BlockPos findTarget(final Minecraft client, final LocalPlayer player) {
        if (this.parsedBlocks.isEmpty()) {
            return null;
        }
        BlockPos center = player.blockPosition();
        int radius = (int) Math.ceil(this.range.value());
        double rangeSqr = this.range.value() * this.range.value();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
            for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (this.openedBlocks.containsKey(pos)
                            || !client.level.isLoaded(pos)
                            || !this.parsedBlocks.contains(client.level.getBlockState(pos).getBlock())) {
                        continue;
                    }
                    double distance = player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos));
                    if (distance <= rangeSqr && distance < bestDistance) {
                        best = pos.immutable();
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    private void openBlock(final Minecraft client, final LocalPlayer player, final BlockPos pos) {
        client.gameMode.useItemOn(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
        );
    }

    private void markOpened(final BlockState state, final BlockPos pos) {
        this.openedBlocks.put(pos.immutable(), 0);
        if (!state.hasProperty(ChestBlock.TYPE) || !state.hasProperty(ChestBlock.FACING)) {
            return;
        }
        ChestType type = state.getValue(ChestBlock.TYPE);
        Direction facing = state.getValue(ChestBlock.FACING);
        if (type == ChestType.LEFT) {
            this.openedBlocks.put(pos.relative(facing.getClockWise()).immutable(), 0);
        } else if (type == ChestType.RIGHT) {
            this.openedBlocks.put(pos.relative(facing.getCounterClockWise()).immutable(), 0);
        }
    }

    private static boolean isContainerEmpty(final ChestMenu menu) {
        int containerSlots = menu.getRowCount() * 9;
        for (int slot = 0; slot < containerSlots && slot < menu.slots.size(); slot++) {
            Slot menuSlot = menu.slots.get(slot);
            if (menuSlot.hasItem()) {
                return false;
            }
        }
        return true;
    }
}
