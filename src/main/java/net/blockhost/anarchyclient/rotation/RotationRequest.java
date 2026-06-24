package net.blockhost.anarchyclient.rotation;

public record RotationRequest(String owner, Rotation target, int priority, float maxTurnDegrees, int ticksUntilReset,
                              float resetThreshold, RotationTurnMode turnMode, boolean pauseInInventory,
                              Runnable whenReached) {

    public RotationRequest(final String owner, final Rotation target, final int priority, final float maxTurnDegrees,
                           final int ticksUntilReset) {
        this(owner, target, priority, maxTurnDegrees, ticksUntilReset, 1.0F, RotationTurnMode.STEPPED, false, null);
    }

    public RotationRequest(final String owner, final Rotation target, final int priority, final float maxTurnDegrees,
                           final int ticksUntilReset, final float resetThreshold, final RotationTurnMode turnMode,
                           final boolean pauseInInventory) {
        this(owner, target, priority, maxTurnDegrees, ticksUntilReset, resetThreshold, turnMode, pauseInInventory, null);
    }

    public RotationRequest {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Rotation owner is required");
        }
        if (target == null) {
            throw new IllegalArgumentException("Rotation target is required");
        }
        maxTurnDegrees = Math.max(1.0F, maxTurnDegrees);
        ticksUntilReset = Math.max(1, ticksUntilReset);
        resetThreshold = Math.max(0.0F, resetThreshold);
        turnMode = turnMode == null ? RotationTurnMode.STEPPED : turnMode;
    }
}
