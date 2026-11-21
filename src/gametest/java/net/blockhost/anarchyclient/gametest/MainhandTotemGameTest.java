package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.item.Items;

/**
 * Mainhand Totem pulls a totem from storage into the totem slot and holds it at low health, then
 * returns to the previous slot once the player is healed.
 */
public final class MainhandTotemGameTest implements GameScenario {

    @Override
    public void run(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.hotbar(0, "dirt");
            arena.run("item replace entity " + arena.name() + " inventory.5 with totem_of_undying");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            context.waitTicks(5);

            GameTestSupport.enable(context, "mainhand_totem", true);
            try {
                context.waitTicks(10);
                GameTestSupport.check(
                        context.computeOnClient(
                                        client -> client.player.getInventory().getSelectedSlot())
                                == 0,
                        "Mainhand Totem switched while the player was healthy");

                arena.run("damage " + arena.name() + " 15");
                context.waitFor(client -> client.player.getMainHandItem().is(Items.TOTEM_OF_UNDYING), 60);
                arena.server()
                        .waitFor(
                                server -> arena.serverPlayer(server)
                                        .getMainHandItem()
                                        .is(Items.TOTEM_OF_UNDYING),
                                40);
                GameTestSupport.check(
                        context.computeOnClient(
                                        client -> client.player.getInventory().getSelectedSlot())
                                == 8,
                        "Mainhand Totem did not use the configured totem slot");

                arena.run("effect give " + arena.name() + " instant_health 1 5");
                context.waitFor(client -> client.player.getInventory().getSelectedSlot() == 0, 60);
            } finally {
                GameTestSupport.enable(context, "mainhand_totem", false);
            }
        }
    }
}
