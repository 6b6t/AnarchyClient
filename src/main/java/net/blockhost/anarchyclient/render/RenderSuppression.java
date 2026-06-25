package net.blockhost.anarchyclient.render;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RenderSuppression {

    private static final Map<String, EnumSet<Kind>> REQUESTS = new ConcurrentHashMap<>();

    private RenderSuppression() {
    }

    public static void enable(final String owner, final Kind... kinds) {
        if (owner == null || owner.isBlank() || kinds == null || kinds.length == 0) {
            return;
        }
        EnumSet<Kind> set = EnumSet.noneOf(Kind.class);
        for (Kind kind : kinds) {
            if (kind != null) {
                set.add(kind);
            }
        }
        if (set.isEmpty()) {
            REQUESTS.remove(owner);
        } else {
            REQUESTS.put(owner, set);
        }
    }

    public static void disable(final String owner) {
        if (owner != null) {
            REQUESTS.remove(owner);
        }
    }

    public static boolean suppresses(final Kind kind) {
        if (kind == null) {
            return false;
        }
        for (EnumSet<Kind> kinds : REQUESTS.values()) {
            if (kinds.contains(kind)) {
                return true;
            }
        }
        return false;
    }

    public static void clear() {
        REQUESTS.clear();
    }

    public enum Kind {
        BLINDNESS,
        DARKNESS,
        HURT_CAMERA,
        VIEW_BOB,
        CAMERA_CLIP,
        DYNAMIC_FOV
    }
}
