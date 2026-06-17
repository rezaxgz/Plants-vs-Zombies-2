package model;

public class CommandResult {
    private String message;
    private boolean isSuccsesful;

    private CommandResult(String message, boolean isSuccsesful) {
        this.message = message;
        this.isSuccsesful = isSuccsesful;
    }

    public static CommandResult success(String message) {
        return new CommandResult(message, true);
    }

    public static CommandResult error(String message) {
        return new CommandResult(message, false);
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccsesful() {
        return isSuccsesful;
    }

}
