package net.minestom.server.command;

import net.minestom.server.command.Graph.Node;
import net.minestom.server.command.builder.ArgumentCallback;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.CommandData;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class CommandParserImpl implements CommandParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandParserImpl.class);
    static final CommandParserImpl PARSER = new CommandParserImpl();

    static final class Chain {
        CommandExecutor defaultExecutor = null;
        final ArrayDeque<NodeResult> nodeResults = new ArrayDeque<>();
        final List<CommandCondition> conditions = new ArrayList<>();
        final List<CommandExecutor> globalListeners = new ArrayList<>();

        void append(NodeResult result) {
            this.nodeResults.add(result);
            final Graph.Execution execution = result.node.execution();
            if (execution != null) {
                // Create condition chain
                final CommandCondition condition = execution.condition();
                if (condition != null) conditions.add(condition);
                // Track default executor
                final CommandExecutor defExec = execution.defaultExecutor();
                if (defExec != null) defaultExecutor = defExec;
                // Merge global listeners
                final CommandExecutor globalListener = execution.globalListener();
                if (globalListener != null) globalListeners.add(globalListener);
            }
        }

        CommandCondition mergedConditions() {
            return (sender, commandString) -> {
                for (CommandCondition condition : conditions) {
                    if (!condition.canUse(sender, commandString)) return false;
                }
                return true;
            };
        }

        CommandExecutor mergedGlobalExecutors() {
            return (sender, context) -> globalListeners.forEach(x -> x.apply(sender, context));
        }

        Arg.Suggestion.Type extractSuggestion() {
            return nodeResults.peekLast().suggestionType;
        }

        Map<String, ArgumentResult<Object>> collectArguments() {
            return nodeResults.stream()
                    .skip(1) // skip root
                    .collect(Collectors.toUnmodifiableMap(NodeResult::name, NodeResult::argumentResult));
        }

        List<Arg<?>> getArgs() {
            return nodeResults.stream().map(x -> x.node.argument()).collect(Collectors.toList());
        }
    }

    @Override
    public @NotNull CommandParser.Result parse(@NotNull Graph graph, @NotNull String input) {
        final CommandStringReader reader = new CommandStringReader(input);
        final Chain chain = new Chain();
        // Read from input
        NodeResult result;
        Node parent = graph.root();
        while ((result = parseChild(parent, reader)) != null) {
            chain.append(result);
            if (result.argumentResult instanceof ArgumentResult.SyntaxError<?> e) {
                // Syntax error stop at this arg
                final ArgumentCallback argumentCallback = null;
                if (argumentCallback == null && chain.defaultExecutor != null) {
                    return ValidCommand.defaultExecutor(input, chain);
                } else {
                    return new InvalidCommand(input, chain.mergedConditions(),
                            argumentCallback, e, chain.collectArguments(), chain.mergedGlobalExecutors(),
                            chain.extractSuggestion(), chain.getArgs());
                }
            }
            parent = result.node;
        }
        // Check children for arguments with default values
        do {
            Node tmp = parent;
            parent = null;
            for (Node child : tmp.next()) {
                final Arg<?> argument = child.argument();
                final Supplier<?> defaultSupplier = null;
                if (defaultSupplier != null) {
                    final Object value = defaultSupplier.get();
                    final ArgumentResult<Object> argumentResult = new ArgumentResult.Success<>(value, "");
                    chain.append(new NodeResult(child, argumentResult, null));
                    parent = child;
                    break;
                }
            }
        } while (parent != null);
        // Check if any syntax has been found
        final NodeResult lastNode = chain.nodeResults.peekLast();
        if (lastNode == null) return UnknownCommandResult.INSTANCE;
        // Verify syntax(s)
        final CommandExecutor executor = nullSafeGetter(lastNode.node().execution(), Graph.Execution::executor);
        if (executor == null) {
            // Syntax error
            if (chain.defaultExecutor != null) {
                return ValidCommand.defaultExecutor(input, chain);
            } else {
                return InvalidCommand.invalid(input, chain);
            }
        }
        if (reader.hasRemaining()) {
            // Command had trailing data
            if (chain.defaultExecutor != null) {
                return ValidCommand.defaultExecutor(input, chain);
            } else {
                return InvalidCommand.invalid(input, chain);
            }
        }
        return ValidCommand.executor(input, chain, executor);
    }

    @Contract("null, _ -> null; !null, null -> fail; !null, !null -> _")
    private static <R, T> @Nullable R nullSafeGetter(@Nullable T obj, Function<T, R> getter) {
        return obj == null ? null : getter.apply(obj);
    }

    private static NodeResult parseChild(Node parent, CommandStringReader reader) {
        if (!reader.hasRemaining()) return null;
        for (Node child : parent.next()) {
            final Arg<?> argument = child.argument();
            final int start = reader.cursor();
            final ArgumentResult<?> parse = parse(argument, reader);
            if (parse instanceof ArgumentResult.Success<?> success) {
                return new NodeResult(child, (ArgumentResult<Object>) success,
                        null);
            } else if (parse instanceof ArgumentResult.SyntaxError<?> syntaxError) {
                return new NodeResult(child, (ArgumentResult<Object>) syntaxError,
                        null);
            } else {
                // Reset cursor & try next
                reader.cursor(start);
            }
        }
        for (Node node : parent.next()) {
            final Arg.Suggestion.Type suggestionType = node.argument().suggestionType();
            if (suggestionType != null) {
                return new NodeResult(parent,
                        new ArgumentResult.SyntaxError<>("None of the arguments were compatible, but a suggestion callback was found.", "", -1),
                        suggestionType);
            }
        }
        return null;
    }

    record UnknownCommandResult() implements Result.UnknownCommand {
        private static final Result INSTANCE = new UnknownCommandResult();

        @Override
        public @NotNull ExecutableCommand executable() {
            return UnknownExecutableCmd.INSTANCE;
        }

        @Override
        public @Nullable Suggestion suggestion(CommandSender sender) {
            return null;
        }

        @Override
        public List<Arg<?>> args() {
            return null;
        }
    }

    sealed interface InternalKnownCommand extends Result.KnownCommand {
        String input();

        @Nullable CommandCondition condition();

        @NotNull Map<String, ArgumentResult<Object>> arguments();

        CommandExecutor globalListener();

        @Nullable Arg.Suggestion.Type suggestionType();

        @Override
        default @Nullable Suggestion suggestion(CommandSender sender) {
            final Arg.Suggestion.Type suggestionType = suggestionType();
            if (suggestionType == null) return null;
            final CommandContext context = createCommandContext(input(), arguments());
            final Arg.Suggestion.Entry result = suggestionType.suggest(sender, context);

            Suggestion suggestion = new Suggestion(input(), result.start(), result.length());
            for (var match : result.matches()) {
                suggestion.addEntry(new SuggestionEntry(match.text(), match.tooltip()));
            }
            return suggestion;
        }
    }

    record InvalidCommand(String input, CommandCondition condition, ArgumentCallback callback,
                          ArgumentResult.SyntaxError<?> error,
                          @NotNull Map<String, ArgumentResult<Object>> arguments, CommandExecutor globalListener,
                          @Nullable Arg.Suggestion.Type suggestionType, List<Arg<?>> args)
            implements InternalKnownCommand, Result.KnownCommand.Invalid {

        static InvalidCommand invalid(String input, Chain chain) {
            return new InvalidCommand(input, chain.mergedConditions(),
                    null/*todo command syntax callback*/,
                    new ArgumentResult.SyntaxError<>("Command has trailing data.", null, -1),
                    chain.collectArguments(), chain.mergedGlobalExecutors(), chain.extractSuggestion(), chain.getArgs());
        }

        @Override
        public @NotNull ExecutableCommand executable() {
            return new InvalidExecutableCmd(condition, globalListener, callback, error, input, arguments);
        }
    }

    record ValidCommand(String input, CommandCondition condition, CommandExecutor executor,
                        @NotNull Map<String, ArgumentResult<Object>> arguments,
                        CommandExecutor globalListener, @Nullable Arg.Suggestion.Type suggestionType,
                        List<Arg<?>> args)
            implements InternalKnownCommand, Result.KnownCommand.Valid {

        static ValidCommand defaultExecutor(String input, Chain chain) {
            return new ValidCommand(input, chain.mergedConditions(), chain.defaultExecutor, chain.collectArguments(),
                    chain.mergedGlobalExecutors(), chain.extractSuggestion(), chain.getArgs());
        }

        static ValidCommand executor(String input, Chain chain, CommandExecutor executor) {
            return new ValidCommand(input, chain.mergedConditions(), executor, chain.collectArguments(), chain.mergedGlobalExecutors(),
                    chain.extractSuggestion(), chain.getArgs());
        }

        @Override
        public @NotNull ExecutableCommand executable() {
            return new ValidExecutableCmd(condition, globalListener, executor, input, arguments);
        }
    }

    record UnknownExecutableCmd() implements ExecutableCommand {
        static final ExecutableCommand INSTANCE = new UnknownExecutableCmd();

        @Override
        public @NotNull Result execute(@NotNull CommandSender sender) {
            return ExecutionResultImpl.UNKNOWN;
        }
    }

    record ValidExecutableCmd(CommandCondition condition, CommandExecutor globalListener, CommandExecutor executor,
                              String input,
                              Map<String, ArgumentResult<Object>> arguments) implements ExecutableCommand {
        @Override
        public @NotNull Result execute(@NotNull CommandSender sender) {
            final CommandContext context = createCommandContext(input, arguments);

            globalListener().apply(sender, context);

            if (condition != null && !condition.canUse(sender, input())) {
                return ExecutionResultImpl.PRECONDITION_FAILED;
            }
            try {
                executor().apply(sender, context);
                return new ExecutionResultImpl(ExecutableCommand.Result.Type.SUCCESS, context.getReturnData());
            } catch (Exception e) {
                LOGGER.error("An exception was encountered while executing command: " + input(), e);
                return ExecutionResultImpl.EXECUTOR_EXCEPTION;
            }
        }
    }

    record InvalidExecutableCmd(CommandCondition condition, CommandExecutor globalListener, ArgumentCallback callback,
                                ArgumentResult.SyntaxError<?> error, String input,
                                Map<String, ArgumentResult<Object>> arguments) implements ExecutableCommand {
        @Override
        public @NotNull Result execute(@NotNull CommandSender sender) {
            globalListener().apply(sender, createCommandContext(input, arguments));

            if (condition != null && !condition.canUse(sender, input())) {
                return ExecutionResultImpl.PRECONDITION_FAILED;
            }
            if (callback != null)
                callback.apply(sender, new ArgumentSyntaxException(error.message(), error.input(), error.code()));
            return ExecutionResultImpl.INVALID_SYNTAX;
        }
    }

    private static CommandContext createCommandContext(String input, Map<String, ArgumentResult<Object>> arguments) {
        final CommandContext context = new CommandContext(input);
        for (var entry : arguments.entrySet()) {
            final String identifier = entry.getKey();
            final ArgumentResult<Object> value = entry.getValue();

            final Object argOutput = value instanceof ArgumentResult.Success<Object> success ? success.value() : null;
            final String argInput = value instanceof ArgumentResult.Success<Object> success ? success.input() : "";

            context.setArg(identifier, argOutput, argInput);
        }
        return context;
    }

    record ExecutionResultImpl(Type type, CommandData commandData) implements ExecutableCommand.Result {
        static final ExecutableCommand.Result CANCELLED = new ExecutionResultImpl(Type.CANCELLED, null);
        static final ExecutableCommand.Result UNKNOWN = new ExecutionResultImpl(Type.UNKNOWN, null);
        static final ExecutableCommand.Result EXECUTOR_EXCEPTION = new ExecutionResultImpl(Type.EXECUTOR_EXCEPTION, null);
        static final ExecutableCommand.Result PRECONDITION_FAILED = new ExecutionResultImpl(Type.PRECONDITION_FAILED, null);
        static final ExecutableCommand.Result INVALID_SYNTAX = new ExecutionResultImpl(Type.INVALID_SYNTAX, null);
    }

    private record NodeResult(Node node, ArgumentResult<Object> argumentResult, Arg.Suggestion.Type suggestionType) {
        public String name() {
            return node.argument().id();
        }
    }

    static final class CommandStringReader {
        private final String input;
        private int cursor = 0;

        CommandStringReader(String input) {
            this.input = input;
        }

        boolean hasRemaining() {
            return cursor < input.length();
        }

        int cursor() {
            return cursor;
        }

        void cursor(int cursor) {
            assert cursor >= 0 && cursor <= input.length();
            this.cursor = cursor;
        }
    }

    // ARGUMENT

    private static <T> ArgumentResult<T> parse(Arg<T> argument, CommandStringReader reader) {
        final String input = reader.input;
        final ParserSpec.Result<T> result = argument.parser().spec().read(input, reader.cursor);
        if (result != null) {
            int index = result.index();
            assert index >= 0 && index <= input.length() : "index out of bounds: " + index + " > " + input.length() + " for " + input;
            assert index == input.length() ||
                    input.charAt(index) == ' ' : "Invalid result index: " + index + " '" + input.charAt(index) + "'" + " '" + input + "'";
            // Find next non-space index from input
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
            reader.cursor(index);
            return new ArgumentResult.Success<>(result.value(), result.input());
        } else {
            return new ArgumentResult.IncompatibleType<>();
        }
    }

    private sealed interface ArgumentResult<R> {
        record Success<T>(T value, String input)
                implements ArgumentResult<T> {
        }

        record IncompatibleType<T>()
                implements ArgumentResult<T> {
        }

        record SyntaxError<T>(String message, String input, int code)
                implements ArgumentResult<T> {
        }
    }
}
