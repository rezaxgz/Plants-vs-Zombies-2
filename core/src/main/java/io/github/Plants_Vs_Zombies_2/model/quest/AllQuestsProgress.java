package io.github.Plants_Vs_Zombies_2.model.quest;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.Settings;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/** Persistent per-user quest state and daily rotation. */
public final class AllQuestsProgress {
    private int completedDailyQuests;
    private int completedNonDailyQuests;
    private int maximumDifficultyWinStreak;
    private String lastDailyRefresh = "";
    private final List<Quest> activeQuests = new ArrayList<>();

    public static AllQuestsProgress restore(int completedDailyQuests,
            int completedNonDailyQuests, int maximumDifficultyWinStreak,
            String lastDailyRefresh, List<Quest> quests) {
        AllQuestsProgress progress = new AllQuestsProgress();
        progress.completedDailyQuests = Math.max(0, completedDailyQuests);
        progress.completedNonDailyQuests = Math.max(0,
                completedNonDailyQuests);
        progress.maximumDifficultyWinStreak = Math.max(0,
                maximumDifficultyWinStreak);
        progress.lastDailyRefresh = validDateOrEmpty(lastDailyRefresh);
        if (quests != null) {
            progress.activeQuests.addAll(quests);
        }
        return progress;
    }

    public void ensureInitialized(User user) {
        ensureInitialized(user, LocalDate.now());
    }

    public void ensureInitialized(User user, LocalDate today) {
        if (user == null || today == null) {
            throw new IllegalArgumentException("user and date cannot be null");
        }
        boolean hasNonDaily = activeQuests.stream()
                .anyMatch(quest -> quest.getType() != QuestType.DAILY);
        if (!hasNonDaily) {
            activeQuests.addAll(QuestCatalog.createNonDailyQuests(user));
        }

        String date = today.toString();
        boolean hasDaily = activeQuests.stream()
                .anyMatch(quest -> quest.getType() == QuestType.DAILY);
        if (!date.equals(lastDailyRefresh) || !hasDaily) {
            activeQuests.removeIf(
                    quest -> quest.getType() == QuestType.DAILY);
            activeQuests.addAll(QuestCatalog.createDailyQuests(user, today));
            lastDailyRefresh = date;
            maximumDifficultyWinStreak = 0;
        }
    }

    /** Evaluates all current quests and grants newly earned rewards. */
    public List<String> recordCompletedRun(User user, QuestRunSummary run) {
        if (user == null || run == null) {
            throw new IllegalArgumentException("user and run cannot be null");
        }
        ensureInitialized(user);
        updateMaximumDifficultyStreak(run);
        List<String> results = new ArrayList<>();
        for (Quest quest : activeQuests) {
            if (!quest.applyRun(run, maximumDifficultyWinStreak)) {
                continue;
            }
            if (quest.getType() == QuestType.DAILY) {
                completedDailyQuests++;
            } else {
                completedNonDailyQuests++;
            }
            String granted = quest.grantReward(user);
            String message = "Quest completed: " + quest.getName()
                    + ". Reward: " + granted + ".";
            results.add(message);
            user.addNews("Quest Completed", message);
        }
        return Collections.unmodifiableList(results);
    }

    private void updateMaximumDifficultyStreak(QuestRunSummary run) {
        if (run.isWon()
                && run.getDifficultyLevel() == Settings.MAX_DIFFICULTY) {
            maximumDifficultyWinStreak++;
        } else {
            maximumDifficultyWinStreak = 0;
        }
    }

    private static String validDateOrEmpty(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            return LocalDate.parse(value).toString();
        } catch (DateTimeParseException exception) {
            return "";
        }
    }

    public int getCompletedDailyQuests() {
        return completedDailyQuests;
    }

    public void addCompletedDailyQuest() {
        completedDailyQuests++;
    }

    public int getCompletedNonDailyQuests() {
        return completedNonDailyQuests;
    }

    /** Applies only persisted aggregate counters; active quest state is untouched. */
    public void restoreCompletedCountsForStorage(int completedDailyQuests,
            int completedNonDailyQuests) {
        if (completedDailyQuests < 0 || completedNonDailyQuests < 0) {
            throw new IllegalArgumentException("quest completion counts cannot be negative");
        }
        this.completedDailyQuests = completedDailyQuests;
        this.completedNonDailyQuests = completedNonDailyQuests;
    }

    public void addCompletedNonDailyQuest() {
        completedNonDailyQuests++;
    }

    public int getTotalCompletedQuests() {
        return completedDailyQuests + completedNonDailyQuests;
    }

    public int getMaximumDifficultyWinStreak() {
        return maximumDifficultyWinStreak;
    }

    public String getLastDailyRefresh() {
        return lastDailyRefresh;
    }

    public List<Quest> getActiveQuests() {
        return activeQuests;
    }

    public void addQuest(Quest quest) {
        if (quest != null) {
            activeQuests.add(quest);
        }
    }
}
