package net.blockhost.anarchyclient.command;

import static net.blockhost.anarchyclient.command.CommandSupport.SUCCESS;
import static net.blockhost.anarchyclient.command.CommandSupport.requireInGame;
import static net.blockhost.anarchyclient.command.TokenArgument.getToken;
import static net.blockhost.anarchyclient.command.TokenArgument.token;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.blockhost.anarchyclient.event.PacketEventSilencer;
import net.blockhost.anarchyclient.i18n.Translations;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * Registers and handles the {@code reconnect}, {@code server-info}, {@code server-list}, and {@code kick} subcommands.
 */
final class ServerCommands {

    private ServerCommands() {}

    static void register(final LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root.then(literal("reconnect").executes(ServerCommands::reconnect))
                .then(literal("server-info")
                        .executes(ServerCommands::serverInfo)
                        .then(literal("ports")
                                .executes(ServerCommands::serverPortsKnown)
                                .then(literal("known").executes(ServerCommands::serverPortsKnown))
                                .then(argument("from", IntegerArgumentType.integer(1, 65535))
                                        .then(argument("to", IntegerArgumentType.integer(1, 65535))
                                                .executes(ServerCommands::serverPortsRange)))))
                .then(literal("server-list")
                        .then(literal("list").executes(ServerCommands::serverListList))
                        .then(literal("add")
                                .then(argument("address", token())
                                        .executes(context -> serverListAdd(context, null))
                                        .then(argument("name", StringArgumentType.greedyString())
                                                .executes(context -> serverListAdd(
                                                        context, StringArgumentType.getString(context, "name"))))))
                        .then(literal("cleanup").executes(ServerCommands::serverListCleanup))
                        .then(literal("export").executes(ServerCommands::serverListExport))
                        .then(literal("import")
                                .then(argument("path", StringArgumentType.greedyString())
                                        .executes(ServerCommands::serverListImport))))
                .then(literal("kick")
                        .then(literal("disconnect").executes(ServerCommands::kickDisconnect))
                        .then(literal("position").executes(ServerCommands::kickPosition)));
    }

    private static int reconnect(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        ServerData server = client.getCurrentServer();
        if (server == null) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.server.no_multiplayer_server_is_available_to",
                            "No multiplayer server is available to reconnect to."));
            return 0;
        }
        ServerAddress address = ServerAddress.parseString(server.ip);
        // Run after the command returns: the chat screen closes itself after a submitted command and
        // would replace the connect screen if it opened now. schedule() always queues the task.
        client.schedule(client.wrapRunnable(
                () -> ConnectScreen.startConnecting(new TitleScreen(), client, address, server, false, null)));
        return SUCCESS;
    }

    private static int serverInfo(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        ServerData server = client.getCurrentServer();
        ClientPacketListener connection = client.getConnection();
        String address = server == null ? "singleplayer" : server.ip;
        String brand = connection == null || connection.serverBrand() == null ? "unknown" : connection.serverBrand();
        context.getSource()
                .sendFeedback(Translations.component(
                        "anarchyclient.message.server.server_brand", "Server: %s | Brand: %s", address, brand));
        return SUCCESS;
    }

    private static int serverListList(final CommandContext<FabricClientCommandSource> context) {
        ServerList list = ServerListTools.load(Minecraft.getInstance());
        List<ServerListTools.ServerEntry> entries = ServerListTools.entries(list);
        if (entries.isEmpty()) {
            context.getSource()
                    .sendFeedback(Translations.component(
                            "anarchyclient.message.server.no_saved_multiplayer_servers",
                            "No saved multiplayer servers."));
            return SUCCESS;
        }
        for (ServerListTools.ServerEntry entry : entries) {
            context.getSource()
                    .sendFeedback(Component.literal(entry.name() + " = ")
                            .append(CommandFeedback.copyable(entry.address(), entry.address())));
        }
        return SUCCESS;
    }

    private static int serverListAdd(final CommandContext<FabricClientCommandSource> context, final String name) {
        Minecraft client = Minecraft.getInstance();
        ServerList list = ServerListTools.load(client);
        String address = getToken(context, "address");
        int added = ServerListTools.addOrUpdate(list, address, name);
        list.save();
        String normalized = ServerListTools.normalizeAddress(address);
        context.getSource()
                .sendFeedback(
                        added == 0
                                ? Translations.component(
                                        "anarchyclient.message.server.updated_server", "Updated %s.", normalized)
                                : Translations.component(
                                        "anarchyclient.message.server.added_server", "Added %s.", normalized));
        return SUCCESS;
    }

    private static int serverListCleanup(final CommandContext<FabricClientCommandSource> context) {
        ServerList list = ServerListTools.load(Minecraft.getInstance());
        int removed = ServerListTools.cleanup(list);
        list.save();
        context.getSource()
                .sendFeedback(Translations.component(
                        "anarchyclient.message.server.removed_duplicate_servers",
                        "Duplicate servers removed: %s.",
                        removed));
        return SUCCESS;
    }

    private static int serverListExport(final CommandContext<FabricClientCommandSource> context) {
        ServerList list = ServerListTools.load(Minecraft.getInstance());
        Path path = serverExportPath();
        try {
            ServerListTools.exportJson(list, path);
            context.getSource()
                    .sendFeedback(Translations.component(
                                    "anarchyclient.message.server.exported_servers",
                                    "Servers exported: %s. File: ",
                                    list.size())
                            .append(CommandFeedback.copyable(path.toString(), path.toString())));
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.server.failed_to_export_servers",
                            "Failed to export servers: %s",
                            exception.getMessage()));
            return 0;
        }
    }

    private static int serverListImport(final CommandContext<FabricClientCommandSource> context) {
        ServerList list = ServerListTools.load(Minecraft.getInstance());
        Path path;
        try {
            path = CommandSupport.resolveGamePath(StringArgumentType.getString(context, "path"));
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(exception.getMessage()));
            return 0;
        }
        try {
            int added = ServerListTools.importJson(list, path);
            int duplicates = ServerListTools.cleanup(list);
            list.save();
            context.getSource()
                    .sendFeedback(Translations.component(
                            "anarchyclient.message.server.imported_servers",
                            "New servers imported: %s. Duplicates removed: %s.",
                            added,
                            duplicates));
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.server.failed_to_import_servers",
                            "Failed to import servers: %s",
                            exception.getMessage()));
            return 0;
        }
    }

    private static int serverPortsKnown(final CommandContext<FabricClientCommandSource> context) {
        return scanServerPorts(context, ServerPortScanner.knownPorts(), "known ports");
    }

    private static int serverPortsRange(final CommandContext<FabricClientCommandSource> context) {
        int first = IntegerArgumentType.getInteger(context, "from");
        int second = IntegerArgumentType.getInteger(context, "to");
        try {
            ServerPortScanner.PortRange range = ServerPortScanner.range(first, second);
            return scanServerPorts(context, range.checks(), "ports " + range.min() + "-" + range.max());
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private static int scanServerPorts(
            final CommandContext<FabricClientCommandSource> context,
            final List<ServerPortScanner.PortCheck> ports,
            final String label) {
        Minecraft client = Minecraft.getInstance();
        ServerData server = client.getCurrentServer();
        if (server == null) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.server.you_must_be_connected_to_a",
                            "You must be connected to a multiplayer server to scan ports."));
            return 0;
        }
        ServerAddress address = ServerAddress.parseString(server.ip);
        String host = address.getHost();
        if (host.isBlank()) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.server.could_not_resolve_server_host_from",
                            "Could not resolve server host from %s.",
                            server.ip));
            return 0;
        }
        context.getSource()
                .sendFeedback(Translations.component(
                        "anarchyclient.message.server.scanning_on", "Scanning %s on %s...", label, host));
        // Resolve like a real connection (SRV records included) off the client thread, then scan that host.
        CompletableFuture.supplyAsync(() -> ServerPortScanner.scan(
                        ServerNameResolver.DEFAULT
                                .resolveAddress(address)
                                .map(ResolvedServerAddress::getHostIp)
                                .orElse(host),
                        ports))
                .thenAccept(results -> client.execute(() -> sendPortResults(context, results)));
        return SUCCESS;
    }

    private static void sendPortResults(
            final CommandContext<FabricClientCommandSource> context, final List<ServerPortScanner.PortResult> results) {
        Component message = Component.literal(ServerPortScanner.format(results));
        String openPorts = ServerPortScanner.openPortList(results);
        if (!openPorts.isBlank()) {
            message = Component.empty()
                    .append(message)
                    .append(Component.literal(" "))
                    .append(CommandFeedback.copyable("[copy ports]", openPorts));
        }
        context.getSource().sendFeedback(message);
    }

    private static int kickDisconnect(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        client.getConnection()
                .handleDisconnect(new ClientboundDisconnectPacket(Translations.component(
                        "anarchyclient.message.server.disconnected_by_anarchyclient",
                        "Disconnected by AnarchyClient.")));
        return SUCCESS;
    }

    private static int kickPosition(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        ServerboundMovePlayerPacket packet = new ServerboundMovePlayerPacket.Pos(
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NaN,
                client.player.onGround(),
                client.player.horizontalCollision);
        // Skip module packet hooks: Anti Packet Kick exists to drop exactly this packet, and the user asked for it.
        ClientPacketListener connection = client.getConnection();
        PacketEventSilencer.runSilently(() -> connection.send(packet));
        context.getSource()
                .sendFeedback(Translations.component(
                        "anarchyclient.message.server.sent_invalid_position_packet", "Sent invalid position packet."));
        return SUCCESS;
    }

    private static Path serverExportPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("anarchyclient")
                .resolve("exports")
                .resolve("servers.json");
    }
}
