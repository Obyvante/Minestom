package net.minestom.server.command;

import net.minestom.server.command.builder.arguments.ArgumentType;
import org.junit.jupiter.api.Test;

import java.lang.Integer;
import java.lang.String;
import java.util.Set;

import static net.minestom.server.command.Parser.Boolean;
import static net.minestom.server.command.Parser.Double;
import static net.minestom.server.command.Parser.Float;
import static net.minestom.server.command.Parser.Integer;
import static net.minestom.server.command.Parser.Long;
import static net.minestom.server.command.Parser.String;
import static net.minestom.server.command.Parser.*;
import static org.junit.jupiter.api.Assertions.*;

public class ArgSpecTest {

    @Test
    public void literalParse() {
        // Exact parsing
        assertValidSpecExact(Literal("test"), "test", "test");

        assertInvalidSpecExact(Literal("test"), "text");

        // Sequence parsing
        assertValidSpec(Literal("test"), "test", 4, "test");
        assertValidSpec(Literal("test"), "test", 4, "test 5");

        assertInvalidSpec(Literal("test"), "text");
        assertInvalidSpec(Literal("test"), "5");
        assertInvalidSpec(Literal("test"), "");
    }

    @Test
    public void literalsParse() {
        // Exact parsing
        assertValidSpecExact(Literals("test"), "test", "test");
        assertValidSpecExact(Literals("first", "second"), "first", "first");
        assertValidSpecExact(Literals(Set.of("first", "second")), "first", "first");
        assertValidSpecExact(Literals("first", "second"), "second", "second");

        assertInvalidSpecExact(Literals("test"), "text");
        assertInvalidSpecExact(Literals("first", "second"), "first second");
        assertInvalidSpecExact(Literals("first", "second"), "second first");

        // Sequence parsing
        assertValidSpec(Literals("test"), "test", 4, "test");
        assertValidSpec(Literals("test"), "test", 4, "test 5");
        assertValidSpec(Literals("first", "second"), "first", 5, "first");
        assertValidSpec(Literals("first", "second"), "first", 5, "first second");
        assertValidSpec(Literals("first", "second"), "second", 6, "second");
        assertValidSpec(Literals("first", "second"), "second", 6, "second first");

        assertInvalidSpec(Literals("test"), "text");
        assertInvalidSpec(Literals("test"), "5");
        assertInvalidSpec(Literals("test"), "");
        assertInvalidSpec(Literals("first", "second"), "text");
    }

    @Test
    public void booleanParse() {
        // Exact parsing
        assertValidSpecExact(Boolean(), true, "true");
        assertValidSpecExact(Boolean(), false, "false");

        assertInvalidSpecExact(Boolean(), "truee");
        assertInvalidSpecExact(Boolean(), "falsee");
        assertInvalidSpecExact(Boolean(), "TRuE");
        assertInvalidSpecExact(Boolean(), "ttrue");
        assertInvalidSpecExact(Boolean(), "t true");
        assertInvalidSpecExact(Boolean(), "  false");
        assertInvalidSpecExact(Boolean(), "  true");

        // Sequence parsing
        assertValidSpec(Boolean(), true, 4, "true");
        assertValidSpec(Boolean(), false, 5, "false");
        assertValidSpec(Boolean(), true, 4, "true test");
        assertValidSpec(Boolean(), false, 5, "false test");

        assertInvalidSpec(Boolean(), "text");
        assertInvalidSpec(Boolean(), "text text");
        assertInvalidSpec(Boolean(), "text 55");
    }

    @Test
    public void floatParse() {
        // Exact parsing
        assertValidSpecExact(Float(), 1f, "1");
        assertValidSpecExact(Float(), 1.5f, "1.5");
        assertValidSpecExact(Float(), -1.5f, "-1.5");
        assertValidSpecExact(Float(), -99f, "-99");
        assertValidSpecExact(Float().min(5f).max(10f), 5f, "5");
        assertValidSpecExact(Float().min(5f), 5f, "5");
        assertValidSpecExact(Float().max(10f), -99f, "-99");
        assertValidSpecExact(Float().min(5f).max(10f), 10f, "10");

        assertInvalidSpecExact(Float(), "text");
        assertInvalidSpecExact(Float(), "text text");
        assertInvalidSpecExact(Float(), "1 1");
        assertInvalidSpecExact(Float().min(5f), "-5");
        assertInvalidSpecExact(Float().min(5f), "4");
        assertInvalidSpecExact(Float().max(10f), "11");

        // Sequence parsing
        assertValidSpec(Float(), 1f, 1, "1");
        assertValidSpec(Float(), 11f, 2, "11");
        assertValidSpec(Float(), 5f, 3, "5.0 1");
        assertValidSpec(Float(), 55f, 2, "55 1");
        assertValidSpec(Float(), 55f, 2, "55 text");

        assertInvalidSpec(Float(), "text");
        assertInvalidSpec(Float(), "text text");
        assertInvalidSpec(Float(), "text 55");
    }

    @Test
    public void doubleParse() {
        // Exact parsing
        assertValidSpecExact(Double(), 1d, "1");
        assertValidSpecExact(Double(), 1.5d, "1.5");
        assertValidSpecExact(Double(), -1.5d, "-1.5");
        assertValidSpecExact(Double(), -99d, "-99");
        assertValidSpecExact(Double().min(5d).max(10d), 5d, "5");
        assertValidSpecExact(Double().min(5d), 5d, "5");
        assertValidSpecExact(Double().max(10d), -99d, "-99");
        assertValidSpecExact(Double().min(5d).max(10d), 10d, "10");

        assertInvalidSpecExact(Double(), "text");
        assertInvalidSpecExact(Double(), "text text");
        assertInvalidSpecExact(Double(), "1 1");
        assertInvalidSpecExact(Double().min(5d), "-5");
        assertInvalidSpecExact(Double().min(5d), "4");
        assertInvalidSpecExact(Double().max(10d), "11");

        // Sequence parsing
        assertValidSpec(Double(), 1d, 1, "1");
        assertValidSpec(Double(), 11d, 2, "11");
        assertValidSpec(Double(), 5d, 3, "5.0 1");
        assertValidSpec(Double(), 55d, 2, "55 1");
        assertValidSpec(Double(), 55d, 2, "55 text");

        assertInvalidSpec(Double(), "text");
        assertInvalidSpec(Double(), "text text");
        assertInvalidSpec(Double(), "text 55");
    }

    @Test
    public void integerParse() {
        // Exact parsing
        assertValidSpecExact(Integer(), 1, "1");
        assertValidSpecExact(Integer(), -99, "-99");
        assertValidSpecExact(Integer().min(5).max(10), 5, "5");
        assertValidSpecExact(Integer().min(5), 5, "5");
        assertValidSpecExact(Integer().max(10), -99, "-99");
        assertValidSpecExact(Integer().min(5).max(10), 10, "10");

        assertInvalidSpecExact(Integer(), "text");
        assertInvalidSpecExact(Integer(), "text text");
        assertInvalidSpecExact(Integer(), "1 1");
        assertInvalidSpecExact(Integer().min(5), "-5");
        assertInvalidSpecExact(Integer().min(5), "4");
        assertInvalidSpecExact(Integer().max(10), "11");

        // Sequence parsing
        assertValidSpec(Integer(), 1, 1, "1");
        assertValidSpec(Integer(), 11, 2, "11");
        assertValidSpec(Integer(), 5, 1, "5 1");
        assertValidSpec(Integer(), 55, 2, "55 1");
        assertValidSpec(Integer(), 55, 2, "55 text");

        assertInvalidSpec(Integer(), "text");
        assertInvalidSpec(Integer(), "text text");
        assertInvalidSpec(Integer(), "text 55");
    }

    @Test
    public void longParse() {
        // Exact parsing
        assertValidSpecExact(Long(), 1L, "1");
        assertValidSpecExact(Long(), -99L, "-99");
        assertValidSpecExact(Long().min(5L).max(10L), 5L, "5");
        assertValidSpecExact(Long().min(5L), 5L, "5");
        assertValidSpecExact(Long().max(10L), -99L, "-99");
        assertValidSpecExact(Long().min(5L).max(10L), 10L, "10");

        assertInvalidSpecExact(Long(), "text");
        assertInvalidSpecExact(Long(), "text text");
        assertInvalidSpecExact(Long(), "1 1");
        assertInvalidSpecExact(Long().min(5L), "-5");
        assertInvalidSpecExact(Long().min(5L), "4");
        assertInvalidSpecExact(Long().max(10L), "11");

        // Sequence parsing
        assertValidSpec(Long(), 1L, 1, "1");
        assertValidSpec(Long(), 11L, 2, "11");
        assertValidSpec(Long(), 5L, 1, "5 1");
        assertValidSpec(Long(), 55L, 2, "55 1");
        assertValidSpec(Long(), 55L, 2, "55 text");

        assertInvalidSpec(Long(), "text");
        assertInvalidSpec(Long(), "text text");
        assertInvalidSpec(Long(), "text 55");
    }

    @Test
    public void stringParse() {
        // Exact parsing
        assertValidSpecExact(String(), "test", "test");
        assertValidSpecExact(String().type(StringParser.Type.GREEDY), "test 1 2 3", "test 1 2 3");
        assertValidSpecExact(String().type(StringParser.Type.QUOTED), "test", "test");
        assertValidSpecExact(String().type(StringParser.Type.QUOTED), "Hey there", """
                "Hey there"\
                """);
        assertValidSpecExact(String().type(StringParser.Type.QUOTED), "Hey  there", """
                "Hey  there"\
                """);
        assertValidSpecExact(String().type(StringParser.Type.QUOTED), "text", "text");

        assertInvalidSpecExact(String().type(StringParser.Type.QUOTED), """
                "Hey\
                """);
        assertInvalidSpecExact(String().type(StringParser.Type.QUOTED), """
                there"\
                """);

        // Sequence parsing
        assertValidSpec(String(), "test", 4, "test");
        assertValidSpec(String(), "test", 4, "test a");
        assertValidSpec(String().type(StringParser.Type.GREEDY), "test 1 2 3", 10, "test 1 2 3");
        assertValidSpec(String().type(StringParser.Type.QUOTED), "Hey there", 11, """
                "Hey there"\
                """);
        assertValidSpec(String().type(StringParser.Type.QUOTED), "Hey  there", 12, """
                "Hey  there"\
                """);
        assertValidSpec(String().type(StringParser.Type.QUOTED), "Hey there", 11, """
                "Hey there" test\
                """);

        assertValidSpec(String().type(StringParser.Type.QUOTED), "text", 4, "text");
        assertValidSpec(String().type(StringParser.Type.QUOTED), "text", 4, "text test");
    }

    @Test
    public void customSingleParse() {
        Custom<Integer> parser = custom(ParserSpec.Type.INTEGER);
        assertValidSpec(parser, 1, 1, "1");
        assertValidSpec(parser, 11, 2, "11");
        assertValidSpec(parser, 5, 1, "5 1");
        assertValidSpec(parser, 55, 2, "55 1");
        assertValidSpec(parser, 55, 2, "55 text");
    }

    @Test
    public void customReaderParse() {
        Custom<Integer> parser = custom(ParserSpec.reader((s, startIndex) -> {
            final String input = s.substring(startIndex);
            if (!input.startsWith("1")) return null;
            return ParserSpec.Result.success("1", startIndex + 1, 1);
        }));
        assertValidSpec(parser, 1, 1, "1");
        assertInvalidSpec(parser, "5 1");
        assertValidSpec(parser, 1, 1, "1 55");
    }

    @Test
    public void customLegacyParse() {
        final ParserSpec<String> spec = ParserSpec.legacy(ArgumentType.String("test"));
        final Custom<String> parser = Parser.custom(spec);
        assertValidSpecExact(parser, "test", "test");
        assertValidSpecExact(parser, "Hey there", """
                "Hey there"\
                """);
        assertValidSpecExact(parser, "Hey  there", """
                "Hey  there"\
                """);
        assertValidSpecExact(parser, "text", "text");

        assertInvalidSpecExact(parser, """
                "Hey\
                """);
        assertInvalidSpecExact(parser, """
                there"\
                """);

        // Sequence parsing
        assertValidSpec(parser, "Hey there", 11, """
                "Hey there"\
                """);
        assertValidSpec(parser, "Hey  there", 12, """
                "Hey  there"\
                """);
        assertValidSpec(parser, "Hey there", 11, """
                "Hey there" test\
                """);

        assertValidSpec(parser, "text", 4, "text");
    }

    static <T> void assertValidSpec(Parser<T> parser, T expectedValue, int expectedIndex, String input) {
        final ParserSpec<T> spec = parser.spec();
        final ParserSpec.Result.Success<T> result = (ParserSpec.Result.Success<T>) spec.read(input);
        assertNotNull(result);
        assertEquals(input.substring(0, expectedIndex), result.input(), "Invalid input(" + expectedIndex + ") for '" + input + "'");
        assertEquals(expectedValue, result.value(), "Invalid value");
        assertEquals(expectedIndex, result.index(), "Invalid index");

        // Assert read with non-zero initial index
        input = "1 " + input;
        expectedIndex += 2;
        final ParserSpec.Result.Success<T> result2 = (ParserSpec.Result.Success<T>) spec.read(input, 2);
        assertNotNull(result2);
        assertEquals(input.substring(2, expectedIndex), result2.input(), "Invalid input(" + expectedIndex + ") for '" + input + "'");
        assertEquals(expectedValue, result2.value(), "Invalid value");
        assertEquals(expectedIndex, result2.index(), "Invalid index");
    }

    static <T> void assertInvalidSpec(Parser<T> parser, String input) {
        final ParserSpec<?> spec = parser.spec();
        final ParserSpec.Result<?> result = spec.read(input);
        if (result instanceof ParserSpec.Result.Success<?>) {
            fail("Expected failure for '" + input + "'");
        }
    }

    static <T> void assertValidSpecExact(Parser<T> parser, T expected, String input) {
        final ParserSpec<T> spec = parser.spec();
        final T result = spec.readExact(input);
        assertEquals(expected, result);
    }

    static void assertInvalidSpecExact(Parser<?> parser, String input) {
        final ParserSpec<?> spec = parser.spec();
        assertNull(spec.readExact(input));
    }
}
