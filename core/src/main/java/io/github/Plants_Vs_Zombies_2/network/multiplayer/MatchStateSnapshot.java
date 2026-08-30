package io.github.Plants_Vs_Zombies_2.network.multiplayer;

import java.util.List;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchStatus;

public final class MatchStateSnapshot {
    private final String matchId;
    private final MatchStatus status;
    private final long revision;
    private final long serverTimestampEpochMillis;
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
    private final List<Boolean> brainsAvailable;

    public MatchStateSnapshot(String matchId, MatchStatus status, long revision,
            long serverTimestampEpochMillis, String level, long seed,
            int boardRows, int boardColumns, int redLineColumn,
            List<MatchPlayerSnapshot> players, int plantResource,
            int zombieResource, List<MatchEntitySnapshot> plants,
            List<MatchEntitySnapshot> zombies, List<Boolean> brainsAvailable) {
        this.matchId = matchId;
        this.status = status;
        this.revision = revision;
        this.serverTimestampEpochMillis = serverTimestampEpochMillis;
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
        this.brainsAvailable = List.copyOf(brainsAvailable);
    }

    public String getMatchId() { return matchId; }
    public MatchStatus getStatus() { return status; }
    public long getRevision() { return revision; }
    public long getServerTimestampEpochMillis() { return serverTimestampEpochMillis; }
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
    public List<Boolean> getBrainsAvailable() { return brainsAvailable; }
}
