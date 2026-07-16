package model.collections.zombies;

import model.App;
import java.util.ArrayList;
import java.util.List;

public class ZombieCollection {
    private List<String> unlockedZombies;

    public ZombieCollection() {
        this.unlockedZombies = new ArrayList<>();
    }

    public void unlockZombie(String zombieName) {
        if (!unlockedZombies.contains(zombieName)) {
            unlockedZombies.add(zombieName);

            // Dispatch notification to user
            if (App.getInstance() != null && App.getInstance().getLoggedInUser() != null) {
                App.getInstance().getLoggedInUser().addNews(
                        "New Zombie Encountered!",
                        "A new threat has appeared: " + zombieName + ".");
            }
        }
    }

    public List<String> getUnlockedZombies() {
        return unlockedZombies;
    }
}