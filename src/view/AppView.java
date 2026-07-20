package view;

import java.util.Scanner;

import commands.CommandRegistry;
import model.App;
import model.CommandResult;
import model.auth.UserManager;
import model.menu.MainMenu;
import model.news.NewsMessages;

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
