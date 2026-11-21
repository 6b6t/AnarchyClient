package net.blockhost.anarchyclient.rotation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.blockhost.anarchyclient.request.TimedRequestQueue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RotationManager {

    private static final TimedRequestQueue<RotationRequest> REQUESTS = new TimedRequestQueue<>();
    private static final Set<String> REACHED_OWNERS = new HashSet<>();

    private RotationManager() {}

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
        if (request.pauseInInventory()
                && player.containerMenu != null
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
        Runnable whenReached = request.whenReached();
        if (next.angleTo(request.target()) <= request.resetThreshold()
                && whenReached != null
                && REACHED_OWNERS.add(request.owner())) {
            whenReached.run();
        }
        return Optional.of(next);
    }

    /** Returns whether {@code owner} holds the request {@link #apply} currently follows. */
    public static boolean isActiveOwner(final String owner) {
        return activeRequest().map(request -> request.owner().equals(owner)).orElse(false);
    }

    /**
     * Returns the rotation the server last received. Rotations set by {@link #apply} reach the
     * server with the next movement packet, so actions must wait until this rotation matches.
     * Before any rotation was sent, the player's current rotation is used.
     */
    public static Rotation serverRotation(final LocalPlayer player) {
        return ServerRotationTracker.lastSent().orElseGet(() -> new Rotation(player.getYRot(), player.getXRot()));
    }

    /**
     * Returns whether the server-side look ray from the player's eyes hits {@code box} within
     * {@code range}. Use it to delay a click until the rotation has been sent.
     */
    public static boolean serverLookHits(final LocalPlayer player, final AABB box, final double range) {
        return lookHits(player.getEyePosition(), serverRotation(player), box, range);
    }

    static boolean lookHits(final Vec3 eye, final Rotation rotation, final AABB box, final double range) {
        if (box.contains(eye)) {
            return true;
        }
        Vec3 end = eye.add(
                Vec3.directionFromRotation(rotation.pitch(), rotation.yaw()).scale(range));
        return box.clip(eye, end).isPresent();
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
