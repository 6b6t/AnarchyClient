package net.blockhost.anarchyclient.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TickClaimTest {

    @Test
    void holdsWhileRenewedEveryTick() {
        TickClaim claim = new TickClaim();

        claim.claim("nuker");
        assertTrue(claim.consume());
        claim.claim("nuker");
        assertTrue(claim.consume());
        assertTrue(claim.isHeldBy("nuker"));
    }

    @Test
    void expiresWhenOwnerStopsRenewing() {
        TickClaim claim = new TickClaim();

        claim.claim("nuker");
        assertTrue(claim.consume());
        assertFalse(claim.consume());
        assertFalse(claim.isHeld());
    }

    @Test
    void onlyOwnerCanRelease() {
        TickClaim claim = new TickClaim();
        claim.claim("nuker");

        assertFalse(claim.release("excavator"));
        assertTrue(claim.release("nuker"));
        assertFalse(claim.consume());
    }
}
