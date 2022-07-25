package net.minestom.server.command;

import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import org.junit.jupiter.api.Test;

import static net.minestom.server.command.Arg.arg;
import static net.minestom.server.command.Arg.literalArg;
import static net.minestom.server.command.Parser.Boolean;
import static net.minestom.server.command.Parser.Double;
import static net.minestom.server.command.Parser.Float;
import static net.minestom.server.command.Parser.Integer;
import static net.minestom.server.command.Parser.Long;
import static net.minestom.server.command.Parser.String;
import static net.minestom.server.command.Parser.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ArgTest {
    @Test
    public void basic() {
        var arg = arg("id", Integer());
        assertEquals("id", arg.id());
        assertEquals(Integer(), arg.parser());
        assertNull(arg.suggestionType());
    }

    @Test
    public void equality() {
        assertEquals(literalArg("test"), literalArg("test"));
        assertEquals(arg("id", Integer()), arg("id", Integer()));
    }

    @Test
    public void conversionParser() {
        // Boolean
        assertParserConversion(Boolean(), ArgumentType.Boolean("id"));
        // Float
        assertParserConversion(Float(), ArgumentType.Float("id"));
        assertParserConversion(Float().min(5f), ArgumentType.Float("id").min(5f));
        assertParserConversion(Float().max(5f), ArgumentType.Float("id").max(5f));
        assertParserConversion(Float().min(5f).max(5f), ArgumentType.Float("id").min(5f).max(5f));
        // Double
        assertParserConversion(Double(), ArgumentType.Double("id"));
        assertParserConversion(Double().min(5d), ArgumentType.Double("id").min(5d));
        assertParserConversion(Double().max(5d), ArgumentType.Double("id").max(5d));
        assertParserConversion(Double().min(5d).max(5d), ArgumentType.Double("id").min(5d).max(5d));
        // Integer
        assertParserConversion(Integer(), ArgumentType.Integer("id"));
        assertParserConversion(Integer().min(5), ArgumentType.Integer("id").min(5));
        assertParserConversion(Integer().max(5), ArgumentType.Integer("id").max(5));
        assertParserConversion(Integer().min(5).max(5), ArgumentType.Integer("id").min(5).max(5));
        // Long
        assertParserConversion(Long(), ArgumentType.Long("id"));
        assertParserConversion(Long().min(5L), ArgumentType.Long("id").min(5L));
        assertParserConversion(Long().max(5L), ArgumentType.Long("id").max(5L));
        assertParserConversion(Long().min(5L).max(5L), ArgumentType.Long("id").min(5L).max(5L));
        // Word
        assertParserConversion(String(), ArgumentType.Word("id"));
        assertParserConversion(String().type(StringParser.Type.QUOTED), ArgumentType.String("id"));
        assertParserConversion(String().type(StringParser.Type.GREEDY), ArgumentType.StringArray("id"));
        assertParserConversion(Literals("first", "second"), ArgumentType.Word("id").from("first", "second"));
    }

    static <T> void assertParserConversion(Parser<?> expected, Argument<?> argument) {
        final Arg<?> converted = ArgImpl.fromLegacy(argument);
        final Parser<?> convertedParser = converted.parser();
        assertEquals(expected, convertedParser);
    }
}
