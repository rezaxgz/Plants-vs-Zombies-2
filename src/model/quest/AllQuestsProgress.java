package model.quest;

import java.util.ArrayList;
import java.util.List;

public class AllQuestsProgress {
    private int completedDailyQuests = 0;
    private int completedNonDailyQuests = 0;
    private final List<Quest> activeQuests = new ArrayList<>();

    public int getCompletedDailyQuests() {
        return completedDailyQuests;
    }

    public void addCompletedDailyQuest() {
        this.completedDailyQuests++;
    }

    public int getCompletedNonDailyQuests() {
        return completedNonDailyQuests;
    }

    public void addCompletedNonDailyQuest() {
        this.completedNonDailyQuests++;
    }

    public int getTotalCompletedQuests() {
        return completedDailyQuests + completedNonDailyQuests;
    }

    public List<Quest> getActiveQuests() {
        return activeQuests;
    }

    public void addQuest(Quest quest) {
        this.activeQuests.add(quest);
    }
}