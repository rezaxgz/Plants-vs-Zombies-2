package io.github.Plants_Vs_Zombies_2.model.roadmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.Constants;
import io.github.Plants_Vs_Zombies_2.model.game.ChapterRuleset;
import io.github.Plants_Vs_Zombies_2.model.game.ZombieWave;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.special.ProtectedPlantSpec;

/**
 * Static adventure definition for the four required chapters.
 */
public final class ChapterCatalog {
    private static final List<Chapter> CHAPTERS = Collections.unmodifiableList(
            createChapters());

    private ChapterCatalog() {
    }

    private static List<Chapter> createChapters() {
        List<Chapter> chapters = new ArrayList<>();
        chapters.add(createChapter(
                "ancient-egypt",
                "Ancient Egypt",
                List.of("egypt"),
                "egypt",
                SpecialLevelType.CONVEYOR_BELT,
                SpecialLevelType.LOCKED_PLANTS));
        chapters.add(createChapter(
                "frostbite-caves",
                "Frostbite Caves",
                List.of("frostbite", "iceage"),
                "iceage",
                SpecialLevelType.SAVE_OUR_SEEDS,
                SpecialLevelType.TIMED_WAR));
        chapters.add(createChapter(
                "big-wave-beach",
                "Big Wave Beach",
                List.of("beach", "bigwave"),
                "beach",
                SpecialLevelType.NIGHT_OPS,
                SpecialLevelType.DEAD_LINE));
        chapters.add(createChapter(
                "dark-ages",
                "Dark Ages",
                List.of("dark", "medieval"),
                "dark",
                SpecialLevelType.LOVE_YOUR_PLANTS,
                SpecialLevelType.PLANT_WHAT_YOU_GET));
        return chapters;
    }

    private static Chapter createChapter(
            String id, String displayName,
            List<String> aliases, String theme,
            SpecialLevelType firstSpecial,
            SpecialLevelType secondSpecial) {
        List<Level> levels = List.of(
                createRegularLevel(
                        1, displayName + " - Normal",
                        LevelKind.NORMAL,
                        SpecialLevelType.NONE,
                        theme, 150, 400, 500, 1000),
                createRegularLevel(
                        2, displayName + " - "
                                + firstSpecial
                                        .getDisplayName(),
                        LevelKind.SPECIAL,
                        firstSpecial, theme,
                        initialSunFor(
                                firstSpecial, 200),
                        800, 1000, 2000),
                createRegularLevel(
                        3, displayName + " - "
                                + secondSpecial
                                        .getDisplayName(),
                        LevelKind.SPECIAL,
                        secondSpecial, theme,
                        initialSunFor(
                                secondSpecial, 250),
                        1200, 1500, 3000),
                createDeferredFinalLevel(
                        displayName
                                + " - Final Challenge",
                        theme));
        return new Chapter(
                id, displayName, aliases, levels);
    }

    private static int initialSunFor(
            SpecialLevelType type,
            int normalAmount) {
        if (type == SpecialLevelType.CONVEYOR_BELT) {
            return 0;
        }
        if (type == SpecialLevelType.PLANT_WHAT_YOU_GET) {
            return 800;
        }
        return normalAmount;
    }

    private static Level createRegularLevel(
            int number, String name, LevelKind kind,
            SpecialLevelType specialLevelType,
            String theme, int sun,
            int firstDifficulty,
            int secondDifficulty,
            int finalDifficulty) {
        List<ZombieWave> waves = List.of(
                ZombieWave.themedWave(
                        theme, firstDifficulty, false),
                ZombieWave.themedWave(
                        theme, secondDifficulty, false),
                ZombieWave.themedWave(
                        theme, finalDifficulty, true));
        return new Level(
                number, name, kind,
                specialLevelType,
                configFor(specialLevelType),
                ChapterRuleset.fromTheme(theme),
                Constants.DEFAULT_BOARD_ROWS,
                Constants.DEFAULT_BOARD_COLUMNS,
                sun, waves);
    }

    private static SpecialLevelConfig configFor(
            SpecialLevelType type) {
        switch (type) {
            case CONVEYOR_BELT:
                return SpecialLevelConfig.plantPool(
                        conveyorPlantPool());
            case LOCKED_PLANTS:
                return SpecialLevelConfig.plantPool(
                        lockedPlantPool());
            case SAVE_OUR_SEEDS:
                return SpecialLevelConfig.saveOurSeeds(
                        protectedPlants());
            case TIMED_WAR:
                return SpecialLevelConfig.timedWarWithSunCollection(
                        30.0, 5, 400);
            case NIGHT_OPS:
            case PLANT_WHAT_YOU_GET:
                return SpecialLevelConfig.none();
            case DEAD_LINE:
                return SpecialLevelConfig.deadLine(3.0);
            case LOVE_YOUR_PLANTS:
                return SpecialLevelConfig
                        .loveYourPlants(5);
            case NONE:
                return SpecialLevelConfig.none();
            default:
                throw new IllegalStateException(
                        "unknown special level type");
        }
    }

    private static List<String> conveyorPlantPool() {
        return List.of(
                "Peashooter",
                "Sunflower",
                "Wall-nut",
                "Potato Mine",
                "Cabbage-pult");
    }

    private static List<String> lockedPlantPool() {
        return List.of(
                "Sunflower",
                "Peashooter",
                "Cabbage-pult",
                "Wall-nut",
                "Potato Mine");
    }

    private static List<ProtectedPlantSpec> protectedPlants() {
        return List.of(
                new ProtectedPlantSpec(
                        "Sunflower",
                        new EntityPosition(1, 4)),
                new ProtectedPlantSpec(
                        "Wall-nut",
                        new EntityPosition(2, 4)),
                new ProtectedPlantSpec(
                        "Cabbage-pult",
                        new EntityPosition(3, 4)));
    }

    private static Level createDeferredFinalLevel(
            String name, String theme) {
        List<ZombieWave> waves = List.of(
                ZombieWave.themedWave(
                        theme, 1600, false),
                ZombieWave.themedWave(
                        theme, 2000, false),
                ZombieWave.themedWave(
                        theme, 4000, true));
        return new Level(
                4, name, LevelKind.NORMAL,
                SpecialLevelType.NONE,
                SpecialLevelConfig.none(),
                ChapterRuleset.fromTheme(theme),
                Constants.DEFAULT_BOARD_ROWS,
                Constants.DEFAULT_BOARD_COLUMNS,
                250, waves);
    }

    public static List<Chapter> getChapters() {
        return CHAPTERS;
    }

    public static Chapter getFirstChapter() {
        return CHAPTERS.get(0);
    }

    public static Chapter findChapter(String name) {
        for (Chapter chapter : CHAPTERS) {
            if (chapter.matches(name)) {
                return chapter;
            }
        }
        return null;
    }

    public static Chapter findById(String id) {
        for (Chapter chapter : CHAPTERS) {
            if (chapter.getId().equals(id)) {
                return chapter;
            }
        }
        return null;
    }

    public static Chapter getNextChapter(
            Chapter chapter) {
        int index = CHAPTERS.indexOf(chapter);
        if (index < 0
                || index + 1 >= CHAPTERS.size()) {
            return null;
        }
        return CHAPTERS.get(index + 1);
    }
}
