package io.github.Plants_Vs_Zombies_2.model.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.quest.Quest;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestCondition;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestPriority;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestReward;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestRewardType;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestType;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/** Package-level JSON round-trip check used by the built-in self-test. */
public final class QuestPersistenceCheck {
    private QuestPersistenceCheck() {
    }

    public static void run() {
        Path database = null;
        try {
            database = Files.createTempFile("pvz-quest-round-trip-", ".json");
            User user = new User("quest-json-test", "Password1!",
                    "JSON Tester", "json@test.local", Gender.MALE);
            Quest completed = Quest.restore("json-completed-quest",
                    "JSON completed quest", "Persistence self-test quest.",
                    QuestType.EPIC, QuestPriority.CRITICAL,
                    QuestCondition.COLLECT_SUN, "", 5,
                    new QuestReward(QuestRewardType.COINS, 75),
                    5, true, false);
            completed.giveReward(user);
            user.getQuestProgress().addQuest(completed);
            user.getQuestProgress().addCompletedNonDailyQuest();

            UserJsonDatabase.save(database, List.of(user));
            List<User> loaded = UserJsonDatabase.load(database);
            require(loaded.size() == 1, "round-trip lost the user");
            User restored = loaded.get(0);
            require(restored.getCoins() == 75,
                    "round-trip lost quest currency reward");
            require(restored.getQuestProgress().getCompletedNonDailyQuests() == 1,
                    "round-trip lost completed quest count");
            Quest restoredQuest = restored.getQuestProgress().getActiveQuests()
                    .stream()
                    .filter(quest -> "json-completed-quest".equals(quest.getId()))
                    .findFirst().orElseThrow(() -> new AssertionError(
                            "round-trip lost the completed quest"));
            require(restoredQuest.isCompleted(),
                    "round-trip lost completed state");
            require(restoredQuest.isRewardGranted(),
                    "round-trip lost reward-granted state");
            require(restoredQuest.getReward().getType() == QuestRewardType.COINS
                            && restoredQuest.getReward().getAmount() == 75,
                    "round-trip changed reward data");
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "could not create quest persistence test file", exception);
        } finally {
            if (database != null) {
                try {
                    Files.deleteIfExists(database);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
