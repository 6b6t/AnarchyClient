package net.blockhost.anarchyclient.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.blockhost.anarchyclient.i18n.Translations;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.blockhost.anarchyclient.inventory.InventoryTracker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.blockhost.anarchyclient.command.CommandSupport.SUCCESS;
import static net.blockhost.anarchyclient.command.CommandSupport.parsePosition;
import static net.blockhost.anarchyclient.command.CommandSupport.requireInGame;
import static net.blockhost.anarchyclient.command.CommandSupport.sanitizeFileName;
import static net.blockhost.anarchyclient.command.CommandSupport.suggestOnlinePlayers;
import static net.blockhost.anarchyclient.command.CoordinateArgument.coordinate;
import static net.blockhost.anarchyclient.command.CoordinateArgument.getCoordinate;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Registers and handles the {@code center}, {@code set-velocity}, {@code hclip}, {@code vclip}, {@code damage},
 * {@code dismount}, {@code teleport}, {@code save-skin}, and {@code inventory} subcommands.
 */
final class PlayerCommands {

    /** Farthest a single move packet may go on a vanilla server before it sets the player back. */
    static final double MAX_MOVE_DISTANCE = 10.0;
    private static final int TRACKED_INVENTORY_LIMIT = 10;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    private PlayerCommands() {
    }

    static void register(final LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root
                .then(literal("center")
                        .then(literal("middle")
                                .executes(context -> centerPlayer(context, CenterMode.MIDDLE)))
                        .then(literal("corner")
                                .executes(context -> centerPlayer(context, CenterMode.CORNER))))
                .then(literal("save-skin")
                        .then(argument("player", StringArgumentType.word())
                                .suggests(CommandSupport::suggestOnlinePlayers)
                                .executes(PlayerCommands::saveSkin)))
                .then(literal("set-velocity")
                        .then(argument("value", DoubleArgumentType.doubleArg())
                                .executes(PlayerCommands::setVelocityY)
                                .then(argument("second", DoubleArgumentType.doubleArg())
                                        .executes(PlayerCommands::setVelocityXZ)
                                        .then(argument("third", DoubleArgumentType.doubleArg())
                                                .executes(PlayerCommands::setVelocityXYZ)))))
                .then(literal("hclip")
                        .then(argument("distance", DoubleArgumentType.doubleArg())
                                .executes(PlayerCommands::hclip)))
                .then(literal("vclip")
                        .then(argument("distance", DoubleArgumentType.doubleArg())
                                .executes(PlayerCommands::vclip)))
                .then(literal("damage")
                        .executes(context -> damage(context, 1.0))
                        .then(argument("amount", DoubleArgumentType.doubleArg(0.5, 20.0))
                                .executes(context -> damage(context, DoubleArgumentType.getDouble(context, "amount")))))
                .then(literal("dismount")
                        .executes(PlayerCommands::dismount))
                .then(literal("inventory")
                        .executes(PlayerCommands::listTrackedInventories)
                        .then(argument("player", StringArgumentType.word())
                                .suggests(CommandSupport::suggestOnlinePlayers)
                                .executes(context -> showTrackedInventory(context,
                                        StringArgumentType.getString(context, "player")))))
                .then(literal("teleport")
                        .then(argument("x", coordinate())
                                .then(argument("y", coordinate())
                                        .then(argument("z", coordinate())
                                                .executes(context -> teleport(context, false))
                                                .then(argument("yaw", FloatArgumentType.floatArg())
                                                        .then(argument("pitch", FloatArgumentType.floatArg())
                                                                .executes(context -> teleport(context, true))))))));
    }

    private static int listTrackedInventories(final CommandContext<FabricClientCommandSource> context) {
        List<InventoryTracker.Snapshot> snapshots = InventoryTracker.newest(TRACKED_INVENTORY_LIMIT);
        if (snapshots.isEmpty()) {
            context.getSource().sendError(Translations.component("anarchyclient.message.player.no_tracked_players_enable_inventory_tracker", "No tracked players. Enable Inventory Tracker and get near players."));
            return 0;
        }
        long now = System.currentTimeMillis();
        for (InventoryTracker.Snapshot snapshot : snapshots) {
            context.getSource().sendFeedback(Component.literal(describeSnapshot(snapshot, now)));
        }
        return SUCCESS;
    }

    private static int showTrackedInventory(final CommandContext<FabricClientCommandSource> context, final String playerName) {
        return InventoryTracker.snapshots().stream()
                .filter(snapshot -> snapshot.name().equalsIgnoreCase(playerName))
                .findFirst()
                .map(snapshot -> {
                    context.getSource().sendFeedback(Component.literal(
                            describeSnapshot(snapshot, System.currentTimeMillis())));
                    return SUCCESS;
                })
                .orElseGet(() -> {
                    context.getSource().sendError(Translations.component("anarchyclient.message.player.no_tracked_inventory_for", "No tracked inventory for %s.", playerName));
                    return 0;
                });
    }

    static String describeSnapshot(final InventoryTracker.Snapshot snapshot, final long nowMillis) {
        long secondsAgo = Math.max(0L, (nowMillis - snapshot.lastSeenMillis()) / 1000L);
        StringBuilder text = new StringBuilder()
                .append(snapshot.name())
                .append(String.format(Locale.ROOT, " (%.1f HP", snapshot.health()));
        if (snapshot.absorption() > 0F) {
            text.append(String.format(Locale.ROOT, " +%.1f", snapshot.absorption()));
        }
        text.append(String.format(Locale.ROOT, ", %.0fm, %ds ago): main ", snapshot.distance(), secondsAgo))
                .append(itemName(snapshot.mainHand()))
                .append(", off ")
                .append(itemName(snapshot.offhand()))
                .append(", armor ");
        List<String> armor = snapshot.armor().stream().map(PlayerCommands::itemName).toList();
        text.append(String.join(" / ", armor));
        return text.toString();
    }

    private static String itemName(final ItemStack stack) {
        if (stack.isEmpty()) {
            return "-";
        }
        String name = stack.getHoverName().getString();
        return stack.getCount() > 1 ? stack.getCount() + "x " + name : name;
    }

    private static int centerPlayer(final CommandContext<FabricClientCommandSource> context, final CenterMode mode) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.connection == null) {
            context.getSource().sendError(Translations.component("anarchyclient.message.player.you_must_be_in_game_to", "You must be in-game to center yourself."));
            return 0;
        }
        double x = mode.coordinate(client.player.getX());
        double z = mode.coordinate(client.player.getZ());
        client.player.setPos(x, client.player.getY(), z);
        client.player.connection.send(new ServerboundMovePlayerPacket.Pos(
                x,
                client.player.getY(),
                z,
                client.player.onGround(),
                client.player.horizontalCollision
        ));
        context.getSource().sendFeedback(Translations.component("anarchyclient.message.player.centered_player", "Centered player."));
        return SUCCESS;
    }

    private static int saveSkin(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        ClientPacketListener connection = client.getConnection();
        if (connection == null) {
            context.getSource().sendError(Translations.component("anarchyclient.message.player.you_must_be_connected_to_a", "You must be connected to a server to save a skin."));
            return 0;
        }
        String playerName = StringArgumentType.getString(context, "player");
        PlayerInfo info = connection.getPlayerInfoIgnoreCase(playerName);
        if (info == null) {
            context.getSource().sendError(Translations.component("anarchyclient.message.player.unknown_online_player", "Unknown online player: %s", playerName));
            return 0;
        }
        String profileName = info.getProfile().name();
        String uuid = info.getProfile().id().toString().replace("-", "");
        downloadSkin(client, context.getSource(), profileName, uuid);
        context.getSource().sendFeedback(Translations.component("anarchyclient.message.player.saving_skin_for", "Saving skin for %s...", profileName));
        return SUCCESS;
    }

    private static int setVelocityY(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        if (client == null) {
            return 0;
        }
        Vec3 current = client.player.getDeltaMovement();
        client.player.setDeltaMovement(current.x, DoubleArgumentType.getDouble(context, "value"), current.z);
        return SUCCESS;
    }

    private static int setVelocityXZ(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        if (client == null) {
            return 0;
        }
        client.player.setDeltaMovement(
                DoubleArgumentType.getDouble(context, "value"),
                client.player.getDeltaMovement().y,
                DoubleArgumentType.getDouble(context, "second")
        );
        return SUCCESS;
    }

    private static int setVelocityXYZ(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        if (client == null) {
            return 0;
        }
        client.player.setDeltaMovement(
                DoubleArgumentType.getDouble(context, "value"),
                DoubleArgumentType.getDouble(context, "second"),
                DoubleArgumentType.getDouble(context, "third")
        );
        return SUCCESS;
    }

    private static int hclip(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        double distance = DoubleArgumentType.getDouble(context, "distance");
        double yaw = Math.toRadians(client.player.getYRot());
        Vec3 offset = new Vec3(-Math.sin(yaw) * distance, 0.0, Math.cos(yaw) * distance);
        return movePlayer(context, client.player.position().add(offset));
    }

    private static int vclip(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        return movePlayer(context, client.player.position().add(0.0, DoubleArgumentType.getDouble(context, "distance"), 0.0));
    }

    private static int damage(final CommandContext<FabricClientCommandSource> context, final double amount) {
        Minecraft client = requireInGame(context);
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        Vec3 pos = client.player.position();
        int packets = Math.max(4, Math.min(80, (int) Math.ceil(amount * 8.0)));
        for (int index = 0; index < packets; index++) {
            client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                    pos.x,
                    pos.y + 0.0625,
                    pos.z,
                    false,
                    client.player.horizontalCollision
            ));
            client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                    pos.x,
                    pos.y,
                    pos.z,
                    false,
                    client.player.horizontalCollision
            ));
        }
        client.getConnection().send(new ServerboundMovePlayerPacket.Pos(pos, true, client.player.horizontalCollision));
        // Vanilla 26.3 servers reset fall distance on every upward move, so this only works on servers that do not.
        context.getSource().sendFeedback(Translations.component("anarchyclient.message.player.sent_fall_packet_pairs_vanilla_servers", "Sent %s fall packet pairs. Vanilla servers reset fall distance on upward moves, so they deal no damage there.", packets));
        return SUCCESS;
    }

    private static int dismount(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        if (client == null) {
            return 0;
        }
        if (client.player.isPassenger()) {
            client.player.stopRiding();
            context.getSource().sendFeedback(Translations.component("anarchyclient.message.player.dismounted", "Dismounted."));
        } else {
            context.getSource().sendFeedback(Translations.component("anarchyclient.message.player.you_are_not_riding_anything", "You are not riding anything."));
        }
        return SUCCESS;
    }

    private static int movePlayer(final CommandContext<FabricClientCommandSource> context, final Vec3 target) {
        Minecraft client = requireInGame(context);
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        if (!withinMoveLimit(context, client.player.position(), target)) {
            return 0;
        }
        client.player.absSnapTo(target.x, target.y, target.z);
        client.getConnection().send(new ServerboundMovePlayerPacket.Pos(target, client.player.onGround(), client.player.horizontalCollision));
        context.getSource().sendFeedback(Translations.component("anarchyclient.message.player.sent_a_move_to_the_server", "Sent a move to %s, %s, %s. The server can still set you back.", formatCoordinate(target.x), formatCoordinate(target.y), formatCoordinate(target.z)));
        return SUCCESS;
    }

    /**
     * A vanilla server sets you back when one move packet goes more than 10 blocks past your velocity. Refuse such
     * moves up front instead of reporting a move that will not stick.
     */
    private static boolean withinMoveLimit(final CommandContext<FabricClientCommandSource> context, final Vec3 from,
                                           final Vec3 to) {
        double distance = from.distanceTo(to);
        if (exceedsMoveLimit(distance)) {
            context.getSource().sendError(Translations.component("anarchyclient.message.player.that_is_blocks_the_server_only", "That is %s blocks; the server only accepts moves of up to %s blocks.", formatCoordinate(distance), (int) MAX_MOVE_DISTANCE));
            return false;
        }
        return true;
    }

    static boolean exceedsMoveLimit(final double distance) {
        return distance > MAX_MOVE_DISTANCE;
    }

    private static String formatCoordinate(final double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static int teleport(final CommandContext<FabricClientCommandSource> context, final boolean includeRotation) {
        Minecraft client = requireInGame(context);
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        Vec3 pos;
        try {
            pos = parsePosition(
                    getCoordinate(context, "x"),
                    getCoordinate(context, "y"),
                    getCoordinate(context, "z"),
                    client.player.position()
            );
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(exception.getMessage()));
            return 0;
        }
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        if (!withinMoveLimit(context, client.player.position(), pos)) {
            return 0;
        }
        if (includeRotation) {
            float yaw = FloatArgumentType.getFloat(context, "yaw");
            float pitch = FloatArgumentType.getFloat(context, "pitch");
            client.player.absSnapTo(x, y, z, yaw, pitch);
            client.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                    x, y, z, yaw, pitch, client.player.onGround(), client.player.horizontalCollision
            ));
        } else {
            client.player.absSnapTo(x, y, z);
            client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                    x, y, z, client.player.onGround(), client.player.horizontalCollision
            ));
        }
        return SUCCESS;
    }

    private static void downloadSkin(final Minecraft client, final FabricClientCommandSource source,
                                     final String profileName, final String uuid) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpClient http = HttpClient.newBuilder()
                        .connectTimeout(HTTP_TIMEOUT)
                        .build();
                HttpRequest profileRequest = HttpRequest.newBuilder(URI.create(
                                "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
                        .timeout(HTTP_TIMEOUT)
                        .GET()
                        .build();
                HttpResponse<String> profileResponse = http.send(profileRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (profileResponse.statusCode() != 200) {
                    throw new IOException("profile request returned HTTP " + profileResponse.statusCode());
                }
                String skinUrl = skinUrl(profileResponse.body());
                if (skinUrl == null) {
                    throw new IOException("profile does not contain a skin texture");
                }
                HttpResponse<byte[]> skinResponse = http.send(
                        HttpRequest.newBuilder(URI.create(skinUrl)).timeout(HTTP_TIMEOUT).GET().build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                if (skinResponse.statusCode() != 200) {
                    throw new IOException("skin request returned HTTP " + skinResponse.statusCode());
                }
                Path output = skinPath(profileName);
                Files.createDirectories(output.getParent());
                Files.write(output, skinResponse.body());
                client.execute(() -> source.sendFeedback(Translations.component("anarchyclient.message.player.saved_skin_to", "Saved skin to %s", output)));
            } catch (IOException | RuntimeException exception) {
                // RuntimeException covers malformed profile JSON, bad Base64, and bad skin URLs, which
                // the async task would otherwise drop silently.
                client.execute(() -> source.sendError(Translations.component("anarchyclient.message.player.failed_to_save_skin", "Failed to save skin: %s", exception.getMessage())));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                client.execute(() -> source.sendError(Translations.component("anarchyclient.message.player.failed_to_save_skin_interrupted", "Failed to save skin: interrupted")));
            }
        });
    }

    static String skinUrl(final String profileJson) {
        JsonObject root = JsonParser.parseString(profileJson).getAsJsonObject();
        JsonArray properties = root.getAsJsonArray("properties");
        if (properties == null) {
            return null;
        }
        for (JsonElement element : properties) {
            JsonObject property = element.getAsJsonObject();
            if (!"textures".equals(property.get("name").getAsString()) || !property.has("value")) {
                continue;
            }
            String decoded = new String(Base64.getDecoder().decode(property.get("value").getAsString()), StandardCharsets.UTF_8);
            JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
            if (textures != null && textures.has("SKIN")) {
                return textures.getAsJsonObject("SKIN").get("url").getAsString();
            }
        }
        return null;
    }

    private static Path skinPath(final String playerName) {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("anarchyclient")
                .resolve("skins")
                .resolve(sanitizeFileName(playerName) + ".png");
    }

    enum CenterMode {
        MIDDLE {
            @Override
            double coordinate(final double value) {
                return Math.floor(value) + 0.5;
            }
        },
        CORNER {
            @Override
            double coordinate(final double value) {
                return Math.floor(value);
            }
        };

        abstract double coordinate(double value);
    }
}
