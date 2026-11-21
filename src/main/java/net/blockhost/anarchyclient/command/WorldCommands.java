package net.blockhost.anarchyclient.command;

import static net.blockhost.anarchyclient.command.CommandSupport.SUCCESS;
import static net.blockhost.anarchyclient.command.CommandSupport.blockArea;
import static net.blockhost.anarchyclient.command.CommandSupport.parseBlock;
import static net.blockhost.anarchyclient.command.CommandSupport.parseBlockPosition;
import static net.blockhost.anarchyclient.command.CommandSupport.requireInGame;
import static net.blockhost.anarchyclient.command.CoordinateArgument.coordinate;
import static net.blockhost.anarchyclient.command.CoordinateArgument.getCoordinate;
import static net.blockhost.anarchyclient.command.TokenArgument.getToken;
import static net.blockhost.anarchyclient.command.TokenArgument.token;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.blockhost.anarchyclient.command.CommandSupport.BlockArea;
import net.blockhost.anarchyclient.i18n.Translations;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;

/**
 * Registers and handles the {@code seed}, {@code locate}, {@code terrain-export}, {@code save-map}, {@code setblock}, and {@code fill} subcommands.
 */
final class WorldCommands {

    private static final int MAX_FILL_BLOCKS = 32_768;
    private static final long SLIME_CHUNK_SALT = 987234911L;
    // The block snapshot has to run on the render thread. A 65-block cube (about 275k reads) stays
    // short enough not to freeze the game; 64 meant 2.1M reads.
    private static final int MAX_EXPORT_DISTANCE = 32;

    private WorldCommands() {}

    static void register(final LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root.then(literal("seed")
                        .executes(WorldCommands::showSeed)
                        .then(literal("set")
                                .then(argument("seed", StringArgumentType.greedyString())
                                        .executes(WorldCommands::setSeed)))
                        .then(literal("list").executes(WorldCommands::listSeeds))
                        .then(literal("delete")
                                .executes(context -> deleteSeed(context, SeedStore.worldKey(Minecraft.getInstance())))
                                // Any saved key, including entries from before seeds were keyed per world.
                                .then(argument("world", StringArgumentType.greedyString())
                                        .suggests(WorldCommands::suggestSavedSeeds)
                                        .executes(context -> deleteSeed(
                                                context,
                                                StringArgumentType.getString(context, "world")
                                                        .trim())))))
                .then(literal("locate").then(literal("slime-chunk").executes(WorldCommands::locateSlimeChunk)))
                .then(literal("terrain-export")
                        .then(argument("distance", IntegerArgumentType.integer(1, MAX_EXPORT_DISTANCE))
                                .executes(WorldCommands::terrainExport)))
                .then(literal("save-map")
                        .then(argument("distance", IntegerArgumentType.integer(1, MAX_EXPORT_DISTANCE))
                                .executes(WorldCommands::terrainExport)))
                .then(literal("setblock")
                        .then(argument("x", coordinate())
                                .then(argument("y", coordinate())
                                        .then(argument("z", coordinate())
                                                .then(argument("block", token())
                                                        .suggests(WorldCommands::suggestBlocks)
                                                        .executes(WorldCommands::setBlock))))))
                .then(
                        literal("fill")
                                .then(
                                        argument("from_x", coordinate())
                                                .then(
                                                        argument("from_y", coordinate())
                                                                .then(
                                                                        argument("from_z", coordinate())
                                                                                .then(
                                                                                        argument("to_x", coordinate())
                                                                                                .then(
                                                                                                        argument(
                                                                                                                        "to_y",
                                                                                                                        coordinate())
                                                                                                                .then(
                                                                                                                        argument(
                                                                                                                                        "to_z",
                                                                                                                                        coordinate())
                                                                                                                                .then(
                                                                                                                                        argument(
                                                                                                                                                        "block",
                                                                                                                                                        token())
                                                                                                                                                .suggests(
                                                                                                                                                        WorldCommands
                                                                                                                                                                ::suggestBlocks)
                                                                                                                                                .executes(
                                                                                                                                                        context ->
                                                                                                                                                                fill(
                                                                                                                                                                        context,
                                                                                                                                                                        false))
                                                                                                                                                .then(
                                                                                                                                                        literal(
                                                                                                                                                                        "replace")
                                                                                                                                                                .then(
                                                                                                                                                                        argument(
                                                                                                                                                                                        "filter",
                                                                                                                                                                                        token())
                                                                                                                                                                                .suggests(
                                                                                                                                                                                        WorldCommands
                                                                                                                                                                                                ::suggestBlocks)
                                                                                                                                                                                .executes(
                                                                                                                                                                                        context ->
                                                                                                                                                                                                fill(
                                                                                                                                                                                                        context,
                                                                                                                                                                                                        true))))))))))));
    }

    private static int showSeed(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        String world = SeedStore.worldKey(client);
        try {
            SeedStore.SeedRecord record = SeedStore.get(seedPath(), world);
            if (record == null) {
                context.getSource()
                        .sendFeedback(Translations.component(
                                "anarchyclient.message.world.no_saved_seed_for", "No saved seed for %s.", world));
            } else {
                context.getSource()
                        .sendFeedback(Translations.component("anarchyclient.message.world.seed", "%s seed: ", world)
                                .append(CommandFeedback.copyable(record.seed(), record.seed()))
                                .append(Component.literal(seedMetadata(record))));
            }
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.world.failed_to_read_seeds",
                            "Failed to read seeds: %s",
                            exception.getMessage()));
            return 0;
        }
    }

    private static int setSeed(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        String world = SeedStore.worldKey(client);
        try {
            SeedStore.SeedRecord record = SeedStore.put(
                    seedPath(),
                    world,
                    StringArgumentType.getString(context, "seed"),
                    Instant.now(),
                    SharedConstants.getCurrentVersion().name(),
                    currentDimension(client),
                    "manual");
            context.getSource()
                    .sendFeedback(Translations.component(
                            "anarchyclient.message.world.saved_seed_for",
                            "Saved seed for %s: %s",
                            record.world(),
                            record.seed()));
            return SUCCESS;
        } catch (IllegalArgumentException | IOException exception) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.world.failed_to_save_seed",
                            "Failed to save seed: %s",
                            exception.getMessage()));
            return 0;
        }
    }

    private static int listSeeds(final CommandContext<FabricClientCommandSource> context) {
        try {
            List<SeedStore.SeedRecord> records = SeedStore.list(seedPath());
            if (records.isEmpty()) {
                context.getSource()
                        .sendFeedback(Translations.component(
                                "anarchyclient.message.world.no_saved_seeds", "No saved seeds."));
                return SUCCESS;
            }
            for (SeedStore.SeedRecord record : records) {
                context.getSource()
                        .sendFeedback(Component.literal(record.world() + " = " + record.seed() + seedMetadata(record)));
            }
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.world.failed_to_read_seeds",
                            "Failed to read seeds: %s",
                            exception.getMessage()));
            return 0;
        }
    }

    private static CompletableFuture<Suggestions> suggestSavedSeeds(
            final CommandContext<FabricClientCommandSource> context, final SuggestionsBuilder builder) {
        try {
            return SharedSuggestionProvider.suggest(
                    SeedStore.list(seedPath()).stream().map(SeedStore.SeedRecord::world), builder);
        } catch (IOException exception) {
            return builder.buildFuture();
        }
    }

    private static int deleteSeed(final CommandContext<FabricClientCommandSource> context, final String world) {
        try {
            if (SeedStore.delete(seedPath(), world)) {
                context.getSource()
                        .sendFeedback(Translations.component(
                                "anarchyclient.message.world.deleted_saved_seed_for",
                                "Deleted saved seed for %s.",
                                world));
            } else {
                context.getSource()
                        .sendFeedback(Translations.component(
                                "anarchyclient.message.world.no_saved_seed_for", "No saved seed for %s.", world));
            }
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.world.failed_to_delete_seed",
                            "Failed to delete seed: %s",
                            exception.getMessage()));
            return 0;
        }
    }

    private static int locateSlimeChunk(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        if (client == null) {
            return 0;
        }
        try {
            SeedStore.SeedRecord record = SeedStore.get(seedPath(), SeedStore.worldKey(client));
            if (record == null) {
                context.getSource()
                        .sendError(Translations.component(
                                "anarchyclient.message.world.save_a_seed_first_with_ac",
                                "Save a seed first with /ac seed set <seed>."));
                return 0;
            }
            long seed = numericSeed(record.seed());
            BlockPos playerPos = client.player.blockPosition();
            BlockPos result = closestSlimeChunk(
                    seed, Math.floorDiv(playerPos.getX(), 16), Math.floorDiv(playerPos.getZ(), 16), 128);
            String coords = result.getX() + ", " + result.getZ();
            context.getSource()
                    .sendFeedback(Translations.component(
                                    "anarchyclient.message.world.nearest_slime_chunk_starts_at",
                                    "Nearest slime chunk starts at ")
                            .append(CommandFeedback.copyable(coords, coords)));
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.world.failed_to_read_seeds",
                            "Failed to read seeds: %s",
                            exception.getMessage()));
            return 0;
        }
    }

    private static int terrainExport(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.world.you_must_be_in_game_to",
                            "You must be in-game to export terrain."));
            return 0;
        }
        int distance = IntegerArgumentType.getInteger(context, "distance");
        BlockPos center = client.player.blockPosition();
        Path output = terrainExportPath(center, distance);
        List<String> lines = terrainLines(client, center, distance);
        FabricClientCommandSource source = context.getSource();
        source.sendFeedback(Translations.component(
                "anarchyclient.message.world.exporting_blocks", "Exporting %s blocks...", (lines.size() - 1)));
        // Write off the render thread, then report back on it.
        CompletableFuture.runAsync(
                        () -> {
                            try {
                                Files.createDirectories(output.getParent());
                                Files.write(output, lines, StandardCharsets.UTF_8);
                            } catch (IOException exception) {
                                throw new UncheckedIOException(exception);
                            }
                        },
                        Util.ioPool())
                .whenCompleteAsync(
                        (ignored, error) -> {
                            if (error == null) {
                                source.sendFeedback(Translations.component(
                                        "anarchyclient.message.world.exported_terrain_to",
                                        "Exported terrain to %s",
                                        output));
                                return;
                            }
                            Throwable cause = error instanceof CompletionException && error.getCause() != null
                                    ? error.getCause()
                                    : error;
                            if (cause instanceof UncheckedIOException unchecked) {
                                cause = unchecked.getCause();
                            }
                            source.sendError(Translations.component(
                                    "anarchyclient.message.world.failed_to_export_terrain",
                                    "Failed to export terrain: %s",
                                    cause.getMessage()));
                        },
                        client);
        return SUCCESS;
    }

    private static int setBlock(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        if (client == null || client.level == null) {
            return 0;
        }
        try {
            BlockPos pos = parseBlockPosition(
                    getCoordinate(context, "x"),
                    getCoordinate(context, "y"),
                    getCoordinate(context, "z"),
                    client.player.position());
            client.level.setBlockAndUpdate(
                    pos, parseBlock(getToken(context, "block")).defaultBlockState());
            return SUCCESS;
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private static int fill(final CommandContext<FabricClientCommandSource> context, final boolean replaceOnly) {
        Minecraft client = requireInGame(context);
        if (client == null || client.level == null) {
            return 0;
        }
        try {
            BlockArea area = blockArea(
                    parseBlockPosition(
                            getCoordinate(context, "from_x"),
                            getCoordinate(context, "from_y"),
                            getCoordinate(context, "from_z"),
                            client.player.position()),
                    parseBlockPosition(
                            getCoordinate(context, "to_x"),
                            getCoordinate(context, "to_y"),
                            getCoordinate(context, "to_z"),
                            client.player.position()),
                    MAX_FILL_BLOCKS);
            BlockState state = parseBlock(getToken(context, "block")).defaultBlockState();
            Block filter = replaceOnly ? parseBlock(getToken(context, "filter")) : null;
            int changed = 0;
            for (int x = area.minX(); x <= area.maxX(); x++) {
                for (int y = area.minY(); y <= area.maxY(); y++) {
                    for (int z = area.minZ(); z <= area.maxZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (filter != null && client.level.getBlockState(pos).getBlock() != filter) {
                            continue;
                        }
                        if (client.level.setBlockAndUpdate(pos, state)) {
                            changed++;
                        }
                    }
                }
            }
            context.getSource()
                    .sendFeedback(Translations.component(
                            "anarchyclient.message.world.changed_client_blocks",
                            "Client blocks changed: %s.",
                            changed));
            return SUCCESS;
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private static CompletableFuture<Suggestions> suggestBlocks(
            final CommandContext<FabricClientCommandSource> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                BuiltInRegistries.BLOCK.keySet().stream().map(Identifier::toString), builder);
    }

    private static List<String> terrainLines(final Minecraft client, final BlockPos center, final int distance) {
        List<String> lines = new ArrayList<>();
        lines.add("dx,dy,dz,block");
        for (int y = center.getY() - distance; y <= center.getY() + distance; y++) {
            if (y < client.level.getMinY() || y >= client.level.getMinY() + client.level.getHeight()) {
                continue;
            }
            for (int x = center.getX() - distance; x <= center.getX() + distance; x++) {
                for (int z = center.getZ() - distance; z <= center.getZ() + distance; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!client.level.isLoaded(pos)) {
                        continue;
                    }
                    BlockState state = client.level.getBlockState(pos);
                    if (state.isAir()
                            || state.getCollisionShape(client.level, pos).isEmpty()) {
                        continue;
                    }
                    lines.add((x - center.getX()) + "," + (y - center.getY()) + "," + (z - center.getZ()) + ","
                            + BuiltInRegistries.BLOCK.getKey(state.getBlock()));
                }
            }
        }
        return List.copyOf(lines);
    }

    private static Path terrainExportPath(final BlockPos center, final int distance) {
        String fileName =
                "terrain-" + center.getX() + "-" + center.getY() + "-" + center.getZ() + "-r" + distance + ".csv";
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("anarchyclient")
                .resolve("exports")
                .resolve(fileName);
    }

    private static Path seedPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("anarchyclient")
                .resolve("seeds.json");
    }

    private static String currentDimension(final Minecraft client) {
        return client.level == null ? "" : client.level.dimension().identifier().toString();
    }

    private static String seedMetadata(final SeedStore.SeedRecord record) {
        List<String> parts = new ArrayList<>();
        if (!record.version().isBlank()) {
            parts.add("version " + record.version());
        }
        if (!record.dimension().isBlank()) {
            parts.add(record.dimension());
        }
        if (!record.source().isBlank()) {
            parts.add("source " + record.source());
        }
        return parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
    }

    static long numericSeed(final String seed) {
        try {
            return Long.parseLong(seed.trim());
        } catch (NumberFormatException exception) {
            return seed.hashCode();
        }
    }

    static boolean isSlimeChunk(final long seed, final int chunkX, final int chunkZ) {
        // Same call as Slime.checkSlimeSpawnRules, including vanilla's int overflow in the seed mix.
        return WorldgenRandom.seedSlimeChunk(chunkX, chunkZ, seed, SLIME_CHUNK_SALT)
                        .nextInt(10)
                == 0;
    }

    static BlockPos closestSlimeChunk(
            final long seed, final int centerChunkX, final int centerChunkZ, final int radiusChunks) {
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int radius = 0; radius <= radiusChunks; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int chunkX = centerChunkX + dx;
                    int chunkZ = centerChunkZ + dz;
                    if (!isSlimeChunk(seed, chunkX, chunkZ)) {
                        continue;
                    }
                    int distance = dx * dx + dz * dz;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new BlockPos(chunkX * 16, 0, chunkZ * 16);
                    }
                }
            }
            // A later ring can still hold a closer chunk (a ring corner is farther than the next ring's
            // edge midpoint), so stop only when the next ring cannot beat the best distance.
            if (best != null && (radius + 1) * (radius + 1) >= bestDistance) {
                return best;
            }
        }
        if (best != null) {
            return best;
        }
        return new BlockPos(centerChunkX * 16, 0, centerChunkZ * 16);
    }
}
