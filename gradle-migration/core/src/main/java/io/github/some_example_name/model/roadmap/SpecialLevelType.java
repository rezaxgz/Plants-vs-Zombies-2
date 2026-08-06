package io.github.some_example_name.model.roadmap;

/**
 * The eight special-level types required by the adventure specification.
 */
public enum SpecialLevelType {
    NONE("Normal"),
    CONVEYOR_BELT("Conveyor Belt"),
    LOCKED_PLANTS("Locked Plants"),
    SAVE_OUR_SEEDS("Save Our Seeds"),
    TIMED_WAR("Timed War"),
    NIGHT_OPS("Night Ops"),
    DEAD_LINE("Dead Line"),
    LOVE_YOUR_PLANTS("Love Your Plants"),
    PLANT_WHAT_YOU_GET("Plant What You Get");

    private final String displayName;

    SpecialLevelType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSpecial() {
        return this != NONE;
    }
}
