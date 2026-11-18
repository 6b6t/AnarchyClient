package net.blockhost.anarchyclient.gametest;

import com.mojang.authlib.GameProfile;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.DebugValueRegistry;
import net.blockhost.anarchyclient.module.Module;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Drives Piston Crystal against a server-side player standing in an obsidian hole.
 *
 * <p>Layout along the x axis, at the player's feet level y: the target stands at T in a hole of
 * obsidian, the crystal goes on the surround block west of T at head height, the piston one block
 * further west on an obsidian support, and the player stands two blocks south of the piston.
 */
public final class PistonCrystalGameTest implements FabricClientGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("PistonCrystalGameTest");

    @Override
    public void runTest(final ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.level != null);
            context.waitTicks(40);
            TestServerContext server = world.getServer();
            String name = context.computeOnClient(client -> client.player.getScoreboardName());
            BlockPos origin = context.computeOnClient(client -> client.player.blockPosition());
            BlockPos target = origin.offset(3, 0, 0);
            // Beside the piston, within the 3 block attack reach of the pushed crystal, and clear of
            // the spot behind the piston where the redstone block goes.
            BlockPos me = target.offset(-2, 0, 2);
            BlockPos piston = target.offset(-2, 1, 0);

            server.runCommand("difficulty hard");
            server.runCommand("gamerule doDaylightCycle false");
            server.runCommand("time set day");
            server.runCommand("fill " + box(origin.offset(-4, 0, -4), origin.offset(8, 4, 4)) + " air");
            for (BlockPos pos : List.of(target.below(), target.east(), target.west(), target.north(), target.south(),
                    target.offset(-2, 0, 0))) {
                server.runCommand("setblock " + at(pos) + " obsidian");
            }
            server.runCommand("gamemode survival " + name);
            server.runCommand("effect give " + name + " resistance infinite 4 true");
            server.runCommand("clear " + name);
            server.runCommand("give " + name + " piston 16");
            server.runCommand("give " + name + " end_crystal 16");
            server.runCommand("give " + name + " redstone_block 16");
            server.runCommand("give " + name + " diamond_pickaxe[enchantments={efficiency:5}]");
            server.runCommand("tp " + name + " " + (me.getX() + 0.5) + " " + me.getY() + " " + (me.getZ() + 0.5) + " 135 0");

            server.runOnServer(minecraftServer -> {
                ServerLevel level = minecraftServer.overworld();
                FakePlayer dummy = new DamageableTarget(level, new GameProfile(UUID.randomUUID(), "Target"));
                dummy.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 90.0F, 0.0F);
                minecraftServer.getPlayerList().broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(dummy)));
                level.addNewPlayer(dummy);
            });
            context.waitFor(client -> client.level.players().stream().anyMatch(player -> "Target".equals(player.getScoreboardName())));
            context.waitTicks(20);
            context.takeScreenshot("piston_crystal_setup");

            context.runOnClient(client -> module().enabled(true));
            float startHealth = targetHealth(server);
            LOGGER.info("Target health before: {}", startHealth);

            boolean sawPiston = false;
            boolean sawCrystal = false;
            boolean sawExtended = false;
            int firstHitTick = -1;
            for (int tick = 0; tick < 300; tick++) {
                context.waitTick();
                BlockState pistonState = server.computeOnServer(minecraftServer -> minecraftServer.overworld().getBlockState(piston));
                List<String> crystalPositions = server.computeOnServer(minecraftServer -> minecraftServer.overworld()
                        .getEntitiesOfClass(EndCrystal.class, new AABB(target).inflate(3.0)).stream()
                        .map(crystal -> String.format("%.2f,%.2f,%.2f", crystal.getX() - target.getX(),
                                crystal.getY() - target.getY(), crystal.getZ() - target.getZ()))
                        .toList());
                int crystals = crystalPositions.size();
                float health = targetHealth(server);
                sawPiston |= pistonState.getBlock().getDescriptionId().contains("piston");
                sawExtended |= pistonState.toString().contains("extended=true");
                sawCrystal |= crystals > 0;
                String stage = String.valueOf(DebugValueRegistry.snapshot().get("piston_crystal"));
                if (tick % 10 == 0 || health < startHealth && firstHitTick < 0) {
                    LOGGER.info("tick {} piston={} crystals={} health={} debug={}", tick, pistonState, crystalPositions, health, stage);
                }
                if (tick == 15 || tick == 40) {
                    context.takeScreenshot("piston_crystal_tick_" + tick);
                }
                if (health < startHealth && firstHitTick < 0) {
                    firstHitTick = tick;
                    context.takeScreenshot("piston_crystal_hit");
                }
                if (firstHitTick >= 0 && tick > firstHitTick + 120) {
                    break;
                }
            }
            float endHealth = targetHealth(server);
            context.takeScreenshot("piston_crystal_end");
            LOGGER.info("RESULT piston={} extended={} crystal={} firstHitTick={} health {} -> {}",
                    sawPiston, sawExtended, sawCrystal, firstHitTick, startHealth, endHealth);
            context.runOnClient(client -> module().enabled(false));
            if (firstHitTick < 0) {
                throw new AssertionError("Piston Crystal never damaged the target (piston=" + sawPiston
                        + ", extended=" + sawExtended + ", crystal=" + sawCrystal + ")");
            }
        }
    }

    /** A server-side stand-in player. Fabric's fake players ignore damage, which this test needs. */
    private static final class DamageableTarget extends FakePlayer {

        private DamageableTarget(final ServerLevel level, final GameProfile profile) {
            super(level, profile);
        }

        @Override
        public boolean isInvulnerableTo(final ServerLevel level, final DamageSource source) {
            return false;
        }
    }

    private static Module module() {
        return AnarchyClient.MODULES.find("piston_crystal").orElseThrow();
    }

    private static float targetHealth(final TestServerContext server) {
        return server.computeOnServer(minecraftServer -> minecraftServer.overworld().players().stream()
                .filter(player -> "Target".equals(player.getScoreboardName()))
                .findFirst()
                .map(player -> player.getHealth() + player.getAbsorptionAmount())
                .orElse(-1.0F));
    }

    private static String at(final BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static String box(final BlockPos from, final BlockPos to) {
        return at(from) + " " + at(to);
    }
}
