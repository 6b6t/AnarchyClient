package net.blockhost.anarchyclient.command;

import static net.blockhost.anarchyclient.command.CommandSupport.SUCCESS;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.profile.ProfileManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * Registers and handles the {@code profile} subcommand.
 */
final class ProfileCommands {

    private ProfileCommands() {}

    static void register(final LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root.then(literal("profile")
                .then(literal("list").executes(ProfileCommands::listProfiles))
                .then(literal("save")
                        .then(argument("name", StringArgumentType.string()).executes(ProfileCommands::saveProfile)))
                .then(literal("load")
                        .then(argument("name", StringArgumentType.string())
                                .suggests(ProfileCommands::suggestProfiles)
                                .executes(ProfileCommands::loadProfile)))
                .then(literal("delete")
                        .then(argument("name", StringArgumentType.string())
                                .suggests(ProfileCommands::suggestProfiles)
                                .executes(ProfileCommands::deleteProfile)))
                .then(literal("export")
                        .then(argument("name", StringArgumentType.string())
                                .suggests(ProfileCommands::suggestProfiles)
                                .then(argument("path", StringArgumentType.greedyString())
                                        .executes(ProfileCommands::exportProfile))))
                .then(literal("import")
                        .then(argument("path", StringArgumentType.greedyString())
                                .executes(ProfileCommands::importProfile))));
    }

    private static int listProfiles(final CommandContext<FabricClientCommandSource> context) {
        List<ProfileManager.ProfileSummary> profiles = AnarchyClient.PROFILES.summaries();
        if (profiles.isEmpty()) {
            context.getSource()
                    .sendFeedback(Translations.component(
                            "anarchyclient.message.profile.no_saved_profiles", "No saved profiles."));
            return SUCCESS;
        }
        for (ProfileManager.ProfileSummary profile : profiles) {
            String updated = profile.updatedAt().isBlank() ? "unknown" : profile.updatedAt();
            context.getSource()
                    .sendFeedback(Translations.component(
                            "anarchyclient.message.profile.modules_updated",
                            "%s | modules: %s | updated: %s",
                            profile.name(),
                            profile.modules(),
                            updated));
        }
        return SUCCESS;
    }

    private static int saveProfile(final CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        try {
            ProfileManager.Profile profile = AnarchyClient.PROFILES.capture(name, AnarchyClient.MODULES);
            context.getSource()
                    .sendFeedback(Translations.component(
                            "anarchyclient.message.profile.saved_profile", "Saved profile %s.", profile.name));
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
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.profile.unknown_profile", "Unknown profile: %s", name));
            return 0;
        }
        AnarchyClient.CONFIG.save();
        context.getSource()
                .sendFeedback(Translations.component(
                        "anarchyclient.message.profile.loaded_profile",
                        "Loaded profile %s. Values changed: %s.",
                        name,
                        changed));
        return SUCCESS;
    }

    private static int deleteProfile(final CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        if (AnarchyClient.PROFILES.delete(name)) {
            context.getSource()
                    .sendFeedback(Translations.component(
                            "anarchyclient.message.profile.deleted_profile", "Deleted profile %s.", name));
        } else {
            context.getSource()
                    .sendFeedback(Translations.component(
                            "anarchyclient.message.profile.no_saved_profile_named",
                            "No saved profile named %s.",
                            name));
        }
        return SUCCESS;
    }

    private static int exportProfile(final CommandContext<FabricClientCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");
        Path output;
        try {
            output = CommandSupport.resolveGamePath(StringArgumentType.getString(context, "path"));
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(exception.getMessage()));
            return 0;
        }
        try {
            if (!AnarchyClient.PROFILES.exportProfile(name, output)) {
                context.getSource()
                        .sendError(Translations.component(
                                "anarchyclient.message.profile.unknown_profile", "Unknown profile: %s", name));
                return 0;
            }
            context.getSource()
                    .sendFeedback(Translations.component(
                            "anarchyclient.message.profile.exported_profile_to",
                            "Exported profile %s to %s.",
                            name,
                            output));
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.profile.failed_to_export_profile",
                            "Failed to export profile: %s",
                            exception.getMessage()));
            return 0;
        }
    }

    private static int importProfile(final CommandContext<FabricClientCommandSource> context) {
        Path input;
        try {
            input = CommandSupport.resolveGamePath(StringArgumentType.getString(context, "path"));
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal(exception.getMessage()));
            return 0;
        }
        try {
            ProfileManager.Profile profile = AnarchyClient.PROFILES.importProfile(input);
            context.getSource()
                    .sendFeedback(Translations.component(
                            "anarchyclient.message.profile.imported_profile", "Imported profile %s.", profile.name));
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource()
                    .sendError(Translations.component(
                            "anarchyclient.message.profile.failed_to_import_profile",
                            "Failed to import profile: %s",
                            exception.getMessage()));
            return 0;
        }
    }

    private static CompletableFuture<Suggestions> suggestProfiles(
            final CommandContext<FabricClientCommandSource> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                AnarchyClient.PROFILES.summaries().stream()
                        .map(ProfileManager.ProfileSummary::name)
                        .map(StringArgumentType::escapeIfRequired),
                builder);
    }
}
