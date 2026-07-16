package model.roadmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.Constants;
import model.game.ZombieWave;
import model.game.entities.zombies.ZombieType;

/**
 * Static adventure definition for the four chapters required by the project.
 */
public final class ChapterCatalog {
    private static final List<Chapter> CHAPTERS =
            Collections.unmodifiableList(
                    createChapters());

    private ChapterCatalog() {
    }

    private static List<Chapter> createChapters() {
        List<Chapter> chapters =
                new ArrayList<>();
        chapters.add(createChapter(
                "ancient-egypt",
                "Ancient Egypt",
                List.of("egypt"),
                "egypt",
                SpecialLevelType.CONVEYOR_BELT,
                SpecialLevelType.LOCKED_PLANTS,
                ZombieType.ZOMBOSS_EGYPT));
        chapters.add(createChapter(
                "frostbite-caves",
                "Frostbite Caves",
                List.of("frostbite", "iceage"),
                "iceage",
                SpecialLevelType.SAVE_OUR_SEEDS,
                SpecialLevelType.TIMED_WAR,
                ZombieType.ZOMBOSS_PIRATE));
        chapters.add(createChapter(
                "big-wave-beach",
                "Big Wave Beach",
                List.of("beach", "bigwave"),
                "beach",
                SpecialLevelType.NIGHT_OPS,
                SpecialLevelType.DEAD_LINE,
                ZombieType.ZOMBOSS_COWBOY));
        chapters.add(createChapter(
                "dark-ages",
                "Dark Ages",
                List.of("dark", "medieval"),
                "dark",
                SpecialLevelType.LOVE_YOUR_PLANTS,
                SpecialLevelType.PLANT_WHAT_YOU_GET,
                ZombieType.ZOMBOSS_DARK));
        return chapters;
    }

    private static Chapter createChapter(
            String id, String displayName,
            List<String> aliases, String theme,
            SpecialLevelType firstSpecial,
            SpecialLevelType secondSpecial,
            ZombieType bossType) {
        List<Level> levels = List.of(
                createRegularLevel(
                        1, displayName + " - Normal",
                        LevelKind.NORMAL,
                        SpecialLevelType.NONE,
                        theme, 150, 400, 650, 900),
                createRegularLevel(
                        2, displayName + " - "
                                + firstSpecial
                                        .getDisplayName(),
                        LevelKind.SPECIAL,
                        firstSpecial,
                        theme,
                        initialSunFor(firstSpecial, 200),
                        550, 850, 1200),
                createRegularLevel(
                        3, displayName + " - "
                                + secondSpecial
                                        .getDisplayName(),
                        LevelKind.SPECIAL,
                        secondSpecial,
                        theme, 250, 700, 1050, 1500),
                createBossLevel(
                        displayName + " - Zomboss",
                        theme, bossType));
        return new Chapter(
                id, displayName, aliases, levels);
    }

    private static int initialSunFor(
            SpecialLevelType type, int normalAmount) {
        return type == SpecialLevelType.CONVEYOR_BELT
                ? 0 : normalAmount;
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
                plantPoolFor(specialLevelType),
                Constants.DEFAULT_BOARD_ROWS,
                Constants.DEFAULT_BOARD_COLUMNS,
                sun, waves);
    }

    private static List<String> plantPoolFor(
            SpecialLevelType type) {
        if (type == SpecialLevelType.CONVEYOR_BELT) {
            return List.of(
                    "Peashooter",
                    "Sunflower",
                    "Wall-nut",
                    "Potato Mine",
                    "Cabbage-pult");
        }
        if (type == SpecialLevelType.LOCKED_PLANTS) {
            return List.of(
                    "Sunflower",
                    "Peashooter",
                    "Cabbage-pult",
                    "Wall-nut",
                    "Potato Mine");
        }
        return Collections.emptyList();
    }

    private static Level createBossLevel(
            String name, String theme,
            ZombieType bossType) {
        List<ZombieWave> waves = List.of(
                ZombieWave.themedWave(
                        theme, 800, false),
                ZombieWave.themedWave(
                        theme, 1200, false),
                new ZombieWave(
                        List.of(bossType),
                        bossType.getHitpoints(),
                        true));
        return new Level(
                4, name, LevelKind.BOSS,
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
