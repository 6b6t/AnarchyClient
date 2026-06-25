package net.blockhost.anarchyclient.command;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.lwjgl.glfw.GLFW;

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
