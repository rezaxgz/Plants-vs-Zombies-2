package commands;

public class GameMenuCommand implements Command {
    ;
    private final String pattern;

    GameMenuCommand(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return this.pattern;
    }
}
