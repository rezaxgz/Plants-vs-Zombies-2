package io.github.some_example_name.model.game;

import java.util.Locale;

/**
 * Chapter-wide terrain and spawning rules applied to an adventure level.
 */
public enum ChapterRuleset {
    NONE,
    ANCIENT_EGYPT,
    FROSTBITE_CAVES,
    BIG_WAVE_BEACH,
    DARK_AGES;

    public static ChapterRuleset fromTheme(String theme) {
        if (theme == null) {
            return NONE;
        }
        switch (theme.toLowerCase(Locale.ROOT)) {
            case "egypt":
                return ANCIENT_EGYPT;
            case "iceage":
                return FROSTBITE_CAVES;
            case "beach":
                return BIG_WAVE_BEACH;
            case "dark":
                return DARK_AGES;
            default:
                return NONE;
        }
    }
}
