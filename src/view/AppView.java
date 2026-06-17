package view;

import java.util.Scanner;

import commands.CommandRegistry;
import model.App;
import model.CommandResult;

public class AppView {
    private Scanner scanner = new Scanner(System.in);
    private CommandProcessor commandProcessor = new CommandProcessor(new CommandRegistry());

    private String getInput() {
        return scanner.nextLine();
    }

    private boolean hasNext() {
        return scanner.hasNextLine();
    }

    public void run() {
        while (hasNext()) {
            parseCommand(getInput());
        }
    }

    public void parseCommand(String command) {
        CommandResult res = commandProcessor.process(App.getInstance().getCurrentMenu().getName(), command);
        printOutput(res.getMessage());
    }

    public static void printError(String error) {
        System.err.println(error);
    }

    public static void printOutput(String output) {
        System.out.println(output);
    }
}
