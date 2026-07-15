package model.game.entities.projectile.movement;

import model.game.entities.projectile.Projectile;
import model.game.entities.zombies.Zombie;

public final class LobbedProjectileMovement extends ProjectileMovement {
    private static final double TIMER_EPSILON = 0.000001;

    private final double startRow;
    private final double startColumn;
    private final double flightDurationSeconds;
    private final double peakHeight;

    private Zombie lockedTarget;
    private double landingRow;
    private double landingColumn;
    private double elapsedSeconds;
    private double altitude;

    public LobbedProjectileMovement(double startRow, double startColumn,
            Zombie lockedTarget, double flightDurationSeconds, double peakHeight) {
        if (!Double.isFinite(startRow) || !Double.isFinite(startColumn)) {
            throw new IllegalArgumentException("lobbed projectile start must be finite");
        }
        if (lockedTarget == null) {
            throw new IllegalArgumentException("lockedTarget cannot be null");
        }
        if (!Double.isFinite(flightDurationSeconds) || flightDurationSeconds <= 0.0
                || !Double.isFinite(peakHeight) || peakHeight < 0.0) {
            throw new IllegalArgumentException("lobbed projectile arc values are invalid");
        }
        this.startRow = startRow;
        this.startColumn = startColumn;
        this.lockedTarget = lockedTarget;
        this.flightDurationSeconds = flightDurationSeconds;
        this.peakHeight = peakHeight;
        this.landingRow = lockedTarget.getLane();
        this.landingColumn = lockedTarget.getColumnPosition();
    }

    @Override
    public void move(Projectile projectile, float deltaSeconds) {
        if (projectile == null) {
            throw new IllegalArgumentException("projectile cannot be null");
        }
        rememberLiveTargetPosition();
        elapsedSeconds = Math.min(flightDurationSeconds, elapsedSeconds + deltaSeconds);
        double progress = Math.min(1.0, elapsedSeconds / flightDurationSeconds);
        double desiredRow = interpolate(startRow, landingRow, progress);
        double desiredColumn = interpolate(startColumn, landingColumn, progress);
        projectile.translate(desiredRow - projectile.getRowPosition(),
                desiredColumn - projectile.getColumnPosition());
        altitude = 4.0 * peakHeight * progress * (1.0 - progress);
    }

    private void rememberLiveTargetPosition() {
        if (lockedTarget == null) {
            return;
        }
        if (lockedTarget.isDead() || lockedTarget.isRemoved()) {
            lockedTarget = null;
            return;
        }
        landingRow = lockedTarget.getLane();
        landingColumn = lockedTarget.getColumnPosition();
    }

    private static double interpolate(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    public boolean hasLanded() {
        return elapsedSeconds + TIMER_EPSILON >= flightDurationSeconds;
    }

    public double getAltitude() {
        return altitude;
    }

    public double getLandingRow() {
        return landingRow;
    }

    public double getLandingColumn() {
        return landingColumn;
    }

    public Zombie getLockedTarget() {
        return lockedTarget;
    }
}
