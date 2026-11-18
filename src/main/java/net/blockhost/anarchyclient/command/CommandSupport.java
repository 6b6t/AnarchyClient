package net.blockhost.anarchyclient.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.blockhost.anarchyclient.i18n.Translations;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Shared argument parsing, validation, and suggestions for the client commands.
 */
final class CommandSupport {

    static final int SUCCESS = Command.SINGLE_SUCCESS;

    private CommandSupport() {
    }

    static CompletableFuture<Suggestions> suggestOnlinePlayers(final CommandContext<FabricClientCommandSource> context,
                                                               final SuggestionsBuilder builder) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return builder.buildFuture();
        }
        return SharedSuggestionProvider.suggest(connection.getOnlinePlayers().stream()
                .map(info -> info.getProfile().name()), builder);
    }

    static String sanitizeFileName(final String value) {
        if (value == null || value.isBlank()) {
            return "skin";
        }
        String sanitized = value.replaceAll("[^A-Za-z0-9_.-]", "_");
        return sanitized.isBlank() ? "skin" : sanitized;
    }

    static Minecraft requireInGame(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            context.getSource().sendError(Translations.component("anarchyclient.message.command_support.must_be_in_game", "You must be in a world to use this command."));
            return null;
        }
        return client;
    }

    static Minecraft requireCreative(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        if (client == null || client.player == null || !client.player.getAbilities().instabuild) {
            context.getSource().sendError(Translations.component("anarchyclient.message.command_support.you_must_be_in_creative_mode", "You must be in creative mode to use this command."));
            return null;
        }
        return client;
    }

    static double parseCoordinate(final String token, final double origin) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Expected a coordinate.");
        }
        if (token.charAt(0) == '~') {
            if (token.length() == 1) {
                return origin;
            }
            return origin + parseDouble(token.substring(1), token);
        }
        return parseDouble(token, token);
    }

    static Vec3 parsePosition(final String xToken, final String yToken, final String zToken, final Vec3 origin) {
        return new Vec3(
                parseCoordinate(xToken, origin.x),
                parseCoordinate(yToken, origin.y),
                parseCoordinate(zToken, origin.z)
        );
    }

    static BlockPos parseBlockPosition(final String xToken, final String yToken, final String zToken,
                                       final Vec3 origin) {
        return BlockPos.containing(
                parseCoordinate(xToken, origin.x),
                parseCoordinate(yToken, origin.y),
                parseCoordinate(zToken, origin.z)
        );
    }

    /**
     * Resolves a user-supplied file path against the game directory and rejects paths that leave it,
     * such as {@code ../../.ssh/key} or an absolute path elsewhere on disk.
     */
    static Path resolveGamePath(final String value) {
        return resolveInside(FabricLoader.getInstance().getGameDir(), value);
    }

    static Path resolveInside(final Path base, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Expected a file path.");
        }
        Path root = base.toAbsolutePath().normalize();
        Path resolved;
        try {
            resolved = root.resolve(value.trim()).normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("Invalid path: " + value, exception);
        }
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalArgumentException("The path must be a file inside the game directory.");
        }
        return resolved;
    }

    static Block parseBlock(final String id) {
        Identifier identifier = id != null && id.contains(":")
                ? Identifier.tryParse(id)
                : Identifier.withDefaultNamespace(id == null ? "" : id);
        if (identifier == null) {
            throw new IllegalArgumentException("Invalid block id: " + id);
        }
        return BuiltInRegistries.BLOCK.getOptional(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Unknown block: " + id));
    }

    static BlockArea blockArea(final BlockPos first, final BlockPos second, final int maxBlocks) {
        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX());
        int maxY = Math.max(first.getY(), second.getY());
        int maxZ = Math.max(first.getZ(), second.getZ());
        long count = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (count > maxBlocks) {
            throw new IllegalArgumentException("Fill area is too large: " + count + " blocks, max " + maxBlocks + ".");
        }
        return new BlockArea(minX, minY, minZ, maxX, maxY, maxZ, (int) count);
    }

    static double parseDouble(final String token, final String original) {
        double value;
        try {
            value = Double.parseDouble(token);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid coordinate: " + original, exception);
        }
        // Java also parses "NaN" and "Infinity"; a NaN position passes every distance check and the server
        // disconnects the player for it.
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Invalid coordinate: " + original);
        }
        return value;
    }

    record BlockArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int count) {
    }
}
