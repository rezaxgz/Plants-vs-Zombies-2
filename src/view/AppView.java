package view;

import java.util.Scanner;

public class AppView {
    private Scanner scanner;

    public String getInput() {
        return scanner.nextLine();
    }

    public void run() {

    }

    public void parseCommand(String command) {

    }

    public static void printError(String error) {
        System.err.println(error);
    }

    public static void printOutput(String output) {
        System.out.println(output);
    }
}
