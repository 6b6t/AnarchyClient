package net.blockhost.anarchyclient.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleBindAction;
import net.blockhost.anarchyclient.profile.ProfileManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.RegistryListSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.waypoint.Waypoint;
import net.blockhost.anarchyclient.waypoint.WaypointStore;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class AnarchyClientCommands {

    private static final int SUCCESS = com.mojang.brigadier.Command.SINGLE_SUCCESS;
    private static final int MAX_FILL_BLOCKS = 32_768;

    private AnarchyClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> register(dispatcher));
    }

    static void register(final CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = root("anarchyclient");
        dispatcher.register(root);
        dispatcher.register(literal("ac").redirect(dispatcher.getRoot().getChild("anarchyclient")));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> root(final String name) {
        return literal(name)
                .then(literal("modules")
                        .executes(AnarchyClientCommands::listModules))
                .then(literal("toggle")
                        .then(argument("module", StringArgumentType.word())
                                .suggests(AnarchyClientCommands::suggestModules)
                                .executes(AnarchyClientCommands::toggleModule)))
                .then(literal("panic")
                        .executes(AnarchyClientCommands::panic))
                .then(literal("center")
                        .then(literal("middle")
                                .executes(context -> centerPlayer(context, CenterMode.MIDDLE)))
                        .then(literal("corner")
                                .executes(context -> centerPlayer(context, CenterMode.CORNER))))
                .then(literal("clear-chat")
                        .executes(AnarchyClientCommands::clearChat))
                .then(literal("reconnect")
                        .executes(AnarchyClientCommands::reconnect))
                .then(literal("profile")
                        .then(literal("list")
                                .executes(AnarchyClientCommands::listProfiles))
                        .then(literal("save")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(AnarchyClientCommands::saveProfile)))
                        .then(literal("load")
                                .then(argument("name", StringArgumentType.word())
                                        .suggests(AnarchyClientCommands::suggestProfiles)
                                        .executes(AnarchyClientCommands::loadProfile)))
                        .then(literal("delete")
                                .then(argument("name", StringArgumentType.word())
                                        .suggests(AnarchyClientCommands::suggestProfiles)
                                        .executes(AnarchyClientCommands::deleteProfile))))
                .then(literal("waypoint")
                        .then(literal("list")
                                .executes(AnarchyClientCommands::listWaypoints))
                        .then(literal("add")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(AnarchyClientCommands::addWaypoint)))
                        .then(literal("remove")
                                .then(argument("name", StringArgumentType.word())
                                        .suggests(AnarchyClientCommands::suggestWaypoints)
                                        .executes(AnarchyClientCommands::removeWaypoint))))
                .then(literal("server-info")
                        .executes(AnarchyClientCommands::serverInfo)
                        .then(literal("ports")
                                .executes(AnarchyClientCommands::serverPortsKnown)
                                .then(literal("known")
                                        .executes(AnarchyClientCommands::serverPortsKnown))
                                .then(argument("from", IntegerArgumentType.integer(1, 65535))
                                        .then(argument("to", IntegerArgumentType.integer(1, 65535))
                                                .executes(AnarchyClientCommands::serverPortsRange)))))
                .then(literal("server-list")
                        .then(literal("list")
                                .executes(AnarchyClientCommands::serverListList))
                        .then(literal("add")
                                .then(argument("address", StringArgumentType.word())
                                        .executes(context -> serverListAdd(context, null))
                                        .then(argument("name", StringArgumentType.greedyString())
                                                .executes(context -> serverListAdd(context, StringArgumentType.getString(context, "name"))))))
                        .then(literal("cleanup")
                                .executes(AnarchyClientCommands::serverListCleanup))
                        .then(literal("export")
                                .executes(AnarchyClientCommands::serverListExport))
                        .then(literal("import")
                                .then(argument("path", StringArgumentType.greedyString())
                                        .executes(AnarchyClientCommands::serverListImport))))
                .then(literal("kick")
                        .then(literal("disconnect")
                                .executes(AnarchyClientCommands::kickDisconnect))
                        .then(literal("position")
                                .executes(AnarchyClientCommands::kickPosition)))
                .then(literal("give")
                        .then(literal("head")
                                .then(argument("player", StringArgumentType.word())
                                        .suggests(AnarchyClientCommands::suggestOnlinePlayers)
                                        .executes(AnarchyClientCommands::giveHead)))
                        .then(literal("hologram")
                                .then(argument("text", StringArgumentType.greedyString())
                                        .executes(AnarchyClientCommands::giveHologram)))
                        .then(literal("bossbar")
                                .then(argument("text", StringArgumentType.greedyString())
                                        .executes(AnarchyClientCommands::giveBossbar)))
                        .then(literal("random")
                                .executes(context -> giveRandom(context, 1))
                                .then(argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> giveRandom(context, IntegerArgumentType.getInteger(context, "count"))))))
                .then(literal("heads")
                        .then(argument("player", StringArgumentType.word())
                                .suggests(AnarchyClientCommands::suggestOnlinePlayers)
                                .executes(AnarchyClientCommands::giveHead)))
                .then(literal("seed")
                        .executes(AnarchyClientCommands::showSeed)
                        .then(literal("set")
                                .then(argument("seed", StringArgumentType.greedyString())
                                        .executes(AnarchyClientCommands::setSeed)))
                        .then(literal("list")
                                .executes(AnarchyClientCommands::listSeeds))
                        .then(literal("delete")
                                .executes(AnarchyClientCommands::deleteSeed)))
                .then(literal("locate")
                        .then(literal("slime-chunk")
                                .executes(AnarchyClientCommands::locateSlimeChunk)))
                .then(literal("terrain-export")
                        .then(argument("distance", IntegerArgumentType.integer(1, 64))
                                .executes(AnarchyClientCommands::terrainExport)))
                .then(literal("save-skin")
                        .then(argument("player", StringArgumentType.word())
                                .suggests(AnarchyClientCommands::suggestOnlinePlayers)
                                .executes(AnarchyClientCommands::saveSkin)))
                .then(literal("ghost")
                        .executes(context -> ghostBlocks(context, 4))
                        .then(argument("radius", IntegerArgumentType.integer(1, 16))
                                .executes(context -> ghostBlocks(context, IntegerArgumentType.getInteger(context, "radius")))))
                .then(literal("set-velocity")
                        .then(argument("value", DoubleArgumentType.doubleArg())
                                .executes(AnarchyClientCommands::setVelocityY)
                                .then(argument("second", DoubleArgumentType.doubleArg())
                                        .executes(AnarchyClientCommands::setVelocityXZ)
                                        .then(argument("third", DoubleArgumentType.doubleArg())
                                                .executes(AnarchyClientCommands::setVelocityXYZ)))))
                .then(literal("teleport")
                        .then(argument("x", StringArgumentType.word())
                                .then(argument("y", StringArgumentType.word())
                                        .then(argument("z", StringArgumentType.word())
                                                .executes(context -> teleport(context, false))
                                                .then(argument("yaw", FloatArgumentType.floatArg())
                                                        .then(argument("pitch", FloatArgumentType.floatArg())
                                                                .executes(context -> teleport(context, true))))))))
                .then(literal("setblock")
                        .then(argument("x", StringArgumentType.word())
                                .then(argument("y", StringArgumentType.word())
                                        .then(argument("z", StringArgumentType.word())
                                                .then(argument("block", StringArgumentType.word())
                                                        .suggests(AnarchyClientCommands::suggestBlocks)
                                                        .executes(AnarchyClientCommands::setBlock))))))
                .then(literal("fill")
                        .then(argument("from_x", StringArgumentType.word())
                                .then(argument("from_y", StringArgumentType.word())
                                        .then(argument("from_z", StringArgumentType.word())
                                                .then(argument("to_x", StringArgumentType.word())
                                                        .then(argument("to_y", StringArgumentType.word())
                                                                .then(argument("to_z", StringArgumentType.word())
                                                                        .then(argument("block", StringArgumentType.word())
                                                                                .suggests(AnarchyClientCommands::suggestBlocks)
                                                                                .executes(context -> fill(context, false))
                                                                                .then(literal("replace")
                                                                                        .then(argument("filter", StringArgumentType.word())
                                                                                                .suggests(AnarchyClientCommands::suggestBlocks)
                                                                                                .executes(context -> fill(context, true))))))))))))
                .then(literal("bind")
                        .then(argument("module", StringArgumentType.word())
                                .suggests(AnarchyClientCommands::suggestModules)
                                .then(argument("key", IntegerArgumentType.integer(GLFW.GLFW_KEY_SPACE))
                                        .executes(context -> bindModule(context, ModuleBindAction.TOGGLE))
                                        .then(argument("action", StringArgumentType.word())
                                                .suggests(AnarchyClientCommands::suggestBindActions)
                                                .executes(AnarchyClientCommands::bindModule)))))
                .then(literal("unbind")
                        .then(argument("module", StringArgumentType.word())
                                .suggests(AnarchyClientCommands::suggestModules)
                                .executes(AnarchyClientCommands::unbindModule)))
                .then(literal("setting")
                        .then(argument("module", StringArgumentType.word())
                                .suggests(AnarchyClientCommands::suggestModules)
                                .then(argument("setting", StringArgumentType.word())
                                        .suggests(AnarchyClientCommands::suggestSettings)
                                        .executes(AnarchyClientCommands::showSetting)
                                        .then(argument("value", StringArgumentType.greedyString())
                                                .suggests(AnarchyClientCommands::suggestSettingValues)
                                                .executes(AnarchyClientCommands::setSetting)))));
    }

    private static int listModules(final CommandContext<FabricClientCommandSource> context) {
        String modules = AnarchyClient.MODULES.all().stream()
                .map(module -> module.id() + "=" + (module.enabled() ? "on" : "off"))
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("No modules registered.");
        context.getSource().sendFeedback(Component.literal(modules));
        return SUCCESS;
    }

    private static int toggleModule(final CommandContext<FabricClientCommandSource> context) {
        Module module = findModule(context);
        if (module == null) {
            return 0;
        }
        module.toggle();
        AnarchyClient.CONFIG.save();
        context.getSource().sendFeedback(Component.literal(module.name() + " is now " + (module.enabled() ? "on" : "off") + "."));
        return SUCCESS;
    }

    private static int panic(final CommandContext<FabricClientCommandSource> context) {
        long disabled = AnarchyClient.MODULES.all().stream()
                .filter(Module::enabled)
                .peek(module -> module.enabled(false))
                .count();
        AnarchyClient.CONFIG.save();
        context.getSource().sendFeedback(Component.literal("Disabled " + disabled + " module" + (disabled == 1 ? "" : "s") + "."));
        return SUCCESS;
    }

    private static int centerPlayer(final CommandContext<FabricClientCommandSource> context, final CenterMode mode) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.connection == null) {
            context.getSource().sendError(Component.literal("You must be in-game to center yourself."));
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
        context.getSource().sendFeedback(Component.literal("Centered player."));
        return SUCCESS;
    }

    private static int clearChat(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        client.gui.hud.getChat().clearMessages(false);
        context.getSource().sendFeedback(Component.literal("Cleared chat."));
        return SUCCESS;
    }

    private static int reconnect(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        ServerData server = client.getCurrentServer();
        if (server == null) {
            context.getSource().sendError(Component.literal("No multiplayer server is available to reconnect to."));
            return 0;
        }
        ServerAddress address = ServerAddress.parseString(server.ip);
        ConnectScreen.startConnecting(new TitleScreen(), client, address, server, false, null);
        return SUCCESS;
    }

    private static int listProfiles(final CommandContext<FabricClientCommandSource> context) {
        List<ProfileManager.ProfileSummary> profiles = AnarchyClient.PROFILES.summaries();
        if (profiles.isEmpty()) {
            context.getSource().sendFeedback(Component.literal("No saved profiles."));
            return SUCCESS;
        }
        for (ProfileManager.ProfileSummary profile : profiles) {
            String updated = profile.updatedAt().isBlank() ? "unknown" : profile.updatedAt();
            context.getSource().sendFeedback(Component.literal(profile.name() + " | modules: " + profile.modules() + " | updated: " + updated));
        }
        return SUCCESS;
    }

    private static int saveProfile(final CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        try {
            ProfileManager.Profile profile = AnarchyClient.PROFILES.capture(name, AnarchyClient.MODULES);
            context.getSource().sendFeedback(Component.literal("Saved profile " + profile.name + "."));
            return SUCCESS;
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private static int loadProfile(final CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        int changed = AnarchyClient.PROFILES.apply(name, AnarchyClient.MODULES);
        if (changed < 0) {
            context.getSource().sendError(Component.literal("Unknown profile: " + name));
            return 0;
        }
        AnarchyClient.CONFIG.save();
        context.getSource().sendFeedback(Component.literal("Loaded profile " + name + " and changed " + changed + " value" + (changed == 1 ? "" : "s") + "."));
        return SUCCESS;
    }

    private static int deleteProfile(final CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        if (AnarchyClient.PROFILES.delete(name)) {
            context.getSource().sendFeedback(Component.literal("Deleted profile " + name + "."));
        } else {
            context.getSource().sendFeedback(Component.literal("No saved profile named " + name + "."));
        }
        return SUCCESS;
    }

    private static int listWaypoints(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        String world = WaypointStore.currentWorld(client);
        List<Waypoint> waypoints = AnarchyClient.WAYPOINTS.byWorld(world);
        if (waypoints.isEmpty()) {
            context.getSource().sendFeedback(Component.literal("No waypoints for " + world + "."));
            return SUCCESS;
        }
        for (Waypoint waypoint : waypoints) {
            BlockPos pos = waypoint.pos();
            context.getSource().sendFeedback(Component.literal(waypoint.name() + " = "
                    + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
        }
        return SUCCESS;
    }

    private static int addWaypoint(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context, "add a waypoint");
        if (client == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, "name");
        String world = WaypointStore.currentWorld(client);
        BlockPos pos = client.player.blockPosition();
        AnarchyClient.WAYPOINTS.add(new Waypoint(world, name, pos, WaypointStore.DEFAULT_COLOR));
        context.getSource().sendFeedback(Component.literal("Saved waypoint " + name + " at "
                + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "."));
        return SUCCESS;
    }

    private static int removeWaypoint(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        String name = StringArgumentType.getString(context, "name");
        String world = WaypointStore.currentWorld(client);
        if (AnarchyClient.WAYPOINTS.remove(world, name)) {
            context.getSource().sendFeedback(Component.literal("Removed waypoint " + name + "."));
        } else {
            context.getSource().sendFeedback(Component.literal("No waypoint named " + name + " for this world."));
        }
        return SUCCESS;
    }

    private static int serverInfo(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        ServerData server = client.getCurrentServer();
        ClientPacketListener connection = client.getConnection();
        String address = server == null ? "singleplayer" : server.ip;
        String brand = connection == null || connection.serverBrand() == null ? "unknown" : connection.serverBrand();
        context.getSource().sendFeedback(Component.literal("Server: " + address + " | Brand: " + brand));
        return SUCCESS;
    }

    private static int serverListList(final CommandContext<FabricClientCommandSource> context) {
        ServerList list = ServerListTools.load(Minecraft.getInstance());
        List<ServerListTools.ServerEntry> entries = ServerListTools.entries(list);
        if (entries.isEmpty()) {
            context.getSource().sendFeedback(Component.literal("No saved multiplayer servers."));
            return SUCCESS;
        }
        for (ServerListTools.ServerEntry entry : entries) {
            context.getSource().sendFeedback(Component.literal(entry.name() + " = ")
                    .append(CommandFeedback.copyable(entry.address(), entry.address())));
        }
        return SUCCESS;
    }

    private static int serverListAdd(final CommandContext<FabricClientCommandSource> context, final String name) {
        Minecraft client = Minecraft.getInstance();
        ServerList list = ServerListTools.load(client);
        String address = StringArgumentType.getString(context, "address");
        int added = ServerListTools.addOrUpdate(list, address, name);
        list.save();
        context.getSource().sendFeedback(Component.literal((added == 0 ? "Updated " : "Added ")
                + ServerListTools.normalizeAddress(address) + "."));
        return SUCCESS;
    }

    private static int serverListCleanup(final CommandContext<FabricClientCommandSource> context) {
        ServerList list = ServerListTools.load(Minecraft.getInstance());
        int removed = ServerListTools.cleanup(list);
        list.save();
        context.getSource().sendFeedback(Component.literal("Removed " + removed + " duplicate server"
                + (removed == 1 ? "" : "s") + "."));
        return SUCCESS;
    }

    private static int serverListExport(final CommandContext<FabricClientCommandSource> context) {
        ServerList list = ServerListTools.load(Minecraft.getInstance());
        Path path = serverExportPath();
        try {
            ServerListTools.exportJson(list, path);
            context.getSource().sendFeedback(Component.literal("Exported " + list.size() + " server"
                    + (list.size() == 1 ? "" : "s") + " to ")
                    .append(CommandFeedback.copyable(path.toString(), path.toString())));
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource().sendError(Component.literal("Failed to export servers: " + exception.getMessage()));
            return 0;
        }
    }

    private static int serverListImport(final CommandContext<FabricClientCommandSource> context) {
        ServerList list = ServerListTools.load(Minecraft.getInstance());
        Path path = Path.of(StringArgumentType.getString(context, "path"));
        try {
            int added = ServerListTools.importJson(list, path);
            int duplicates = ServerListTools.cleanup(list);
            list.save();
            context.getSource().sendFeedback(Component.literal("Imported " + added + " new server"
                    + (added == 1 ? "" : "s") + " and removed " + duplicates + " duplicate"
                    + (duplicates == 1 ? "" : "s") + "."));
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource().sendError(Component.literal("Failed to import servers: " + exception.getMessage()));
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

    private static int scanServerPorts(final CommandContext<FabricClientCommandSource> context,
                                       final List<ServerPortScanner.PortCheck> ports,
                                       final String label) {
        Minecraft client = Minecraft.getInstance();
        ServerData server = client.getCurrentServer();
        if (server == null) {
            context.getSource().sendError(Component.literal("You must be connected to a multiplayer server to scan ports."));
            return 0;
        }
        String host = ServerAddress.parseString(server.ip).getHost();
        if (host.isBlank()) {
            context.getSource().sendError(Component.literal("Could not resolve server host from " + server.ip + "."));
            return 0;
        }
        context.getSource().sendFeedback(Component.literal("Scanning " + label + " on " + host + "..."));
        CompletableFuture
                .supplyAsync(() -> ServerPortScanner.scan(host, ports))
                .thenAccept(results -> client.execute(() -> sendPortResults(context, results)));
        return SUCCESS;
    }

    private static void sendPortResults(final CommandContext<FabricClientCommandSource> context,
                                        final List<ServerPortScanner.PortResult> results) {
        Component message = Component.literal(ServerPortScanner.format(results));
        String openPorts = ServerPortScanner.openPortList(results);
        if (!openPorts.isBlank()) {
            message = Component.empty().append(message).append(Component.literal(" "))
                    .append(CommandFeedback.copyable("[copy ports]", openPorts));
        }
        context.getSource().sendFeedback(message);
    }

    private static int kickDisconnect(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context, "kick yourself");
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        client.getConnection().handleDisconnect(new ClientboundDisconnectPacket(Component.literal("Disconnected by AnarchyClient.")));
        return SUCCESS;
    }

    private static int kickPosition(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context, "kick yourself");
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NaN,
                client.player.onGround(),
                client.player.horizontalCollision
        ));
        context.getSource().sendFeedback(Component.literal("Sent invalid position packet."));
        return SUCCESS;
    }

    private static int giveHead(final CommandContext<FabricClientCommandSource> context) {
        return giveCreativeStack(context, CreativeItemFactory.playerHead(StringArgumentType.getString(context, "player")));
    }

    private static int giveHologram(final CommandContext<FabricClientCommandSource> context) {
        return giveCreativeStack(context, CreativeItemFactory.hologramArmorStand(StringArgumentType.getString(context, "text")));
    }

    private static int giveBossbar(final CommandContext<FabricClientCommandSource> context) {
        return giveCreativeStack(context, CreativeItemFactory.bossbarEgg(StringArgumentType.getString(context, "text")));
    }

    private static int giveRandom(final CommandContext<FabricClientCommandSource> context, final int count) {
        Minecraft client = requireCreative(context);
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        int slot = selectedCreativeSlot(client.player);
        for (int index = 0; index < count; index++) {
            client.getConnection().send(new ServerboundSetCreativeModeSlotPacket(slot, CreativeItemFactory.randomItem(index)));
        }
        context.getSource().sendFeedback(Component.literal("Sent " + count + " creative item packet"
                + (count == 1 ? "" : "s") + "."));
        return SUCCESS;
    }

    private static int giveCreativeStack(final CommandContext<FabricClientCommandSource> context, final ItemStack stack) {
        Minecraft client = requireCreative(context);
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        client.getConnection().send(new ServerboundSetCreativeModeSlotPacket(selectedCreativeSlot(client.player), stack));
        context.getSource().sendFeedback(Component.literal("Sent creative item: ")
                .append(stack.getHoverName()));
        return SUCCESS;
    }

    private static int showSeed(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        String world = SeedStore.worldKey(client);
        try {
            SeedStore.SeedRecord record = SeedStore.get(seedPath(), world);
            if (record == null) {
                context.getSource().sendFeedback(Component.literal("No saved seed for " + world + "."));
            } else {
                context.getSource().sendFeedback(Component.literal(world + " seed: ")
                        .append(CommandFeedback.copyable(record.seed(), record.seed()))
                        .append(Component.literal(seedMetadata(record))));
            }
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource().sendError(Component.literal("Failed to read seeds: " + exception.getMessage()));
            return 0;
        }
    }

    private static int setSeed(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        String world = SeedStore.worldKey(client);
        try {
            SeedStore.SeedRecord record = SeedStore.put(seedPath(), world,
                    StringArgumentType.getString(context, "seed"),
                    Instant.now(),
                    SharedConstants.getCurrentVersion().name(),
                    currentDimension(client),
                    "manual");
            context.getSource().sendFeedback(Component.literal("Saved seed for " + record.world() + ": " + record.seed()));
            return SUCCESS;
        } catch (IllegalArgumentException | IOException exception) {
            context.getSource().sendError(Component.literal("Failed to save seed: " + exception.getMessage()));
            return 0;
        }
    }

    private static int listSeeds(final CommandContext<FabricClientCommandSource> context) {
        try {
            List<SeedStore.SeedRecord> records = SeedStore.list(seedPath());
            if (records.isEmpty()) {
                context.getSource().sendFeedback(Component.literal("No saved seeds."));
                return SUCCESS;
            }
            for (SeedStore.SeedRecord record : records) {
                context.getSource().sendFeedback(Component.literal(record.world() + " = " + record.seed()
                        + seedMetadata(record)));
            }
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource().sendError(Component.literal("Failed to read seeds: " + exception.getMessage()));
            return 0;
        }
    }

    private static int deleteSeed(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        String world = SeedStore.worldKey(client);
        try {
            if (SeedStore.delete(seedPath(), world)) {
                context.getSource().sendFeedback(Component.literal("Deleted saved seed for " + world + "."));
            } else {
                context.getSource().sendFeedback(Component.literal("No saved seed for " + world + "."));
            }
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource().sendError(Component.literal("Failed to delete seed: " + exception.getMessage()));
            return 0;
        }
    }

    private static int locateSlimeChunk(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context, "locate a slime chunk");
        if (client == null) {
            return 0;
        }
        try {
            SeedStore.SeedRecord record = SeedStore.get(seedPath(), SeedStore.worldKey(client));
            if (record == null) {
                context.getSource().sendError(Component.literal("Save a seed first with /ac seed set <seed>."));
                return 0;
            }
            long seed = numericSeed(record.seed());
            BlockPos playerPos = client.player.blockPosition();
            BlockPos result = closestSlimeChunk(seed, Math.floorDiv(playerPos.getX(), 16),
                    Math.floorDiv(playerPos.getZ(), 16), 128);
            String coords = result.getX() + ", " + result.getZ();
            context.getSource().sendFeedback(Component.literal("Nearest slime chunk starts at ")
                    .append(CommandFeedback.copyable(coords, coords)));
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource().sendError(Component.literal("Failed to read seeds: " + exception.getMessage()));
            return 0;
        }
    }

    private static int terrainExport(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            context.getSource().sendError(Component.literal("You must be in-game to export terrain."));
            return 0;
        }
        int distance = IntegerArgumentType.getInteger(context, "distance");
        BlockPos center = client.player.blockPosition();
        Path output = terrainExportPath(center, distance);
        try {
            Files.createDirectories(output.getParent());
            Files.write(output, terrainLines(client, center, distance), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            context.getSource().sendError(Component.literal("Failed to export terrain: " + exception.getMessage()));
            return 0;
        }
        context.getSource().sendFeedback(Component.literal("Exported terrain to " + output));
        return SUCCESS;
    }

    private static int saveSkin(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        ClientPacketListener connection = client.getConnection();
        if (connection == null) {
            context.getSource().sendError(Component.literal("You must be connected to a server to save a skin."));
            return 0;
        }
        String playerName = StringArgumentType.getString(context, "player");
        PlayerInfo info = connection.getPlayerInfoIgnoreCase(playerName);
        if (info == null) {
            context.getSource().sendError(Component.literal("Unknown online player: " + playerName));
            return 0;
        }
        String profileName = info.getProfile().name();
        String uuid = info.getProfile().id().toString().replace("-", "");
        downloadSkin(client, context.getSource(), profileName, uuid);
        context.getSource().sendFeedback(Component.literal("Saving skin for " + profileName + "..."));
        return SUCCESS;
    }

    private static int ghostBlocks(final CommandContext<FabricClientCommandSource> context, final int radius) {
        Minecraft client = requireInGame(context, "refresh ghost blocks");
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        BlockPos center = client.player.blockPosition();
        int sent = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    client.getConnection().send(new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                            center.offset(dx, dy, dz),
                            Direction.UP
                    ));
                    sent++;
                }
            }
        }
        context.getSource().sendFeedback(Component.literal("Sent " + sent + " ghost-block refresh packets."));
        return SUCCESS;
    }

    private static int setVelocityY(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context, "set velocity");
        if (client == null) {
            return 0;
        }
        Vec3 current = client.player.getDeltaMovement();
        client.player.setDeltaMovement(current.x, DoubleArgumentType.getDouble(context, "value"), current.z);
        return SUCCESS;
    }

    private static int setVelocityXZ(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context, "set velocity");
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
        Minecraft client = requireInGame(context, "set velocity");
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

    private static int teleport(final CommandContext<FabricClientCommandSource> context, final boolean includeRotation) {
        Minecraft client = requireInGame(context, "teleport");
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        BlockPos pos;
        try {
            pos = parseBlockPosition(
                    StringArgumentType.getString(context, "x"),
                    StringArgumentType.getString(context, "y"),
                    StringArgumentType.getString(context, "z"),
                    client.player.position()
            );
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(exception.getMessage()));
            return 0;
        }
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
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

    private static int setBlock(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context, "set a client block");
        if (client == null || client.level == null) {
            return 0;
        }
        try {
            BlockPos pos = parseBlockPosition(
                    StringArgumentType.getString(context, "x"),
                    StringArgumentType.getString(context, "y"),
                    StringArgumentType.getString(context, "z"),
                    client.player.position()
            );
            client.level.setBlockAndUpdate(pos, parseBlock(StringArgumentType.getString(context, "block")).defaultBlockState());
            return SUCCESS;
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private static int fill(final CommandContext<FabricClientCommandSource> context, final boolean replaceOnly) {
        Minecraft client = requireInGame(context, "fill client blocks");
        if (client == null || client.level == null) {
            return 0;
        }
        try {
            BlockArea area = blockArea(
                    parseBlockPosition(
                            StringArgumentType.getString(context, "from_x"),
                            StringArgumentType.getString(context, "from_y"),
                            StringArgumentType.getString(context, "from_z"),
                            client.player.position()
                    ),
                    parseBlockPosition(
                            StringArgumentType.getString(context, "to_x"),
                            StringArgumentType.getString(context, "to_y"),
                            StringArgumentType.getString(context, "to_z"),
                            client.player.position()
                    ),
                    MAX_FILL_BLOCKS
            );
            BlockState state = parseBlock(StringArgumentType.getString(context, "block")).defaultBlockState();
            Block filter = replaceOnly ? parseBlock(StringArgumentType.getString(context, "filter")) : null;
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
            context.getSource().sendFeedback(Component.literal("Changed " + changed + " client block" + (changed == 1 ? "" : "s") + "."));
            return SUCCESS;
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(exception.getMessage()));
            return 0;
        }
    }

    private static int bindModule(final CommandContext<FabricClientCommandSource> context) {
        ModuleBindAction action = ModuleBindAction.parse(StringArgumentType.getString(context, "action"));
        return bindModule(context, action);
    }

    private static int bindModule(final CommandContext<FabricClientCommandSource> context, final ModuleBindAction action) {
        Module module = findModule(context);
        if (module == null) {
            return 0;
        }
        int key = IntegerArgumentType.getInteger(context, "key");
        module.keybind().key(key);
        module.keybind().action(action);
        AnarchyClient.CONFIG.save();
        context.getSource().sendFeedback(Component.literal("Bound " + module.name() + " to key " + key + " as " + action.name().toLowerCase(Locale.ROOT) + "."));
        return SUCCESS;
    }

    private static int unbindModule(final CommandContext<FabricClientCommandSource> context) {
        Module module = findModule(context);
        if (module == null) {
            return 0;
        }
        module.keybind().key(GLFW.GLFW_KEY_UNKNOWN);
        AnarchyClient.CONFIG.save();
        context.getSource().sendFeedback(Component.literal("Unbound " + module.name() + "."));
        return SUCCESS;
    }

    private static int showSetting(final CommandContext<FabricClientCommandSource> context) {
        Module module = findModule(context);
        Setting<?> setting = findSetting(context, module);
        if (setting == null) {
            return 0;
        }
        context.getSource().sendFeedback(Component.literal(module.name() + " " + setting.name() + " = " + setting.value()));
        return SUCCESS;
    }

    private static int setSetting(final CommandContext<FabricClientCommandSource> context) {
        Module module = findModule(context);
        Setting<?> setting = findSetting(context, module);
        if (setting == null) {
            return 0;
        }
        String value = StringArgumentType.getString(context, "value");
        try {
            setSettingValue(setting, value);
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(exception.getMessage()));
            return 0;
        }
        AnarchyClient.CONFIG.save();
        context.getSource().sendFeedback(Component.literal(module.name() + " " + setting.name() + " = " + setting.value()));
        return SUCCESS;
    }

    static void setSettingValue(final Setting<?> setting, final String value) {
        if (setting instanceof BooleanSetting bool) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                throw new IllegalArgumentException("Expected true or false.");
            }
            bool.value(Boolean.parseBoolean(value));
            return;
        }
        if (setting instanceof NumberSetting number) {
            try {
                number.value(Double.parseDouble(value));
                return;
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Expected a number.", exception);
            }
        }
        if (setting instanceof SelectSetting select) {
            if (!select.options().contains(value)) {
                throw new IllegalArgumentException("Expected one of: " + String.join(", ", select.options()));
            }
            select.value(value);
            return;
        }
        if (setting instanceof RegistryListSetting<?> list) {
            list.valueFromString(value);
            return;
        }
        if (setting instanceof StringSetting string) {
            string.value(value);
            return;
        }
        throw new IllegalArgumentException("This setting cannot be changed from commands.");
    }

    private static Module findModule(final CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "module");
        Module module = AnarchyClient.MODULES.find(id).orElse(null);
        if (module == null) {
            context.getSource().sendError(Component.literal("Unknown module: " + id));
        }
        return module;
    }

    private static Setting<?> findSetting(final CommandContext<FabricClientCommandSource> context, final Module module) {
        if (module == null) {
            return null;
        }
        String id = StringArgumentType.getString(context, "setting");
        for (Setting<?> setting : module.settings()) {
            if (setting.id().equals(id) || setting.aliases().contains(id)) {
                return setting;
            }
        }
        context.getSource().sendError(Component.literal("Unknown setting: " + id));
        return null;
    }

    private static CompletableFuture<Suggestions> suggestModules(final CommandContext<FabricClientCommandSource> context,
                                                                 final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(AnarchyClient.MODULES.all().stream().map(Module::id), builder);
    }

    private static CompletableFuture<Suggestions> suggestSettings(final CommandContext<FabricClientCommandSource> context,
                                                                  final SuggestionsBuilder builder) {
        Module module = AnarchyClient.MODULES.find(StringArgumentType.getString(context, "module")).orElse(null);
        if (module == null) {
            return builder.buildFuture();
        }
        return SharedSuggestionProvider.suggest(module.settings().stream().map(Setting::id), builder);
    }

    private static CompletableFuture<Suggestions> suggestBindActions(final CommandContext<FabricClientCommandSource> context,
                                                                    final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(java.util.Arrays.stream(ModuleBindAction.values())
                .map(action -> action.name().toLowerCase(Locale.ROOT)), builder);
    }

    private static CompletableFuture<Suggestions> suggestProfiles(final CommandContext<FabricClientCommandSource> context,
                                                                  final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(AnarchyClient.PROFILES.summaries().stream()
                .map(ProfileManager.ProfileSummary::name), builder);
    }

    private static CompletableFuture<Suggestions> suggestWaypoints(final CommandContext<FabricClientCommandSource> context,
                                                                   final SuggestionsBuilder builder) {
        String world = WaypointStore.currentWorld(Minecraft.getInstance());
        return SharedSuggestionProvider.suggest(AnarchyClient.WAYPOINTS.byWorld(world).stream()
                .map(Waypoint::name), builder);
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(final CommandContext<FabricClientCommandSource> context,
                                                                       final SuggestionsBuilder builder) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return builder.buildFuture();
        }
        return SharedSuggestionProvider.suggest(connection.getOnlinePlayers().stream()
                .map(info -> info.getProfile().name()), builder);
    }

    private static CompletableFuture<Suggestions> suggestBlocks(final CommandContext<FabricClientCommandSource> context,
                                                                final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(BuiltInRegistries.BLOCK.keySet().stream()
                .map(Identifier::toString), builder);
    }

    private static CompletableFuture<Suggestions> suggestSettingValues(final CommandContext<FabricClientCommandSource> context,
                                                                      final SuggestionsBuilder builder) {
        Module module = AnarchyClient.MODULES.find(StringArgumentType.getString(context, "module")).orElse(null);
        Setting<?> setting = module == null ? null : module.settings().stream()
                .filter(candidate -> candidate.id().equals(StringArgumentType.getString(context, "setting")))
                .findFirst()
                .orElse(null);
        if (setting instanceof BooleanSetting) {
            return SharedSuggestionProvider.suggest(new String[]{"true", "false"}, builder);
        }
        if (setting instanceof SelectSetting select) {
            return SharedSuggestionProvider.suggest(select.options(), builder);
        }
        if (setting instanceof RegistryListSetting<?> list) {
            return SharedSuggestionProvider.suggest(list.suggestions(), builder);
        }
        return builder.buildFuture();
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
                    if (state.isAir() || state.getCollisionShape(client.level, pos).isEmpty()) {
                        continue;
                    }
                    lines.add((x - center.getX()) + "," + (y - center.getY()) + "," + (z - center.getZ())
                            + "," + BuiltInRegistries.BLOCK.getKey(state.getBlock()));
                }
            }
        }
        return List.copyOf(lines);
    }

    private static Path terrainExportPath(final BlockPos center, final int distance) {
        String fileName = "terrain-" + center.getX() + "-" + center.getY() + "-" + center.getZ()
                + "-r" + distance + ".csv";
        return FabricLoader.getInstance().getConfigDir().resolve("anarchyclient").resolve("exports").resolve(fileName);
    }

    private static void downloadSkin(final Minecraft client, final FabricClientCommandSource source,
                                     final String profileName, final String uuid) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpClient http = HttpClient.newHttpClient();
                HttpRequest profileRequest = HttpRequest.newBuilder(URI.create(
                                "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
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
                HttpResponse<byte[]> skinResponse = http.send(HttpRequest.newBuilder(URI.create(skinUrl)).GET().build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                if (skinResponse.statusCode() != 200) {
                    throw new IOException("skin request returned HTTP " + skinResponse.statusCode());
                }
                Path output = skinPath(profileName);
                Files.createDirectories(output.getParent());
                Files.write(output, skinResponse.body());
                client.execute(() -> source.sendFeedback(Component.literal("Saved skin to " + output)));
            } catch (IOException exception) {
                client.execute(() -> source.sendError(Component.literal("Failed to save skin: " + exception.getMessage())));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                client.execute(() -> source.sendError(Component.literal("Failed to save skin: interrupted")));
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

    private static Path seedPath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("anarchyclient")
                .resolve("seeds.json");
    }

    private static Path serverExportPath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("anarchyclient")
                .resolve("exports")
                .resolve("servers.json");
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
        long randomSeed = seed
                + (long) (chunkX * chunkX) * 4987142L
                + (long) chunkX * 5947611L
                + (long) (chunkZ * chunkZ) * 4392871L
                + (long) chunkZ * 389711L
                ^ 987234911L;
        return new Random(randomSeed).nextInt(10) == 0;
    }

    static BlockPos closestSlimeChunk(final long seed, final int centerChunkX, final int centerChunkZ,
                                      final int radiusChunks) {
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
            if (best != null) {
                return best;
            }
        }
        return new BlockPos(centerChunkX * 16, 0, centerChunkZ * 16);
    }

    static String sanitizeFileName(final String value) {
        if (value == null || value.isBlank()) {
            return "skin";
        }
        String sanitized = value.replaceAll("[^A-Za-z0-9_.-]", "_");
        return sanitized.isBlank() ? "skin" : sanitized;
    }

    private static Minecraft requireInGame(final CommandContext<FabricClientCommandSource> context,
                                           final String action) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            context.getSource().sendError(Component.literal("You must be in-game to " + action + "."));
            return null;
        }
        return client;
    }

    private static Minecraft requireCreative(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context, "send creative item packets");
        if (client == null || client.player == null || !client.player.getAbilities().instabuild) {
            context.getSource().sendError(Component.literal("You must be in creative mode to use this command."));
            return null;
        }
        return client;
    }

    private static int selectedCreativeSlot(final Player player) {
        return 36 + player.getInventory().getSelectedSlot();
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

    static BlockPos parseBlockPosition(final String xToken, final String yToken, final String zToken,
                                       final Vec3 origin) {
        return BlockPos.containing(
                parseCoordinate(xToken, origin.x),
                parseCoordinate(yToken, origin.y),
                parseCoordinate(zToken, origin.z)
        );
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

    private static double parseDouble(final String token, final String original) {
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid coordinate: " + original, exception);
        }
    }

    record BlockArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int count) {
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
