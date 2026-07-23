package net.blockhost.anarchyclient.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.blockhost.anarchyclient.command.ClientCommands;
import net.blockhost.anarchyclient.command.CommandPrefix;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Reuses the vanilla chat suggestion popup and tab-fill for the client's own prefix commands, without any
 * {@code /} involvement:
 *
 * <ul>
 *   <li>The {@code command} rewrite makes {@code updateCommandInfo} enter its command branch for a line
 *       starting with the single-char {@link CommandPrefix} (it lines up 1:1 with {@code /}, so cursor
 *       positions, accepted completions and ghost text all stay correct on the real input).</li>
 *   <li>The {@code getCommands} redirect swaps in the {@link ClientCommands} mirror of the {@code /ac}
 *       tree, so ONLY the client's own commands are suggested — vanilla and server {@code /} commands
 *       never appear in the prefix list.</li>
 * </ul>
 */
@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {

    @Shadow
    @Final
    EditBox input;

    @ModifyVariable(method = "updateCommandInfo", at = @At("STORE"), ordinal = 0)
    private String anarchyclient$prefixAsSlash(final String command) {
        char prefix = CommandPrefix.first();
        if (prefix != '/' && !command.isEmpty() && command.charAt(0) == prefix) {
            return "/" + command.substring(1);
        }
        return command;
    }

    @Redirect(method = "updateCommandInfo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;getCommands()Lcom/mojang/brigadier/CommandDispatcher;"))
    private CommandDispatcher<ClientSuggestionProvider> anarchyclient$prefixCommands(final ClientPacketListener connection) {
        char prefix = CommandPrefix.first();
        if (prefix != '/' && this.input.getValue().startsWith(CommandPrefix.get())) {
            return ClientCommands.dispatcher();
        }
        return connection.getCommands();
    }
}
