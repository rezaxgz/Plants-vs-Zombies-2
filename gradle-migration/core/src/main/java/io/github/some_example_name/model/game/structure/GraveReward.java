package io.github.some_example_name.model.game.structure;

public enum GraveReward {
    NONE("empty"),
    SUN("50 sun"),
    PLANT_FOOD("one plant food");

    private final String description;

    GraveReward(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
