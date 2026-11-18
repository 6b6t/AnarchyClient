package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class AnnouncerModule extends Module {

    private static final int MINING_TIMEOUT_TICKS = 200;

    private final NumberSetting interval = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("interval")
            .name("Interval")
            .defaultValue(60.0)
            .min(10.0)
            .max(600.0)
            .step(5.0)
            .description("Seconds between announcements.")
            .build()));
    private final BooleanSetting walking = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("walking")
            .name("Walking")
            .defaultValue(true)
            .build()));
    private final BooleanSetting placing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("placing")
            .name("Placing")
            .defaultValue(true)
            .build()));
    private final BooleanSetting mining = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("mining")
            .name("Mining")
            .defaultValue(true)
            .build()));
    private final Map<BlockPos, Integer> pendingMining = new HashMap<>();
    private Vec3 lastPosition;
    private double walked;
    private int placed;
    private int mined;
    private int ticks;

    public AnnouncerModule() {
        super("announcer", "Announcer", ModuleCategory.MISC,
                "Tells chat what you have been doing, such as blocks walked, placed, or mined, at a set interval.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return;
        }
        Vec3 position = player.position();
        if (this.lastPosition != null && !player.isPassenger()) {
            double dx = position.x - this.lastPosition.x;
            double dz = position.z - this.lastPosition.z;
            double step = Math.sqrt(dx * dx + dz * dz);
            // Ignore teleports and respawns.
            if (step < 10.0) {
                this.walked += step;
            }
        }
        this.lastPosition = position;
        this.trackMining(client);
        if (++this.ticks < this.interval.value().intValue() * 20) {
            return;
        }
        this.ticks = 0;
        String message = announcement(this.walking.value() ? (int) this.walked : 0,
                this.placing.value() ? this.placed : 0, this.mining.value() ? this.mined : 0);
        if (message != null) {
            ChatActions.send(client, message);
            this.walked = 0.0;
            this.placed = 0;
            this.mined = 0;
        }
    }

    private void trackMining(final Minecraft client) {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = this.pendingMining.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            if (client.level.getBlockState(entry.getKey()).isAir()) {
                this.mined++;
                iterator.remove();
            } else if (entry.getValue() + 1 > MINING_TIMEOUT_TICKS) {
                iterator.remove();
            } else {
                entry.setValue(entry.getValue() + 1);
            }
        }
    }

    /** Returns the message for the largest counter, or {@code null} when nothing happened. */
    static String announcement(final int walked, final int placed, final int mined) {
        // Walking piles up much faster than block counts, so it only wins when it is clearly larger.
        int walkScore = walked / 10;
        if (walked >= 10 && walkScore >= placed && walkScore >= mined) {
            return "I just walked " + walked + " blocks.";
        }
        if (placed > 0 && placed >= mined) {
            return "I just placed " + placed + (placed == 1 ? " block." : " blocks.");
        }
        if (mined > 0) {
            return "I just mined " + mined + (mined == 1 ? " block." : " blocks.");
        }
        return null;
    }

    @Override
    public boolean attackBlock(final Minecraft client, final BlockPos pos, final Direction direction) {
        this.pendingMining.putIfAbsent(pos.immutable(), 0);
        return false;
    }

    @Override
    public void sentPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (packet instanceof ServerboundUseItemOnPacket use && client.player != null
                && client.player.getItemInHand(use.hand()).getItem() instanceof BlockItem) {
            this.placed++;
        }
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.reset();
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    private void reset() {
        this.pendingMining.clear();
        this.lastPosition = null;
        this.walked = 0.0;
        this.placed = 0;
        this.mined = 0;
        this.ticks = 0;
    }
}
