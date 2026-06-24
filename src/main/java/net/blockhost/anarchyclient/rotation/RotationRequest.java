package net.blockhost.anarchyclient.rotation;

public record RotationRequest(String owner, Rotation target, int priority, float maxTurnDegrees, int ticksUntilReset) {

    public RotationRequest {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Rotation owner is required");
        }
        maxTurnDegrees = Math.max(1.0F, maxTurnDegrees);
        ticksUntilReset = Math.max(1, ticksUntilReset);
    }
}
