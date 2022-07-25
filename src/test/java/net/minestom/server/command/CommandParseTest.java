package net.minestom.server.command;

import net.minestom.server.command.builder.arguments.ArgumentType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.String;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static net.minestom.server.command.Arg.arg;
import static net.minestom.server.command.Arg.literalArg;
import static net.minestom.server.command.ArgImpl.fromLegacy;
import static net.minestom.server.command.Parser.Integer;
import static net.minestom.server.command.Parser.String;
import static net.minestom.server.command.Parser.*;
import static org.junit.jupiter.api.Assertions.*;

public class CommandParseTest {

    @Test
    public void singleParameterlessCommand() {
        final AtomicBoolean b = new AtomicBoolean();
        var foo = Graph.merge(Graph.builder(literalArg("foo"), createExecutor(b)).build());
        assertValid(foo, "foo", b);
        assertUnknown(foo, "bar");
        assertSyntaxError(foo, "foo bar baz");
    }

    @Test
    public void twoParameterlessCommand() {
        final AtomicBoolean b = new AtomicBoolean();
        final AtomicBoolean b1 = new AtomicBoolean();
        var graph = Graph.merge(
                Graph.builder(literalArg("foo"), createExecutor(b)).build(),
                Graph.builder(literalArg("bar"), createExecutor(b1)).build()
        );
        assertValid(graph, "foo", b);
        assertValid(graph, "bar", b1);
        assertUnknown(graph, "baz");
        assertSyntaxError(graph, "foo bar baz");
        assertSyntaxError(graph, "bar 25");
    }

    @Test
    public void singleCommandWithMultipleSyntax() {
        final AtomicBoolean add = new AtomicBoolean();
        final AtomicBoolean action = new AtomicBoolean();
        var foo = Graph.merge(Graph.builder(literalArg("foo"))
                .append(literalArg("add"),
                        x -> x.append(arg("name", String()), createExecutor(add)))
                .append(arg("action", Literals("inc", "dec")),
                        x -> x.append(arg("num", Integer()), createExecutor(action)))
                .build());
        assertValid(foo, "foo add test", add);
        assertValid(foo, "foo add inc", add);
        assertValid(foo, "foo add 157", add);
        assertValid(foo, "foo inc 157", action);
        assertValid(foo, "foo dec 157", action);
        assertSyntaxError(foo, "foo 15");
        assertSyntaxError(foo, "foo asd");
        assertSyntaxError(foo, "foo inc");
        assertSyntaxError(foo, "foo inc asd");
        assertSyntaxError(foo, "foo inc 15 dec");
        assertSyntaxError(foo, "foo inc 15 20");
        assertUnknown(foo, "bar");
        assertUnknown(foo, "add");
    }

    @Test
    public void singleCommandOptionalArgs() {
        final AtomicBoolean b = new AtomicBoolean();
        final AtomicReference<String> expectedFirstArg = new AtomicReference<>("T");
        var foo = Graph.merge(Graph.builder(literalArg("foo"))
                .append(arg("a", String()).defaultValue("A"),
                        x -> x.append(arg("b", String()).defaultValue("B"),
                                x1 -> x1.append(arg("c", String()).defaultValue("C"),
                                        x2 -> x2.append(arg("d", String()).defaultValue("D"),
                                                new GraphImpl.ExecutionImpl(null, null, null,
                                                        (sender, context) -> {
                                                            b.set(true);
                                                            assertEquals(expectedFirstArg.get(), context.get("a"));
                                                            assertEquals("B", context.get("b"));
                                                            assertEquals("C", context.get("c"));
                                                            assertEquals("D", context.get("d"));
                                                        }, null)))))
                .build());
        assertValid(foo, "foo T", b);
        expectedFirstArg.set("A");
        assertValid(foo, "foo", b);
    }

    @Test
    public void singleCommandSingleEnumArg() {
        enum A {a, b}
        final AtomicBoolean b = new AtomicBoolean();
        var foo = Graph.merge(Graph.builder(literalArg("foo"))
                .append(fromLegacy(ArgumentType.Enum("test", A.class)), createExecutor(b))
                .build());
        assertValid(foo, "foo a", b);
        assertValid(foo, "foo b", b);
        assertSyntaxError(foo, "foo c");
        assertSyntaxError(foo, "foo");
    }

    @Test
    public void aliasWithoutArgs() {
        final AtomicBoolean b = new AtomicBoolean();
        var foo = Graph.merge(Graph.builder(arg("", Literals("foo", "bar")), createExecutor(b))
                .build());
        assertValid(foo, "foo", b);
        assertValid(foo, "bar", b);
        assertUnknown(foo, "test");
    }

    @Test
    public void aliasWithArgs() {
        final AtomicBoolean b = new AtomicBoolean();
        var foo = Graph.merge(Graph.builder(arg("", Literals("foo", "bar")))
                .append(arg("test", Integer()), createExecutor(b))
                .build());
        assertValid(foo, "foo 1", b);
        assertValid(foo, "bar 1", b);
        assertSyntaxError(foo, "foo");
        assertSyntaxError(foo, "bar");
    }

    private static void assertSyntaxError(Graph graph, String input) {
        assertInstanceOf(CommandParser.Result.KnownCommand.Invalid.class, parseCommand(graph, input));
    }

    private static void assertUnknown(Graph graph, String input) {
        assertInstanceOf(CommandParser.Result.UnknownCommand.class, parseCommand(graph, input));
    }

    private static void assertValid(Graph graph, String input, AtomicBoolean executorTest) {
        final CommandParser.Result result = parseCommand(graph, input);
        assertInstanceOf(CommandParser.Result.KnownCommand.Valid.class, result);
        result.executable().execute(null);
        assertTrue(executorTest.get(), "Parser returned valid syntax, but with the wrong executor.");
        executorTest.set(false);
    }

    private static CommandParser.Result parseCommand(Graph graph, String input) {
        return CommandParser.parser().parse(graph, input);
    }

    @NotNull
    private static Graph.Execution createExecutor(AtomicBoolean atomicBoolean) {
        return new GraphImpl.ExecutionImpl(null, null, null, (sender, context) -> atomicBoolean.set(true), null);
    }
}
