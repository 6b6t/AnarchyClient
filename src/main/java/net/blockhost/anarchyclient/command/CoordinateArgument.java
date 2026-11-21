package net.blockhost.anarchyclient.command;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Reads one coordinate token such as {@code 12}, {@code -3.5}, {@code ~}, or {@code ~-2}.
 *
 * <p>{@code StringArgumentType.word()} rejects {@code ~}, so relative coordinates need their own reader.
 * The token is resolved later with {@link CommandSupport#parseCoordinate(String, double)}.</p>
 */
final class CoordinateArgument implements ArgumentType<String> {

    private static final SimpleCommandExceptionType EXPECTED_COORDINATE =
            new SimpleCommandExceptionType(new LiteralMessage("Expected a coordinate"));
    private static final Collection<String> EXAMPLES = List.of("0", "~", "~-2", "12.5");

    private CoordinateArgument() {}

    static CoordinateArgument coordinate() {
        return new CoordinateArgument();
    }

    static String getCoordinate(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        String token = TokenArgument.readToken(reader);
        try {
            CommandSupport.parseCoordinate(token, 0.0);
        } catch (IllegalArgumentException exception) {
            reader.setCursor(start);
            throw EXPECTED_COORDINATE.createWithContext(reader);
        }
        return token;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
            final CommandContext<S> context, final SuggestionsBuilder builder) {
        if (builder.getRemaining().isEmpty()) {
            builder.suggest("~");
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
