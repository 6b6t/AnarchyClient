package net.blockhost.anarchyclient.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MarkerManager {

    private static final Map<String, Marker> MARKERS = new ConcurrentHashMap<>();

    private MarkerManager() {
    }

    public static void put(final Marker marker) {
        if (marker != null && marker.id() != null && !marker.id().isBlank()) {
            MARKERS.put(marker.id(), marker);
        }
    }

    public static boolean remove(final String id) {
        return MARKERS.remove(id) != null;
    }

    public static Collection<Marker> markers() {
        return List.copyOf(MARKERS.values());
    }

    public static void tick() {
        for (Map.Entry<String, Marker> entry : MARKERS.entrySet()) {
            Marker ticked = entry.getValue().ticked();
            if (ticked.expired()) {
                MARKERS.remove(entry.getKey(), entry.getValue());
            } else {
                MARKERS.replace(entry.getKey(), entry.getValue(), ticked);
            }
        }
    }

    public static void render(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        for (Marker marker : MARKERS.values()) {
            marker.render(context, camera);
        }
    }

    public static void clear() {
        MARKERS.clear();
    }
}
