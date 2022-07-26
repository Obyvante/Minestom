package net.minestom.server.command;

import net.minestom.server.command.builder.arguments.Argument;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

sealed interface ParserSpec<T>
        permits ParserSpec.Type, ParserSpecImpl.Constant1, ParserSpecImpl.ConstantN,
        ParserSpecImpl.Legacy, ParserSpecImpl.Reader, ParserSpecImpl.Specialized {

    static <T> @NotNull ParserSpec<T> constant(@NotNull Type<T> type, @NotNull T constant) {
        return new ParserSpecImpl.Constant1<>(type, constant);
    }

    static <T> @NotNull ParserSpec<T> constants(@NotNull Type<T> type, @NotNull Set<@NotNull T> constants) {
        return new ParserSpecImpl.ConstantN<>(type, constants);
    }

    static <T> @NotNull ParserSpec<T> reader(@NotNull BiFunction<@NotNull String, @NotNull Integer, @Nullable Result<T>> reader) {
        return new ParserSpecImpl.Reader<>(reader);
    }

    static <T> @NotNull ParserSpec<T> specialized(@NotNull ParserSpec<T> spec, @NotNull Predicate<T> filter) {
        return new ParserSpecImpl.Specialized<>(spec, filter);
    }

    @ApiStatus.Internal
    static <T> @NotNull ParserSpec<T> legacy(@NotNull Argument<T> argument) {
        return new ParserSpecImpl.Legacy<>(argument);
    }

    @Nullable Result<T> read(@NotNull String input, int startIndex);

    default @Nullable Result<T> read(@NotNull String input) {
        return read(input, 0);
    }

    default @Nullable T readExact(@NotNull String input) {
        final Result<T> result = read(input);
        return result != null && result.index() == input.length() ?
                result.value() : null;
    }

    sealed interface Type<T> extends ParserSpec<T>
            permits ParserSpecTypes.TypeImpl {
        Type<Boolean> BOOLEAN = ParserSpecTypes.BOOLEAN;
        Type<Float> FLOAT = ParserSpecTypes.FLOAT;
        Type<Double> DOUBLE = ParserSpecTypes.DOUBLE;
        Type<Integer> INTEGER = ParserSpecTypes.INTEGER;
        Type<Long> LONG = ParserSpecTypes.LONG;

        Type<String> WORD = ParserSpecTypes.WORD;
        Type<String> QUOTED_PHRASE = ParserSpecTypes.QUOTED_PHRASE;
        Type<String> GREEDY_PHRASE = ParserSpecTypes.GREEDY_PHRASE;

        @Nullable ParserSpec.Result<T> equals(@NotNull String input, int startIndex, @NotNull T constant);

        @Nullable ParserSpec.Result<T> find(@NotNull String input, int startIndex, @NotNull Set<@NotNull T> constants);

        @Nullable T equalsExact(@NotNull String input, @NotNull T constant);

        @Nullable T findExact(@NotNull String input, @NotNull Set<@NotNull T> constants);
    }

    sealed interface Result<T>
            permits ParserSpecTypes.ResultImpl {
        static <T> @NotNull Result<T> of(@NotNull String input, int index, @NotNull T value) {
            return new ParserSpecTypes.ResultImpl<>(input, index, value);
        }

        @NotNull String input();

        /**
         * Indicates how much data was read from the input
         *
         * @return the index of the next unread character
         */
        int index();

        @NotNull T value();
    }
}
