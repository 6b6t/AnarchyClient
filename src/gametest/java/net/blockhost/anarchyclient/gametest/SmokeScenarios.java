package net.blockhost.anarchyclient.gametest;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Turns modules on next to entities and blocks they react to, to catch crashes and handler
 * exceptions that no focused scenario covers.
 */
final class SmokeScenarios {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmokeScenarios.class);

    /** Modules that would end the test session or need a setup the smoke world does not have. */
    private static final Set<String> SKIPPED = Set.of(
            "illegal_disconnect",
            "swarm",
            "auto_reconnect",
            "auto_log",
            "anti_quit",
            "panic"
    );

    private SmokeScenarios() {
    }

    /** Enables every render and HUD module for a few frames with players, mobs, items and crystals around. */
    static void renderModules(final ClientGameTestContext context) {
        toggleAll(context, module -> module.category() == ModuleCategory.RENDER || module.category() == ModuleCategory.HUD,
                "render");
    }

    /** Enables every other module for a few ticks in the same busy world. */
    static void otherModules(final ClientGameTestContext context) {
        toggleAll(context, module -> module.category() != ModuleCategory.RENDER && module.category() != ModuleCategory.HUD,
                "other");
    }

    private static void toggleAll(final ClientGameTestContext context, final Predicate<Module> filter,
                                  final String label) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            populate(arena);
            List<String> failures = new ArrayList<>();
            List<Module> modules = context.computeOnClient(client -> AnarchyClient.MODULES.all().stream()
                    .filter(filter)
                    .filter(module -> !SKIPPED.contains(module.id()))
                    .toList());
            for (Module module : modules) {
                HandlerErrors.drain();
                context.runOnClient(client -> module.enabled(true));
                context.waitTicks(4);
                context.runOnClient(client -> module.enabled(false));
                context.waitTick();
                List<String> errors = HandlerErrors.drain();
                if (!errors.isEmpty()) {
                    failures.add(module.id() + " -> " + errors.getFirst());
                }
                boolean inWorld = context.computeOnClient(client -> client.level != null && client.player != null);
                if (!inWorld) {
                    throw new AssertionError(module.id() + " left the world");
                }
                context.setScreen(() -> null);
            }
            LOGGER.info("Smoke tested {} {} modules", modules.size(), label);
            if (!failures.isEmpty()) {
                throw new AssertionError(failures.size() + " " + label + " module(s) threw:\n" + String.join("\n", failures));
            }
        }
    }

    private static void populate(final GameTestSupport.Arena arena) {
        BlockPos origin = arena.origin();
        arena.spawnTarget(origin.offset(4, 0, 0));
        arena.run("summon minecraft:cow " + GameTestSupport.at(origin.offset(-3, 0, 2)) + " {NoAI:1b}");
        arena.run("summon minecraft:zombie " + GameTestSupport.at(origin.offset(-4, 0, -3)) + " {NoAI:1b,PersistenceRequired:1b}");
        arena.run("summon minecraft:end_crystal " + (origin.getX() + 3.5) + " " + origin.getY() + " " + (origin.getZ() - 3.5)
                + " {ShowBottom:0b}");
        arena.run("summon minecraft:item " + GameTestSupport.at(origin.offset(1, 0, 3)) + " {Item:{id:\"minecraft:diamond\",count:1}}");
        arena.setBlock(origin.offset(-2, 0, -2), "chest");
        arena.setBlock(origin.offset(-2, 0, 2), "diamond_ore");
        arena.hole(origin.offset(6, 0, 6));
        arena.give("obsidian 64");
        arena.give("end_crystal 16");
        arena.give("totem_of_undying 2");
        arena.give("diamond_pickaxe");
        arena.give("diamond_sword");
    }

    /** Skybox turns the daytime sky from vanilla blue to its purple default. */
    static void skybox(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.arena(context, world);
            context.getInput().lookAt(0.0F, -60.0F);
            context.waitTicks(10);
            int[] vanilla = topCenter(context.takeScreenshot("skybox_vanilla"));
            GameTestSupport.enable(context, "skybox", true);
            try {
                context.waitTicks(10);
                int[] custom = topCenter(context.takeScreenshot("skybox_custom"));
                LOGGER.info("Sky pixel vanilla {} custom {}", vanilla, custom);
                GameTestSupport.check(vanilla[1] > vanilla[0], "Vanilla sky was not blue: " + Arrays.toString(vanilla));
                GameTestSupport.check(custom[1] < custom[0] && custom[2] > custom[1],
                        "Skybox did not color the sky purple: " + Arrays.toString(custom));
            } finally {
                GameTestSupport.enable(context, "skybox", false);
            }
        }
    }

    /** Fullbright lights up the ground at midnight. */
    static void fullbright(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("time set midnight");
            context.getInput().lookAt(0.0F, 60.0F);
            context.waitTicks(10);
            int vanilla = brightness(lowerCenter(context.takeScreenshot("fullbright_vanilla")));
            GameTestSupport.enable(context, "fullbright", true);
            try {
                context.waitTicks(10);
                int bright = brightness(lowerCenter(context.takeScreenshot("fullbright_on")));
                LOGGER.info("Ground brightness vanilla {} fullbright {}", vanilla, bright);
                GameTestSupport.check(bright > vanilla + 40, "Fullbright raised the ground brightness only from "
                        + vanilla + " to " + bright);
            } finally {
                GameTestSupport.enable(context, "fullbright", false);
            }
        }
    }

    private static int brightness(final int[] rgb) {
        return (rgb[0] + rgb[1] + rgb[2]) / 3;
    }

    private static int[] topCenter(final Path screenshot) {
        return pixel(screenshot, 0.125);
    }

    /** Below the crosshair, which sits in the exact center. */
    private static int[] lowerCenter(final Path screenshot) {
        return pixel(screenshot, 0.75);
    }

    /** Reads the pixel in the middle column at {@code heightFraction} of the image height. */
    private static int[] pixel(final Path screenshot, final double heightFraction) {
        try {
            BufferedImage image = ImageIO.read(screenshot.toFile());
            int rgb = image.getRGB(image.getWidth() / 2, (int) (image.getHeight() * heightFraction));
            return new int[] {rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF};
        } catch (IOException exception) {
            throw new AssertionError("Cannot read " + screenshot, exception);
        }
    }
}
