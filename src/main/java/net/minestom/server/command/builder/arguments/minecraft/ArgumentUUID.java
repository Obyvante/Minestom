package net.minestom.server.command.builder.arguments.minecraft;

import net.minestom.server.command.CommandReader;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ArgumentUUID extends Argument<UUID> {

    public static final int INVALID_UUID = -1;

    public ArgumentUUID(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull UUID parse(CommandReader reader) throws ArgumentSyntaxException {
        final String input = reader.getWord();
        try {
            final UUID uuid = UUID.fromString(input);
            reader.consume();
            return uuid;
        } catch (IllegalArgumentException exception) {
            throw new ArgumentSyntaxException("Invalid UUID", input, INVALID_UUID);
        }
    }

    @Override
    public String parser() {
        return "minecraft:uuid";
    }

    @Override
    public String toString() {
        return String.format("UUID<%s>", getId());
    }
}
