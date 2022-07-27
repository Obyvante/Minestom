package net.minestom.server.command;

import net.minestom.server.utils.StringReaderUtils;
import net.minestom.server.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

import static net.minestom.server.command.ParserSpec.Result.*;

final class ParserSpecTypes {
    static final ParserSpec.Type<Boolean> BOOLEAN = ParserSpecTypes.builder((input, startIndex) -> {
                final int index = input.indexOf(' ', startIndex);
                if (index == -1) {
                    // Whole input is a float
                    final String word = input.substring(startIndex);
                    final Boolean value = word.equals("true") ? Boolean.TRUE : word.equals("false") ? Boolean.FALSE : null;
                    if (value == null) return incompatible();
                    return success(word, input.length(), value);
                } else {
                    // Part of input is a float
                    final String word = input.substring(startIndex, index);
                    final Boolean value = word.equals("true") ? Boolean.TRUE : word.equals("false") ? Boolean.FALSE : null;
                    if (value == null) return incompatible();
                    return success(word, index, value);
                }
            })
            .build();
    static final ParserSpec.Type<Float> FLOAT = ParserSpecTypes.builder((input, startIndex) -> {
                final int index = input.indexOf(' ', startIndex);
                final String word = index == -1 ? input.substring(startIndex) : input.substring(startIndex, index);
                final int resultIndex = index == -1 ? input.length() : index;
                try {
                    final float value = Float.parseFloat(word);
                    return success(word, resultIndex, value);
                } catch (NumberFormatException e) {
                    return incompatible();
                }
            })
            .build();
    static final ParserSpec.Type<Double> DOUBLE = ParserSpecTypes.builder((input, startIndex) -> {
                final int index = input.indexOf(' ', startIndex);
                final String word = index == -1 ? input.substring(startIndex) : input.substring(startIndex, index);
                final int resultIndex = index == -1 ? input.length() : index;
                try {
                    final double value = Double.parseDouble(word);
                    return success(word, resultIndex, value);
                } catch (NumberFormatException e) {
                    return incompatible();
                }
            })
            .build();
    static final ParserSpec.Type<Integer> INTEGER = ParserSpecTypes.builder((input, startIndex) -> {
                final int index = input.indexOf(' ', startIndex);
                final String word = index == -1 ? input.substring(startIndex) : input.substring(startIndex, index);
                final int resultIndex = index == -1 ? input.length() : index;
                try {
                    final int value = Integer.parseInt(input, startIndex, resultIndex, 10);
                    return success(word, resultIndex, value);
                } catch (NumberFormatException e) {
                    return incompatible();
                }
            })
            .build();
    static final ParserSpec.Type<Long> LONG = ParserSpecTypes.builder((input, startIndex) -> {
                final int index = input.indexOf(' ', startIndex);
                final String word = index == -1 ? input.substring(startIndex) : input.substring(startIndex, index);
                final int resultIndex = index == -1 ? input.length() : index;
                try {
                    final long value = Long.parseLong(input, startIndex, resultIndex, 10);
                    return success(word, resultIndex, value);
                } catch (NumberFormatException e) {
                    return incompatible();
                }
            })
            .build();
    static final ParserSpec.Type<String> WORD = ParserSpecTypes.builder((input, startIndex) -> {
                final int index = input.indexOf(' ', startIndex);
                if (index == -1) {
                    // No space found, so it's a word
                    final String word = input.substring(startIndex);
                    return success(word, input.length(), word);
                } else {
                    // Space found, substring the word
                    final String word = input.substring(startIndex, index);
                    return success(word, index, word);
                }
            })
            .equals((input, startIndex, constant) -> {
                final int length = constant.length();
                if (input.regionMatches(startIndex, constant, 0, length)) {
                    final int index = startIndex + length;
                    return success(constant, index, constant);
                } else {
                    return incompatible();
                }
            })
            .find((input, startIndex, constants) -> {
                for (String constant : constants) {
                    final int length = constant.length();
                    if (input.regionMatches(startIndex, constant, 0, length)) {
                        final int index = startIndex + length;
                        return success(constant, index, constant);
                    }
                }
                return incompatible();
            })
            .equalsExact((input, constant) -> input.equals(constant) ? constant : null)
            .findExact((input, constants) -> constants.contains(input) ? input : null)
            .build();
    static final ParserSpec.Type<String> QUOTED_PHRASE = ParserSpecTypes.builder((input, startIndex) -> {
                final int inclusiveEnd = StringReaderUtils.endIndexOfQuotableString(input, startIndex);
                if (inclusiveEnd == -1) {
                    return incompatible();
                } else {
                    final char type = input.charAt(startIndex);
                    final int exclusiveEnd = inclusiveEnd + 1;
                    if (type == '"' || type == '\'') {
                        // Quoted
                        return success(input.substring(startIndex, exclusiveEnd), exclusiveEnd,
                                StringUtils.unescapeJavaString(input.substring(startIndex + 1, inclusiveEnd)));
                    } else {
                        // Unquoted
                        final String substring = input.substring(startIndex, exclusiveEnd);
                        return success(substring, exclusiveEnd, substring);
                    }
                }
            })
            .build();
    static final ParserSpec.Type<String> GREEDY_PHRASE = ParserSpecTypes.builder((input, startIndex) -> {
                final String result = input.substring(startIndex);
                return success(result, input.length(), result);
            })
            .build();

    static <T> Builder<T> builder(Functions.Read<T> read) {
        return new Builder<>(read);
    }

    private interface Functions {
        @FunctionalInterface
        interface Read<T> {
            ParserSpec.Result<T> read(String input, int startIndex);
        }

        @FunctionalInterface
        interface Find<T> {
            ParserSpec.Result<T> find(String input, int startIndex, Set<T> constants);
        }

        @FunctionalInterface
        interface Equals<T> {
            ParserSpec.Result<T> equals(String input, int startIndex, T constant);
        }

        @FunctionalInterface
        interface ReadExact<T> {
            T readExact(String input);
        }

        @FunctionalInterface
        interface FindExact<T> {
            T findExact(String input, Set<T> constants);
        }

        @FunctionalInterface
        interface EqualsExact<T> {
            T equalsExact(String input, T constant);
        }
    }

    static final class Builder<T> {
        final Functions.Read<T> read;
        Functions.Equals<T> equals;
        Functions.Find<T> find;

        Functions.ReadExact<T> readExact;
        Functions.EqualsExact<T> equalsExact;
        Functions.FindExact<T> findExact;

        Builder(Functions.Read<T> read) {
            this.read = read;
        }

        public Builder<T> equals(Functions.Equals<T> equals) {
            this.equals = equals;
            return this;
        }

        public Builder<T> find(Functions.Find<T> find) {
            this.find = find;
            return this;
        }

        Builder<T> readExact(Functions.ReadExact<T> exact) {
            this.readExact = exact;
            return this;
        }

        Builder<T> equalsExact(Functions.EqualsExact<T> equalsExact) {
            this.equalsExact = equalsExact;
            return this;
        }

        Builder<T> findExact(Functions.FindExact<T> findExact) {
            this.findExact = findExact;
            return this;
        }

        ParserSpec.Type<T> build() {
            return new TypeImpl<>(read, equals, find, readExact, equalsExact, findExact);
        }
    }

    record TypeImpl<T>(Functions.Read<T> read,
                       Functions.Equals<T> equals, Functions.Find<T> find,
                       Functions.ReadExact<T> readExact,
                       Functions.EqualsExact<T> equalsExact, Functions.FindExact<T> findExact)
            implements ParserSpec.Type<T> {

        TypeImpl {
            // Create fallback if no specialized function is provided
            equals = Objects.requireNonNullElse(equals, (input, startIndex, constant) -> {
                final ParserSpec.Result<T> result = read(input, startIndex);
                assertInput(result, input);
                if (result instanceof Result.Success<T> success && !constant.equals(success.value())) {
                    return error(success.input(), "Expected constant '" + constant + "' but found '" + success.value() + "'", 0);
                }
                return result;
            });
            find = Objects.requireNonNullElse(find, (input, startIndex, constants) -> {
                final ParserSpec.Result<T> result = read(input, startIndex);
                assertInput(result, input);
                if (result instanceof Result.Success<T> success && !constants.contains(success.value())) {
                    return error(success.input(), "Expected constants '" + constants + "' but found '" + success.value() + "'", 0);
                }
                return result;
            });
            readExact = Objects.requireNonNullElse(readExact, (input) -> {
                final ParserSpec.Result<T> result = read(input, 0);
                if (result instanceof Result.Success<T> success && input.length() == success.index()) {
                    assertInput(result, input);
                    return success.value();
                }
                return null;
            });
            equalsExact = Objects.requireNonNullElse(equalsExact, (input, constant) -> {
                final T value = readExact(input);
                return Objects.equals(value, constant) ? constant : null;
            });
            findExact = Objects.requireNonNullElse(findExact, (input, constants) -> {
                final T value = readExact(input);
                return constants.contains(value) ? value : null;
            });
        }

        @Override
        public ParserSpec.@NotNull Result<T> read(@NotNull String input, int startIndex) {
            return read.read(input, startIndex);
        }

        @Override
        public ParserSpec.@NotNull Result<T> equals(@NotNull String input, int startIndex, @NotNull T constant) {
            return equals.equals(input, startIndex, constant);
        }

        @Override
        public ParserSpec.@NotNull Result<T> find(@NotNull String input, int startIndex, @NotNull Set<@NotNull T> constants) {
            return find.find(input, startIndex, constants);
        }

        @Override
        public @Nullable T readExact(@NotNull String input) {
            return readExact.readExact(input);
        }

        @Override
        public @Nullable T equalsExact(@NotNull String input, @NotNull T constant) {
            return equalsExact.equalsExact(input, constant);
        }

        @Override
        public @Nullable T findExact(@NotNull String input, @NotNull Set<@NotNull T> constants) {
            return findExact.findExact(input, constants);
        }
    }

    record ResultSuccessImpl<T>(String input, int index, T value) implements ParserSpec.Result.Success<T> {
    }

    record ResultIncompatibleImpl<T>() implements ParserSpec.Result.IncompatibleType<T> {
    }

    record ResultErrorImpl<T>(String input, String message, int error) implements ParserSpec.Result.SyntaxError<T> {
    }

    static void assertInput(ParserSpec.Result<?> result, String input) {
        assert result != null : "Result must not be null";
        assert !(result instanceof ParserSpec.Result.Success<?> su)
                || su.input().equals(input) : "input mismatch: " + result + " != " + input;
    }
}
