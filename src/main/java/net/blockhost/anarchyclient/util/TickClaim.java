package net.blockhost.anarchyclient.util;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A claim that a module renews every client tick while it needs control of a vanilla action.
 *
 * <p>The owner calls {@link #claim(String)} from its tick. The vanilla hook calls
 * {@link #consume()} once per tick, which returns whether the claim was renewed since the last
 * check. A module that stops renewing (disabled, target gone) loses the claim on the next check,
 * so vanilla takes over again without explicit cleanup.
 */
public final class TickClaim {

    /** An owner that missed this many client ticks has stopped, even if no vanilla check ran. */
    private static final long STALE_TICKS = 2;

    private @Nullable String owner;
    private boolean renewed;
    private long renewedAtTick;

    public void claim(final String owner) {
        this.claim(owner, ClientTicks.current());
    }

    void claim(final String owner, final long nowTick) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.renewed = true;
        this.renewedAtTick = nowTick;
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
        return this.tryClaim(owner, ClientTicks.current());
    }

    boolean tryClaim(final String owner, final long nowTick) {
        if (!this.canClaim(owner, nowTick)) {
            return false;
        }
        this.claim(owner, nowTick);
        return true;
    }

    /**
     * Whether {@code owner} could claim now. A different owner is refused only while it keeps renewing: {@link
     * #consume()} runs from a vanilla hook that skips ticks with a screen open, so an owner that stopped would
     * otherwise block everyone until the screen closed.
     */
    public boolean canClaim(final String owner) {
        return this.canClaim(owner, ClientTicks.current());
    }

    /** Counted in client ticks, so a lag spike or a paused game does not make a live claim look stale. */
    boolean canClaim(final String owner, final long nowTick) {
        Objects.requireNonNull(owner, "owner");
        return this.owner == null || this.owner.equals(owner) || nowTick - this.renewedAtTick >= STALE_TICKS;
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
