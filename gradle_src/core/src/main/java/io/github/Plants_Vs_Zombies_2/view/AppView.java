package io.github.Plants_Vs_Zombies_2.view;

import java.util.Scanner;

import io.github.Plants_Vs_Zombies_2.commands.CommandRegistry;
import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.menu.MainMenu;
import io.github.Plants_Vs_Zombies_2.model.news.NewsMessages;

public class AppView {
    private static final AppView instance = new AppView();

    private final Scanner scanner = new Scanner(System.in);
    private final CommandProcessor commandProcessor = new CommandProcessor(new CommandRegistry());

    public static AppView getInstance() {
        return instance;
    }

    public String getInput() {
        return scanner.nextLine();
    }

    public boolean hasNext() {
        return scanner.hasNextLine();
    }

    public void run() {
        App app = App.getInstance();
        showStartupNewsBadge(app);
        try {
            while (app.isRunning() && hasNext()) {
                parseCommand(getInput());
            }
        } finally {
            UserManager.saveAllUsers();
        }
    }

    private static void showStartupNewsBadge(App app) {
        if (!(app.getCurrentMenu() instanceof MainMenu)) {
            return;
        }
        String badge = NewsMessages.unreadBadge(app.getLoggedInUser());
        if (!badge.isBlank()) {
            printOutput(badge);
        }
    }

    public void parseCommand(String command) {
        CommandResult result = commandProcessor.process(
                App.getInstance().getCurrentMenu().getName(), command);
        String output = result.getMessage();
        if (!output.isBlank()) {
            printOutput(output);
        }
    }

    public static void printError(String error) {
        System.err.println(error);
    }

    public static void printOutput(String output) {
        System.out.println(output);
    }
}
