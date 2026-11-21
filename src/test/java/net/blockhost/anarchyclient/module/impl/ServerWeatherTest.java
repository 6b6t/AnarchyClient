package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import org.junit.jupiter.api.Test;

class ServerWeatherTest {

    @Test
    void tracksServerRainAndThunderLevels() {
        ServerWeather weather = new ServerWeather();
        weather.observe(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
        weather.observe(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 0.6F));
        weather.observe(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 1.0F));

        assertEquals(0.6F, weather.rain());
        assertEquals(1.0F, weather.thunder());
    }

    @Test
    void stopRainingStartsTheFadeOutFromFullRain() {
        ServerWeather weather = new ServerWeather();
        weather.observe(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0F));

        assertEquals(1.0F, weather.rain());
    }
}
