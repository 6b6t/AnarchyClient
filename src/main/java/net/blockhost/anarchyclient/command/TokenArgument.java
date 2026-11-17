package net.blockhost.anarchyclient.command;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import java.util.Collection;
import java.util.List;

/**
 * Reads one space-free token, such as {@code minecraft:stone} or {@code play.example.net:25566}.
 *
 * <p>{@code StringArgumentType.word()} only accepts letters, digits, and {@code _-.+}, so it rejects
 * colons and other punctuation.</p>
 */
final class TokenArgument implements ArgumentType<String> {

    private static final SimpleCommandExceptionType EXPECTED_TOKEN =
            new SimpleCommandExceptionType(new LiteralMessage("Expected a value"));
    private static final Collection<String> EXAMPLES = List.of("minecraft:stone", "example.net:25565");

    private TokenArgument() {
    }

    static TokenArgument token() {
        return new TokenArgument();
    }

    static String getToken(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    static String readToken(final StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        if (reader.getCursor() == start) {
            throw EXPECTED_TOKEN.createWithContext(reader);
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        return readToken(reader);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
