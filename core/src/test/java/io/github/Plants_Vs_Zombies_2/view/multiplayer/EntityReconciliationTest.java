package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class EntityReconciliationTest {
    @Test
    void reportsAddedRetainedAndRemovedStableIds() {
        EntityReconciliation.Changes changes = EntityReconciliation.between(
                List.of("plant-1", "zombie-1"),
                List.of("zombie-1", "projectile-1"));

        assertEquals(Set.of("projectile-1"), changes.added());
        assertEquals(Set.of("zombie-1"), changes.retained());
        assertEquals(Set.of("plant-1"), changes.removed());
    }

    @Test
    void rejectsBlankAndDuplicateIdsBeforeActorMutation() {
        assertThrows(IllegalArgumentException.class,
                () -> EntityReconciliation.between(List.of(), List.of("")));
        assertThrows(IllegalArgumentException.class,
                () -> EntityReconciliation.between(
                        List.of("same", "same"), List.of()));
    }
}
