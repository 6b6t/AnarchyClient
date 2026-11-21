package net.blockhost.anarchyclient.command;

import static net.blockhost.anarchyclient.command.CommandSupport.SUCCESS;
import static net.blockhost.anarchyclient.command.CommandSupport.requireInGame;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleBindAction;
import net.blockhost.anarchyclient.module.impl.SwarmModule;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.TextValueSetting;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLScancode;

/**
 * Registers and handles the {@code modules}, {@code toggle}, {@code panic}, {@code prefix}, {@code bind}, {@code unbind}, {@code setting}, {@code notebot}, and {@code swarm} subcommands.
 */
final class ModuleCommands {

    private ModuleCommands() {}

    static void register(final LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root.then(literal("modules").executes(ModuleCommands::listModules))
                .then(literal("toggle")
                        .then(argument("module", StringArgumentType.word())
                                .suggests(ModuleCommands::suggestModules)
                                .executes(ModuleCommands::toggleModule)))
                .then(literal("panic").executes(ModuleCommands::panic))
                .then(literal("prefix")
                        .executes(ModuleCommands::showPrefix)
                        // word() only accepts letters, digits and _-.+, which rules out most prefixes.
                        .then(argument("prefix", StringArgumentType.greedyString())
                                .executes(ModuleCommands::setPrefix)))
                .then(literal("notebot")
                        .then(literal("status").executes(ModuleCommands::notebotStatus))
                        .then(literal("play")
                                .then(argument("note", IntegerArgumentType.integer(0, 24))
                                        .executes(ModuleCommands::notebotPlayNote))))
                .then(literal("swarm")
                        .then(literal("status").executes(ModuleCommands::swarmStatus))
                        .then(literal("send")
                                .then(argument("message", StringArgumentType.greedyString())
                                        .executes(ModuleCommands::swarmSend))))
                .then(literal("bind")
                        .then(argument("module", StringArgumentType.word())
                                .suggests(ModuleCommands::suggestModules)
                                .then(argument(
                                                "key",
                                                IntegerArgumentType.integer(1, SDLScancode.SDL_SCANCODE_COUNT - 1))
                                        .executes(context -> bindModule(context, ModuleBindAction.TOGGLE))
                                        .then(argument("action", StringArgumentType.word())
                                                .suggests(ModuleCommands::suggestBindActions)
                                                .executes(ModuleCommands::bindModule)))))
                .then(literal("unbind")
                        .then(argument("module", StringArgumentType.word())
                                .suggests(ModuleCommands::suggestModules)
                                .executes(ModuleCommands::unbindModule)))
                .then(literal("setting")
                        .then(argument("module", StringArgumentType.word())
                                .suggests(ModuleCommands::suggestModules)
                                .then(argument("setting", StringArgumentType.word())
                                        .suggests(ModuleCommands::suggestSettings)
                                        .executes(ModuleCommands::showSetting)
                                        .then(argument("value", StringArgumentType.greedyString())
                                                .suggests(ModuleCommands::suggestSettingValues)
                                                .executes(ModuleCommands::setSetting)))));
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

    private static int showPrefix(final CommandContext<FabricClientCommandSource> context) {
        context.getSource()
                .sendFeedback(Translations.component(
                        "anarchyclient.message.module.command_prefix_is",
                        "Command prefix is \"%s\".",
                        CommandPrefix.get()));
        return SUCCESS;
    }

    private static int setPrefix(final CommandContext<FabricClientCommandSource> context) {
        String prefix = StringArgumentType.getString(context, "prefix").trim();
        if (!CommandPrefix.isAllowed(prefix)) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.module.prefix_must_be_a_symbol",
                            "The prefix must be a single symbol, not a letter, digit, or /."));
            return 0;
        }
        CommandPrefix.set(prefix);
        AnarchyClient.CONFIG.save();
        context.getSource()
                .sendFeedback(Translations.component(
                        "anarchyclient.message.module.command_prefix_set_to",
                        "Command prefix set to \"%s\".",
                        CommandPrefix.get()));
        return SUCCESS;
    }

    private static int toggleModule(final CommandContext<FabricClientCommandSource> context) {
        Module module = findModule(context);
        if (module == null) {
            return 0;
        }
        module.toggle();
        AnarchyClient.CONFIG.save();
        context.getSource()
                .sendFeedback(
                        (module.enabled()
                                ? Translations.component(
                                        "anarchyclient.message.module.is_now_on", "%s is now on.", module.name())
                                : Translations.component(
                                        "anarchyclient.message.module.is_now_off", "%s is now off.", module.name())));
        return SUCCESS;
    }

    private static int panic(final CommandContext<FabricClientCommandSource> context) {
        long disabled = AnarchyClient.MODULES.all().stream()
                .filter(Module::enabled)
                .peek(module -> module.enabled(false))
                .count();
        AnarchyClient.CONFIG.save();
        context.getSource()
                .sendFeedback(Translations.component(
                        "anarchyclient.message.module.disabled_modules", "Modules disabled: %s.", disabled));
        return SUCCESS;
    }

    private static int notebotStatus(final CommandContext<FabricClientCommandSource> context) {
        context.getSource()
                .sendFeedback(
                        Translations.component(
                                "anarchyclient.message.module.notebot_scans_nearby_note_blocks_when",
                                "Notebot scans nearby note blocks when the module is enabled. /ac notebot play <note> is a local pitch preview."));
        return SUCCESS;
    }

    private static int notebotPlayNote(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        LocalPlayer player = client == null ? null : client.player;
        if (client == null || player == null) {
            return 0;
        }
        int note = IntegerArgumentType.getInteger(context, "note");
        player.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 2.0F, notePitch(note));
        context.getSource()
                .sendFeedback(
                        Translations.component("anarchyclient.message.module.played_note", "Played note %s.", note));
        return SUCCESS;
    }

    private static int swarmStatus(final CommandContext<FabricClientCommandSource> context) {
        SwarmModule swarm = swarmModule();
        if (swarm == null) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.module.swarm_module_is_not_registered",
                            "Swarm module is not registered."));
            return 0;
        }
        context.getSource()
                .sendFeedback(Translations.component("anarchyclient.message.module.swarm", "Swarm %s", swarm.status()));
        return SUCCESS;
    }

    private static int swarmSend(final CommandContext<FabricClientCommandSource> context) {
        SwarmModule swarm = swarmModule();
        if (swarm == null) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.module.swarm_module_is_not_registered",
                            "Swarm module is not registered."));
            return 0;
        }
        int sent = swarm.broadcast(StringArgumentType.getString(context, "message"));
        context.getSource()
                .sendFeedback(Translations.component(
                        "anarchyclient.message.module.sent_swarm_action_to_worker_s",
                        "Sent swarm action to %s worker(s).",
                        sent));
        return sent > 0 ? SUCCESS : 0;
    }

    private static @Nullable SwarmModule swarmModule() {
        Module module = AnarchyClient.MODULES.find("swarm").orElse(null);
        return module instanceof SwarmModule swarm ? swarm : null;
    }

    private static float notePitch(final int note) {
        return (float) Math.pow(2.0, (note - 12) / 12.0);
    }

    private static int bindModule(final CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "action");
        Optional<ModuleBindAction> action = ModuleBindAction.tryParse(name);
        if (action.isEmpty()) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.module.unknown_bind_action_use_toggle_hold",
                            "Unknown bind action '%s'. Use toggle, hold, or smart.",
                            name));
            return 0;
        }
        return bindModule(context, action.get());
    }

    private static int bindModule(
            final CommandContext<FabricClientCommandSource> context, final ModuleBindAction action) {
        Module module = findModule(context);
        if (module == null) {
            return 0;
        }
        int key = IntegerArgumentType.getInteger(context, "key");
        module.keybind().key(key);
        module.keybind().action(action);
        AnarchyClient.CONFIG.save();
        // Literal keys, one per action, so the language files can list them.
        String actionLabel = switch (action) {
            case TOGGLE -> Translations.text("anarchyclient.ui.bind_action.toggle", "toggle");
            case HOLD -> Translations.text("anarchyclient.ui.bind_action.hold", "hold");
            case SMART -> Translations.text("anarchyclient.ui.bind_action.smart", "smart");
        };
        context.getSource()
                .sendFeedback(Translations.component(
                        "anarchyclient.message.module.bound_to_key_as",
                        "Bound %s to key %s as %s.",
                        module.name(),
                        key,
                        actionLabel));
        return SUCCESS;
    }

    private static int unbindModule(final CommandContext<FabricClientCommandSource> context) {
        Module module = findModule(context);
        if (module == null) {
            return 0;
        }
        module.keybind().key(InputConstants.UNKNOWN.getValue());
        AnarchyClient.CONFIG.save();
        context.getSource()
                .sendFeedback(
                        Translations.component("anarchyclient.message.module.unbound", "Unbound %s.", module.name()));
        return SUCCESS;
    }

    private static int showSetting(final CommandContext<FabricClientCommandSource> context) {
        Module module = findModule(context);
        Setting<?> setting = module == null ? null : findSetting(context, module);
        if (module == null || setting == null) {
            return 0;
        }
        context.getSource()
                .sendFeedback(
                        Component.literal(module.name() + " " + setting.name() + " = " + settingValueString(setting)));
        return SUCCESS;
    }

    private static int setSetting(final CommandContext<FabricClientCommandSource> context) {
        Module module = findModule(context);
        Setting<?> setting = module == null ? null : findSetting(context, module);
        if (module == null || setting == null) {
            return 0;
        }
        String value = StringArgumentType.getString(context, "value");
        try {
            setSettingValue(setting, value);
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(CommandFeedback.message(exception)));
            return 0;
        }
        AnarchyClient.CONFIG.save();
        context.getSource()
                .sendFeedback(
                        Component.literal(module.name() + " " + setting.name() + " = " + settingValueString(setting)));
        return SUCCESS;
    }

    static void setSettingValue(final Setting<?> setting, final String value) {
        if (setting instanceof BooleanSetting bool) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                throw new IllegalArgumentException("Expected true or false.");
            }
            bool.value(Boolean.valueOf(value));
            return;
        }
        if (setting instanceof NumberSetting number) {
            double parsed;
            try {
                parsed = Double.parseDouble(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Expected a number.", exception);
            }
            if (!Double.isFinite(parsed)) {
                throw new IllegalArgumentException("Expected a finite number.");
            }
            number.value(parsed);
            return;
        }
        if (setting instanceof SelectSetting select) {
            if (!select.options().contains(value)) {
                throw new IllegalArgumentException("Expected one of: " + String.join(", ", select.options()));
            }
            select.value(value);
            return;
        }
        if (setting instanceof TextValueSetting text) {
            text.valueFromString(value);
            return;
        }
        throw new IllegalArgumentException("This setting cannot be changed from commands.");
    }

    private static String settingValueString(final Setting<?> setting) {
        if (setting instanceof TextValueSetting text) {
            return text.valueString();
        }
        return String.valueOf(setting.value());
    }

    private static @Nullable Module findModule(final CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "module");
        Module module = AnarchyClient.MODULES.find(id).orElse(null);
        if (module == null) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.module.unknown_module", "Unknown module: %s", id));
        }
        return module;
    }

    private static @Nullable Setting<?> findSetting(
            final CommandContext<FabricClientCommandSource> context, final Module module) {
        String id = StringArgumentType.getString(context, "setting");
        for (Setting<?> setting : module.settings()) {
            if (setting.id().equals(id) || setting.aliases().contains(id)) {
                return setting;
            }
        }
        context.getSource()
                .sendError(Translations.component(
                        "anarchyclient.message.module.unknown_setting", "Unknown setting: %s", id));
        return null;
    }

    private static CompletableFuture<Suggestions> suggestModules(
            final CommandContext<FabricClientCommandSource> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                AnarchyClient.MODULES.all().stream().map(Module::id), builder);
    }

    private static CompletableFuture<Suggestions> suggestSettings(
            final CommandContext<FabricClientCommandSource> context, final SuggestionsBuilder builder) {
        Module module = AnarchyClient.MODULES
                .find(StringArgumentType.getString(context, "module"))
                .orElse(null);
        if (module == null) {
            return builder.buildFuture();
        }
        return SharedSuggestionProvider.suggest(module.settings().stream().map(Setting::id), builder);
    }

    private static CompletableFuture<Suggestions> suggestBindActions(
            final CommandContext<FabricClientCommandSource> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                Arrays.stream(ModuleBindAction.values())
                        .map(action -> action.name().toLowerCase(Locale.ROOT)),
                builder);
    }

    private static CompletableFuture<Suggestions> suggestSettingValues(
            final CommandContext<FabricClientCommandSource> context, final SuggestionsBuilder builder) {
        Module module = AnarchyClient.MODULES
                .find(StringArgumentType.getString(context, "module"))
                .orElse(null);
        String typed = StringArgumentType.getString(context, "setting");
        // Match aliases too, like the set command itself does.
        Setting<?> setting = module == null
                ? null
                : module.settings().stream()
                        .filter(candidate -> candidate.id().equals(typed)
                                || candidate.aliases().contains(typed))
                        .findFirst()
                        .orElse(null);
        if (setting instanceof BooleanSetting) {
            return SharedSuggestionProvider.suggest(new String[] {"true", "false"}, builder);
        }
        if (setting instanceof SelectSetting select) {
            return SharedSuggestionProvider.suggest(select.options(), builder);
        }
        if (setting instanceof TextValueSetting text) {
            return SharedSuggestionProvider.suggest(text.suggestions(), builder);
        }
        return builder.buildFuture();
    }
}
