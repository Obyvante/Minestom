package net.minestom.server.command;

import net.minestom.server.command.builder.arguments.Argument;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    static <T> @NotNull ParserSpec<T> specialized(@NotNull ParserSpec<T> spec,
                                                  @NotNull Function<Result.@NotNull Success<T>, @NotNull Result<T>> filter) {
        return new ParserSpecImpl.Specialized<>(spec, filter);
    }

    @ApiStatus.Internal
    static <T> @NotNull ParserSpec<T> legacy(@NotNull Argument<T> argument) {
        return new ParserSpecImpl.Legacy<>(argument);
    }

    @NotNull Result<T> read(@NotNull String input, int startIndex);

    default @NotNull Result<T> read(@NotNull String input) {
        return read(input, 0);
    }

    default @Nullable T readExact(@NotNull String input) {
        final Result<T> result = read(input);
        return result instanceof Result.Success<T> success && success.index() == input.length() ?
                success.value() : null;
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

        @NotNull ParserSpec.Result<T> equals(@NotNull String input, int startIndex, @NotNull T constant);

        @NotNull ParserSpec.Result<T> find(@NotNull String input, int startIndex, @NotNull Set<@NotNull T> constants);

        @Nullable T equalsExact(@NotNull String input, @NotNull T constant);

        @Nullable T findExact(@NotNull String input, @NotNull Set<@NotNull T> constants);
    }


    sealed interface Result<T> {
        static <T> Result.@NotNull Success<T> success(@NotNull String input, int index, @NotNull T value) {
            return new ParserSpecTypes.ResultSuccessImpl<>(input, index, value);
        }

        static <T> Result.@NotNull SyntaxError<T> error(@NotNull String input, @NotNull String message, int error) {
            return new ParserSpecTypes.ResultErrorImpl<>(input, message, error);
        }

        static <T> Result.@NotNull IncompatibleType<T> incompatible() {
            return new ParserSpecTypes.ResultIncompatibleImpl<>();
        }

        sealed interface Success<T> extends Result<T>
                permits ParserSpecTypes.ResultSuccessImpl {

            @NotNull String input();

            /**
             * Indicates how much data was read from the input
             *
             * @return the index of the next unread character
             */
            int index();

            @NotNull T value();
        }

        sealed interface IncompatibleType<T> extends Result<T>
                permits ParserSpecTypes.ResultIncompatibleImpl {
        }

        sealed interface SyntaxError<T> extends Result<T>
                permits ParserSpecTypes.ResultErrorImpl {

            @NotNull String input();

            @NotNull String message();

            int error();
        }
    }
}
