package io.github.some_example_name.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.some_example_name.model.CommandResult;

public class CommandRegistry {
    private final Map<String, List<Command<CommandResult>>> menus = new HashMap<>();

    public CommandRegistry() {
        registerMenu("main", MainMenuCommand.class);
        registerMenu("game", GameMenuCommand.class);
        registerMenu("plantselection", PlantSelectionMenuCommand.class);
        registerMenu("collection", CollectionMenuCommand.class);
        registerMenu("login", LoginMenuCommand.class);
        registerMenu("signup", SignUpMenuCommand.class);
        registerMenu("greenhouse", GreenhouseMenuCommand.class);
        registerMenu("shop", ShopMenuCommand.class);
        registerMenu("leaderboard", LeaderboardMenuCommand.class);
        registerMenu("travellog", TravelLogMenuCommand.class);
        registerMenu("news", NewsMenuCommand.class);
        registerMenu("settings", SettingsMenuCommand.class);
        registerMenu("profile", ProfileMenuCommand.class);
    }

    private <E extends Enum<E> & Command<CommandResult>> void registerMenu(
            String menuId, Class<E> commandEnum) {
        menus.put(menuId, commandsFrom(commandEnum));
    }

    private static <E extends Enum<E> & Command<CommandResult>> List<Command<CommandResult>> commandsFrom(
            Class<E> commandEnum) {
        List<Command<CommandResult>> commands = new ArrayList<>();
        for (E command : commandEnum.getEnumConstants()) {
            commands.add(command);
        }
        return Collections.unmodifiableList(commands);
    }

    public List<Command<CommandResult>> getCommands(String menuId) {
        List<Command<CommandResult>> commands = new ArrayList<>();
        commands.add(MenuCommand.SHOW_CURRENT);
        commands.add(MenuCommand.EXIT);
        commands.addAll(menus.getOrDefault(menuId, List.of()));
        commands.add(MenuCommand.ENTER);
        return Collections.unmodifiableList(commands);
    }
}