package net.blockhost.anarchyclient.rotation;

import net.minecraft.world.phys.Vec3;

public record Rotation(float yaw, float pitch) {

    public static Rotation lookingAt(final Vec3 point, final Vec3 from) {
        Vec3 delta = point.subtract(from);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        return new Rotation(
                (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F,
                (float) -Math.toDegrees(Math.atan2(delta.y, horizontal))
        ).clampPitch();
    }

    public Rotation stepToward(final Rotation target, final float maxStep) {
        float yawDelta = wrapDegrees(target.yaw - this.yaw);
        float pitchDelta = wrapDegrees(target.pitch - this.pitch);
        return new Rotation(
                this.yaw + clamp(yawDelta, maxStep),
                this.pitch + clamp(pitchDelta, maxStep)
        ).clampPitch();
    }

    public Rotation linearStepToward(final Rotation target, final float maxStep) {
        double angle = this.angleTo(target);
        if (angle <= maxStep) {
            return target.clampPitch();
        }
        double factor = maxStep / angle;
        float yawDelta = wrapDegrees(target.yaw - this.yaw);
        float pitchDelta = wrapDegrees(target.pitch - this.pitch);
        return new Rotation(
                this.yaw + (float) (yawDelta * factor),
                this.pitch + (float) (pitchDelta * factor)
        ).clampPitch();
    }

    public double angleTo(final Rotation target) {
        double yawDelta = wrapDegrees(target.yaw - this.yaw);
        double pitchDelta = wrapDegrees(target.pitch - this.pitch);
        return Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }

    public Rotation clampPitch() {
        return new Rotation(this.yaw, Math.max(-90.0F, Math.min(90.0F, this.pitch)));
    }

    private static float clamp(final float value, final float maxStep) {
        return Math.max(-maxStep, Math.min(maxStep, value));
    }

    public static float wrapDegrees(final float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }
}
