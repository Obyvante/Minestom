package net.minestom.server.command;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

interface Arg<T> {
    static <T> @NotNull Arg<T> arg(@NotNull String id, @NotNull Parser<T> parser, @Nullable Suggestion.Type suggestionType) {
        return new ArgImpl<>(id, parser, suggestionType);
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

    interface Suggestion {
        interface Type {
            @NotNull String name();

            @NotNull Entry suggest(@NotNull CommandSender sender, @NotNull String input);

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

        interface Entry {
            static @NotNull Entry of(int start, int length, @NotNull List<Match> matches) {
                return new ArgImpl.SuggestionEntryImpl(start, length, matches);
            }

            int start();

            int length();

            @NotNull List<@NotNull Match> matches();

            interface Match {
                @NotNull String text();

                @Nullable Component tooltip();
            }
        }

        @FunctionalInterface
        interface Callback {
            @NotNull Entry apply(@NotNull CommandSender sender, @NotNull String input);
        }
    }
}
