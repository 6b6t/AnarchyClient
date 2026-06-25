package net.blockhost.anarchyclient.module.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EspOutlineRegistry {

    private static final Map<Integer, Integer> COLORS = new ConcurrentHashMap<>();

    private EspOutlineRegistry() {
    }

    public static void set(final int entityId, final int argb) {
        COLORS.put(entityId, argb);
    }

    public static int get(final int entityId) {
        return COLORS.getOrDefault(entityId, 0);
    }

    public static void clear() {
        COLORS.clear();
    }
}
