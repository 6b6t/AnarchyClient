package net.blockhost.anarchyclient.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.blockhost.anarchyclient.AnarchyClient;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * Makes the chat {@link CommandPrefix} (default {@code .}) expose the exact same commands as {@code
 * /anarchyclient} (aka {@code /ac}), just top-level: {@code .vclip} == {@code /ac vclip}, {@code .toggle x}
 * == {@code /ac toggle x}, and so on.
 *
 * <p>Rather than duplicating the (large) command tree, this mirrors Fabric's live client command
 * dispatcher (its {@code getActiveDispatcher()} — the one that actually holds the executable nodes;
 * {@code connection.getCommands()} only keeps suggestion-only copies) by reparenting the
 * children of the {@code anarchyclient} node under a throwaway dispatcher root. The vanilla suggestion
 * popup is pointed at that mirror (see {@code CommandSuggestionsMixin}) so only these commands autofill,
 * and {@link #execute} runs the same nodes with the real client command source — never touching {@code /}
 * or server commands.</p>
 */
public final class ClientCommands {

    private static final String ROOT = "anarchyclient";

    private static CommandDispatcher<FabricClientCommandSource> cachedActive;
    private static CommandDispatcher<ClientSuggestionProvider> cachedMirror;

    private ClientCommands() {
    }

    /** A dispatcher whose root children are the {@code /ac} subcommands, for prefix suggestions + execution. */
    @SuppressWarnings("unchecked")
    public static CommandDispatcher<ClientSuggestionProvider> dispatcher() {
        // Fabric's own API class is also named ClientCommands, hence the fully-qualified call.
        CommandDispatcher<FabricClientCommandSource> active =
                net.fabricmc.fabric.api.client.command.v2.ClientCommands.getActiveDispatcher();
        if (active == cachedActive && cachedMirror != null) {
            return cachedMirror;
        }
        CommandDispatcher<ClientSuggestionProvider> mirror = new CommandDispatcher<>();
        if (active != null) {
            CommandNode<FabricClientCommandSource> root = active.getRoot().getChild(ROOT);
            if (root != null) {
                for (CommandNode<FabricClientCommandSource> child : root.getChildren()) {
                    // The nodes are generic over the command source; at runtime the source object
                    // (getSuggestionsProvider()) implements both FabricClientCommandSource and
                    // ClientSuggestionProvider, so reparenting across the erased type is safe.
                    mirror.getRoot().addChild((CommandNode<ClientSuggestionProvider>) (CommandNode<?>) child);
                }
            }
        }
        cachedActive = active;
        cachedMirror = mirror;
        return mirror;
    }

    /** Run a prefix command (prefix already stripped), e.g. {@code "toggle killaura"}. */
    public static void execute(final ClientPacketListener connection, final String input) {
        if (connection == null || input.isBlank()) {
            return;
        }
        try {
            dispatcher().execute(input, connection.getSuggestionsProvider());
        } catch (CommandSyntaxException exception) {
            error(exception.getMessage());
        } catch (Exception exception) {
            // A command threw at runtime: report it instead of crashing the key handler / chat screen.
            AnarchyClient.LOGGER.error("Error running client command '{}'", input, exception);
            error("Error running command: " + exception.getMessage());
        }
    }

    private static void error(final String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
        }
    }
}
