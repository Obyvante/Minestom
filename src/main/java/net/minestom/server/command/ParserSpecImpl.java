package net.minestom.server.command;

import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.minestom.server.command.ParserSpec.Result.*;

final class ParserSpecImpl {

    /**
     * Reads from a trusted type, and compare to the constant.
     * <p>
     * Reading can be optimized using a raw string comparison to avoid parsing altogether.
     */
    record Constant1<T>(Type<T> type, T constant) implements ParserSpec<T> {
        @Override
        public @NotNull Result<T> read(@NotNull String input, int startIndex) {
            return type.equals(input, startIndex, constant);
        }

        @Override
        public @Nullable T readExact(@NotNull String input) {
            return type.equalsExact(input, constant);
        }
    }

    /**
     * Reads from a trusted type, and compare to a set of constants.
     * <p>
     * Reading can be optimized using map lookups.
     *
     * @see Constant1 for raw string comparison, also relevant here
     */
    record ConstantN<T>(Type<T> type, Set<T> constants) implements ParserSpec<T> {
        ConstantN {
            constants = Set.copyOf(constants);
        }

        @Override
        public @NotNull Result<T> read(@NotNull String input, int startIndex) {
            return type.find(input, startIndex, constants);
        }

        @Override
        public @Nullable T readExact(@NotNull String input) {
            return type.findExact(input, constants);
        }
    }

    /**
     * Reads from arbitrary code.
     * <p>
     * Cannot be optimized at all, but more flexible.
     */
    record Reader<T>(BiFunction<String, Integer, Result<T>> reader) implements ParserSpec<T> {
        @Override
        public @NotNull Result<T> read(@NotNull String input, int startIndex) {
            return reader.apply(input, startIndex);
        }
    }

    /**
     * Reuses an existing spec but with an additional filter.
     * <p>
     * The filter means that the parsec input has to pass through the arbitrary function, limiting potential optimizations.
     */
    record Specialized<T>(ParserSpec<T> spec, Function<Success<T>, Result<T>> filter) implements ParserSpec<T> {
        @Override
        public @NotNull Result<T> read(@NotNull String input, int startIndex) {
            final Result<T> result = spec.read(input);
            if (!(result instanceof Result.Success<T> success)) return result;
            return filter.apply(success);
        }
    }

    record Legacy<T>(Argument<T> argument) implements ParserSpec<T> {
        @Override
        public @NotNull Result<T> read(@NotNull String input, int startIndex) {
            final String sub = input.substring(startIndex);
            final String[] split = sub.split(" ");
            // Handle specific type without loop
            try {
                // Single word argument
                if (!argument.allowSpace()) {
                    final String word = split[0];
                    final int index = startIndex + word.length();
                    final T value = argument.parse(word);
                    return success(input, index, value);
                }
                // Complete input argument
                if (argument.useRemaining()) {
                    final T value = argument.parse(sub);
                    return success(input, input.length(), value);
                }
            } catch (ArgumentSyntaxException exception) {
                return error(exception.getInput(), exception.getMessage(), exception.getErrorCode());
            }
            // Bruteforce
            assert argument.allowSpace() && !argument.useRemaining();
            StringBuilder current = new StringBuilder();

            ArgumentSyntaxException lastException = null;
            for (String word : split) {
                if (!current.isEmpty()) current.append(' ');
                current.append(word);
                try {
                    final String result = current.toString();
                    final T value = argument.parse(result);
                    final int index = result.length() + startIndex;
                    return success(result, index, value);
                } catch (ArgumentSyntaxException exception) {
                    lastException = exception;
                }
            }
            if (lastException != null) {
                return error(lastException.getInput(), lastException.getMessage(), lastException.getErrorCode());
            }
            return incompatible();
        }
    }
}
