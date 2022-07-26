package net.minestom.server.command;

import net.minestom.server.command.builder.arguments.*;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import net.minestom.server.utils.binary.BinaryWriter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class GraphConverter {
    private static final Map<Class<? extends Parser<?>>, String> parserNames = Map.of(
            Parser.BooleanParser.class, "brigadier:bool",
            Parser.DoubleParser.class, "brigadier:double",
            Parser.FloatParser.class, "brigadier:float",
            Parser.IntegerParser.class, "brigadier:integer",
            Parser.LongParser.class, "brigadier:long",
            Parser.StringParser.class, "brigadier:string"
    );

    private static final Map<Class<? extends Parser<?>>, Function<Parser<?>, byte[]>> propertiesFunctions = Map.ofEntries(
            numberProps(Parser.DoubleParser.class, BinaryWriter::writeDouble, Parser.DoubleParser::min, Parser.DoubleParser::max),
            numberProps(Parser.FloatParser.class, BinaryWriter::writeFloat, Parser.FloatParser::min, Parser.FloatParser::max),
            numberProps(Parser.IntegerParser.class, BinaryWriter::writeInt, Parser.IntegerParser::min, Parser.IntegerParser::max),
            numberProps(Parser.LongParser.class, BinaryWriter::writeLong, Parser.LongParser::min, Parser.LongParser::max),
            propEntry(Parser.StringParser.class, p -> BinaryWriter.makeArray(w -> w.writeVarInt(switch (p.type()) {
                case WORD -> 0;
                case QUOTED -> 1;
                case GREEDY -> 2;
            })))
    );

    private static <T extends Parser<?>> Map.Entry<Class<? extends Parser<?>>, Function<Parser<?>, byte[]>> propEntry(Class<T> parser, Function<T, byte[]> func) {
        return Map.entry(parser, p -> func.apply(parser.cast(p)));
    }

    private static <T extends Number, P extends Parser<T>> Map.Entry<Class<? extends Parser<?>>, Function<Parser<?>, byte[]>>
    numberProps(Class<P> parserClass, BiConsumer<BinaryWriter, T> numWriter, Function<P, T> minGetter, Function<P, T> maxGetter) {
        return propEntry(parserClass, p -> BinaryWriter.makeArray(w -> {
            final T min = minGetter.apply(p);
            final T max = maxGetter.apply(p);
            if (min != null && max != null) {
                w.write(0x03);
                numWriter.accept(w, min);
                numWriter.accept(w, max);
            } else if (min != null) {
                w.write(0x01);
                numWriter.accept(w, min);
            } else if (max != null) {
                w.write(0x02);
                numWriter.accept(w, max);
            } else {
                w.write(0x00);
            }
        }));
    }

    private GraphConverter() {
        //no instance
    }

    @Contract("_, _ -> new")
    public static DeclareCommandsPacket createPacket(Graph graph, @Nullable Player player) {
        List<DeclareCommandsPacket.Node> nodes = new ArrayList<>();
        List<BiConsumer<Graph, Integer>> redirects = new ArrayList<>();
        Map<Arg<?>, Integer> argToPacketId = new HashMap<>();
        final AtomicInteger idSource = new AtomicInteger(0);
        final int rootId = append(graph.root(), nodes, redirects, idSource, null, player, argToPacketId)[0];
        for (var r : redirects) {
            r.accept(graph, rootId);
        }
        return new DeclareCommandsPacket(nodes, rootId);
    }

    private static int[] append(Graph.Node graphNode, List<DeclareCommandsPacket.Node> to,
                                List<BiConsumer<Graph, Integer>> redirects, AtomicInteger id, @Nullable AtomicInteger redirect,
                                @Nullable Player player, Map<Arg<?>, Integer> argToPacketId) {
        final Graph.Execution execution = graphNode.execution();
        if (player != null && execution != null) {
            if (!execution.test(player)) return new int[0];
        }

        final Arg<?> arg = graphNode.argument();
        final Parser<?> parser = arg.parser();
        final List<Graph.Node> children = graphNode.next();

        final DeclareCommandsPacket.Node node = new DeclareCommandsPacket.Node();
        int[] packetNodeChildren = new int[children.size()];
        for (int i = 0, appendIndex = 0; i < children.size(); i++) {
            final int[] append = append(children.get(i), to, redirects, id, redirect, player, argToPacketId);
            if (append.length > 0) {
                argToPacketId.put(children.get(i).argument(), append[0]);
            }
            if (append.length == 1) {
                packetNodeChildren[appendIndex++] = append[0];
            } else {
                packetNodeChildren = Arrays.copyOf(packetNodeChildren, packetNodeChildren.length + append.length - 1);
                System.arraycopy(append, 0, packetNodeChildren, appendIndex, append.length);
                appendIndex += append.length;
            }
        }
        node.children = packetNodeChildren;
        if (parser instanceof Parser.LiteralParser literal) {
            if (literal.literal().isEmpty()) {
                node.flags = 0; //root
            } else {
                node.flags = literal(false, false);
                node.name = arg.id();
                if (redirect != null) {
                    node.flags |= 0x8;
                    redirects.add((graph, root) -> node.redirectedNode = redirect.get());
                }
            }
            to.add(node);
            return new int[]{id.getAndIncrement()};
        } else if (parser instanceof Parser.LiteralsParser literalsArg) {
            return spreadLiteral(to, redirects, redirect, node, literalsArg.literals(), id);
        } else {
            if (parser.spec() instanceof ParserSpecImpl.Legacy<?> legacyArg) {
                final Argument<?> argument = legacyArg.argument();
                if (argument instanceof ArgumentLiteral literal) {
                    if (literal.getId().isEmpty()) {
                        node.flags = 0; //root
                    } else {
                        node.flags = literal(false, false);
                        node.name = argument.getId();
                        if (redirect != null) {
                            node.flags |= 0x8;
                            redirects.add((graph, root) -> node.redirectedNode = redirect.get());
                        }
                    }
                    to.add(node);
                    return new int[]{id.getAndIncrement()};
                } else if (argument instanceof ArgumentCommand argCmd) {
                    node.flags = literal(false, true);
                    node.name = argument.getId();
                    final String shortcut = argCmd.getShortcut();
                    if (shortcut.isEmpty()) {
                        redirects.add((graph, root) -> node.redirectedNode = root);
                    } else {
                        redirects.add((graph, root) -> {
                            node.redirectedNode = argToPacketId.get(findRedirectTargetForArgCmdShortcut(graph, argCmd.getShortcut()));
                        });
                    }
                    to.add(node);

                    return new int[]{id.getAndIncrement()};
                } else if (argument instanceof ArgumentEnum<?> || (argument instanceof ArgumentWord word && word.hasRestrictions())) {
                    return spreadLiteral(to, redirects, redirect, node, argument instanceof ArgumentEnum<?> ?
                            ((ArgumentEnum<?>) argument).entries() :
                            Arrays.stream(((ArgumentWord) argument).getRestrictions()).toList(), id);
                } else if (argument instanceof ArgumentGroup special) {
                    List<Argument<?>> entries = special.group();
                    int[] res = null;
                    int[] last = new int[0];
                    for (int i = 0; i < entries.size(); i++) {
                        Arg<?> entry = ArgImpl.fromLegacy(entries.get(i));
                        if (i == entries.size() - 1) {
                            // Last will be the parent of next args
                            final int[] l = append(new GraphImpl.NodeImpl(entry, null, List.of()), to, redirects,
                                    id, redirect, player, argToPacketId);
                            for (int n : l) {
                                to.get(n).children = node.children;
                            }
                            for (int n : last) {
                                to.get(n).children = l;
                            }
                            return res == null ? l : res;
                        } else if (i == 0) {
                            // First will be the children & parent of following
                            res = append(new GraphImpl.NodeImpl(entry, null, List.of()), to, redirects, id,
                                    null, player, argToPacketId);
                            last = res;
                        } else {
                            final int[] l = append(new GraphImpl.NodeImpl(entry, null, List.of()), to, redirects,
                                    id, null, player, argToPacketId);
                            for (int n : last) {
                                to.get(n).children = l;
                            }
                            last = l;
                        }
                    }
                    throw new RuntimeException("Arg group must have child args.");
                } else if (argument instanceof ArgumentLoop<?> special) {
                    AtomicInteger r = new AtomicInteger();
                    int[] res = new int[special.arguments().size()];
                    List<? extends Argument<?>> arguments = special.arguments();
                    for (int i = 0, appendIndex = 0; i < arguments.size(); i++) {
                        final int[] append = append(new GraphImpl.NodeImpl(ArgImpl.fromLegacy(arguments.get(i)), null, List.of()), to,
                                redirects, id, r, player, argToPacketId);
                        if (append.length == 1) {
                            res[appendIndex++] = append[0];
                        } else {
                            res = Arrays.copyOf(res, res.length + append.length - 1);
                            System.arraycopy(append, 0, res, appendIndex, append.length);
                            appendIndex += append.length;
                        }
                    }
                    r.set(id.get());
                    return res;
                } else {
                    // Normal legacy arg
                    final boolean hasSuggestion = argument.hasSuggestion();
                    node.flags = arg(false, hasSuggestion);
                    node.name = argument.getId();
                    node.parser = argument.parser();
                    node.properties = argument.nodeProperties();
                    if (redirect != null) {
                        node.flags |= 0x8;
                        redirects.add((graph, root) -> node.redirectedNode = redirect.get());
                    }
                    if (hasSuggestion) {
                        node.suggestionsType = argument.suggestionType().getIdentifier();
                    }
                    to.add(node);
                    return new int[]{id.getAndIncrement()};
                }
            } else {
                // Normal arg
                final boolean hasSuggestion = arg.suggestionType() != null;
                node.flags = arg(false, hasSuggestion);
                node.name = arg.id();
                node.parser = getParserName(arg);
                node.properties = getProperties(arg);
                if (redirect != null) {
                    node.flags |= 0x8;
                    redirects.add((graph, root) -> node.redirectedNode = redirect.get());
                }
                if (hasSuggestion) {
                    node.suggestionsType = arg.suggestionType().name();
                }
                to.add(node);
                return new int[]{id.getAndIncrement()};
            }
        }
    }

    private static int[] spreadLiteral(List<DeclareCommandsPacket.Node> nodeList,
                                       List<BiConsumer<Graph, Integer>> redirects,
                                       @Nullable AtomicInteger redirect,
                                       DeclareCommandsPacket.Node node,
                                       Collection<String> entries,
                                       AtomicInteger id) {
        final int[] res = new int[entries.size()];
        int i = 0;
        for (String entry : entries) {
            final DeclareCommandsPacket.Node subNode = new DeclareCommandsPacket.Node();
            subNode.children = node.children;
            subNode.flags = literal(false, false);
            subNode.name = entry;
            if (redirect != null) {
                subNode.flags |= 0x8;
                redirects.add((graph, root) -> subNode.redirectedNode = redirect.get());
            }
            nodeList.add(subNode);
            res[i++] = id.getAndIncrement();
        }
        return res;
    }

    private static byte literal(boolean executable, boolean hasRedirect) {
        return DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.LITERAL, executable, hasRedirect, false);
    }

    private static byte arg(boolean executable, boolean hasSuggestion) {
        return DeclareCommandsPacket.getFlag(DeclareCommandsPacket.NodeType.ARGUMENT, executable, false, hasSuggestion);
    }

    private static byte @Nullable [] getProperties(Arg<?> arg) {
        final Parser<?> parser = arg.parser();
        if (parser.spec() instanceof ParserSpecImpl.Legacy<?> legacy) {
            return legacy.argument().nodeProperties();
        } else {
            final Function<Parser<?>, byte[]> parserFunction = propertiesFunctions.get(parser.getClass().getInterfaces()[0]);
            if (parserFunction == null) return null;
            return parserFunction.apply(parser);
        }
    }

    private static String getParserName(Arg<?> arg) {
        if (arg.parser().spec() instanceof ParserSpecImpl.Legacy<?> legacy) {
            return legacy.argument().parser();
        } else {
            final Class<?> parserClass = arg.parser().getClass().getInterfaces()[0];
            final String s = parserNames.get(parserClass);
            if (s == null) throw new RuntimeException("Unsupported parser type: " + parserClass.getSimpleName());
            return s;
        }
    }

    private static Arg<?> findRedirectTargetForArgCmdShortcut(Graph graph, String shortcut) {
        // TODO verify if this works as intended in every case
        final List<Arg<?>> args = CommandParser.parser().parse(graph, shortcut).args();
        return args.get(args.size() - 1);
    }
}
