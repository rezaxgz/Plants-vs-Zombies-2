package model.game;

import java.util.List;

import model.collections.zombies.ZombieCollectionItem;

public class ZombieWave {
    private float secondsBeforeSpawn;
    private List<ZombieCollectionItem> zombies;

    public float getSecondsBeforeSpawn() {
        return secondsBeforeSpawn;
    }

    public void setSecondsBeforeSpawn(float secondsBeforeSpawn) {
        if (!Float.isFinite(secondsBeforeSpawn) || secondsBeforeSpawn < 0.0f) {
            throw new IllegalArgumentException("secondsBeforeSpawn must be finite and non-negative");
        }
        this.secondsBeforeSpawn = secondsBeforeSpawn;
    }

    public List<ZombieCollectionItem> getZombies() {
        return zombies;
    }
}
