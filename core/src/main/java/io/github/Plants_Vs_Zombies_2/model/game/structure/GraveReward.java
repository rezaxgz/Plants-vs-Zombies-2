package io.github.Plants_Vs_Zombies_2.model.game.structure;

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
