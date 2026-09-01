package io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.model.game.minigame.IZombieLevel;

class MultiplayerIZombieGameTest {
    @Test
    void sideRulesAndResourcesAreIndependentAndAtomic() throws Exception {
        MultiplayerIZombieGame game = game(300, 100);

        String plantId = game.placePlant("Peashooter", 0, 0);
        assertEquals(200, game.getPlantResource());
        assertEquals(100, game.getZombieResource());

        assertRule(MultiplayerRuleError.INVALID_POSITION,
                () -> game.placePlant("Peashooter", 0, 4));
        assertRule(MultiplayerRuleError.POSITION_OCCUPIED,
                () -> game.placePlant("Sunflower", 0, 0));
        assertRule(MultiplayerRuleError.UNKNOWN_PLANT,
                () -> game.placePlant("DefinitelyNotAPlant", 0, 1));
        assertEquals(200, game.getPlantResource());

        String zombieId = game.placeZombie("BASIC", 0, 4);
        assertEquals(75, game.getZombieResource());
        assertEquals(200, game.getPlantResource());
        assertRule(MultiplayerRuleError.INVALID_POSITION,
                () -> game.placeZombie("BASIC", 1, 3));
        assertRule(MultiplayerRuleError.UNKNOWN_ZOMBIE,
                () -> game.placeZombie("ZOMBOSS_EGYPT", 1, 4));
        assertNotEquals(plantId, zombieId);
    }

    @Test
    void removalHasNoRefundAndEntityIdsAreNeverReused() throws Exception {
        MultiplayerIZombieGame game = game(300, 100);
        String first = game.placePlant("Peashooter", 0, 0);
        assertEquals(first, game.removePlant(first));
        assertEquals(200, game.getPlantResource());
        String second = game.placePlant("Sunflower", 0, 1);

        assertNotEquals(first, second);
        assertEquals(150, game.getPlantResource());
        assertRule(MultiplayerRuleError.ENTITY_NOT_FOUND,
                () -> game.removePlant(first));
        String zombie = game.placeZombie("BASIC", 0, 4);
        assertRule(MultiplayerRuleError.NOT_ENTITY_OWNER,
                () -> game.removePlant(zombie));
    }

    @Test
    void outsideBoardAndInsufficientResourceDoNotMutate() {
        MultiplayerIZombieGame game = game(50, 20);
        assertRule(MultiplayerRuleError.INVALID_POSITION,
                () -> game.placePlant("Sunflower", -1, 0));
        assertRule(MultiplayerRuleError.INSUFFICIENT_RESOURCE,
                () -> game.placePlant("Peashooter", 0, 0));
        assertRule(MultiplayerRuleError.INSUFFICIENT_RESOURCE,
                () -> game.placeZombie("BASIC", 0, 4));
        assertEquals(50, game.getPlantResource());
        assertEquals(20, game.getZombieResource());
        assertEquals(0, game.getPlants().size());
        assertEquals(0, game.getZombies().size());
    }

    @Test
    void authoritativeSubzeroBrainApproachDoesNotBreakLegacyBoardMirror()
            throws Exception {
        MultiplayerIZombieGame game = game(300, 100);
        String zombieId = game.placeZombie("BASIC", 2, 4);

        game.synchronizeZombiePosition(zombieId, 2, -0.1);

        assertEquals(1, game.getZombies().size());
        assertEquals(0, game.getZombies().get(0).getColumn());
        assertEquals(2, game.getZombies().get(0).getRow());
    }

    private static MultiplayerIZombieGame game(int plants, int zombies) {
        return new MultiplayerIZombieGame(new MultiplayerIZombieConfig(
                IZombieLevel.FIRST_BITE, 5, 9, plants, zombies), 1234L);
    }

    private static void assertRule(MultiplayerRuleError expected,
            ThrowingAction action) {
        MultiplayerRuleException failure = assertThrows(
                MultiplayerRuleException.class, action::run);
        assertEquals(expected, failure.getError());
    }

    @FunctionalInterface
    private interface ThrowingAction { void run() throws Exception; }
}
