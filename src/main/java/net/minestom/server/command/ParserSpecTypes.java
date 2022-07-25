package net.minestom.server.command;

import net.minestom.server.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;
import java.util.Set;

final class ParserSpecTypes {
    static final ParserSpec.Type<Boolean> BOOLEAN = ParserSpecTypes.builder((input, startIndex) -> {
                if (input.equals("true") || input.startsWith("true ", startIndex)) {
                    return new ResultImpl<>("true", startIndex + 4, true);
                } else if (input.equals("false") || input.startsWith("false ", startIndex)) {
                    return new ResultImpl<>("false", startIndex + 5, false);
                }
                return null;
            })
            .build();
    static final ParserSpec.Type<Float> FLOAT = ParserSpecTypes.builder((input, startIndex) -> {
                final int index = input.indexOf(' ', startIndex);
                if (index == -1) {
                    // Whole input is a float
                    final float value = Float.parseFloat(input.substring(startIndex));
                    return new ResultImpl<>(input, input.length(), value);
                } else {
                    // Part of input is a float
                    final float value = Float.parseFloat(input.substring(startIndex, index));
                    return new ResultImpl<>(input, index, value);
                }
            })
            .build();
    static final ParserSpec.Type<Double> DOUBLE = ParserSpecTypes.builder((input, startIndex) -> {
                final int index = input.indexOf(' ', startIndex);
                if (index == -1) {
                    // Whole input is a double
                    final double value = Double.parseDouble(input.substring(startIndex));
                    return new ResultImpl<>(input, input.length(), value);
                } else {
                    // Part of input is a double
                    final double value = Double.parseDouble(input.substring(startIndex, index));
                    return new ResultImpl<>(input, index, value);
                }
            })
            .build();
    static final ParserSpec.Type<Integer> INTEGER = ParserSpecTypes.builder((input, startIndex) -> {
                final int index = input.indexOf(' ', startIndex);
                if (index == -1) {
                    // Whole input is an integer
                    final int value = Integer.parseInt(input, startIndex, input.length(), 10);
                    return new ResultImpl<>(input, input.length(), value);
                } else {
                    // Part of input is an integer
                    final int value = Integer.parseInt(input, startIndex, index, 10);
                    return new ResultImpl<>(input, index, value);
                }
            })
            .readExact(Integer::parseInt)
            .equalsExact((input, constant) -> Integer.parseInt(input) == constant ? constant : null)
            .findExact((input, constants) -> {
                final int value = Integer.parseInt(input);
                return constants.contains(value) ? value : null;
            })
            .build();
    static final ParserSpec.Type<Long> LONG = ParserSpecTypes.builder((input, startIndex) -> {
                final int index = input.indexOf(' ', startIndex);
                if (index == -1) {
                    // Whole input is an integer
                    final long value = Long.parseLong(input, startIndex, input.length(), 10);
                    return new ResultImpl<>(input, input.length(), value);
                } else {
                    // Part of input is an integer
                    final long value = Long.parseLong(input, startIndex, index, 10);
                    return new ResultImpl<>(input, index, value);
                }
            })
            .readExact(Long::parseLong)
            .equalsExact((input, constant) -> Long.parseLong(input) == constant ? constant : null)
            .findExact((input, constants) -> {
                final long value = Long.parseLong(input);
                return constants.contains(value) ? value : null;
            })
            .build();
    static final ParserSpec.Type<String> WORD = ParserSpecTypes.builder((input, startIndex) -> {
                final int index = input.indexOf(' ', startIndex);
                if (index == -1) {
                    // No space found, so it's a word
                    return new ResultImpl<>(input, input.length(), input);
                } else {
                    // Space found, substring the word
                    return new ResultImpl<>(input, index, input.substring(startIndex, index));
                }
            })
            .equals((input, startIndex, constant) -> {
                final int length = constant.length();
                if (input.regionMatches(startIndex, constant, 0, length)) {
                    return new ResultImpl<>(input, startIndex + length, constant);
                } else {
                    return null;
                }
            })
            .find((input, startIndex, constants) -> {
                for (String constant : constants) {
                    final int length = constant.length();
                    if (input.regionMatches(startIndex, constant, 0, length)) {
                        return new ResultImpl<>(input, startIndex + length, constant);
                    }
                }
                return null;
            })
            .equalsExact((input, constant) -> input.equals(constant) ? constant : null)
            .findExact((input, constants) -> constants.contains(input) ? input : null)
            .build();
    static final ParserSpec.Type<String> QUOTED_PHRASE = ParserSpecTypes.builder((input, startIndex) -> {
                final String tmp = input;
                input = input.trim();
                final char BACKSLASH = '\\';
                final char DOUBLE_QUOTE = '"';
                final char QUOTE = '\'';

                input = input.substring(startIndex);

                // Return if not quoted
                if (!input.contains(String.valueOf(DOUBLE_QUOTE)) &&
                        !input.contains(String.valueOf(QUOTE)) &&
                        !input.contains(StringUtils.SPACE)) {
                    return new ResultImpl<>(input, input.length(), input);
                }

                // Check if value start and end with quote
                final char first = input.charAt(0);
                final char last = input.charAt(input.length() - 1);
                final boolean quote = input.length() >= 2 &&
                        first == last && (first == DOUBLE_QUOTE || first == QUOTE);
                if (!quote) {
                    return null; // String argument needs to start and end with quotes
                }

                // Remove first and last characters (quotes)
                input = input.substring(1, input.length() - 1);

                // Verify backslashes
                for (int i = 1; i < input.length(); i++) {
                    final char c = input.charAt(i);
                    if (c == first) {
                        final char lastChar = input.charAt(i - 1);
                        if (lastChar != BACKSLASH) {
                            return null; // Non-escaped quote
                        }
                    }
                }

                final String result = StringUtils.unescapeJavaString(input);
                final int index = tmp.indexOf(result, startIndex) + result.length() + 1;
                return new ResultImpl<>(input, index, result);
            })
            .build();
    static final ParserSpec.Type<String> GREEDY_PHRASE = ParserSpecTypes.builder((input, startIndex) -> {
                final String result = input.substring(startIndex);
                return new ResultImpl<>(input, input.length(), result);
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
                return result != null && constant.equals(result.value()) ? result : null;
            });
            find = Objects.requireNonNullElse(find, (input, startIndex, constants) -> {
                final ParserSpec.Result<T> result = read(input, startIndex);
                assertInput(result, input);
                return result != null && constants.contains(result.value()) ? result : null;
            });
            readExact = Objects.requireNonNullElse(readExact, (input) -> {
                final ParserSpec.Result<T> result = read(input, 0);
                assertInput(result, input);
                return result != null && input.length() == result.index() ? result.value() : null;
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
        public ParserSpec.@Nullable Result<T> read(@NotNull String input, int startIndex) {
            try {
                return read.read(input, startIndex);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public ParserSpec.@Nullable Result<T> equals(@NotNull String input, int startIndex, @NotNull T constant) {
            try {
                return equals.equals(input, startIndex, constant);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public ParserSpec.@Nullable Result<T> find(@NotNull String input, int startIndex, @NotNull Set<@NotNull T> constants) {
            try {
                return find.find(input, startIndex, constants);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public @Nullable T readExact(@NotNull String input) {
            try {
                return readExact.readExact(input);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public @Nullable T equalsExact(@NotNull String input, @NotNull T constant) {
            try {
                return equalsExact.equalsExact(input, constant);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public @Nullable T findExact(@NotNull String input, @NotNull Set<@NotNull T> constants) {
            try {
                return findExact.findExact(input, constants);
            } catch (Exception e) {
                return null;
            }
        }
    }

    record ResultImpl<T>(String input, int index, T value) implements ParserSpec.Result<T> {
    }

    static void assertInput(ParserSpec.@UnknownNullability Result<?> result, String input) {
        assert result == null || result.input().equals(input) : "input mismatch: " + result.input() + " != " + input;
    }
}
