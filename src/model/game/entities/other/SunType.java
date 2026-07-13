package model.game.entities.other;

public enum SunType {
    NORMAL("normal", 25),
    SPECIAL("special", 100);

    private final String displayName;
    private final int amount;

    SunType(String displayName, int amount) {
        this.displayName = displayName;
        this.amount = amount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getAmount() {
        return amount;
    }
}
