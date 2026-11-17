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

    private @Nullable String owner;
    private boolean renewed;

    public void claim(final String owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.renewed = true;
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
