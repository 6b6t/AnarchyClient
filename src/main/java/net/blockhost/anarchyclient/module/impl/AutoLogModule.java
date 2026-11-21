package net.blockhost.anarchyclient.module.impl;

import java.util.Locale;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.util.MainThread;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;

public final class AutoLogModule extends Module {

    private static volatile boolean safetyDisconnect;

    private final NumberSetting healthThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health_threshold")
            .name("Health")
            .defaultValue(8.0)
            .min(0.0)
            .max(36.0)
            .step(0.5)
            .build()));
    private final BooleanSetting includeAbsorption = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("include_absorption")
            .name("Absorption")
            .defaultValue(true)
            .build()));
    private final NumberSetting totemPopThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("totem_pop_threshold")
            .name("Pops")
            .defaultValue(0.0)
            .min(0.0)
            .max(10.0)
            .step(1.0)
            .build()));
    private final BooleanSetting players = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("players")
            .name("Players")
            .defaultValue(false)
            .build()));
    private final NumberSetting playerRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("player_range")
            .name("Player Range")
            .defaultValue(12.0)
            .min(2.0)
            .max(96.0)
            .step(1.0)
            .build()));
    private final NumberSetting dangerousEntityCount = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("dangerous_entity_count")
            .name("Danger Count")
            .defaultValue(0.0)
            .min(0.0)
            .max(10.0)
            .step(1.0)
            .build()));
    private final NumberSetting dangerousEntityRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("dangerous_entity_range")
            .name("Danger Range")
            .defaultValue(6.0)
            .min(2.0)
            .max(16.0)
            .step(0.5)
            .build()));
    private final BooleanSetting illegalDisconnect = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("illegal_disconnect")
            .name("Illegal Disconnect")
            .defaultValue(false)
            .description("Also sends an invalid packet so the server kicks you at once, even behind a proxy.")
            .build()));
    private int selfTotemPops;
    private boolean logged;

    public AutoLogModule() {
        super(
                "auto_log",
                "Auto Log",
                ModuleCategory.COMBAT,
                "Disconnects you when health drops too low, totems pop, an unfriended player comes close, or crystals and TNT gather nearby.");
    }

    @Override
    protected void onEnable() {
        this.reset();
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.logged || client.player == null || client.level == null) {
            return;
        }
        Player player = client.player;
        if (shouldDisconnectForHealth(
                player.getHealth(),
                player.getAbsorptionAmount(),
                this.healthThreshold.value(),
                this.includeAbsorption.value())) {
            this.disconnect(
                    client,
                    "health reached "
                            + formatHealth(effectiveHealth(
                                    player.getHealth(), player.getAbsorptionAmount(), this.includeAbsorption.value())));
            return;
        }
        if (this.players.value()) {
            String untrusted = nearestUntrustedPlayer(client, player, this.playerRange.value());
            if (untrusted != null) {
                this.disconnect(client, untrusted + " entered range");
                return;
            }
        }
        int dangerThreshold = this.dangerousEntityCount.value().intValue();
        if (dangerThreshold > 0) {
            int count = dangerousEntityCount(
                    client.level.entitiesForRendering(), player, this.dangerousEntityRange.value());
            if (count >= dangerThreshold) {
                this.disconnect(client, count + " dangerous entities nearby");
            }
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (packet instanceof ClientboundEntityEventPacket event
                && event.getEventId() == EntityEvent.PROTECTED_FROM_DEATH) {
            // Runs on the network thread; entity lookups and the disconnect belong on the client thread.
            MainThread.run(() -> this.handleTotemPop(client, connection, event));
        }
        return false;
    }

    private void handleTotemPop(
            final Minecraft client, final Connection connection, final ClientboundEntityEventPacket event) {
        if (this.logged
                || client.level == null
                || client.player == null
                || event.getEntity(client.level) != client.player) {
            return;
        }
        this.selfTotemPops++;
        if (shouldDisconnectForTotemPops(
                this.selfTotemPops, this.totemPopThreshold.value().intValue())) {
            this.disconnect(connection, "totem popped " + this.selfTotemPops + " times");
        }
    }

    @Override
    public void gameJoined(final Minecraft client, final ClientPacketListener listener) {
        clearSafetyDisconnect();
        this.reset();
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.reset();
    }

    static boolean shouldDisconnectForHealth(
            final float health, final float absorption, final double threshold, final boolean includeAbsorption) {
        return threshold > 0.0 && effectiveHealth(health, absorption, includeAbsorption) <= threshold;
    }

    static boolean shouldDisconnectForTotemPops(final int pops, final int threshold) {
        return threshold > 0 && pops >= threshold;
    }

    static float effectiveHealth(final float health, final float absorption, final boolean includeAbsorption) {
        return health + (includeAbsorption ? absorption : 0.0F);
    }

    static int dangerousEntityCount(
            final Iterable<? extends Entity> entities, final Player player, final double range) {
        int count = 0;
        double rangeSqr = range * range;
        for (Entity entity : entities) {
            if (isDangerousEntity(entity) && player.distanceToSqr(entity) <= rangeSqr) {
                count++;
            }
        }
        return count;
    }

    private static boolean isDangerousEntity(final Entity entity) {
        return entity instanceof EndCrystal || entity instanceof PrimedTnt;
    }

    private static String nearestUntrustedPlayer(final Minecraft client, final Player self, final double range) {
        double rangeSqr = range * range;
        String nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player)
                    || player == self
                    || player.isSpectator()
                    || !player.isAlive()
                    || AnarchyClient.FRIENDS.isFriend(player.getScoreboardName())) {
                continue;
            }
            double distance = self.distanceToSqr(player);
            if (distance <= rangeSqr && distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player.getScoreboardName();
            }
        }
        return nearest;
    }

    /**
     * Returns whether the last disconnect came from Auto Log, and clears the flag. Auto Reconnect uses this so it does
     * not rejoin the danger Auto Log just left.
     */
    public static boolean consumeSafetyDisconnect() {
        boolean value = safetyDisconnect;
        safetyDisconnect = false;
        return value;
    }

    /** Forgets an Auto Log disconnect from an earlier session. */
    static void clearSafetyDisconnect() {
        safetyDisconnect = false;
    }

    private void disconnect(final Minecraft client, final String reason) {
        ClientPacketListener listener = client.getConnection();
        if (listener != null) {
            this.disconnect(listener.getConnection(), reason);
        }
    }

    private void disconnect(final Connection connection, final String reason) {
        if (connection == null || this.logged) {
            return;
        }
        this.logged = true;
        safetyDisconnect = true;
        if (this.illegalDisconnect.value()) {
            IllegalDisconnect.send(connection);
        }
        connection.disconnect(
                Translations.component("anarchyclient.message.auto_log.auto_log", "Auto Log: %s", reason));
    }

    private void reset() {
        this.selfTotemPops = 0;
        this.logged = false;
    }

    private static String formatHealth(final float health) {
        return String.format(Locale.ROOT, "%.1f", health);
    }
}
