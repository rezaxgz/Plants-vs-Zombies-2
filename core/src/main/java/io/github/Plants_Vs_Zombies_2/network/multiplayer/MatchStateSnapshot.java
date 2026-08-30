package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import java.util.List;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;

/** Immutable wire snapshot of one authoritative multiplayer I, Zombie match. */
public final class MatchStateSnapshot {
    private final String matchId;
    private final MatchStatus status;
    private final long simulationTick;
    private final long revision;
    private final long serverTimestampEpochMillis;
    private final double elapsedSeconds;
    private final double remainingSeconds;
    private final String level;
    private final long seed;
    private final int boardRows;
    private final int boardColumns;
    private final int redLineColumn;
    private final List<MatchPlayerSnapshot> players;
    private final int plantResource;
    private final int zombieResource;
    private final List<MatchEntitySnapshot> plants;
    private final List<MatchEntitySnapshot> zombies;
    private final List<MatchProjectileSnapshot> projectiles;
    private final List<Boolean> brainsAvailable;
    private final MatchRole winner;
    private final MatchFinishReason finishReason;

    /** Stage 5-compatible constructor retained for existing callers/tests. */
    public MatchStateSnapshot(String matchId, MatchStatus status, long revision,
            long serverTimestampEpochMillis, String level, long seed,
            int boardRows, int boardColumns, int redLineColumn,
            List<MatchPlayerSnapshot> players, int plantResource,
            int zombieResource, List<MatchEntitySnapshot> plants,
            List<MatchEntitySnapshot> zombies, List<Boolean> brainsAvailable) {
        this(matchId, status, 0L, revision, serverTimestampEpochMillis,
                0.0, 0.0, level, seed, boardRows, boardColumns,
                redLineColumn, players, plantResource, zombieResource,
                plants, zombies, List.of(), brainsAvailable, null, null);
    }

    public MatchStateSnapshot(String matchId, MatchStatus status,
            long simulationTick, long revision, long serverTimestampEpochMillis,
            double elapsedSeconds, double remainingSeconds,
            String level, long seed, int boardRows, int boardColumns,
            int redLineColumn, List<MatchPlayerSnapshot> players,
            int plantResource, int zombieResource,
            List<MatchEntitySnapshot> plants,
            List<MatchEntitySnapshot> zombies,
            List<MatchProjectileSnapshot> projectiles,
            List<Boolean> brainsAvailable, MatchRole winner,
            MatchFinishReason finishReason) {
        this.matchId = matchId;
        this.status = status;
        this.simulationTick = simulationTick;
        this.revision = revision;
        this.serverTimestampEpochMillis = serverTimestampEpochMillis;
        this.elapsedSeconds = elapsedSeconds;
        this.remainingSeconds = remainingSeconds;
        this.level = level;
        this.seed = seed;
        this.boardRows = boardRows;
        this.boardColumns = boardColumns;
        this.redLineColumn = redLineColumn;
        this.players = List.copyOf(players);
        this.plantResource = plantResource;
        this.zombieResource = zombieResource;
        this.plants = List.copyOf(plants);
        this.zombies = List.copyOf(zombies);
        this.projectiles = List.copyOf(projectiles);
        this.brainsAvailable = List.copyOf(brainsAvailable);
        this.winner = winner;
        this.finishReason = finishReason;
    }

    public String getMatchId() { return matchId; }
    public MatchStatus getStatus() { return status; }
    public long getSimulationTick() { return simulationTick; }
    public long getRevision() { return revision; }
    public long getServerTimestampEpochMillis() { return serverTimestampEpochMillis; }
    public double getElapsedSeconds() { return elapsedSeconds; }
    public double getRemainingSeconds() { return remainingSeconds; }
    public String getLevel() { return level; }
    public long getSeed() { return seed; }
    public int getBoardRows() { return boardRows; }
    public int getBoardColumns() { return boardColumns; }
    public int getRedLineColumn() { return redLineColumn; }
    public List<MatchPlayerSnapshot> getPlayers() { return players; }
    public int getPlantResource() { return plantResource; }
    public int getZombieResource() { return zombieResource; }
    public List<MatchEntitySnapshot> getPlants() { return plants; }
    public List<MatchEntitySnapshot> getZombies() { return zombies; }
    public List<MatchProjectileSnapshot> getProjectiles() { return projectiles; }
    public List<Boolean> getBrainsAvailable() { return brainsAvailable; }
    public MatchRole getWinner() { return winner; }
    public MatchFinishReason getFinishReason() { return finishReason; }
}
