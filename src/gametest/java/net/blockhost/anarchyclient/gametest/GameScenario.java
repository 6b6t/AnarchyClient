package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** One client game test scenario, run by {@link AnarchyClientGameTests}. */
interface GameScenario {

    void run(ClientGameTestContext context);
}
