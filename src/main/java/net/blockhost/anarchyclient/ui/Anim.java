package net.blockhost.anarchyclient.ui;

/**
 * Exponentially smoothed value for lightweight per-frame animation. The menu re-renders every
 * frame, so calling {@link #toward} inside {@code render} advances the animation without extra
 * layout invalidation.
 */
final class Anim {

    private float value;
    private boolean initialized;
    private long lastNanos;

    float toward(final float target, final float speed) {
        long now = System.nanoTime();
        if (!this.initialized) {
            this.initialized = true;
            this.value = target;
            this.lastNanos = now;
            return this.value;
        }
        float deltaSeconds = Math.min(0.1F, (now - this.lastNanos) / 1_000_000_000F);
        this.lastNanos = now;
        this.value += (target - this.value) * (1F - (float) Math.exp(-speed * deltaSeconds));
        if (Math.abs(target - this.value) < 0.002F) {
            this.value = target;
        }
        return this.value;
    }
}
