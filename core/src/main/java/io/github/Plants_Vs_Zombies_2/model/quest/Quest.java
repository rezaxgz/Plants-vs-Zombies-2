package io.github.Plants_Vs_Zombies_2.model.quest;

import io.github.Plants_Vs_Zombies_2.model.game.ChapterRuleset;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/** A persistent, executable quest instance. */
public final class Quest implements Comparable<Quest> {
    private final String id;
    private final String name;
    private final String instructions;
    private final QuestType type;
    private final QuestPriority priority;
    private final QuestCondition condition;
    private final String parameter;
    private final int target;
    private final QuestReward reward;
    private int progress;
    private boolean completed;
    private boolean rewardGranted;

    public Quest(String id, String name, String instructions,
            QuestType type, QuestPriority priority,
            QuestCondition condition, String parameter, int target,
            QuestReward reward) {
        this(id, name, instructions, type, priority, condition,
                parameter, target, reward, 0, false, false);
    }

    private Quest(String id, String name, String instructions,
            QuestType type, QuestPriority priority,
            QuestCondition condition, String parameter, int target,
            QuestReward reward, int progress, boolean completed,
            boolean rewardGranted) {
        if (id == null || id.isBlank() || name == null || name.isBlank()
                || instructions == null || instructions.isBlank()) {
            throw new IllegalArgumentException(
                    "quest id, name, and instructions cannot be blank");
        }
        if (type == null || priority == null || condition == null
                || reward == null) {
            throw new IllegalArgumentException(
                    "quest type, priority, condition, and reward are required");
        }
        if (target <= 0 || progress < 0) {
            throw new IllegalArgumentException(
                    "quest target must be positive and progress non-negative");
        }
        this.id = id;
        this.name = name;
        this.instructions = instructions;
        this.type = type;
        this.priority = priority;
        this.condition = condition;
        this.parameter = parameter == null ? "" : parameter;
        this.target = target;
        this.reward = reward;
        this.progress = Math.min(progress, target);
        this.completed = completed || this.progress >= target;
        this.rewardGranted = rewardGranted;
    }

    public static Quest restore(String id, String name, String instructions,
            QuestType type, QuestPriority priority,
            QuestCondition condition, String parameter, int target,
            QuestReward reward, int progress, boolean completed,
            boolean rewardGranted) {
        return new Quest(id, name, instructions, type, priority,
                condition, parameter, target, reward, progress,
                completed, rewardGranted);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getInstructions() { return instructions; }
    public QuestType getType() { return type; }
    public QuestPriority getPriority() { return priority; }
    public QuestCondition getCondition() { return condition; }
    public String getParameter() { return parameter; }
    public int getTarget() { return target; }
    public int getProgress() { return progress; }
    public QuestReward getReward() { return reward; }
    public boolean isCompleted() { return completed; }
    public boolean isRewardGranted() { return rewardGranted; }

    public String getProgressText() {
        return Math.min(progress, target) + "/" + target;
    }

    public boolean isConditionSatisfied() {
        return completed || progress >= target;
    }

    /** Returns true only on the transition from active to completed. */
    public boolean applyRun(QuestRunSummary run, int maximumDifficultyStreak) {
        if (run == null || completed) {
            return false;
        }
        switch (condition) {
            case COLLECT_SUN -> addProgress(run.getCollectedSun());
            case KILL_ZOMBIES_IN_CHAPTER -> {
                if (QuestRunSummary.normalize(parameter)
                        .equals(run.getChapterId())) {
                    addProgress(run.getZombieKills());
                }
            }
            case KILL_ONLY_WITH_PLANT -> {
                addProgress(run.getKillsByOnlyPlant(parameter));
            }
            case KILL_ONLY_WITH_CACTUS -> {
                addProgress(run.getKillsByOnlyPlant("Cactus"));
            }
            case WIN_WITH_MAXIMUM_PLANT_LOSSES -> completeIf(run.isWon()
                    && run.getLostPlants() <= intParameter());
            case FINISH_WITH_ZERO_SUN -> completeIf(run.isWon()
                    && run.getFinalSun() == 0);
            case KILL_TEN_WITHIN_THIRTY_SECONDS -> completeIf(
                    run.getKillsWithinThirtySeconds() >= target);
            case USE_THREE_EXPLOSIVE_PLANTS -> completeIf(
                    run.getExplosivePlantsUsed() >= target);
            case FINISH_WITH_SYMMETRICAL_GARDEN -> completeIf(run.isWon()
                    && run.isSymmetricalGarden());
            case KILL_ONLY_WITH_FAMILY -> {
                addProgress(run.getKillsByOnlyFamily(parameter));
            }
            case WIN_WITHOUT_FAMILY -> completeIf(run.isWon()
                    && !run.usedPlantFamily(parameter));
            case WIN_DAY_LEVEL_WITH_SHROOMS -> completeIf(run.isWon()
                    && run.getChapterRuleset() != ChapterRuleset.DARK_AGES
                    && run.areAllPlacedPlantsShrooms());
            case WIN_FIVE_AT_MAXIMUM_DIFFICULTY -> {
                progress = Math.min(target, maximumDifficultyStreak);
                completed = progress >= target;
            }
            case KILL_IN_FIRST_COLUMN_WITHOUT_MOWER -> addProgress(
                    run.getFirstColumnKillsWithoutMower());
            case FINISH_WITHOUT_GARDEN_SYMMETRY -> completeIf(run.isWon()
                    && !run.isSymmetricalGarden()
                    && run.getPlacedPlantCount() > 0);
            case WIN_WITH_EXACTLY_THREE_SUN_PRODUCERS -> completeIf(run.isWon()
                    && run.getPlacedPlantCount() == target
                    && run.getSunProducerPlantCount() == target);
            case WIN_WITH_EMPTY_COLUMN -> completeIf(run.isWon()
                    && run.wasColumnNeverPlanted(intParameter()));
            case WIN_WITH_EMPTY_ROW -> completeIf(run.isWon()
                    && run.wasRowNeverPlanted(intParameter()));
            case WIN_WITH_EMPTY_CROSS -> completeIf(run.isWon()
                    && run.wasRowNeverPlanted(intParameter())
                    && run.wasColumnNeverPlanted(intParameter()));
            default -> throw new IllegalStateException(
                    "unsupported quest condition: " + condition);
        }
        return completed;
    }

    private int intParameter() {
        try {
            return Integer.parseInt(parameter);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "quest parameter must be a number: " + parameter,
                    exception);
        }
    }

    private void addProgress(int amount) {
        if (amount <= 0) {
            return;
        }
        progress = Math.min(target, progress + amount);
        completed = progress >= target;
    }

    private void completeIf(boolean conditionMet) {
        if (conditionMet) {
            progress = target;
            completed = true;
        }
    }

    public void giveReward(User user) {
        grantReward(user);
    }

    public String grantReward(User user) {
        if (!completed || rewardGranted) {
            return "";
        }
        String granted = reward.apply(user, id);
        rewardGranted = true;
        return granted;
    }

    @Override
    public int compareTo(Quest other) {
        int priorityOrder = priority.compareTo(other.priority);
        if (priorityOrder != 0) {
            return priorityOrder;
        }
        return name.compareToIgnoreCase(other.name);
    }
}
