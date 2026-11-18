package net.blockhost.anarchyclient.gametest;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.gametest.mixin.EntityAccessor;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.setting.Setting;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

/**
 * Shared setup for client game tests: a cleared arena in a fresh singleplayer world, server-side
 * target players, and helpers for modules and settings.
 */
final class GameTestSupport {

    static final String TARGET = "Target";

    private GameTestSupport() {
    }

    /**
     * Prepares the world for a test: hard difficulty so explosions hurt, daytime, an empty
     * area around the player, a survival player with a clear inventory, and every module off.
     */
    static Arena arena(final ClientGameTestContext context, final TestSingleplayerContext world) {
        context.waitFor(client -> client.player != null && client.level != null);
        context.waitTicks(20);
        TestServerContext server = world.getServer();
        String name = context.computeOnClient(client -> client.player.getScoreboardName());
        BlockPos origin = context.computeOnClient(client -> client.player.blockPosition());
        context.runOnClient(client -> AnarchyClient.MODULES.all().forEach(module -> module.enabled(false)));
        server.runCommand("difficulty hard");
        server.runCommand("time set day");
        server.runCommand("fill " + at(origin.offset(-8, 0, -8)) + " " + at(origin.offset(8, 6, 8)) + " air");
        server.runCommand("gamemode survival " + name);
        server.runCommand("clear " + name);
        server.runCommand("effect clear " + name);
        Arena arena = new Arena(context, server, name, origin);
        arena.teleport(origin, -90.0F);
        context.waitTicks(5);
        return arena;
    }

    static String at(final BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    static Module module(final String id) {
        return AnarchyClient.MODULES.find(id).orElseThrow(() -> new AssertionError("Missing module " + id));
    }

    /** Sets a module setting through its JSON form, the same path a loaded config takes. */
    static void setting(final ClientGameTestContext context, final String moduleId, final String settingId,
                        final JsonElement value) {
        context.runOnClient(client -> {
            Setting<?> setting = module(moduleId).settings().stream()
                    .filter(candidate -> candidate.id().equals(settingId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Missing setting " + moduleId + "." + settingId));
            setting.fromJson(value);
        });
    }

    static void enable(final ClientGameTestContext context, final String moduleId, final boolean enabled) {
        context.runOnClient(client -> module(moduleId).enabled(enabled));
    }

    static void check(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Clears the short invulnerability after a hit. Stand-in players never tick, so it would never
     * wear off by itself.
     */
    static void resetInvulnerability(final Entity entity) {
        ((EntityAccessor) entity).anarchyclientGametest$setInvulnerableTime(0);
    }

    /** The target's health plus absorption, or -1 when it is gone. Call it on the server thread. */
    static float targetHealth(final MinecraftServer server) {
        return server.overworld().players().stream()
                .filter(player -> TARGET.equals(player.getScoreboardName()))
                .findFirst()
                .map(player -> player.getHealth() + player.getAbsorptionAmount())
                .orElse(-1.0F);
    }

    record Arena(ClientGameTestContext context, TestServerContext server, String name, BlockPos origin) {

        void run(final String command) {
            this.server.runCommand(command);
        }

        void give(final String item) {
            this.run("give " + this.name + " " + item);
        }

        /** Puts an item into a hotbar slot, 0 to 8. */
        void hotbar(final int slot, final String item) {
            this.run("item replace entity " + this.name + " hotbar." + slot + " with " + item);
        }

        void setBlock(final BlockPos pos, final String block) {
            this.run("setblock " + at(pos) + " " + block);
        }

        void teleport(final BlockPos feet, final float yaw) {
            this.run("tp " + this.name + " " + (feet.getX() + 0.5) + " " + feet.getY() + " " + (feet.getZ() + 0.5)
                    + " " + yaw + " 0");
        }

        /** Surrounds {@code feet} with obsidian on all four sides and below. */
        void hole(final BlockPos feet) {
            this.setBlock(feet.below(), "obsidian");
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                this.setBlock(feet.relative(direction), "obsidian");
            }
        }

        BlockState blockAt(final BlockPos pos) {
            return this.server.computeOnServer(server -> server.overworld().getBlockState(pos));
        }

        /** Adds a damageable player named {@value #TARGET} at {@code feet} and waits until the client sees it. */
        void spawnTarget(final BlockPos feet) {
            this.server.runOnServer(server -> {
                ServerLevel level = server.overworld();
                FakePlayer target = new DamageableTarget(level, new GameProfile(UUID.randomUUID(), TARGET));
                target.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 90.0F, 0.0F);
                server.getPlayerList().broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(target)));
                level.addNewPlayer(target);
            });
            this.context.waitFor(client -> client.level.players().stream()
                    .anyMatch(player -> TARGET.equals(player.getScoreboardName())));
        }

        float targetHealth() {
            return this.server.computeOnServer(GameTestSupport::targetHealth);
        }

        ServerPlayer serverPlayer(final MinecraftServer server) {
            return server.getPlayerList().getPlayerByName(this.name);
        }
    }

    /** A server-side stand-in player. Fabric's fake players ignore damage, which these tests need. */
    private static final class DamageableTarget extends FakePlayer {

        private DamageableTarget(final ServerLevel level, final GameProfile profile) {
            super(level, profile);
        }

        @Override
        public boolean isInvulnerableTo(final ServerLevel level, final DamageSource source) {
            return false;
        }
    }
}
