package io.github.Plants_Vs_Zombies_2.network.gameplay;

import java.util.Objects;

import io.github.Plants_Vs_Zombies_2.model.quest.Quest;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestCondition;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestPriority;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestReward;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestRewardType;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestType;

/** Explicit immutable representation of one persistent quest instance. */
public final class QuestGameplayState {
    private final String id;
    private final String name;
    private final String instructions;
    private final QuestType type;
    private final QuestPriority priority;
    private final QuestCondition condition;
    private final String parameter;
    private final int target;
    private final QuestRewardType rewardType;
    private final int rewardAmount;
    private final int progress;
    private final boolean completed;
    private final boolean rewardGranted;

    public QuestGameplayState(String id, String name, String instructions,
            QuestType type, QuestPriority priority, QuestCondition condition,
            String parameter, int target, QuestRewardType rewardType,
            int rewardAmount, int progress, boolean completed,
            boolean rewardGranted) {
        this.id = id;
        this.name = name;
        this.instructions = instructions;
        this.type = type;
        this.priority = priority;
        this.condition = condition;
        this.parameter = parameter == null ? "" : parameter;
        this.target = target;
        this.rewardType = rewardType;
        this.rewardAmount = rewardAmount;
        this.progress = progress;
        this.completed = completed;
        this.rewardGranted = rewardGranted;
    }

    public static QuestGameplayState fromQuest(Quest quest) {
        return new QuestGameplayState(quest.getId(), quest.getName(),
                quest.getInstructions(), quest.getType(), quest.getPriority(),
                quest.getCondition(), quest.getParameter(), quest.getTarget(),
                quest.getReward().getType(), quest.getReward().getAmount(),
                quest.getProgress(), quest.isCompleted(), quest.isRewardGranted());
    }

    public Quest toQuest() {
        return Quest.restore(id, name, instructions, type, priority, condition,
                parameter, target, new QuestReward(rewardType, rewardAmount),
                progress, completed, rewardGranted);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getInstructions() { return instructions; }
    public QuestType getType() { return type; }
    public QuestPriority getPriority() { return priority; }
    public QuestCondition getCondition() { return condition; }
    public String getParameter() { return parameter; }
    public int getTarget() { return target; }
    public QuestRewardType getRewardType() { return rewardType; }
    public int getRewardAmount() { return rewardAmount; }
    public int getProgress() { return progress; }
    public boolean isCompleted() { return completed; }
    public boolean isRewardGranted() { return rewardGranted; }

    @Override public boolean equals(Object other) {
        if (!(other instanceof QuestGameplayState value)) return false;
        return target == value.target && rewardAmount == value.rewardAmount
                && progress == value.progress && completed == value.completed
                && rewardGranted == value.rewardGranted
                && Objects.equals(id, value.id) && Objects.equals(name, value.name)
                && Objects.equals(instructions, value.instructions)
                && type == value.type && priority == value.priority
                && condition == value.condition
                && Objects.equals(parameter, value.parameter)
                && rewardType == value.rewardType;
    }

    @Override public int hashCode() {
        return Objects.hash(id, name, instructions, type, priority, condition,
                parameter, target, rewardType, rewardAmount, progress,
                completed, rewardGranted);
    }
}
