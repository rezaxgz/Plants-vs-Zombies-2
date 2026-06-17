package commands;

import java.util.*;

import model.CommandResult;

public class CommandRegistry {
    private final Map<String, List<Command<CommandResult>>> menus = new HashMap<>();

    public CommandRegistry() {
        registerMenu("main", MainMenuCommand.class);
        registerMenu("game", GameMenuCommand.class);
        registerMenu("collection", CollectionMenuCommand.class);
        registerMenu("login", LoginMenuCommand.class);
        registerMenu("signup", SignUpMenuCommand.class);
    }

    private <E extends Enum<E> & Command<CommandResult>> void registerMenu(String menuId, Class<E> commandEnum) {
        List<Command<CommandResult>> commands = new ArrayList<>();
        for (E cmd : commandEnum.getEnumConstants()) {
            commands.add(cmd);
        }
        menus.put(menuId, Collections.unmodifiableList(commands));
    }

    public List<Command<CommandResult>> getCommands(String menuId) {
        return menus.getOrDefault(menuId, List.of());
    }
}