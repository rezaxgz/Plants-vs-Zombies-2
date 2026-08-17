package io.github.Plants_Vs_Zombies_2.model.quest;

import java.util.Comparator;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/** A quest reward that can be applied exactly once by {@link Quest}. */
public final class QuestReward {
    private final QuestRewardType type;
    private final int amount;

    public QuestReward(QuestRewardType type, int amount) {
        if (type == null) {
            throw new IllegalArgumentException("reward type cannot be null");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("reward amount cannot be negative");
        }
        this.type = type;
        this.amount = amount;
    }

    public QuestRewardType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public String describe() {
        return switch (type) {
            case COINS -> amount + " coins";
            case DIAMONDS -> amount + " diamonds";
            case SEED_PACKS -> amount + " seed packs";
            case RANDOM_PLANT -> "one random locked plant";
        };
    }

    /**
     * Applies the reward and returns a human-readable description of what was
     * actually granted. Seed packs are attached to a deterministic unlocked
     * plant so save files and tests remain reproducible.
     */
    public String apply(User user, String questId) {
        if (user == null) {
            throw new IllegalArgumentException("user cannot be null");
        }
        switch (type) {
            case COINS:
                user.addCoins(amount);
                return amount + " coins";
            case DIAMONDS:
                user.addDiamonds(amount);
                return amount + " diamonds";
            case SEED_PACKS:
                PlantCollectionItem seedTarget = choosePlant(
                        user.getPlantCollection().getUnlockedPlants(), questId);
                if (seedTarget == null) {
                    return "no seed packs (no unlocked plant was available)";
                }
                user.addPlantCards(seedTarget.getName(), amount);
                return amount + " " + seedTarget.getName() + " seed packs";
            case RANDOM_PLANT:
                PlantCollectionItem unlockTarget = choosePlant(
                        user.getPlantCollection().getLockedPlants(), questId);
                if (unlockTarget == null) {
                    return "no new plant (every plant is already unlocked)";
                }
                user.unlockPlant(unlockTarget.getName());
                return unlockTarget.getName() + " unlocked";
            default:
                throw new IllegalStateException("unsupported reward type: " + type);
        }
    }

    private static PlantCollectionItem choosePlant(
            List<PlantCollectionItem> candidates, String questId) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        List<PlantCollectionItem> sorted = candidates.stream()
                .sorted(Comparator.comparing(PlantCollectionItem::getName))
                .toList();
        int index = Math.floorMod(questId == null ? 0 : questId.hashCode(),
                sorted.size());
        return sorted.get(index);
    }
}
