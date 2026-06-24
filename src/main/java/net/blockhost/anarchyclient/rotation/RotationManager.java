package net.blockhost.anarchyclient.rotation;

import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class RotationManager {

    private static final Map<String, ActiveRequest> REQUESTS = new HashMap<>();

    private RotationManager() {
    }

    public static void request(final RotationRequest request) {
        REQUESTS.put(request.owner(), new ActiveRequest(request, request.ticksUntilReset()));
    }

    public static Optional<RotationRequest> activeRequest() {
        return REQUESTS.values().stream()
                .map(ActiveRequest::request)
                .max(Comparator.comparingInt(RotationRequest::priority));
    }

    public static Optional<Rotation> apply(final Player player) {
        Optional<RotationRequest> active = activeRequest();
        if (active.isEmpty()) {
            return Optional.empty();
        }
        RotationRequest request = active.orElseThrow();
        Rotation current = new Rotation(player.getYRot(), player.getXRot());
        Rotation next = current.stepToward(request.target(), request.maxTurnDegrees());
        player.setYRot(next.yaw());
        player.setXRot(next.pitch());
        return Optional.of(next);
    }

    public static void tick() {
        REQUESTS.entrySet().removeIf(entry -> entry.getValue().tickExpired());
    }

    public static void clear(final String owner) {
        REQUESTS.remove(owner);
    }

    public static void clearAll() {
        REQUESTS.clear();
    }

    private static final class ActiveRequest {

        private final RotationRequest request;
        private int remainingTicks;

        private ActiveRequest(final RotationRequest request, final int remainingTicks) {
            this.request = request;
            this.remainingTicks = remainingTicks;
        }

        private RotationRequest request() {
            return this.request;
        }

        private boolean tickExpired() {
            this.remainingTicks--;
            return this.remainingTicks <= 1;
        }
    }
}
