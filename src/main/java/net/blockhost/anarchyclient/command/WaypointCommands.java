package net.blockhost.anarchyclient.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.waypoint.Waypoint;
import net.blockhost.anarchyclient.waypoint.WaypointStore;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.blockhost.anarchyclient.command.CommandSupport.SUCCESS;
import static net.blockhost.anarchyclient.command.CommandSupport.requireInGame;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Registers and handles the {@code waypoint} subcommand.
 */
final class WaypointCommands {

    private WaypointCommands() {
    }

    static void register(final LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root
                .then(literal("waypoint")
                        .then(literal("list")
                                .executes(WaypointCommands::listWaypoints))
                        .then(literal("add")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(WaypointCommands::addWaypoint)))
                        .then(literal("remove")
                                .then(argument("name", StringArgumentType.word())
                                        .suggests(WaypointCommands::suggestWaypoints)
                                        .executes(WaypointCommands::removeWaypoint))));
    }

    private static int listWaypoints(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        String world = WaypointStore.currentWorld(client);
        List<Waypoint> waypoints = AnarchyClient.WAYPOINTS.byWorld(world);
        if (waypoints.isEmpty()) {
            context.getSource().sendFeedback(Translations.component("anarchyclient.message.waypoint.no_waypoints_for", "No waypoints for %s.", world));
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
        Minecraft client = requireInGame(context);
        if (client == null) {
            return 0;
        }
        String name = StringArgumentType.getString(context, "name");
        String world = WaypointStore.currentWorld(client);
        BlockPos pos = client.player.blockPosition();
        AnarchyClient.WAYPOINTS.add(new Waypoint(world, name, pos, WaypointStore.DEFAULT_COLOR));
        context.getSource().sendFeedback(Translations.component("anarchyclient.message.waypoint.saved_waypoint_at", "Saved waypoint %s at %s, %s, %s.", name, pos.getX(), pos.getY(), pos.getZ()));
        return SUCCESS;
    }

    private static int removeWaypoint(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        String name = StringArgumentType.getString(context, "name");
        String world = WaypointStore.currentWorld(client);
        if (AnarchyClient.WAYPOINTS.remove(world, name)) {
            context.getSource().sendFeedback(Translations.component("anarchyclient.message.waypoint.removed_waypoint", "Removed waypoint %s.", name));
        } else {
            context.getSource().sendFeedback(Translations.component("anarchyclient.message.waypoint.no_waypoint_named_for_this_world", "No waypoint named %s for this world.", name));
        }
        return SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestWaypoints(final CommandContext<FabricClientCommandSource> context,
                                                                   final SuggestionsBuilder builder) {
        String world = WaypointStore.currentWorld(Minecraft.getInstance());
        return SharedSuggestionProvider.suggest(AnarchyClient.WAYPOINTS.byWorld(world).stream()
                .map(Waypoint::name), builder);
    }
}
