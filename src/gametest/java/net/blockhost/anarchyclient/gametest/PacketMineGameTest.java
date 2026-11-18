package net.blockhost.anarchyclient.gametest;

import com.google.gson.JsonPrimitive;
import net.blockhost.anarchyclient.module.DebugValueRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;

/**
 * Packet Mine's Progress mode mines a clicked obsidian block in the background while the player
 * looks away and holds dirt, and finishes it with the pickaxe from another hotbar slot.
 */
public final class PacketMineGameTest implements FabricClientGameTest {

    @Override
    public void runTest(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos block = arena.origin().offset(2, 1, 0);
            arena.setBlock(block, "obsidian");
            arena.hotbar(0, "dirt");
            arena.hotbar(3, "diamond_pickaxe[enchantments={efficiency:5}]");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            GameTestSupport.setting(context, "packet_mine", "mode", new JsonPrimitive("Progress"));
            GameTestSupport.enable(context, "packet_mine", true);
            context.waitTicks(5);
            try {
                context.getInput().lookAt(block);
                context.waitTicks(2);
                context.getInput().pressKey(options -> options.keyAttack);
                context.waitTicks(2);
                // Look away: the break must finish without the crosshair on the block.
                context.getInput().lookAt(90.0F, -30.0F);
                arena.server().waitFor(server -> server.overworld().getBlockState(block).isAir(), 120);
                context.waitTicks(5);
                int selected = context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
                GameTestSupport.check(selected == 0, "Packet Mine left the hotbar on slot " + selected);
            } catch (AssertionError failure) {
                throw new AssertionError("Packet Mine did not finish the block; debug "
                        + DebugValueRegistry.snapshot().get("packet_mine"), failure);
            } finally {
                GameTestSupport.enable(context, "packet_mine", false);
                GameTestSupport.setting(context, "packet_mine", "mode", new JsonPrimitive("Instant"));
            }
        }
    }
}
