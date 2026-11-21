package net.blockhost.anarchyclient.gametest;

import java.util.Optional;
import java.util.stream.StreamSupport;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Scenarios for chat, camera, and other small modules. */
final class MiscModuleScenarios {

    /** The Fake Player module's default name. */
    private static final String FAKE_PLAYER = "AnarchyClient";

    private MiscModuleScenarios() {}

    static void chatFilter(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            GameTestSupport.enable(context, "chat_filter", true);
            try {
                arena.run("tellraw " + arena.name() + " \"join discord.gg/abcdef now\"");
                arena.run("tellraw " + arena.name() + " \"plain hello\"");
                context.waitFor(client -> ClientChat.contains(client, "plain hello"), 40);
                GameTestSupport.check(
                        !context.computeOnClient(client -> ClientChat.contains(client, "discord.gg/abcdef")),
                        "Chat Filter showed a message that matches its default filter");
            } finally {
                GameTestSupport.enable(context, "chat_filter", false);
            }
        }
    }

    static void antiCyrillic(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.arena(context, world);
            GameTestSupport.enable(context, "anti_cyrillic", true);
            try {
                context.runOnClient(client -> client.player.connection.sendChat("Привет мир"));
                context.waitFor(client -> ClientChat.contains(client, "Privet mir"), 60);
            } finally {
                GameTestSupport.enable(context, "anti_cyrillic", false);
            }
        }
    }

    static void antiAim(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            context.runOnClient(client -> client.player.setXRot(0.0F));
            GameTestSupport.enable(context, "anti_aim", true);
            try {
                arena.server().waitFor(server -> arena.serverPlayer(server).getXRot() > 80.0F, 80);
                float clientPitch = context.computeOnClient(client -> client.player.getXRot());
                GameTestSupport.check(
                        Math.abs(clientPitch) < 1.0F, "Anti Aim moved the real camera to pitch " + clientPitch);
            } finally {
                GameTestSupport.enable(context, "anti_aim", false);
            }
        }
    }

    static void killEffects(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.spawnMortalTarget(arena.origin().offset(4, 0, 0));
            GameTestSupport.enable(context, "kill_effects", true);
            try {
                context.waitTicks(5);
                arena.server()
                        .runOnServer(server -> server.overworld().players().stream()
                                .filter(player -> GameTestSupport.TARGET.equals(player.getScoreboardName()))
                                .findFirst()
                                .ifPresent(target -> target.hurtServer(
                                        server.overworld(),
                                        server.overworld().damageSources().source(DamageTypes.GENERIC_KILL),
                                        Float.MAX_VALUE)));
                context.waitFor(
                        client -> StreamSupport.stream(
                                        client.level.entitiesForRendering().spliterator(), false)
                                .anyMatch(entity -> entity instanceof LightningBolt),
                        60);
            } finally {
                GameTestSupport.enable(context, "kill_effects", false);
            }
        }
    }

    static void heaven(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            GameTestSupport.enable(context, "heaven", true);
            try {
                arena.run("kill " + arena.name());
                context.waitForScreen(DeathScreen.class);
                context.waitTicks(60);
                double rise = context.computeOnClient(
                        client -> client.gameRenderer.mainCamera().position().y - client.player.getEyeY());
                GameTestSupport.check(rise > 3.0, "Heaven raised the death camera by only " + rise + " blocks");
                context.takeScreenshot("heaven_death_camera");
            } finally {
                GameTestSupport.enable(context, "heaven", false);
                context.clickScreenButton("deathScreen.respawn");
                context.waitFor(client -> client.player != null && client.player.isAlive());
            }
        }
    }

    static void portalGodMode(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("execute in minecraft:the_nether run forceload add 0 0");
            arena.run("execute in minecraft:the_nether run fill -2 120 -2 2 124 2 air");
            arena.run("execute in minecraft:the_nether run fill -2 119 -2 2 119 2 obsidian");
            GameTestSupport.enable(context, "portal_god_mode", true);
            try {
                arena.run("execute in minecraft:the_nether run tp " + arena.name() + " 0.5 120 0.5");
                context.waitFor(
                        client -> "the_nether"
                                .equals(client.level.dimension().identifier().getPath()),
                        200);
                context.waitTicks(20);
                arena.run("damage " + arena.name() + " 6 minecraft:generic");
                context.waitTicks(5);
                float held = arena.server()
                        .computeOnServer(server -> arena.serverPlayer(server).getHealth());
                GameTestSupport.check(held >= 20.0F, "Portal God Mode let damage through: health " + held);
            } finally {
                GameTestSupport.enable(context, "portal_god_mode", false);
            }
            context.waitTicks(10);
            arena.run("damage " + arena.name() + " 6 minecraft:generic");
            arena.server().waitFor(server -> arena.serverPlayer(server).getHealth() < 20.0F, 40);
        }
    }

    /** Fake Player adds a client-only copy of the player and removes it when turned off. */
    static void fakePlayer(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            GameTestSupport.Arena arena = GameTestSupport.arena(context, world);
            arena.run("item replace entity " + arena.name() + " armor.chest with diamond_chestplate");
            context.waitTicks(5);
            GameTestSupport.enable(context, "fake_player", true);
            try {
                context.waitFor(client -> fakePlayer(client).isPresent(), 20);
                boolean equipped = context.computeOnClient(client -> fakePlayer(client)
                        .orElseThrow()
                        .getItemBySlot(EquipmentSlot.CHEST)
                        .is(Items.DIAMOND_CHESTPLATE));
                GameTestSupport.check(equipped, "Fake Player did not copy the chestplate");
                boolean serverSees = arena.server()
                        .computeOnServer(server -> server.overworld().players().stream()
                                .anyMatch(player -> FAKE_PLAYER.equals(player.getScoreboardName())));
                GameTestSupport.check(!serverSees, "Fake Player exists on the server");
            } finally {
                GameTestSupport.enable(context, "fake_player", false);
            }
            context.waitTick();
            GameTestSupport.check(
                    context.computeOnClient(client -> fakePlayer(client).isEmpty()),
                    "Fake Player stayed after the module was turned off");
        }
    }

    private static Optional<Player> fakePlayer(final Minecraft client) {
        return client.level.players().stream()
                .filter(player -> FAKE_PLAYER.equals(player.getScoreboardName()) && player.getId() < 0)
                .map(Player.class::cast)
                .findFirst();
    }
}
