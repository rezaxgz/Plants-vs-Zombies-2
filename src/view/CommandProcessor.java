package view;

import java.util.regex.Matcher;

import commands.Command;
import commands.CommandRegistry;
import model.CommandResult;

public class CommandProcessor {
    private final CommandRegistry registry;

    public CommandProcessor(CommandRegistry registry) {
        this.registry = registry;
    }

    public CommandResult process(String menuId, String input) {
        for (Command<CommandResult> cmd : registry.getCommands(menuId)) {
            Matcher m = cmd.getMatcher(input.trim());
            if (m.matches()) {
                return cmd.getAction().execute(m);
            }
        }
        return CommandResult.error("unknown command!");
    }
}