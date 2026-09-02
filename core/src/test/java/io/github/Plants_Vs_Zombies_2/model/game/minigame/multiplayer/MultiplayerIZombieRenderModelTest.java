package io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.Projectile;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchEntitySnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchPlayerSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchProjectileSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;

class MultiplayerIZombieRenderModelTest {
    @Test
    void authoritativeSnapshotsPopulateRealGameEntitiesWithoutSimulation() {
        MatchEntitySnapshot plant = new MatchEntitySnapshot(
                "plant-1", "Peashooter", MatchRole.PLANTS,
                2, 2, 2.0, 250, 300);
        MatchEntitySnapshot zombie = new MatchEntitySnapshot(
                "zombie-1", "BASIC", MatchRole.ZOMBIES,
                2, 7, 7.25, 180, 200);
        MatchProjectileSnapshot projectile = new MatchProjectileSnapshot(
                "projectile-1", "Peashooter_PROJECTILE",
                2, 3.25, 4.0, 20);
        MultiplayerIZombieRenderModel model =
                new MultiplayerIZombieRenderModel(
                        snapshot(500, 300, 79.2,
                                10L, 4L, 1_000L,
                                List.of(plant), List.of(zombie),
                                List.of(projectile),
                                List.of(true, true, true, true, true)),
                        MatchRole.PLANTS, Map.of("Peashooter", 2));

        assertEquals(500, model.getSunCount());
        assertEquals(79.2, model.getRemainingSeconds(), 0.001);
        assertEquals(1, model.getBoard().getPlants().size());
        assertEquals(1, model.getBoard().getZombies().size());
        assertEquals(1, model.getBoard().getProjectiles().size());
        assertEquals(2, model.getBoard().getPlants().get(0).getLevel());
        model.advancePresentation(0.2f);

        MatchEntitySnapshot movedZombie = new MatchEntitySnapshot(
                "zombie-1", "BASIC", MatchRole.ZOMBIES,
                2, 6, 6.85, 175, 200);

        List<MultiplayerIZombieRenderModel.RemoteProjectileLaunch> launches =
                model.applySnapshot(snapshot(425, 275, 63.8,
                        14L, 5L, 1_200L,
                        List.of(), List.of(movedZombie),
                        List.of(new MatchProjectileSnapshot(
                                "projectile-1", "Peashooter_PROJECTILE",
                                2, 4.0, 4.0, 20)),
                        List.of(false, true, true, true, true)));

        assertEquals(425, model.getSunCount());
        assertEquals(63.8, model.getRemainingSeconds(), 0.001);
        assertTrue(model.getBoard().getPlants().isEmpty());
        assertFalse(model.isBrainAvailable(0));
        assertTrue(launches.isEmpty());
        Projectile mirrored = model.getBoard().getProjectiles().get(0);
        assertEquals(4.05, mirrored.getColumnPosition(), 0.001);

        model.advancePresentation(0.1f);
        assertEquals(7.05,
                model.getBoard().getZombies().get(0).getColumnPosition(),
                0.001);
        assertEquals(4.425, mirrored.getColumnPosition(), 0.001);

        model.advancePresentation(0.1f);
        assertEquals(6.85,
                model.getBoard().getZombies().get(0).getColumnPosition(),
                0.001);
        assertEquals(4.8, mirrored.getColumnPosition(), 0.001);
        assertEquals(63.6, model.getRemainingSeconds(), 0.001);

        assertTrue(model.applySnapshot(snapshot(425, 275, 63.8,
                14L, 5L, 1_200L,
                List.of(), List.of(movedZombie), List.of(),
                List.of(false, true, true, true, true))).isEmpty());
        assertEquals(1, model.getBoard().getProjectiles().size());
    }

    private static MatchStateSnapshot snapshot(int plantResource,
            int zombieResource, double remainingSeconds,
            long tick, long revision, long timestamp,
            List<MatchEntitySnapshot> plants,
            List<MatchEntitySnapshot> zombies,
            List<MatchProjectileSnapshot> projectiles,
            List<Boolean> brains) {
        return new MatchStateSnapshot("match-1", MatchStatus.ACTIVE,
                tick, revision, timestamp, 40.0, remainingSeconds,
                "FIRST_BITE", 7L, 5, 9, 3,
                List.of(new MatchPlayerSnapshot(
                                "plants", MatchRole.PLANTS, true),
                        new MatchPlayerSnapshot(
                                "zombies", MatchRole.ZOMBIES, true)),
                plantResource, zombieResource, plants, zombies,
                projectiles, brains, null, null);
    }
}
