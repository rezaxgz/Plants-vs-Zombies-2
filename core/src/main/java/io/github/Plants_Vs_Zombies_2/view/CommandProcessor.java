package io.github.Plants_Vs_Zombies_2.view;

import java.util.regex.Matcher;

import io.github.Plants_Vs_Zombies_2.commands.Command;
import io.github.Plants_Vs_Zombies_2.commands.CommandRegistry;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;

public class CommandProcessor {
    private final CommandRegistry registry;

    public CommandProcessor(CommandRegistry registry) {
        this.registry = registry;
    }

    public CommandResult process(String menuId, String input) {
        for (Command<CommandResult> cmd : registry.getCommands(menuId)) {
            Matcher m = cmd.getMatcher(input.trim());
            if (m != null) {
                return cmd.getAction().execute(m);
            }
        }
        return CommandResult.error("unknown command!");
    }
}