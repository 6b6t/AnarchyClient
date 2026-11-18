package net.blockhost.anarchyclient.command;

import net.blockhost.anarchyclient.i18n.Translations;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

final class CommandFeedback {

    private CommandFeedback() {
    }

    static MutableComponent copyable(final String label, final String value) {
        return Component.literal(label)
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withClickEvent(new ClickEvent.CopyToClipboard(value))
                        .withHoverEvent(new HoverEvent.ShowText(Translations.component("anarchyclient.message.command_feedback.copy", "Copy %s", value))));
    }

    static MutableComponent command(final String label, final String command) {
        return Component.literal(label)
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.SuggestCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(Translations.component("anarchyclient.message.command_feedback.suggest", "Suggest %s", command))));
    }
}
