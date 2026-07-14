package model.game.entities.zombies;

public enum ZombieType {
    BASIC("Basic Zombie", 190, 0.185, 100, 100),
    FLAG("Flag Zombie", 190, 0.185, 100, 100);

    private final String displayName;
    private final int hitPoints;
    private final double speed;
    private final int eatDamagePerSecond;
    private final int wavePointCost;

    ZombieType(String displayName, int hitPoints, double speed,
            int eatDamagePerSecond, int wavePointCost) {
        this.displayName = displayName;
        this.hitPoints = hitPoints;
        this.speed = speed;
        this.eatDamagePerSecond = eatDamagePerSecond;
        this.wavePointCost = wavePointCost;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getHitPoints() {
        return hitPoints;
    }

    public double getSpeed() {
        return speed;
    }

    public int getEatDamagePerSecond() {
        return eatDamagePerSecond;
    }

    public int getWavePointCost() {
        return wavePointCost;
    }
}
