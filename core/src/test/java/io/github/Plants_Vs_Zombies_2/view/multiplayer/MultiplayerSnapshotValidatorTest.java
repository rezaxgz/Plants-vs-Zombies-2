package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchEntitySnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchPlayerSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;

class MultiplayerSnapshotValidatorTest {
    @Test
    void acceptsACompleteAuthoritativeSnapshot() {
        assertNull(MultiplayerSnapshotValidator.snapshotError(
                snapshot(List.of(), List.of()), assignment()));
    }

    @Test
    void rejectsIncompleteAssignmentBeforeScreenConstruction() {
        MatchAssignment malformed = new MatchAssignment("m1", "alice", null,
                null, 1L, MatchStatus.PRE_GAME);
        assertNotNull(MultiplayerSnapshotValidator.assignmentError(malformed));
        assertNotNull(MultiplayerSnapshotValidator.snapshotError(
                snapshot(List.of(), List.of()), malformed));
    }

    @Test
    void rejectsInvalidAndDuplicateGraphicalEntityState() {
        MatchEntitySnapshot duplicatePlant = entity("same", "Peashooter",
                MatchRole.PLANTS);
        MatchEntitySnapshot duplicateZombie = entity("same", "BASIC",
                MatchRole.ZOMBIES);
        assertNotNull(MultiplayerSnapshotValidator.snapshotError(
                snapshot(List.of(duplicatePlant), List.of(duplicateZombie)),
                assignment()));

        MatchEntitySnapshot outOfBounds = new MatchEntitySnapshot(
                "plant-2", "Peashooter", MatchRole.PLANTS,
                8, 0, 0.0, 100, 100);
        assertNotNull(MultiplayerSnapshotValidator.snapshotError(
                snapshot(List.of(outOfBounds), List.of()), assignment()));
    }

    private static MatchAssignment assignment() {
        return new MatchAssignment("m1", "alice", "bob", MatchRole.PLANTS,
                1L, MatchStatus.PRE_GAME);
    }

    private static MatchEntitySnapshot entity(String id, String type,
            MatchRole role) {
        return new MatchEntitySnapshot(id, type, role, 0, 0, 0.0,
                100, 100);
    }

    private static MatchStateSnapshot snapshot(
            List<MatchEntitySnapshot> plants,
            List<MatchEntitySnapshot> zombies) {
        return new MatchStateSnapshot("m1", MatchStatus.ACTIVE,
                20L, 3L, 1_000L, 1.0, 119.0, "FIRST_BITE", 7L,
                5, 9, 3,
                List.of(
                        new MatchPlayerSnapshot("alice", MatchRole.PLANTS,
                                true),
                        new MatchPlayerSnapshot("bob", MatchRole.ZOMBIES,
                                true)),
                500, 300, plants, zombies, List.of(),
                List.of(true, true, true, true, true), null, null);
    }
}
