package net.blockhost.anarchyclient.util;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A claim that a module renews every client tick while it needs control of a vanilla action.
 *
 * <p>The owner calls {@link #claim(String)} from its tick. The vanilla hook calls
 * {@link #consume()} once per tick, which returns whether the claim was renewed since the last
 * check. A module that stops renewing (disabled, target gone) loses the claim on the next check,
 * so vanilla takes over again without explicit cleanup.
 */
public final class TickClaim {

    /** An owner that has not renewed for this long (two ticks) has stopped, even if no vanilla check ran. */
    private static final long STALE_NANOS = 100_000_000L;

    private @Nullable String owner;
    private boolean renewed;
    private long renewedAtNanos;

    public void claim(final String owner) {
        this.claim(owner, System.nanoTime());
    }

    void claim(final String owner, final long nowNanos) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.renewed = true;
        this.renewedAtNanos = nowNanos;
    }

    /** Returns whether the claim was renewed since the previous call, then waits for the next renewal. */
    public boolean consume() {
        boolean held = this.owner != null && this.renewed;
        this.renewed = false;
        if (!held) {
            this.owner = null;
        }
        return held;
    }

    /**
     * Claims only when nobody else holds the claim. The owner keeps it while it renews every tick, so two modules
     * can no longer take turns and undo each other's work.
     */
    public boolean tryClaim(final String owner) {
        return this.tryClaim(owner, System.nanoTime());
    }

    boolean tryClaim(final String owner, final long nowNanos) {
        if (!this.canClaim(owner, nowNanos)) {
            return false;
        }
        this.claim(owner, nowNanos);
        return true;
    }

    /**
     * Whether {@code owner} could claim now. A different owner is refused only while it keeps renewing: {@link
     * #consume()} runs from a vanilla hook that skips ticks with a screen open, so an owner that stopped would
     * otherwise block everyone until the screen closed.
     */
    public boolean canClaim(final String owner) {
        return this.canClaim(owner, System.nanoTime());
    }

    boolean canClaim(final String owner, final long nowNanos) {
        Objects.requireNonNull(owner, "owner");
        return this.owner == null || this.owner.equals(owner) || nowNanos - this.renewedAtNanos > STALE_NANOS;
    }

    public boolean isHeld() {
        return this.owner != null;
    }

    public boolean isHeldBy(final String owner) {
        return this.owner != null && this.owner.equals(owner);
    }

    /** Releases the claim if {@code owner} holds it. Returns whether it did. */
    public boolean release(final String owner) {
        if (!this.isHeldBy(owner)) {
            return false;
        }
        this.owner = null;
        this.renewed = false;
        return true;
    }

    public void clear() {
        this.owner = null;
        this.renewed = false;
    }
}
