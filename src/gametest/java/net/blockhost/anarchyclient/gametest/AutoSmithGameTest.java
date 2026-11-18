package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Auto Smith upgrades a diamond sword to netherite once a smithing table is open. */
public final class AutoSmithGameTest implements GameScenario {

    @Override
    public void run(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            BlockPos table = arena.origin().offset(2, 0, 0);
            arena.setBlock(table, "smithing_table");
            arena.give("diamond_sword");
            arena.give("netherite_upgrade_smithing_template");
            arena.give("netherite_ingot");
            context.waitTicks(5);
            context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(table).relative(Direction.UP, 0.5), Direction.UP, table, false)));
            context.waitForScreen(SmithingScreen.class);

            GameTestSupport.enable(context, "auto_smith", true);
            try {
                arena.server().waitFor(server -> {
                    var inventory = arena.serverPlayer(server).getInventory();
                    return inventory.contains(stack -> stack.is(Items.NETHERITE_SWORD))
                            && !inventory.contains(stack -> stack.is(Items.DIAMOND_SWORD));
                }, 100);
            } finally {
                context.takeScreenshot("auto_smith_result");
                GameTestSupport.enable(context, "auto_smith", false);
                context.setScreen(() -> null);
            }
        }
    }
}
