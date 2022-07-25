package net.minestom.server.command;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.arguments.*;
import net.minestom.server.command.builder.arguments.number.ArgumentDouble;
import net.minestom.server.command.builder.arguments.number.ArgumentFloat;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.arguments.number.ArgumentLong;
import net.minestom.server.command.builder.suggestion.SuggestionCallback;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

record ArgImpl<T>(String id, Parser<T> parser, Suggestion.Type suggestionType) implements Arg<T> {
    static <T> ArgImpl<T> fromLegacy(Argument<T> argument) {
        return new ArgImpl<>(argument.getId(), retrieveParser(argument), retrieveSuggestion(argument));
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
            case ASK_SERVER -> Suggestion.Type.askServer((sender, input) -> {
                final SuggestionCallback suggestionCallback = argument.getSuggestionCallback();
                assert suggestionCallback != null;
                final var sug = new net.minestom.server.command.builder.suggestion.Suggestion(input, 0, 0);
                suggestionCallback.apply(sender, null, sug);

                return new SuggestionEntryImpl(sug.getStart(), sug.getLength(),
                        sug.getEntries().stream().map(entry -> (Suggestion.Entry.Match) new MatchImpl(entry.getEntry(), entry.getTooltip())).toList());
            });
        };
    }

    record SuggestionTypeImpl(String name, Suggestion.Callback callback) implements Suggestion.Type {
        static final Suggestion.Type RECIPES = new SuggestionTypeImpl("minecraft:all_recipes", null);
        static final Suggestion.Type SOUNDS = new SuggestionTypeImpl("minecraft:available_sounds", null);
        static final Suggestion.Type ENTITIES = new SuggestionTypeImpl("minecraft:summonable_entities", null);

        static Suggestion.Type askServer(Suggestion.Callback callback) {
            return new SuggestionTypeImpl("minecraft:ask_server", callback);
        }

        @NotNull
        @Override
        public Suggestion.Entry suggest(@NotNull CommandSender sender, @NotNull String input) {
            final Suggestion.Callback callback = this.callback;
            if (callback == null) {
                throw new IllegalStateException("Suggestion type is not supported");
            }
            return callback.apply(sender, input);
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
