package net.blockhost.anarchyclient.command;

import net.blockhost.anarchyclient.module.impl.ChatActions;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.blockhost.anarchyclient.command.CommandSupport.SUCCESS;
import static net.blockhost.anarchyclient.command.CommandSupport.requireInGame;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Registers and handles the {@code clear-chat} and {@code macro} subcommands.
 */
final class ChatCommands {

    private ChatCommands() {
    }

    static void register(final LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root
                .then(literal("clear-chat")
                        .executes(ChatCommands::clearChat))
                .then(literal("macro")
                        .then(literal("run")
                                .then(argument("message", StringArgumentType.greedyString())
                                        .executes(ChatCommands::runMacro))));
    }

    private static int clearChat(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        client.gui.hud.getChat().clearMessages(false);
        context.getSource().sendFeedback(Component.literal("Cleared chat."));
        return SUCCESS;
    }

    private static int runMacro(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context, "run a macro");
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        ChatActions.send(client, StringArgumentType.getString(context, "message"));
        return SUCCESS;
    }
}
