package io.github.Plants_Vs_Zombies_2.model.collections.zombies;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;

public class ZombieCollection {
    private static final ZombieType[] BONUS_ZOMBIE_TYPES = {
            ZombieType.ARCADE,
            ZombieType.TROGLOBITE,
            ZombieType.FISHERMAN,
            ZombieType.JUGGLER,
            ZombieType.WIZARD,
            ZombieType.DARK_KING
    };

    private final List<ZombieCollectionItem> allZombies;

    public ZombieCollection() {
        allZombies = new ArrayList<>();
        for (ZombieType type : ZombieType.values()) {
            allZombies.add(new ZombieCollectionItem(type));
        }
        allZombies.sort(Comparator.comparing(ZombieCollectionItem::getName));
        validateBonusZombieEntries();
    }

    public List<ZombieCollectionItem> getAllZombies() {
        return List.copyOf(allZombies);
    }

    public ZombieCollectionItem findZombie(String zombieName) {
        String normalized = normalizeName(zombieName);
        if (normalized.isEmpty()) {
            return null;
        }
        String aliasedTypeName = bonusZombieTypeAlias(normalized);
        for (ZombieCollectionItem item : allZombies) {
            if (normalizeName(item.getName()).equals(normalized)
                    || normalizeName(item.getTypeName()).equals(normalized)
                    || aliasedTypeName != null
                            && normalizeName(item.getTypeName())
                                    .equals(aliasedTypeName)) {
                return item;
            }
        }
        return null;
    }

    public boolean unlockZombie(String zombieName) {
        ZombieCollectionItem item = findZombie(zombieName);
        if (item == null || item.isUnlocked()) {
            return false;
        }
        item.setUnlocked(true);
        dispatchUnlockNews(item.getName());
        return true;
    }

    public boolean isZombieUnlocked(String zombieName) {
        ZombieCollectionItem item = findZombie(zombieName);
        return item != null && item.isUnlocked();
    }

    public List<String> getUnlockedZombies() {
        List<String> names = new ArrayList<>();
        for (ZombieCollectionItem item : allZombies) {
            if (item.isUnlocked()) {
                names.add(item.getName());
            }
        }
        return List.copyOf(names);
    }

    public List<ZombieCollectionItem> getUnlockedZombieItems() {
        List<ZombieCollectionItem> items = new ArrayList<>();
        for (ZombieCollectionItem item : allZombies) {
            if (item.isUnlocked()) {
                items.add(item);
            }
        }
        return List.copyOf(items);
    }

    public boolean restoreZombieState(String zombieName, boolean unlocked) {
        ZombieCollectionItem item = findZombie(zombieName);
        if (item == null) {
            return false;
        }
        item.setUnlocked(unlocked);
        return true;
    }

    private void validateBonusZombieEntries() {
        for (ZombieType bonusType : BONUS_ZOMBIE_TYPES) {
            boolean present = false;
            for (ZombieCollectionItem item : allZombies) {
                if (item.getTypeName().equals(bonusType.name())) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                throw new IllegalStateException(
                        "bonus zombie missing from collection: "
                                + bonusType.name());
            }
        }
    }

    private static String bonusZombieTypeAlias(String normalizedName) {
        if ("jester".equals(normalizedName)
                || "jesterzombie".equals(normalizedName)) {
            return normalizeName(ZombieType.JUGGLER.name());
        }
        if ("king".equals(normalizedName)
                || "kingzombie".equals(normalizedName)) {
            return normalizeName(ZombieType.DARK_KING.name());
        }
        return null;
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static void dispatchUnlockNews(String zombieName) {
        if (App.getInstance().getLoggedInUser() == null) {
            return;
        }
        App.getInstance().getLoggedInUser()
                .addNewsIfAbsent(
                        "New Zombie Encountered!",
                        "A new threat has appeared: " + zombieName + ".");
        UserManager.saveAllUsers();
    }
}
