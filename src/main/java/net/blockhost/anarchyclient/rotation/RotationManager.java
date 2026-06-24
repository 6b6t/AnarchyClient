package net.blockhost.anarchyclient.rotation;

import net.blockhost.anarchyclient.request.TimedRequestQueue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class RotationManager {

    private static final TimedRequestQueue<RotationRequest> REQUESTS = new TimedRequestQueue<>();
    private static final Set<String> REACHED_OWNERS = new HashSet<>();

    private RotationManager() {
    }

    public static void request(final RotationRequest request) {
        REQUESTS.request(request.owner(), request.priority(), request.ticksUntilReset(), request);
        REACHED_OWNERS.remove(request.owner());
    }

    public static Optional<RotationRequest> activeRequest() {
        return REQUESTS.activeValue();
    }

    public static boolean hasActiveRequest() {
        return activeRequest().isPresent();
    }

    public static Optional<Rotation> apply(final Player player) {
        Optional<RotationRequest> active = activeRequest();
        if (active.isEmpty()) {
            return Optional.empty();
        }
        RotationRequest request = active.orElseThrow();
        if (request.pauseInInventory() && player.containerMenu != null
                && player.containerMenu.containerId != InventoryMenu.CONTAINER_ID) {
            return Optional.empty();
        }
        Rotation current = new Rotation(player.getYRot(), player.getXRot());
        Rotation next = switch (request.turnMode()) {
            case INSTANT -> request.target().clampPitch();
            case LINEAR -> current.linearStepToward(request.target(), request.maxTurnDegrees());
            case STEPPED -> current.stepToward(request.target(), request.maxTurnDegrees());
        };
        player.setYRot(next.yaw());
        player.setXRot(next.pitch());
        if (next.angleTo(request.target()) <= request.resetThreshold() && request.whenReached() != null
                && REACHED_OWNERS.add(request.owner())) {
            request.whenReached().run();
        }
        return Optional.of(next);
    }

    public static void tick() {
        REQUESTS.tick();
    }

    public static void clear(final String owner) {
        REQUESTS.clear(owner);
        REACHED_OWNERS.remove(owner);
    }

    public static void clearAll() {
        REQUESTS.clearAll();
        REACHED_OWNERS.clear();
    }
}
