package io.github.Plants_Vs_Zombies_2.model.quest;

import io.github.Plants_Vs_Zombies_2.model.user.User;

public abstract class Quest implements Comparable<Quest> {
    private String name;
    private String instructions;
    private QuestType type;
    private QuestPriority priority;

    public Quest(String name, String instructions, QuestType type, QuestPriority priority) {
        this.name = name;
        this.instructions = instructions;
        this.type = type;
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public String getInstructions() {
        return instructions;
    }

    public QuestType getType() {
        return type;
    }

    public QuestPriority getPriority() {
        return priority;
    }

    public abstract boolean isConditionSatisfied();

    public abstract void giveReward(User user);

    @Override
    public int compareTo(Quest other) {
        // Sorts based on Enum ordinal (CRITICAL is top if defined first in enum, but
        // let's be explicit based on your image)
        return this.priority.compareTo(other.priority);
    }
}