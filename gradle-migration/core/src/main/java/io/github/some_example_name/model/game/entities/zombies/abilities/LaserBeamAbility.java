package io.github.some_example_name.model.game.entities.zombies.abilities;

import io.github.some_example_name.model.game.Board;
import io.github.some_example_name.model.game.entities.plants.BasePlant;
import io.github.some_example_name.model.game.entities.zombies.Zombie;

/**
 * Crystal Skull steals stored sun for five seconds while charging, then
 * destroys every plant in the next four tiles of its lane.
 */
public class LaserBeamAbility extends ZombieAbility {
    public static final int SUN_STOLEN_PER_SECOND = 25;
    public static final double DETECTION_RADIUS_TILES = 4.0;
    public static final double LASER_LENGTH_TILES = 4.0;

    private final int laserDamage;
    private final double configuredLaserLength;
    private final double chargingTime;

    private boolean charging;
    private double chargeElapsed;
    private int processedChargeSeconds;
    private int pendingSunTicks;
    private int totalStolenSun;
    private boolean deathRefundReleased;

    private boolean startedChargingThisUse;
    private boolean firedThisUse;
    private int lastDestroyedPlantCount;

    public LaserBeamAbility(int laserDamage,
            double laserLength, double chargingTime) {
        super(0.0);
        if (laserDamage <= 0
                || !Double.isFinite(laserLength)
                || laserLength <= 0.0
                || !Double.isFinite(chargingTime)
                || chargingTime <= 0.0) {
            throw new IllegalArgumentException(
                    "invalid Crystal Skull configuration");
        }
        this.laserDamage = laserDamage;
        this.configuredLaserLength = laserLength;
        this.chargingTime = chargingTime;
    }

    @Override
    public void update(double deltaSeconds) {
        if (!Double.isFinite(deltaSeconds)
                || deltaSeconds < 0.0) {
            throw new IllegalArgumentException(
                    "deltaSeconds must be finite and non-negative");
        }
        if (!charging) {
            return;
        }

        chargeElapsed = Math.min(
                chargingTime, chargeElapsed + deltaSeconds);
        int completedSeconds = Math.min(
                (int) Math.floor(chargingTime),
                (int) Math.floor(
                        chargeElapsed + 0.000001));
        if (completedSeconds > processedChargeSeconds) {
            pendingSunTicks += completedSeconds - processedChargeSeconds;
            processedChargeSeconds = completedSeconds;
        }
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        startedChargingThisUse = false;
        firedThisUse = false;
        lastDestroyedPlantCount = 0;
        if (zombie == null || board == null
                || zombie.isDead() || zombie.isHypnotized()
                || zombie.isFrozen() || zombie.isStunned()) {
            return false;
        }

        if (!charging) {
            if (!hasPlantInDetectionRadius(zombie, board)) {
                return false;
            }
            startCharging();
            startedChargingThisUse = true;
            return true;
        }

        if (chargeElapsed + 0.000001 < chargingTime) {
            return false;
        }

        lastDestroyedPlantCount = fireLaser(zombie, board);
        charging = false;
        chargeElapsed = 0.0;
        processedChargeSeconds = 0;
        firedThisUse = true;
        return true;
    }

    private void startCharging() {
        charging = true;
        chargeElapsed = 0.0;
        processedChargeSeconds = 0;
        pendingSunTicks = 0;
    }

    private boolean hasPlantInDetectionRadius(
            Zombie zombie, Board board) {
        double radiusSquared = DETECTION_RADIUS_TILES
                * DETECTION_RADIUS_TILES;
        for (BasePlant plant : board.getPlants()) {
            if (plant.isRemoved()
                    || plant.getEntityPosition() == null) {
                continue;
            }
            double rowDelta = zombie.getLane()
                    - plant.getEntityPosition().getRow();
            double columnDelta = zombie.getColumnPosition()
                    - plant.getEntityPosition().getColumn();
            if (rowDelta * rowDelta
                    + columnDelta * columnDelta <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private int fireLaser(Zombie zombie, Board board) {
        int destroyed = 0;
        for (BasePlant plant : board.getPlants()) {
            if (!isLaserTarget(zombie, plant)) {
                continue;
            }
            int lethalDamage = Math.max(
                    laserDamage, plant.getCurrentHP());
            plant.takeDamage(lethalDamage);
            destroyed++;
        }
        return destroyed;
    }

    private boolean isLaserTarget(
            Zombie zombie, BasePlant plant) {
        if (plant.isRemoved()
                || plant.getEntityPosition() == null
                || plant.getEntityPosition().getRow() != zombie.getLane()) {
            return false;
        }
        double distance = zombie.getColumnPosition()
                - plant.getEntityPosition().getColumn();
        return distance >= 0.0
                && distance <= LASER_LENGTH_TILES;
    }

    public int drainPendingSunRequest() {
        int request = pendingSunTicks * SUN_STOLEN_PER_SECOND;
        pendingSunTicks = 0;
        return request;
    }

    public void recordStolenSun(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(
                    "stolen sun cannot be negative");
        }
        totalStolenSun += amount;
    }

    public int releaseHalfStolenSun() {
        if (deathRefundReleased) {
            return 0;
        }
        deathRefundReleased = true;
        int refund = totalStolenSun / 2;
        totalStolenSun = 0;
        return refund;
    }

    public boolean didStartChargingThisUse() {
        return startedChargingThisUse;
    }

    public boolean didFireThisUse() {
        return firedThisUse;
    }

    public int getLastDestroyedPlantCount() {
        return lastDestroyedPlantCount;
    }

    public boolean isCharging() {
        return charging;
    }

    public int getTotalStolenSun() {
        return totalStolenSun;
    }

    public int getLaserDamage() {
        return laserDamage;
    }

    public double getLaserLength() {
        return configuredLaserLength;
    }

    public double getLaserLengthTiles() {
        return LASER_LENGTH_TILES;
    }

    public double getChargingTime() {
        return chargingTime;
    }
}
