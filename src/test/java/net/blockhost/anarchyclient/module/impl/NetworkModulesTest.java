package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NetworkModulesTest {

    @Test
    void chatIsCappedAtTheChatPacketLimitAndStripsIllegalCharacters() {
        assertEquals(256, ChatActions.normalize("a".repeat(300)).length());
        // Like the chat screen: the section sign and control characters go, everything else stays.
        assertEquals("hello cworld", ChatActions.normalize("  hello \u00a7cworld\u0007 "));
    }

    @Test
    void serverSpoofKeepsBungeeCordForwardingData() {
        assertEquals("localhost", ServerSpoofModule.spoofedHost("play.example.org", "localhost"));
        assertEquals(
                "localhost\u00001.2.3.4\u0000uuid",
                ServerSpoofModule.spoofedHost("play.example.org\u00001.2.3.4\u0000uuid", "localhost"));
    }

    @Test
    void autoLoginReadsTheTransferHostWithoutForwardingData() {
        assertEquals("lobby.example.org:25566", AutoLoginModule.handshakeHost("Lobby.Example.org\u00001.2.3.4", 25566));
    }

    @Test
    void animationsDependOnThePartialTick() {
        double start = AnimationsModule.transform("Slide", 10.0, 1.0, 0.25).x();
        double halfway = AnimationsModule.transform("Slide", 10.5, 1.0, 0.25).x();

        assertEquals(false, start == halfway);
        assertEquals(AnimationsModule.AnimationTransform.identity(), AnimationsModule.transform("None", 3.0, 1.0, 1.0));
    }
}
