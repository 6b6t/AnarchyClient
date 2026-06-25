package net.blockhost.anarchyclient.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InventoryTracker {

    private static final Map<UUID, Snapshot> SNAPSHOTS = new LinkedHashMap<>();

    private InventoryTracker() {
    }

    public static void recordVisiblePlayers(final Minecraft client) {
        LocalPlayer localPlayer = client == null ? null : client.player;
        if (localPlayer == null || client.level == null) {
            SNAPSHOTS.clear();
            return;
        }
        long now = System.currentTimeMillis();
        int tick = localPlayer.tickCount;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Player player && player != localPlayer) {
                SNAPSHOTS.put(player.getUUID(), Snapshot.from(player, localPlayer.distanceTo(player), tick, now));
            }
        }
    }

    public static Optional<Snapshot> snapshot(final UUID uuid) {
        return Optional.ofNullable(SNAPSHOTS.get(uuid));
    }

    public static Collection<Snapshot> snapshots() {
        return List.copyOf(SNAPSHOTS.values());
    }

    public static List<Snapshot> newest(final int limit) {
        return SNAPSHOTS.values().stream()
                .sorted(Comparator.comparingLong(Snapshot::lastSeenMillis).reversed())
                .limit(Math.max(0, limit))
                .toList();
    }

    public static void pruneOlderThan(final long cutoffMillis) {
        SNAPSHOTS.values().removeIf(snapshot -> snapshot.lastSeenMillis() < cutoffMillis);
    }

    public static void clear() {
        SNAPSHOTS.clear();
    }

    public record Snapshot(
            UUID uuid,
            String name,
            ItemStack mainHand,
            ItemStack offhand,
            List<ItemStack> armor,
            float health,
            float absorption,
            double distance,
            int observedTick,
            long lastSeenMillis
    ) {

        private static Snapshot from(final Player player, final double distance, final int observedTick,
                                     final long lastSeenMillis) {
            return new Snapshot(
                    player.getUUID(),
                    player.getScoreboardName(),
                    player.getMainHandItem().copy(),
                    player.getOffhandItem().copy(),
                    List.of(
                            player.getItemBySlot(EquipmentSlot.HEAD).copy(),
                            player.getItemBySlot(EquipmentSlot.CHEST).copy(),
                            player.getItemBySlot(EquipmentSlot.LEGS).copy(),
                            player.getItemBySlot(EquipmentSlot.FEET).copy()
                    ),
                    player.getHealth(),
                    player.getAbsorptionAmount(),
                    distance,
                    observedTick,
                    lastSeenMillis
            );
        }
    }
}
