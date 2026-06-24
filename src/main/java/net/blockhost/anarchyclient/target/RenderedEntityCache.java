package net.blockhost.anarchyclient.target;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.StreamSupport;

public final class RenderedEntityCache {

    private static final CopyOnWriteArraySet<String> SUBSCRIBERS = new CopyOnWriteArraySet<>();
    private static List<LivingEntity> entities = List.of();

    private RenderedEntityCache() {
    }

    public static void subscribe(final String owner) {
        SUBSCRIBERS.add(owner);
    }

    public static void unsubscribe(final String owner) {
        SUBSCRIBERS.remove(owner);
        if (SUBSCRIBERS.isEmpty()) {
            entities = List.of();
        }
    }

    public static List<LivingEntity> entities() {
        return entities;
    }

    public static void refresh(final Minecraft client) {
        if (SUBSCRIBERS.isEmpty() || client.level == null || client.player == null) {
            entities = List.of();
            return;
        }
        Player player = client.player;
        entities = StreamSupport.stream(client.level.entitiesForRendering().spliterator(), false)
                .filter(entity -> TargetClassifier.isValidLivingTarget(entity, player, false))
                .map(LivingEntity.class::cast)
                .toList();
    }
}
