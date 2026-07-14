package model.game.entities.zombies;

import model.game.entities.Entity;
import model.game.entities.EntityPosition;
import model.game.entities.plants.BasePlant;
import model.game.entities.zombies.attack.AttackBehavior;
import model.game.entities.zombies.attack.PlantEatingAttack;
import model.game.entities.zombies.movement.BasicZombieMovement;
import model.game.entities.zombies.movement.MovementBehavior;

public class Zombie extends Entity {
    public static final double ATTACK_REACH = 0.15;

    private final ZombieType type;
    private final int waveNumber;
    private final int maximumHitPoints;
    private final MovementBehavior movementBehavior;
    private final AttackBehavior attackBehavior;
    private int lane;

    private int hitPoints;
    private double columnPosition;
    private boolean reachedHouse;
    private boolean deathReported;

    public Zombie(ZombieType type, int waveNumber, int lane, double columnPosition) {
        super(createPosition(lane, columnPosition));
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (waveNumber <= 0) {
            throw new IllegalArgumentException("waveNumber must be positive");
        }
        if (lane < 0 || !Double.isFinite(columnPosition) || columnPosition < 0.0) {
            throw new IllegalArgumentException("zombie position is invalid");
        }

        this.type = type;
        this.waveNumber = waveNumber;
        this.lane = lane;
        this.columnPosition = columnPosition;
        this.maximumHitPoints = type.getHitPoints();
        this.hitPoints = maximumHitPoints;
        this.movementBehavior = new BasicZombieMovement(type.getSpeed());
        this.attackBehavior = new PlantEatingAttack(type.getEatDamagePerSecond());
    }

    private static EntityPosition createPosition(int lane, double columnPosition) {
        int column = (int) Math.floor(Math.max(0.0, columnPosition));
        return new EntityPosition(lane, column);
    }

    @Override
    public void update(float deltaSeconds) {
        if (!isRemoved()) {
            super.update(deltaSeconds);
        }
    }

    public void move(float deltaSeconds, double minimumColumn) {
        movementBehavior.move(this, deltaSeconds, minimumColumn);
    }

    public void eat(BasePlant plant, float deltaSeconds) {
        attackBehavior.attack(this, plant, deltaSeconds);
    }

    public void moveToLane(int newLane) {
        if (newLane < 0) {
            throw new IllegalArgumentException("newLane cannot be negative");
        }
        lane = newLane;
        setEntityPosition(createPosition(lane, columnPosition));
    }

    public void moveTo(double newColumnPosition) {
        if (!Double.isFinite(newColumnPosition) || newColumnPosition < 0.0) {
            throw new IllegalArgumentException("newColumnPosition must be finite and non-negative");
        }
        columnPosition = newColumnPosition;
        setEntityPosition(createPosition(lane, columnPosition));
    }

    public void takeDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage cannot be negative");
        }
        hitPoints = Math.max(0, hitPoints - damage);
    }

    public void kill() {
        hitPoints = 0;
    }

    public boolean isDead() {
        return hitPoints <= 0;
    }

    public void markReachedHouse() {
        reachedHouse = true;
    }

    public boolean hasReachedHouse() {
        return reachedHouse;
    }

    public boolean isDeathReported() {
        return deathReported;
    }

    public void markDeathReported() {
        deathReported = true;
    }

    public ZombieType getType() {
        return type;
    }

    public String getName() {
        return type.getDisplayName();
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public int getLane() {
        return lane;
    }

    public double getColumnPosition() {
        return columnPosition;
    }

    public int getHitPoints() {
        return hitPoints;
    }

    public int getMaximumHitPoints() {
        return maximumHitPoints;
    }

    public int getWavePointCost() {
        return type.getWavePointCost();
    }
}
