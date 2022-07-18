package net.minestom.server.command;

import org.junit.jupiter.api.Test;

import java.lang.Double;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.util.Set;

import static net.minestom.server.command.Parser.Boolean;
import static net.minestom.server.command.Parser.Double;
import static net.minestom.server.command.Parser.Float;
import static net.minestom.server.command.Parser.Integer;
import static net.minestom.server.command.Parser.Long;
import static net.minestom.server.command.Parser.String;
import static net.minestom.server.command.Parser.*;
import static org.junit.jupiter.api.Assertions.*;

public class ArgParserTest {

    @Test
    public void literal() {
        LiteralParser parser = Literal("test");
        assertNotNull(parser);
        assertEquals("test", parser.literal());

        parser = parser.literal("test2");
        assertNotNull(parser);
        assertEquals("test2", parser.literal());
    }

    @Test
    public void literals() {
        LiteralsParser parser = Literals("test");
        assertNotNull(parser);
        assertEquals(Set.of("test"), parser.literals());

        parser = parser.literals("first", "second");
        assertNotNull(parser);
        assertEquals(Set.of("first", "second"), parser.literals());

        parser = parser.literals("third");
        assertNotNull(parser);
        assertEquals(Set.of("third"), parser.literals());
    }

    @Test
    public void booleanTest() {
        BooleanParser parser = Boolean();
        assertNotNull(parser);
    }

    @Test
    public void floatTest() {
        FloatParser parser = Float();
        Float min = parser.min();
        Float max = parser.max();
        assertNull(min);
        assertNull(max);

        parser = parser.min(1f);
        assertEquals(1f, parser.min());
        assertNull(parser.max());

        parser = parser.max(2f);
        assertEquals(1f, parser.min());
        assertEquals(2f, parser.max());
    }

    @Test
    public void doubleTest() {
        DoubleParser parser = Double();
        Double min = parser.min();
        Double max = parser.max();
        assertNull(min);
        assertNull(max);

        parser = parser.min(1d);
        assertEquals(1d, parser.min());
        assertNull(parser.max());

        parser = parser.max(2d);
        assertEquals(1d, parser.min());
        assertEquals(2d, parser.max());
    }

    @Test
    public void integerTest() {
        IntegerParser parser = Integer();
        Integer min = parser.min();
        Integer max = parser.max();
        assertNull(min);
        assertNull(max);

        parser = parser.min(1);
        assertEquals(1, parser.min());
        assertNull(parser.max());

        parser = parser.max(2);
        assertEquals(1, parser.min());
        assertEquals(2, parser.max());
    }

    @Test
    public void longTest() {
        LongParser parser = Long();
        Long min = parser.min();
        Long max = parser.max();
        assertNull(min);
        assertNull(max);

        parser = parser.min(1L);
        assertEquals(1L, parser.min());
        assertNull(parser.max());

        parser = parser.max(2L);
        assertEquals(1L, parser.min());
        assertEquals(2L, parser.max());
    }

    @Test
    public void stringTest() {
        StringParser parser = String();
        assertEquals(StringParser.Type.WORD, parser.type());

        parser = parser.type(StringParser.Type.QUOTED);
        assertEquals(StringParser.Type.QUOTED, parser.type());

        parser = parser.type(StringParser.Type.GREEDY);
        assertEquals(StringParser.Type.GREEDY, parser.type());
    }
}
