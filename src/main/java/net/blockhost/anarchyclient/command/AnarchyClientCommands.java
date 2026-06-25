package net.blockhost.anarchyclient.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleBindAction;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class AnarchyClientCommands {

    private static final int SUCCESS = com.mojang.brigadier.Command.SINGLE_SUCCESS;

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
                .then(literal("server-info")
                        .executes(AnarchyClientCommands::serverInfo))
                .then(literal("terrain-export")
                        .then(argument("distance", IntegerArgumentType.integer(1, 64))
                                .executes(AnarchyClientCommands::terrainExport)))
                .then(literal("save-skin")
                        .then(argument("player", StringArgumentType.word())
                                .suggests(AnarchyClientCommands::suggestOnlinePlayers)
                                .executes(AnarchyClientCommands::saveSkin)))
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

    private static int serverInfo(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        ServerData server = client.getCurrentServer();
        ClientPacketListener connection = client.getConnection();
        String address = server == null ? "singleplayer" : server.ip;
        String brand = connection == null || connection.serverBrand() == null ? "unknown" : connection.serverBrand();
        context.getSource().sendFeedback(Component.literal("Server: " + address + " | Brand: " + brand));
        return SUCCESS;
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

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(final CommandContext<FabricClientCommandSource> context,
                                                                       final SuggestionsBuilder builder) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return builder.buildFuture();
        }
        return SharedSuggestionProvider.suggest(connection.getOnlinePlayers().stream()
                .map(info -> info.getProfile().name()), builder);
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

    static String sanitizeFileName(final String value) {
        if (value == null || value.isBlank()) {
            return "skin";
        }
        String sanitized = value.replaceAll("[^A-Za-z0-9_.-]", "_");
        return sanitized.isBlank() ? "skin" : sanitized;
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
