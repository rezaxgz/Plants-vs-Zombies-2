package model.quest;

import model.user.User;

public abstract class Quest {
    private String name;
    private String instructions;
    private QuestType type;

    public abstract boolean isConditionSatisfied();

    public abstract void giveReward(User user);

    
}
