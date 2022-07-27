package net.minestom.server.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

final class ParserImpl {
    static final LiteralParserImpl LITERAL = new LiteralParserImpl("");
    static final LiteralsParserImpl LITERALS = new LiteralsParserImpl(Set.of());
    static final BooleanParserImpl BOOLEAN = new BooleanParserImpl();
    static final FloatParserImpl FLOAT = new FloatParserImpl(null, null);
    static final DoubleParserImpl DOUBLE = new DoubleParserImpl(null, null);
    static final IntegerParserImpl INTEGER = new IntegerParserImpl(null, null);
    static final LongParserImpl LONG = new LongParserImpl(null, null);

    static final StringParserImpl STRING = new StringParserImpl(Parser.StringParser.Type.WORD);

    record LiteralParserImpl(String literal) implements Parser.LiteralParser {
        @Override
        public @NotNull LiteralParser literal(@NotNull String literal) {
            return new LiteralParserImpl(literal);
        }

        @Override
        public @NotNull ParserSpec<String> spec() {
            return ParserSpec.constant(ParserSpec.Type.WORD, literal);
        }
    }

    record LiteralsParserImpl(Set<String> literals) implements Parser.LiteralsParser {
        LiteralsParserImpl {
            literals = Set.copyOf(literals);
        }

        @Override
        public @NotNull LiteralsParser literals(@NotNull Set<String> literals) {
            return new LiteralsParserImpl(literals);
        }

        @Override
        public @NotNull ParserSpec<String> spec() {
            return ParserSpec.constants(ParserSpec.Type.WORD, literals);
        }
    }

    record BooleanParserImpl() implements Parser.BooleanParser {
        @Override
        public @NotNull ParserSpec<Boolean> spec() {
            return ParserSpec.Type.BOOLEAN;
        }
    }

    record FloatParserImpl(Float min, Float max) implements Parser.FloatParser {
        private static final ParserSpec<Float> DEFAULT_SPEC = ParserSpec.Type.FLOAT;

        @Override
        public @NotNull FloatParser max(@Nullable Float max) {
            return new FloatParserImpl(min, max);
        }

        @Override
        public @NotNull FloatParser min(@Nullable Float min) {
            return new FloatParserImpl(min, max);
        }

        @Override
        public @NotNull ParserSpec<Float> spec() {
            if (min == null && max == null) {
                return ParserSpec.Type.FLOAT;
            } else {
                return ParserSpec.specialized(DEFAULT_SPEC,
                        result -> {
                            final Float value = result.value();
                            if (min != null && value < min)
                                return ParserSpec.Result.error(result.input(), "value is too low", 2);
                            if (max != null && value > max)
                                return ParserSpec.Result.error(result.input(), "value is too high", 3);
                            return result;
                        });
            }
        }
    }

    record DoubleParserImpl(Double min, Double max) implements Parser.DoubleParser {
        private static final ParserSpec<Double> DEFAULT_SPEC = ParserSpec.Type.DOUBLE;

        @Override
        public @NotNull DoubleParser max(@Nullable Double max) {
            return new DoubleParserImpl(min, max);
        }

        @Override
        public @NotNull DoubleParser min(@Nullable Double min) {
            return new DoubleParserImpl(min, max);
        }

        @Override
        public @NotNull ParserSpec<Double> spec() {
            if (min == null && max == null) {
                return DEFAULT_SPEC;
            } else {
                return ParserSpec.specialized(DEFAULT_SPEC,
                        result -> {
                            final Double value = result.value();
                            if (min != null && value < min)
                                return ParserSpec.Result.error(result.input(), "value is too low", 2);
                            if (max != null && value > max)
                                return ParserSpec.Result.error(result.input(), "value is too high", 3);
                            return result;
                        });
            }
        }
    }

    record IntegerParserImpl(Integer min, Integer max) implements Parser.IntegerParser {
        private static final ParserSpec<Integer> DEFAULT_SPEC = ParserSpec.Type.INTEGER;

        @Override
        public @NotNull IntegerParser max(@Nullable Integer max) {
            return new IntegerParserImpl(min, max);
        }

        @Override
        public @NotNull IntegerParser min(@Nullable Integer min) {
            return new IntegerParserImpl(min, max);
        }

        @Override
        public @NotNull ParserSpec<Integer> spec() {
            if (min == null && max == null) {
                return DEFAULT_SPEC;
            } else {
                return ParserSpec.specialized(DEFAULT_SPEC,
                        result -> {
                            final Integer value = result.value();
                            if (min != null && value < min)
                                return ParserSpec.Result.error(result.input(), "value is too low", 2);
                            if (max != null && value > max)
                                return ParserSpec.Result.error(result.input(), "value is too high", 3);
                            return result;
                        });
            }
        }
    }

    record LongParserImpl(Long min, Long max) implements Parser.LongParser {
        private static final ParserSpec<Long> DEFAULT_SPEC = ParserSpec.Type.LONG;

        @Override
        public @NotNull LongParser max(@Nullable Long max) {
            return new LongParserImpl(min, max);
        }

        @Override
        public @NotNull LongParser min(@Nullable Long min) {
            return new LongParserImpl(min, max);
        }

        @Override
        public @NotNull ParserSpec<Long> spec() {
            if (min == null && max == null) {
                return DEFAULT_SPEC;
            } else {
                return ParserSpec.specialized(DEFAULT_SPEC,
                        result -> {
                            final Long value = result.value();
                            if (min != null && value < min)
                                return ParserSpec.Result.error(result.input(), "value is too low", 2);
                            if (max != null && value > max)
                                return ParserSpec.Result.error(result.input(), "value is too high", 3);
                            return result;
                        });
            }
        }
    }

    record StringParserImpl(StringParser.Type type) implements Parser.StringParser {
        @Override
        public @NotNull StringParser type(@NotNull Type type) {
            return new StringParserImpl(type);
        }

        @Override
        public @NotNull ParserSpec<String> spec() {
            return switch (type) {
                case WORD -> ParserSpec.Type.WORD;
                case QUOTED -> ParserSpec.Type.QUOTED_PHRASE;
                case GREEDY -> ParserSpec.Type.GREEDY_PHRASE;
            };
        }
    }

    record CustomImpl<T>(ParserSpec<T> spec) implements Parser.Custom<T> {
    }
}
