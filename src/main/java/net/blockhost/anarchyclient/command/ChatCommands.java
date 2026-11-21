package net.blockhost.anarchyclient.command;

import static net.blockhost.anarchyclient.command.CommandSupport.SUCCESS;
import static net.blockhost.anarchyclient.command.CommandSupport.requireInGame;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.impl.ChatActions;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

/**
 * Registers and handles the {@code clear-chat} and {@code macro} subcommands.
 */
final class ChatCommands {

    private ChatCommands() {}

    static void register(final LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root.then(literal("clear-chat").executes(ChatCommands::clearChat))
                .then(literal("macro")
                        .then(literal("run")
                                .then(argument("message", StringArgumentType.greedyString())
                                        .executes(ChatCommands::runMacro))));
    }

    private static int clearChat(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = Minecraft.getInstance();
        client.gui.hud.getChat().clearMessages(false);
        context.getSource()
                .sendFeedback(Translations.component("anarchyclient.message.chat.cleared_chat", "Cleared chat."));
        return SUCCESS;
    }

    private static int runMacro(final CommandContext<FabricClientCommandSource> context) {
        Minecraft client = requireInGame(context);
        if (client == null || client.getConnection() == null) {
            return 0;
        }
        ChatActions.run(client, StringArgumentType.getString(context, "message"));
        return SUCCESS;
    }
}
