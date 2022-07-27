package net.minestom.server.command;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.ArgumentCallback;
import net.minestom.server.command.builder.CommandContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.function.Supplier;

interface Arg<T> {
    static <T> @NotNull Arg<T> arg(@NotNull String id, @NotNull Parser<T> parser, @Nullable Suggestion.Type suggestionType) {
        return new ArgImpl<>(id, parser, suggestionType, null, null);
    }

    static <T> @NotNull Arg<T> arg(@NotNull String id, @NotNull Parser<T> parser) {
        return arg(id, parser, null);
    }

    static @NotNull Arg<String> literalArg(@NotNull String id) {
        return arg(id, Parser.Literal(id), null);
    }

    @NotNull String id();

    @NotNull Parser<T> parser();

    Suggestion.@UnknownNullability Type suggestionType();

    @Nullable Supplier<@NotNull T> defaultValue();

    @NotNull Arg<T> defaultValue(@Nullable Supplier<@NotNull T> defaultValue);

    default @NotNull Arg<T> defaultValue(@NotNull T defaultValue) {
        return defaultValue(() -> defaultValue);
    }

    @ApiStatus.Experimental
    @Nullable ArgumentCallback callback();

    @ApiStatus.Experimental
    @NotNull Arg<T> callback(@Nullable ArgumentCallback callback);

    interface Suggestion {
        sealed interface Type
                permits ArgImpl.SuggestionTypeImpl {
            @NotNull String name();

            @NotNull Entry suggest(@NotNull CommandSender sender, @NotNull CommandContext context);

            static @NotNull Type recipes() {
                return ArgImpl.SuggestionTypeImpl.RECIPES;
            }

            static @NotNull Type sounds() {
                return ArgImpl.SuggestionTypeImpl.SOUNDS;
            }

            static @NotNull Type entities() {
                return ArgImpl.SuggestionTypeImpl.ENTITIES;
            }

            static @NotNull Type askServer(@NotNull Callback callback) {
                return ArgImpl.SuggestionTypeImpl.askServer(callback);
            }
        }

        sealed interface Entry
                permits ArgImpl.SuggestionEntryImpl {
            static @NotNull Entry of(int start, int length, @NotNull List<Match> matches) {
                return new ArgImpl.SuggestionEntryImpl(start, length, matches);
            }

            int start();

            int length();

            @NotNull List<@NotNull Match> matches();

            sealed interface Match
                    permits ArgImpl.MatchImpl {
                @NotNull String text();

                @Nullable Component tooltip();
            }
        }

        @FunctionalInterface
        interface Callback {
            @NotNull Entry apply(@NotNull CommandSender sender, @NotNull CommandContext context);
        }
    }
}
