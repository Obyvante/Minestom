package net.minestom.server.command;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import org.junit.jupiter.api.Test;

import static net.minestom.server.command.Arg.arg;
import static net.minestom.server.command.Arg.literalArg;
import static net.minestom.server.command.Parser.Integer;
import static net.minestom.server.command.Parser.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphConversionTest {
    @Test
    public void empty() {
        final Command foo = new Command("foo");
        var graph = Graph.builder(literalArg("foo")).build();
        assertEqualsGraph(graph, foo);
    }

    @Test
    public void singleLiteral() {
        final Command foo = new Command("foo");
        foo.addSyntax(GraphConversionTest::dummyExecutor, ArgumentType.Literal("first"));
        var graph = Graph.builder(literalArg("foo"))
                .append(literalArg("first")).build();
        assertEqualsGraph(graph, foo);
    }

    @Test
    public void literalsPath() {
        final Command foo = new Command("foo");

        foo.addSyntax(GraphConversionTest::dummyExecutor, ArgumentType.Literal("first"));
        foo.addSyntax(GraphConversionTest::dummyExecutor, ArgumentType.Literal("second"));

        var graph = Graph.builder(literalArg("foo"))
                .append(literalArg("first"))
                .append(literalArg("second"))
                .build();
        assertEqualsGraph(graph, foo);
    }

    @Test
    public void doubleSyntax() {
        enum A {A, B, C, D, E}
        final Command foo = new Command("foo");

        var a = ArgumentType.Enum("a", A.class);

        foo.addSyntax(GraphConversionTest::dummyExecutor,
                ArgumentType.Literal("bar"));
        foo.addSyntax(GraphConversionTest::dummyExecutor,
                ArgumentType.Literal("baz"), a);

        var graph = Graph.builder(literalArg("foo"))
                .append(literalArg("bar"))
                .append(literalArg("baz"), builder ->
                        builder.append(arg("a", legacy(a))))
                .build();
        assertEqualsGraph(graph, foo);
    }

    @Test
    public void doubleSyntaxMerge() {
        final Command foo = new Command("foo");

        foo.addSyntax(GraphConversionTest::dummyExecutor,
                ArgumentType.Literal("bar"));
        foo.addSyntax(GraphConversionTest::dummyExecutor,
                ArgumentType.Literal("bar"), ArgumentType.Integer("number"));

        // The two syntax shall start from the same node
        var graph = Graph.builder(literalArg("foo"))
                .append(literalArg("bar"), builder -> builder.append(arg("number", Integer())))
                .build();
        assertEqualsGraph(graph, foo);
    }

    @Test
    public void subcommand() {
        final Command main = new Command("main");
        final Command sub = new Command("sub");

        sub.addSyntax(GraphConversionTest::dummyExecutor,
                ArgumentType.Literal("bar"));
        sub.addSyntax(GraphConversionTest::dummyExecutor,
                ArgumentType.Literal("bar"), ArgumentType.Integer("number"));

        main.addSubcommand(sub);

        // The two syntax shall start from the same node
        var graph = Graph.builder(literalArg("main"))
                .append(literalArg("sub"), builder ->
                        builder.append(literalArg("bar"),
                                builder1 -> builder1.append(arg("number", Integer()))))
                .build();
        assertEqualsGraph(graph, main);
    }

    @Test
    public void alias() {
        final Command main = new Command("main", "alias");
        var graph = Graph.builder(arg("main", Literals("main", "alias"))).build();
        assertEqualsGraph(graph, main);
    }

    @Test
    public void aliases() {
        final Command main = new Command("main", "first", "second");
        var graph = Graph.builder(arg("main", Literals("main", "first", "second"))).build();
        assertEqualsGraph(graph, main);
    }

    private static void assertEqualsGraph(Graph expected, Command command) {
        final Graph actual = Graph.fromCommand(command);
        assertTrue(expected.compare(actual, Graph.Comparator.TREE), () -> {
            System.out.println("Expected: " + expected);
            System.out.println("Actual:   " + actual);
            return "";
        });
    }

    private static void dummyExecutor(CommandSender sender, CommandContext context) {
    }
}
