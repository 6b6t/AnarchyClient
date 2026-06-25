package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.target.RenderedEntityCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class NotifierModule extends Module {

    private final BooleanSetting joinsLeaves = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("joins_leaves")
            .name("Joins")
            .defaultValue(true)
            .build()));
    private final BooleanSetting visualRange = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("visual_range")
            .name("Visual")
            .defaultValue(true)
            .build()));
    private final BooleanSetting totemPops = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("totem_pops")
            .name("Totems")
            .defaultValue(true)
            .build()));
    private final BooleanSetting ignoreFriends = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ignore_friends")
            .name("Friends")
            .defaultValue(true)
            .build()));
    private Map<UUID, String> onlinePlayers = Map.of();
    private Map<UUID, String> visiblePlayers = Map.of();
    private final Map<UUID, Integer> popCounts = new LinkedHashMap<>();
    private boolean onlineInitialized;
    private boolean visibleInitialized;

    public NotifierModule() {
        super("notifier", "Notifier", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        RenderedEntityCache.subscribe(this.id());
        this.reset();
    }

    @Override
    protected void onDisable() {
        RenderedEntityCache.unsubscribe(this.id());
        this.reset();
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null) {
            this.reset();
            return;
        }
        if (this.joinsLeaves.value()) {
            this.updateOnlinePlayers(client);
        } else {
            this.onlinePlayers = Map.of();
            this.onlineInitialized = false;
        }
        if (this.visualRange.value()) {
            this.updateVisiblePlayers(client);
        } else {
            this.visiblePlayers = Map.of();
            this.visibleInitialized = false;
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!this.totemPops.value() || client.level == null || client.player == null
                || !(packet instanceof ClientboundEntityEventPacket event)) {
            return false;
        }
        if (event.getEventId() == EntityEvent.DEATH) {
            Entity entity = event.getEntity(client.level);
            if (entity != null) {
                this.popCounts.remove(entity.getUUID());
            }
            return false;
        }
        if (event.getEventId() != EntityEvent.PROTECTED_FROM_DEATH) {
            return false;
        }
        Entity entity = event.getEntity(client.level);
        if (!(entity instanceof Player player) || shouldIgnore(player, client.player, this.ignoreFriends.value())) {
            return false;
        }
        int count = this.popCounts.merge(player.getUUID(), 1, Integer::sum);
        notifyPlayer(client, totemPopMessage(player.getScoreboardName(), count));
        return false;
    }

    @Override
    public void gameJoined(final Minecraft client, final ClientPacketListener listener) {
        this.reset();
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.reset();
    }

    static String totemPopMessage(final String name, final int count) {
        if (count <= 1) {
            return name + " popped a totem.";
        }
        return name + " popped a totem (x" + count + ").";
    }

    private void updateOnlinePlayers(final Minecraft client) {
        ClientPacketListener listener = client.getConnection();
        if (listener == null) {
            this.onlinePlayers = Map.of();
            this.onlineInitialized = false;
            return;
        }
        Map<UUID, String> current = onlinePlayers(listener, client.player, this.ignoreFriends.value());
        if (this.onlineInitialized) {
            for (String name : joinedPlayers(this.onlinePlayers, current)) {
                notifyPlayer(client, name + " joined.");
            }
            for (String name : leftPlayers(this.onlinePlayers, current)) {
                notifyPlayer(client, name + " left.");
            }
        }
        this.onlinePlayers = current;
        this.onlineInitialized = true;
    }

    private void updateVisiblePlayers(final Minecraft client) {
        Map<UUID, String> current = visiblePlayers(client.player, this.ignoreFriends.value());
        if (this.visibleInitialized) {
            for (String name : joinedPlayers(this.visiblePlayers, current)) {
                notifyPlayer(client, name + " entered visual range.");
            }
            for (String name : leftPlayers(this.visiblePlayers, current)) {
                notifyPlayer(client, name + " left visual range.");
            }
        }
        this.visiblePlayers = current;
        this.visibleInitialized = true;
    }

    private static Map<UUID, String> onlinePlayers(final ClientPacketListener listener, final Player self,
                                                   final boolean ignoreFriends) {
        Map<UUID, String> players = new LinkedHashMap<>();
        for (PlayerInfo info : listener.getOnlinePlayers()) {
            UUID id = info.getProfile().id();
            String name = info.getProfile().name();
            if (id.equals(self.getUUID()) || ignoreFriends && AnarchyClient.FRIENDS.isFriend(name)) {
                continue;
            }
            players.put(id, name);
        }
        return Map.copyOf(players);
    }

    private static Map<UUID, String> visiblePlayers(final Player self, final boolean ignoreFriends) {
        Map<UUID, String> players = new LinkedHashMap<>();
        for (LivingEntity entity : RenderedEntityCache.entities()) {
            if (entity instanceof Player player && !shouldIgnore(player, self, ignoreFriends)) {
                players.put(player.getUUID(), player.getScoreboardName());
            }
        }
        return Map.copyOf(players);
    }

    private static java.util.List<String> joinedPlayers(final Map<UUID, String> previous, final Map<UUID, String> current) {
        return current.entrySet().stream()
                .filter(entry -> !previous.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }

    private static java.util.List<String> leftPlayers(final Map<UUID, String> previous, final Map<UUID, String> current) {
        return previous.entrySet().stream()
                .filter(entry -> !current.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }

    private static boolean shouldIgnore(final Player player, final Player self, final boolean ignoreFriends) {
        return player == self || player.isSpectator() || ignoreFriends && AnarchyClient.FRIENDS.isFriend(player.getScoreboardName());
    }

    private static void notifyPlayer(final Minecraft client, final String message) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
    }

    private void reset() {
        this.onlinePlayers = Map.of();
        this.visiblePlayers = Map.of();
        this.popCounts.clear();
        this.onlineInitialized = false;
        this.visibleInitialized = false;
    }
}
