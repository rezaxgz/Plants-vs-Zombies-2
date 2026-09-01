package io.github.Plants_Vs_Zombies_2.view.multiplayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchEntitySnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchPlayerSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchProjectileSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.view.presentation.Phase3Text;

/** Structural checks that keep malformed wire state out of graphical screens. */
public final class MultiplayerSnapshotValidator {
    private MultiplayerSnapshotValidator() {
    }

    public static String assignmentError(MatchAssignment assignment) {
        if (assignment == null) {
            return "The server returned no match assignment.";
        }
        if (!Phase3Text.hasText(assignment.getMatchId())) {
            return "The server returned a match without an identifier.";
        }
        if (!Phase3Text.hasText(assignment.getLocalUsername())) {
            return "The match assignment is missing the local player.";
        }
        if (!Phase3Text.hasText(assignment.getOpponentUsername())) {
            return "The match assignment is missing the opponent.";
        }
        if (assignment.getRole() == null) {
            return "The match assignment is missing your role.";
        }
        if (assignment.getStatus() == null) {
            return "The match assignment is missing its status.";
        }
        return null;
    }

    public static String snapshotError(MatchStateSnapshot snapshot,
            MatchAssignment assignment) {
        if (snapshot == null) {
            return "The server returned no match state.";
        }
        String assignmentError = assignmentError(assignment);
        if (assignmentError != null) {
            return assignmentError;
        }
        if (!assignment.getMatchId().equals(snapshot.getMatchId())) {
            return "The server returned state for a different match.";
        }
        if (snapshot.getStatus() == null) {
            return "The match state is missing its status.";
        }
        if (!Phase3Text.hasText(snapshot.getLevel())
                || snapshot.getSimulationTick() < 0
                || snapshot.getRevision() < 0
                || !Double.isFinite(snapshot.getElapsedSeconds())
                || !Double.isFinite(snapshot.getRemainingSeconds())
                || snapshot.getElapsedSeconds() < 0
                || snapshot.getRemainingSeconds() < 0
                || snapshot.getPlantResource() < 0
                || snapshot.getZombieResource() < 0) {
            return "The server returned invalid match metadata.";
        }
        int rows = snapshot.getBoardRows();
        int columns = snapshot.getBoardColumns();
        if (rows <= 0 || columns <= 0
                || snapshot.getRedLineColumn() < 0
                || snapshot.getRedLineColumn() >= columns) {
            return "The server returned invalid board dimensions.";
        }
        List<Boolean> brains = snapshot.getBrainsAvailable();
        if (brains == null || brains.size() != rows
                || brains.stream()
                        .anyMatch(value -> value == null)) {
            return "The server returned an invalid brain layout.";
        }
        List<MatchPlayerSnapshot> players = snapshot.getPlayers();
        if (players == null || players.size() != 2) {
            return "The server returned an invalid player list.";
        }
        Set<String> playerNames = new HashSet<>();
        Set<MatchRole> playerRoles = new HashSet<>();
        for (MatchPlayerSnapshot player : players) {
            if (player == null || !Phase3Text.hasText(player.getUsername())
                    || player.getRole() == null
                    || !playerNames.add(player.getUsername())
                    || !playerRoles.add(player.getRole())) {
                return "The server returned an invalid player entry.";
            }
        }
        if (!playerNames.contains(assignment.getLocalUsername())
                || !playerNames.contains(assignment.getOpponentUsername())
                || !playerRoles.contains(MatchRole.PLANTS)
                || !playerRoles.contains(MatchRole.ZOMBIES)) {
            return "The match players do not match the assignment.";
        }
        Set<String> entityIds = new HashSet<>();
        String entityError = entityError(snapshot, snapshot.getPlants(),
                MatchRole.PLANTS, entityIds);
        if (entityError != null) {
            return entityError;
        }
        entityError = entityError(snapshot, snapshot.getZombies(),
                MatchRole.ZOMBIES, entityIds);
        if (entityError != null) {
            return entityError;
        }
        List<MatchProjectileSnapshot> projectiles = snapshot.getProjectiles();
        if (projectiles == null) {
            return "The server returned an invalid projectile list.";
        }
        Set<String> projectileIds = new HashSet<>();
        for (MatchProjectileSnapshot projectile : projectiles) {
            if (projectile == null
                    || !Phase3Text.hasText(projectile.getProjectileId())
                    || !Phase3Text.hasText(projectile.getProjectileType())
                    || !projectileIds.add(projectile.getProjectileId())
                    || projectile.getLane() < 0 || projectile.getLane() >= rows
                    || !Double.isFinite(projectile.getColumnPosition())
                    || !Double.isFinite(
                            projectile.getVelocityColumnsPerSecond())
                    || projectile.getDamage() < 0) {
                return "The server returned an invalid projectile.";
            }
        }
        if (snapshot.getStatus() == MatchStatus.FINISHED
                && (snapshot.getWinner() == null
                        || snapshot.getFinishReason() == null)) {
            return "The finished match is missing its outcome.";
        }
        return null;
    }

    private static String entityError(MatchStateSnapshot snapshot,
            List<MatchEntitySnapshot> entities, MatchRole expectedRole,
            Set<String> entityIds) {
        if (entities == null) {
            return "The server returned an invalid "
                    + (expectedRole == MatchRole.PLANTS
                            ? "plant" : "zombie") + " list.";
        }
        for (MatchEntitySnapshot entity : entities) {
            if (entity == null || !Phase3Text.hasText(entity.getEntityId())
                    || !Phase3Text.hasText(entity.getEntityType())
                    || !entityIds.add(entity.getEntityId())
                    || entity.getOwnerRole() != expectedRole
                    || entity.getRow() < 0
                    || entity.getRow() >= snapshot.getBoardRows()
                    || !Double.isFinite(entity.getColumnPosition())
                    || entity.getMaximumHealth() <= 0
                    || entity.getHealth() < 0
                    || entity.getHealth() > entity.getMaximumHealth()) {
                return "The server returned an invalid "
                        + (expectedRole == MatchRole.PLANTS
                                ? "plant" : "zombie") + ".";
            }
        }
        return null;
    }
}
