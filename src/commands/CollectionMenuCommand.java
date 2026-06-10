package commands;

public enum CollectionMenuCommand implements Command {
    ;
    private final String pattern;

    CollectionMenuCommand(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return this.pattern;
    }
}
