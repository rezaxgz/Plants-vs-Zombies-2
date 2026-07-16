package model.game.entities.zombies.abilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.game.Board;
import model.game.entities.other.IceBlock;
import model.game.entities.zombies.Zombie;

/**
 * Creates and synchronizes the row of ice blocks pushed by a Troglobite.
 * Destroyed blocks disappear independently; surviving blocks remain in front
 * of the pusher.
 */
public class IceBlockPushAbility extends ZombieAbility {
    private static final double BLOCK_SPACING_TILES = 0.75;

    private final int numberOfIceBlocks;
    private final List<IceBlock> iceBlocks = new ArrayList<>();

    private boolean initialized;
    private boolean spawnedThisUse;

    public IceBlockPushAbility(int numberOfIceBlocks) {
        super(0.0);
        if (numberOfIceBlocks <= 0) {
            throw new IllegalArgumentException(
                    "numberOfIceBlocks must be positive");
        }
        this.numberOfIceBlocks = numberOfIceBlocks;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        spawnedThisUse = false;
        if (zombie == null || board == null
                || zombie.isDead() || zombie.isHypnotized()) {
            return false;
        }

        if (!initialized) {
            spawnIceBlocks(zombie, board);
            initialized = true;
            spawnedThisUse = true;
        }
        synchronizeWithPusher(zombie, board);
        return !getActiveIceBlocks().isEmpty();
    }

    private void spawnIceBlocks(Zombie zombie, Board board) {
        for (int index = 0; index < numberOfIceBlocks; index++) {
            double column = Math.max(0.0,
                    zombie.getColumnPosition()
                            - BLOCK_SPACING_TILES * (index + 1));
            IceBlock block = new IceBlock(
                    zombie.getLane(), column, index);
            board.addEntity(block);
            iceBlocks.add(block);
        }
    }

    private void synchronizeWithPusher(Zombie zombie, Board board) {
        for (IceBlock block : iceBlocks) {
            if (block.isDestroyed()) {
                continue;
            }
            double desiredColumn = zombie.getColumnPosition()
                    - BLOCK_SPACING_TILES
                            * (block.getFormationIndex() + 1);
            block.moveTo(zombie.getLane(), desiredColumn,
                    board.getNumberOfColumns());
        }
    }

    public List<IceBlock> getActiveIceBlocks() {
        List<IceBlock> active = new ArrayList<>();
        for (IceBlock block : iceBlocks) {
            if (!block.isDestroyed()) {
                active.add(block);
            }
        }
        return Collections.unmodifiableList(active);
    }

    public boolean didSpawnThisUse() {
        return spawnedThisUse;
    }

    public int getNumberOfIceBlocks() {
        return numberOfIceBlocks;
    }
}
