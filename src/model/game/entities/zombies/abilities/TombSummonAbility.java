package model.game.entities.zombies.abilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.game.Board;
import model.game.entities.EntityPosition;
import model.game.entities.zombies.Zombie;
import model.game.structure.Grave;
import model.game.tile.Tile;
import model.game.tile.TileType;

/**
 * Tomb Raiser spends one ammo per cast and creates up to two graves on random
 * empty normal tiles.
 */
public class TombSummonAbility extends ZombieAbility {
    private int ammo;
    private final int tombsToSpawn;
    private final double timeBetweenRaisings;
    private List<EntityPosition> lastSpawnedPositions = Collections.emptyList();

    public TombSummonAbility(int ammo, int tombsToSpawn,
            double timeBetweenRaisings) {
        super(timeBetweenRaisings);
        if (ammo < 0 || tombsToSpawn <= 0
                || !Double.isFinite(timeBetweenRaisings)
                || timeBetweenRaisings < 0.0) {
            throw new IllegalArgumentException(
                    "invalid Tomb Raiser ability configuration");
        }
        this.ammo = ammo;
        this.tombsToSpawn = tombsToSpawn;
        this.timeBetweenRaisings = timeBetweenRaisings;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        lastSpawnedPositions = Collections.emptyList();
        if (!canUse() || ammo <= 0 || zombie == null || board == null
                || zombie.isDead() || zombie.isHypnotized()) {
            return false;
        }

        List<EntityPosition> candidates = findValidPositions(board);
        Collections.shuffle(candidates);
        List<EntityPosition> spawned = new ArrayList<>();
        for (EntityPosition position : candidates) {
            if (spawned.size() >= tombsToSpawn) {
                break;
            }
            if (board.addStructure(new Grave(position))) {
                spawned.add(position);
            }
        }
        if (spawned.isEmpty()) {
            return false;
        }

        ammo--;
        lastSpawnedPositions = Collections.unmodifiableList(spawned);
        resetCooldown();
        return true;
    }

    private List<EntityPosition> findValidPositions(Board board) {
        List<EntityPosition> positions = new ArrayList<>();
        for (int row = 0; row < board.getNumberOfRows(); row++) {
            for (int column = 0; column < board.getNumberOfColumns(); column++) {
                EntityPosition position = new EntityPosition(row, column);
                if (isValidGravePosition(board, position)) {
                    positions.add(position);
                }
            }
        }
        return positions;
    }

    private boolean isValidGravePosition(Board board, EntityPosition position) {
        Tile tile = board.getTileAt(position);
        return tile != null
                && tile.getTileType() == TileType.NORMAL
                && board.getStructureAt(position) == null
                && board.getPlantsAt(position).isEmpty()
                && !hasZombieAt(board, position);
    }

    private boolean hasZombieAt(Board board, EntityPosition position) {
        for (Zombie zombie : board.getZombies()) {
            if (zombie.getLane() == position.getRow()
                    && (int) Math.floor(zombie.getColumnPosition())
                    == position.getColumn()) {
                return true;
            }
        }
        return false;
    }

    public int getAmmo() {
        return ammo;
    }

    public int getTombsToSpawn() {
        return tombsToSpawn;
    }

    public int getLastSpawnedCount() {
        return lastSpawnedPositions.size();
    }

    public List<EntityPosition> getLastSpawnedPositions() {
        return lastSpawnedPositions;
    }

    public double getTimeBetweenRaisings() {
        return timeBetweenRaisings;
    }
}
