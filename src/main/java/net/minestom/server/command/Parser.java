package net.minestom.server.command;

import net.minestom.server.command.builder.arguments.Argument;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

sealed interface Parser<T> {
    static @NotNull LiteralParser Literal(@NotNull String literal) {
        return ParserImpl.LITERAL.literal(literal);
    }

    static @NotNull LiteralsParser Literals(@NotNull Set<String> literals) {
        return ParserImpl.LITERALS.literals(literals);
    }

    static @NotNull LiteralsParser Literals(@NotNull String @NotNull ... literals) {
        return ParserImpl.LITERALS.literals(literals);
    }

    static @NotNull BooleanParser Boolean() {
        return ParserImpl.BOOLEAN;
    }

    static @NotNull FloatParser Float() {
        return ParserImpl.FLOAT;
    }

    static @NotNull DoubleParser Double() {
        return ParserImpl.DOUBLE;
    }

    static @NotNull IntegerParser Integer() {
        return ParserImpl.INTEGER;
    }

    static @NotNull LongParser Long() {
        return ParserImpl.LONG;
    }

    static @NotNull StringParser String() {
        return ParserImpl.STRING;
    }

    static <T> @NotNull Custom<T> custom(@NotNull ParserSpec<T> spec) {
        return new ParserImpl.CustomImpl<>(spec);
    }

    @ApiStatus.Internal
    static <T> @NotNull Custom<T> legacy(@NotNull Argument<T> argument) {
        return custom(ParserSpec.legacy(argument));
    }

    @NotNull ParserSpec<T> spec();

    sealed interface LiteralParser extends Parser<String>
            permits ParserImpl.LiteralParserImpl {
        @NotNull String literal();

        @NotNull LiteralParser literal(@NotNull String literal);
    }

    sealed interface LiteralsParser extends Parser<String>
            permits ParserImpl.LiteralsParserImpl {
        @Unmodifiable
        @NotNull Set<String> literals();

        @NotNull LiteralsParser literals(@NotNull Set<String> literals);

        default @NotNull LiteralsParser literals(@NotNull String @NotNull ... literals) {
            return literals(Set.of(literals));
        }
    }

    sealed interface BooleanParser extends Parser<Boolean>
            permits ParserImpl.BooleanParserImpl {
    }

    sealed interface FloatParser extends Parser<Float>
            permits ParserImpl.FloatParserImpl {
        @Nullable Float max();

        @Nullable Float min();

        @NotNull FloatParser max(@Nullable Float max);

        @NotNull FloatParser min(@Nullable Float min);
    }

    sealed interface DoubleParser extends Parser<Double>
            permits ParserImpl.DoubleParserImpl {
        @Nullable Double max();

        @Nullable Double min();

        @NotNull DoubleParser max(@Nullable Double max);

        @NotNull DoubleParser min(@Nullable Double min);
    }

    sealed interface IntegerParser extends Parser<Integer>
            permits ParserImpl.IntegerParserImpl {
        @Nullable Integer max();

        @Nullable Integer min();

        @NotNull IntegerParser max(@Nullable Integer max);

        @NotNull IntegerParser min(@Nullable Integer min);
    }

    sealed interface LongParser extends Parser<Long>
            permits ParserImpl.LongParserImpl {
        @Nullable Long max();

        @Nullable Long min();

        @NotNull LongParser max(@Nullable Long max);

        @NotNull LongParser min(@Nullable Long min);
    }

    sealed interface StringParser extends Parser<String>
            permits ParserImpl.StringParserImpl {
        @NotNull Type type();

        @NotNull StringParser type(@NotNull Type type);

        enum Type {
            WORD,
            QUOTED,
            GREEDY
        }
    }

    sealed interface Custom<T> extends Parser<T>
            permits ParserImpl.CustomImpl {
    }
}
