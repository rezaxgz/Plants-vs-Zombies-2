package io.github.some_example_name.model.enums;

public enum Gender {
    MALE, FEMALE;

    public static Gender getByName(String name) {
        if (name.equalsIgnoreCase("male"))
            return MALE;
        if (name.equalsIgnoreCase("female"))
            return FEMALE;
        return null;
    }
}
