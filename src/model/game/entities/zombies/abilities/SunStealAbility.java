package model.game.entities.zombies.abilities;

import model.game.Board;
import model.game.entities.zombies.Zombie;

/**
 * Ra zombie's ability to steal sun from the player.
 */
public class SunStealAbility extends ZombieAbility {
    private int maxClaimedSun;
    private int sunStolen;

    public SunStealAbility(int maxClaimedSun) {
        super(3.0); // Steal every 3 seconds
        this.maxClaimedSun = maxClaimedSun;
        this.sunStolen = 0;
    }

    @Override
    public boolean tryUse(Zombie zombie, Board board) {
        if (!canUse() || sunStolen >= maxClaimedSun) return false;

        // Steal sun from game - requires access to Game instance
        // Game game = board.getGame();
        // if (game != null) {
        //     int availableSun = game.getSunCount();
        //     int toSteal = Math.min(25, availableSun);
        //     if (toSteal > 0) {
        //         game.spendSun(toSteal);
        //         sunStolen += toSteal;
        //         resetCooldown();
        //         return true;
        //     }
        // }
        return false;
    }

    public int getSunStolen() { return sunStolen; }
    public int getMaxClaimedSun() { return maxClaimedSun; }
}
