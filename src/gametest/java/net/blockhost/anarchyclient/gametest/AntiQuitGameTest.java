package net.blockhost.anarchyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;

/** Anti Quit asks before Save and Quit leaves the world, and No keeps the player in it. */
public final class AntiQuitGameTest implements GameScenario {

    @Override
    public void run(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.arena(context, world);
            GameTestSupport.enable(context, "anti_quit", true);
            try {
                context.setScreen(() -> new PauseScreen(true));
                context.clickScreenButton("menu.returnToMenu");
                context.waitForScreen(ConfirmScreen.class);
                context.takeScreenshot("anti_quit_confirm");
                GameTestSupport.check(
                        context.computeOnClient(client -> client.level != null),
                        "Save and Quit left the world before confirmation");

                context.clickScreenButton("gui.no");
                context.waitForScreen(PauseScreen.class);
                GameTestSupport.check(
                        context.computeOnClient(client -> client.level != null), "Choosing No left the world");
            } finally {
                GameTestSupport.enable(context, "anti_quit", false);
                context.setScreen(() -> null);
            }
        }
    }
}
