package net.blockhost.anarchyclient.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.profile.ProfileManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.blockhost.anarchyclient.command.CommandSupport.SUCCESS;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Registers and handles the {@code profile} subcommand.
 */
final class ProfileCommands {

    private ProfileCommands() {
    }

    static void register(final LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root
                .then(literal("profile")
                        .then(literal("list")
                                .executes(ProfileCommands::listProfiles))
                        .then(literal("save")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(ProfileCommands::saveProfile)))
                        .then(literal("load")
                                .then(argument("name", StringArgumentType.word())
                                        .suggests(ProfileCommands::suggestProfiles)
                                        .executes(ProfileCommands::loadProfile)))
                        .then(literal("delete")
                                .then(argument("name", StringArgumentType.word())
                                        .suggests(ProfileCommands::suggestProfiles)
                                        .executes(ProfileCommands::deleteProfile)))
                        .then(literal("export")
                                .then(argument("name", StringArgumentType.word())
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
                context.getSource().sendError(Component.literal("Unknown profile: " + name));
                return 0;
            }
            context.getSource().sendFeedback(Component.literal("Exported profile " + name + " to " + output + "."));
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource().sendError(Component.literal("Failed to export profile: " + exception.getMessage()));
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
            context.getSource().sendFeedback(Component.literal("Imported profile " + profile.name + "."));
            return SUCCESS;
        } catch (IOException exception) {
            context.getSource().sendError(Component.literal("Failed to import profile: " + exception.getMessage()));
            return 0;
        }
    }

    private static CompletableFuture<Suggestions> suggestProfiles(final CommandContext<FabricClientCommandSource> context,
                                                                  final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(AnarchyClient.PROFILES.summaries().stream()
                .map(ProfileManager.ProfileSummary::name), builder);
    }
}
