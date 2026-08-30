package io.github.Plants_Vs_Zombies_2.model.game.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.model.game.GameStatus;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;

class IZombieRegressionTest {
    @Test
    void initializationAndZombiePlacementRemainSinglePlayerCompatible() {
        IZombie game = new IZombie(IZombieLevel.FIRST_BITE);

        assertEquals(GameStatus.ACTIVE, game.getStatus());
        assertEquals(IZombie.INITIAL_SUN, game.getSunCount());
        assertEquals(IZombieLevel.FIRST_BITE.getPlantCount(),
                game.getRemainingPlantCount());
        assertEquals(game.getBoard().getNumberOfRows(),
                game.getLivingSunProducerCount());
        assertEquals(0, game.getEatenBrainCount());
        assertFalse(game.allowsDirectPlanting());

        assertEquals(IZombiePlacementResult.SUCCESS,
                game.placeZombie("BASIC", new EntityPosition(0, 4)));
        assertEquals(125, game.getSunCount());
        assertEquals(IZombiePlacementResult.POSITION_OCCUPIED,
                game.placeZombie("BASIC", new EntityPosition(0, 4)));
        assertEquals(IZombiePlacementResult.RECHARGING,
                game.placeZombie("BASIC", new EntityPosition(1, 4)));
        assertEquals(125, game.getSunCount());
    }

    @Test
    void eatingEveryBrainStillCompletesSinglePlayerGame() {
        IZombie game = new IZombie(IZombieLevel.FIRST_BITE);
        for (int row = 0; row < game.getBoard().getNumberOfRows(); row++) {
            Zombie zombie = new Zombie(ZombieType.BASIC, 0, row, 4, false);
            zombie.markReachedHouse();
            game.getBoard().addZombie(zombie);
        }

        game.update(0.0f);

        assertEquals(game.getBoard().getNumberOfRows(), game.getEatenBrainCount());
        assertEquals(GameStatus.WON, game.getStatus());
    }
}
