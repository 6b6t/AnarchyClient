package net.blockhost.anarchyclient.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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

    @Test
    void theCurrentOwnerKeepsTheClaimWhileItRenews() {
        TickClaim claim = new TickClaim();
        assertTrue(claim.tryClaim("nuker"));
        claim.consume();

        assertFalse(claim.tryClaim("packet_mine"));
        assertTrue(claim.tryClaim("nuker"));
        claim.consume();
        claim.consume();

        assertTrue(claim.tryClaim("packet_mine"));
    }

    @Test
    void anOwnerThatStoppedRenewingLosesTheClaimWithoutAVanillaCheck() {
        // With a screen open, consume() never runs; the claim must still lapse once the owner stops.
        TickClaim claim = new TickClaim();
        assertTrue(claim.tryClaim("nuker", 10L));

        assertFalse(claim.tryClaim("packet_mine", 11L));
        assertTrue(claim.tryClaim("packet_mine", 12L));
    }
}
