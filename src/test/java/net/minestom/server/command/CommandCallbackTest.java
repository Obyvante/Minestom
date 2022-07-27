package net.minestom.server.command;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommandCallbackTest {

    @Test
    public void argCallback() {
        var command = new Command("name");
        var arg = ArgumentType.Integer("number");

        AtomicInteger callback = new AtomicInteger(-1);

        command.setDefaultExecutor((sender, context) -> callback.set(0));

        arg.setCallback((sender, exception) -> callback.set(1));
        command.addSyntax((sender, context) -> callback.set(2), arg);

        var manager = new CommandManager();
        manager.register(command);

        manager.executeServerCommand("name a");
        assertEquals(1, callback.get());

        callback.set(-1);
        manager.executeServerCommand("name 1");
        assertEquals(2, callback.get());
    }
}
