package commands;

public class LoginMenuCommand implements Command {
    ;
    private final String pattern;

    LoginMenuCommand(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return this.pattern;
    }
}
