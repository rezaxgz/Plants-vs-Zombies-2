package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFactory;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;

/**
 * Multiplayer I, Zombie uses the existing game screen verbatim. The only
 * multiplayer-specific presentation installed by {@link GameScreen} is the
 * remaining-time HUD and match chat; all board, card, entity, animation,
 * effect and projectile rendering stays in the shared single-player path.
 */
public final class MultiplayerIZombieGameScreen extends GameScreen {
    public MultiplayerIZombieGameScreen(ScreenNavigator navigator,
            MatchAssignment assignment, MatchStateSnapshot initialSnapshot) {
        this(navigator, assignment, initialSnapshot, List.of());
    }

    public MultiplayerIZombieGameScreen(ScreenNavigator navigator,
            MatchAssignment assignment, MatchStateSnapshot initialSnapshot,
            List<String> plantLoadout) {
        super(navigator, assignment, initialSnapshot, plantLoadout);
    }

    /** Retained for loadout validation tests and older callers. */
    static List<String> resolvePlantCardTypes(List<String> requested) {
        if (requested != null && requested.size() == 8) {
            List<String> distinct = new ArrayList<>();
            for (String type : requested) {
                if (type == null || type.isBlank()
                        || PlantFactory.createPlant(type,
                                new EntityPosition(0, 0)) == null
                        || distinct.stream().anyMatch(
                                value -> value.equalsIgnoreCase(type))) {
                    distinct.clear();
                    break;
                }
                distinct.add(type);
            }
            if (distinct.size() == 8) return List.copyOf(distinct);
        }
        return MultiplayerVisualCatalog.plantTypes();
    }

    /** Retained for the input-layer regression test. */
    static void configureBoardInputLayers(Actor boardGrid, Group cells,
            Group brains, Group entities, Group projectiles) {
        boardGrid.setTouchable(Touchable.disabled);
        cells.setTouchable(Touchable.childrenOnly);
        brains.setTouchable(Touchable.disabled);
        entities.setTouchable(Touchable.childrenOnly);
        projectiles.setTouchable(Touchable.disabled);
    }

    /** Shared role/red-line validation used by existing boundary tests. */
    static PlacementCommand placementCommand(MatchRole role,
            String canonicalType, int row, int column, int redLine,
            boolean commandInFlight, boolean terminal) {
        if (role == null || canonicalType == null || canonicalType.isBlank()
                || row < 0 || column < 0 || commandInFlight || terminal) {
            return null;
        }
        boolean allowed = role == MatchRole.PLANTS
                ? column <= redLine : column > redLine;
        return allowed
                ? new PlacementCommand(role, canonicalType, row, column)
                : null;
    }

    record PlacementCommand(MatchRole role, String canonicalType,
            int row, int column) { }
}
