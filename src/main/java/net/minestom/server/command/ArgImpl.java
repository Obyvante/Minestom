package net.minestom.server.command;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.ArgumentCallback;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.*;
import net.minestom.server.command.builder.arguments.number.ArgumentDouble;
import net.minestom.server.command.builder.arguments.number.ArgumentFloat;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.arguments.number.ArgumentLong;
import net.minestom.server.command.builder.suggestion.SuggestionCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

record ArgImpl<T>(String id, Parser<T> parser, Suggestion.Type suggestionType,
                  Supplier<T> defaultValue, ArgumentCallback callback) implements Arg<T> {
    static <T> ArgImpl<T> fromLegacy(Argument<T> argument) {
        return new ArgImpl<>(argument.getId(), retrieveParser(argument),
                retrieveSuggestion(argument), argument.getDefaultValue(), retrieveCallback(argument));
    }

    private static <T> Parser<T> retrieveParser(Argument<T> argument) {
        var parserFun = ConversionMap.PARSERS.get(argument.getClass());
        final Parser<T> parser;
        if (parserFun != null) {
            parser = parserFun.apply(argument);
        } else {
            // TODO remove legacy conversion
            parser = Parser.custom(ParserSpec.legacy(argument));
        }
        assert parser != null;
        return parser;
    }

    private static Suggestion.Type retrieveSuggestion(Argument<?> argument) {
        final var type = argument.suggestionType();
        if (type == null) return null;
        return switch (type) {
            case ALL_RECIPES -> Suggestion.Type.recipes();
            case AVAILABLE_SOUNDS -> Suggestion.Type.sounds();
            case SUMMONABLE_ENTITIES -> Suggestion.Type.entities();
            case ASK_SERVER -> Suggestion.Type.askServer((sender, context) -> {
                final SuggestionCallback suggestionCallback = argument.getSuggestionCallback();
                assert suggestionCallback != null;
                final String input = context.getInput();

                final int lastSpace = input.lastIndexOf(" ");

                final int start = lastSpace + 2;
                final int length = input.length() - lastSpace - 1;

                final var sug = new net.minestom.server.command.builder.suggestion.Suggestion(input, start, length);
                suggestionCallback.apply(sender, context, sug);

                return new SuggestionEntryImpl(sug.getStart(), sug.getLength(),
                        sug.getEntries().stream().map(entry -> (Suggestion.Entry.Match) new MatchImpl(entry.getEntry(), entry.getTooltip())).toList());
            });
        };
    }

    private static ArgumentCallback retrieveCallback(Argument<?> argument) {
        final ArgumentCallback callback = argument.getCallback();
        if (callback == null) return null;
        return (sender, context) -> {
            callback.apply(sender, context);
        };
    }

    @Override
    public @NotNull Arg<T> defaultValue(@Nullable Supplier<@NotNull T> defaultValue) {
        return new ArgImpl<>(id, parser, suggestionType, defaultValue, callback);
    }

    @Override
    public @NotNull Arg<T> callback(@Nullable ArgumentCallback callback) {
        return new ArgImpl<>(id, parser, suggestionType, defaultValue, callback);
    }

    record SuggestionTypeImpl(String name, Suggestion.Callback callback) implements Suggestion.Type {
        static final Suggestion.Type RECIPES = new SuggestionTypeImpl("minecraft:all_recipes", null);
        static final Suggestion.Type SOUNDS = new SuggestionTypeImpl("minecraft:available_sounds", null);
        static final Suggestion.Type ENTITIES = new SuggestionTypeImpl("minecraft:summonable_entities", null);

        static Suggestion.Type askServer(Suggestion.Callback callback) {
            return new SuggestionTypeImpl("minecraft:ask_server", callback);
        }

        @Override
        public @NotNull Suggestion.Entry suggest(@NotNull CommandSender sender, @NotNull CommandContext context) {
            final Suggestion.Callback callback = this.callback;
            if (callback == null) {
                throw new IllegalStateException("Suggestion type is not supported");
            }
            return callback.apply(sender, context);
        }
    }

    record SuggestionEntryImpl(int start, int length, List<Match> matches) implements Suggestion.Entry {
        SuggestionEntryImpl {
            matches = List.copyOf(matches);
        }
    }

    record MatchImpl(String text, Component tooltip) implements Suggestion.Entry.Match {
    }

    static final class ConversionMap {
        private static final Map<Class<? extends Argument>, Function<Argument, Parser>> PARSERS = new ConversionMap()
                .append(ArgumentLiteral.class, arg -> Parser.Literal(arg.getId()))
                .append(ArgumentBoolean.class, arg -> Parser.Boolean())
                .append(ArgumentFloat.class, arg -> Parser.Float().min(arg.getMin()).max(arg.getMax()))
                .append(ArgumentDouble.class, arg -> Parser.Double().min(arg.getMin()).max(arg.getMax()))
                .append(ArgumentInteger.class, arg -> Parser.Integer().min(arg.getMin()).max(arg.getMax()))
                .append(ArgumentLong.class, arg -> Parser.Long().min(arg.getMin()).max(arg.getMax()))
                .append(ArgumentWord.class, arg -> {
                    final String[] restrictions = arg.getRestrictions();
                    if (restrictions != null && restrictions.length > 0) {
                        return Parser.Literals(restrictions);
                    } else {
                        return Parser.String();
                    }
                })
                .append(ArgumentString.class, arg -> Parser.String().type(Parser.StringParser.Type.QUOTED))
                .append(ArgumentStringArray.class, arg -> Parser.String().type(Parser.StringParser.Type.GREEDY))
                .toMap();

        private final Map<Class<? extends Argument>, Function<Argument, Parser>> parsers = new HashMap<>();

        <T, A extends Argument<T>> ConversionMap append(Class<A> legacyType, Function<A, Parser<?>> converter) {
            this.parsers.put(legacyType, arg -> converter.apply((A) arg));
            return this;
        }

        Map<Class<? extends Argument>, Function<Argument, Parser>> toMap() {
            return Map.copyOf(parsers);
        }
    }
}
