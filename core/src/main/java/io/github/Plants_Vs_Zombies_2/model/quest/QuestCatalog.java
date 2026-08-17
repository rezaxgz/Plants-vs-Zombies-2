package io.github.Plants_Vs_Zombies_2.model.quest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantCategory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFamily;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/** Creates the nineteen quest templates specified by phase-one Quests.csv. */
public final class QuestCatalog {
    private static final String[] CHAPTERS = {
            "ancient-egypt", "frostbite-caves",
            "big-wave-beach", "dark-ages"
    };

    private QuestCatalog() {
    }

    public static List<Quest> createNonDailyQuests(User user) {
        int seed = stableSeed(user, "permanent");
        String chapter = choose(CHAPTERS, seed);
        int maximumLosses = Math.floorMod(seed / 7, 6);
        List<Quest> quests = new ArrayList<>();
        quests.add(quest("chapter-hunter", "Chapter Hunter",
                "Defeat 50 zombies in chapter " + chapter + ".",
                QuestType.MAIN, QuestPriority.HIGH,
                QuestCondition.KILL_ZOMBIES_IN_CHAPTER, chapter, 50,
                QuestRewardType.SEED_PACKS, 10));
        quests.add(quest("economic-plant-eater", "Economic Plant Eater",
                "Win a level while losing no more than " + maximumLosses
                        + " plants.",
                QuestType.MAIN, QuestPriority.HIGH,
                QuestCondition.WIN_WITH_MAXIMUM_PLANT_LOSSES,
                Integer.toString(maximumLosses), 1,
                QuestRewardType.SEED_PACKS, 20 - maximumLosses));
        quests.add(quest("speed", "Speed",
                "Kill 10 zombies within 30 seconds of the first wave.",
                QuestType.MAIN, QuestPriority.AVARAGE,
                QuestCondition.KILL_TEN_WITHIN_THIRTY_SECONDS, "", 10,
                QuestRewardType.COINS, 500));
        quests.add(quest("master-defense", "Master Defense",
                "Win a level with exactly zero sun remaining.",
                QuestType.EPIC, QuestPriority.CRITICAL,
                QuestCondition.FINISH_WITH_ZERO_SUN, "", 1,
                QuestRewardType.DIAMONDS, 200));
        quests.add(quest("night-or-morning", "Night or Morning",
                "Win a daytime level using only mushroom plants.",
                QuestType.EPIC, QuestPriority.HIGH,
                QuestCondition.WIN_DAY_LEVEL_WITH_SHROOMS, "", 1,
                QuestRewardType.DIAMONDS, 20));
        return List.copyOf(quests);
    }

    public static List<Quest> createDailyQuests(User user, LocalDate date) {
        String day = date.toString();
        int seed = stableSeed(user, day);
        int sunTarget = choose(new int[] { 3000, 4000, 5000 }, seed);
        String plant = chooseKillCapablePlant(user, seed / 3);
        String family = chooseKillCapableFamily(user, seed / 5);
        String excludedFamily = chooseDifferentFamily(family, seed / 11);
        int emptyColumn = 1 + Math.floorMod(seed / 13, 9);
        int emptyRow = 1 + Math.floorMod(seed / 17, 5);
        int emptyCross = 1 + Math.floorMod(seed / 19, 5);
        String prefix = "daily-" + day + "-";

        List<Quest> quests = new ArrayList<>();
        quests.add(quest(prefix + "sun-collector", "Daily Sun Collector",
                "Collect " + sunTarget + " sun during this day.",
                QuestType.DAILY, QuestPriority.AVARAGE,
                QuestCondition.COLLECT_SUN, Integer.toString(sunTarget),
                sunTarget, QuestRewardType.COINS, sunTarget / 100));
        quests.add(quest(prefix + "plant-pro", "Plant Pro",
                "Kill 10 zombies using only " + plant + ".",
                QuestType.DAILY, QuestPriority.HIGH,
                QuestCondition.KILL_ONLY_WITH_PLANT, plant, 10,
                QuestRewardType.RANDOM_PLANT, 1));
        quests.add(quest(prefix + "only-cactus", "Only Cactus",
                "Kill 10 zombies using only Cactus.",
                QuestType.DAILY, QuestPriority.HIGH,
                QuestCondition.KILL_ONLY_WITH_CACTUS, "Cactus", 10,
                QuestRewardType.DIAMONDS, 20));
        quests.add(quest(prefix + "demolition-pro", "Demolition Pro",
                "Use 3 explosive plants in one level.",
                QuestType.DAILY, QuestPriority.LOW,
                QuestCondition.USE_THREE_EXPLOSIVE_PLANTS, "", 3,
                QuestRewardType.COINS, 100));
        quests.add(quest(prefix + "symmetry", "Symmetry",
                "Win with a horizontally symmetrical final garden.",
                QuestType.DAILY, QuestPriority.HIGH,
                QuestCondition.FINISH_WITH_SYMMETRICAL_GARDEN, "", 1,
                QuestRewardType.COINS, 500));
        quests.add(quest(prefix + "family-massacre", "Family Massacre",
                "Kill 10 zombies using only the " + family + " family.",
                QuestType.DAILY, QuestPriority.AVARAGE,
                QuestCondition.KILL_ONLY_WITH_FAMILY, family, 10,
                QuestRewardType.COINS, 1000));
        quests.add(quest(prefix + "limitations-bloom", "Limitations Bloom",
                "Win without planting a member of the " + excludedFamily
                        + " family.",
                QuestType.DAILY, QuestPriority.HIGH,
                QuestCondition.WIN_WITHOUT_FAMILY, excludedFamily, 1,
                QuestRewardType.DIAMONDS, 100));
        quests.add(quest(prefix + "win-streak", "Win Streak",
                "Win 5 consecutive stages at maximum difficulty.",
                QuestType.DAILY, QuestPriority.AVARAGE,
                QuestCondition.WIN_FIVE_AT_MAXIMUM_DIFFICULTY, "", 5,
                QuestRewardType.COINS, 5000));
        quests.add(quest(prefix + "almost-won", "Almost Won",
                "Kill 10 zombies in column one of a row whose mower is gone.",
                QuestType.DAILY, QuestPriority.AVARAGE,
                QuestCondition.KILL_IN_FIRST_COLUMN_WITHOUT_MOWER, "", 10,
                QuestRewardType.COINS, 300));
        quests.add(quest(prefix + "ocd-no", "OCD No",
                "Win with no horizontal garden symmetry (middle row ignored).",
                QuestType.DAILY, QuestPriority.AVARAGE,
                QuestCondition.FINISH_WITHOUT_GARDEN_SYMMETRY, "", 1,
                QuestRewardType.COINS, 800));
        quests.add(quest(prefix + "cloudy-day", "Cloudy Day",
                "Win using exactly 3 plants, all of them sun producers.",
                QuestType.DAILY, QuestPriority.HIGH,
                QuestCondition.WIN_WITH_EXACTLY_THREE_SUN_PRODUCERS, "", 3,
                QuestRewardType.DIAMONDS, 10));
        quests.add(quest(prefix + "one-less-column", "One Less Column",
                "Win without ever planting in column " + emptyColumn + ".",
                QuestType.DAILY, QuestPriority.HIGH,
                QuestCondition.WIN_WITH_EMPTY_COLUMN,
                Integer.toString(emptyColumn), 1,
                QuestRewardType.DIAMONDS, 10));
        quests.add(quest(prefix + "undefended-row", "Undefended Row",
                "Win without ever planting in row " + emptyRow + ".",
                QuestType.DAILY, QuestPriority.HIGH,
                QuestCondition.WIN_WITH_EMPTY_ROW, Integer.toString(emptyRow),
                1, QuestRewardType.DIAMONDS, 20));
        quests.add(quest(prefix + "undefended-cross", "Undefended Cross",
                "Win without ever planting in row and column " + emptyCross
                        + ".",
                QuestType.DAILY, QuestPriority.HIGH,
                QuestCondition.WIN_WITH_EMPTY_CROSS,
                Integer.toString(emptyCross), 1,
                QuestRewardType.DIAMONDS, 25));
        return List.copyOf(quests);
    }

    private static Quest quest(String id, String name, String instructions,
            QuestType type, QuestPriority priority,
            QuestCondition condition, String parameter, int target,
            QuestRewardType rewardType, int rewardAmount) {
        return new Quest(id, name, instructions, type, priority,
                condition, parameter, target,
                new QuestReward(rewardType, rewardAmount));
    }

    private static int stableSeed(User user, String salt) {
        String username = user == null ? "guest" : user.getUsername();
        return (username + ":" + salt).hashCode();
    }

    private static String chooseKillCapablePlant(User user, int seed) {
        List<String> candidates = new ArrayList<>();
        if (user != null) {
            for (PlantCollectionItem item
                    : user.getPlantCollection().getUnlockedPlants()) {
                if (isKillCapable(item.getCategory(), item.getDamage())) {
                    candidates.add(item.getName());
                }
            }
        }
        if (candidates.isEmpty()) {
            candidates.add("Peashooter");
        }
        candidates.sort(String.CASE_INSENSITIVE_ORDER);
        return candidates.get(Math.floorMod(seed, candidates.size()));
    }

    private static boolean isKillCapable(PlantCategory category, int damage) {
        return damage > 0 || switch (category) {
            case SHOOTER, HOMING, STRIKE_THROUGH, LOBBER,
                    EXPLOSIVE, MELEE -> true;
            default -> false;
        };
    }

    private static String chooseKillCapableFamily(User user, int seed) {
        List<String> candidates = new ArrayList<>();
        if (user != null) {
            for (PlantCollectionItem item
                    : user.getPlantCollection().getUnlockedPlants()) {
                String family = item.getCategory().name();
                if (isKillCapable(item.getCategory(), item.getDamage())
                        && !candidates.contains(family)) {
                    candidates.add(family);
                }
            }
        }
        if (candidates.isEmpty()) {
            candidates.add(PlantFamily.SHOOTER.name());
        }
        candidates.sort(String.CASE_INSENSITIVE_ORDER);
        return candidates.get(Math.floorMod(seed, candidates.size()));
    }

    private static String chooseDifferentFamily(String first, int seed) {
        PlantFamily[] families = PlantFamily.values();
        for (int offset = 0; offset < families.length; offset++) {
            String candidate = families[Math.floorMod(seed + offset,
                    families.length)].name();
            if (!candidate.equals(first)) {
                return candidate;
            }
        }
        return PlantFamily.SHOOTER.name();
    }

    private static String choose(String[] values, int seed) {
        return values[Math.floorMod(seed, values.length)];
    }

    private static int choose(int[] values, int seed) {
        return values[Math.floorMod(seed, values.length)];
    }
}
