package io.github.Plants_Vs_Zombies_2.network.multiplayer;

public final class MatchProjectileSnapshot {
    private final String projectileId;
    private final String projectileType;
    private final int lane;
    private final double columnPosition;
    private final double velocityColumnsPerSecond;
    private final int damage;

    public MatchProjectileSnapshot(String projectileId, String projectileType,
            int lane, double columnPosition, double velocityColumnsPerSecond,
            int damage) {
        this.projectileId = projectileId;
        this.projectileType = projectileType;
        this.lane = lane;
        this.columnPosition = columnPosition;
        this.velocityColumnsPerSecond = velocityColumnsPerSecond;
        this.damage = damage;
    }

    public String getProjectileId() { return projectileId; }
    public String getProjectileType() { return projectileType; }
    public int getLane() { return lane; }
    public double getColumnPosition() { return columnPosition; }
    public double getVelocityColumnsPerSecond() { return velocityColumnsPerSecond; }
    public int getDamage() { return damage; }
}
