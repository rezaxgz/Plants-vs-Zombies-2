package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Pure stable-ID diff used by the authoritative graphical actor registries. */
public final class EntityReconciliation {
    public record Changes(Set<String> added, Set<String> retained,
            Set<String> removed) {
        public Changes {
            added = Set.copyOf(added);
            retained = Set.copyOf(retained);
            removed = Set.copyOf(removed);
        }
    }

    private EntityReconciliation() {
    }

    public static Changes between(Collection<String> currentIds,
            Collection<String> incomingIds) {
        LinkedHashSet<String> current = checked(currentIds, "current");
        LinkedHashSet<String> incoming = checked(incomingIds, "incoming");
        LinkedHashSet<String> added = new LinkedHashSet<>(incoming);
        added.removeAll(current);
        LinkedHashSet<String> retained = new LinkedHashSet<>(incoming);
        retained.retainAll(current);
        LinkedHashSet<String> removed = new LinkedHashSet<>(current);
        removed.removeAll(incoming);
        return new Changes(added, retained, removed);
    }

    private static LinkedHashSet<String> checked(Collection<String> ids,
            String name) {
        if (ids == null) {
            throw new IllegalArgumentException(name + " IDs are required");
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank() || !result.add(id)) {
                throw new IllegalArgumentException(
                        name + " IDs must be nonblank and unique");
            }
        }
        return result;
    }
}
