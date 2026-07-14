package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Piano zombie's fast movement and crushing ability.
 */
public class PianoCrushAbility extends ZombieAbility {
    private double fastMoveSpeed;
    private boolean playing;

    public PianoCrushAbility(double fastMoveSpeed) {
        super(0);
        this.fastMoveSpeed = fastMoveSpeed;
        this.playing = true;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        // Move fast and crush plants on collision
        // When playing, moves at fastMoveSpeed and destroys plants instantly
        return true;
    }

    public double getFastMoveSpeed() { return fastMoveSpeed; }
    public boolean isPlaying() { return playing; }
    public void setPlaying(boolean playing) { this.playing = playing; }
}
