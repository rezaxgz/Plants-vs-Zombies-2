package io.github.Plants_Vs_Zombies_2.network.leaderboard;

import java.util.Objects;

/** Bounded leaderboard request. The server remains the validation authority. */
public final class LeaderboardQuery {
    private final LeaderboardSortColumn sortColumn;
    private final LeaderboardSortDirection sortDirection;
    private final int offset;
    private final int limit;

    public LeaderboardQuery(LeaderboardSortColumn sortColumn,
            LeaderboardSortDirection sortDirection, int offset, int limit) {
        this.sortColumn = Objects.requireNonNull(sortColumn, "sortColumn");
        this.sortDirection = Objects.requireNonNull(sortDirection, "sortDirection");
        this.offset = offset;
        this.limit = limit;
    }

    public LeaderboardSortColumn getSortColumn() { return sortColumn; }
    public LeaderboardSortDirection getSortDirection() { return sortDirection; }
    public int getOffset() { return offset; }
    public int getLimit() { return limit; }

    @Override public boolean equals(Object other) {
        return other instanceof LeaderboardQuery query
                && sortColumn == query.sortColumn
                && sortDirection == query.sortDirection
                && offset == query.offset && limit == query.limit;
    }

    @Override public int hashCode() {
        return Objects.hash(sortColumn, sortDirection, offset, limit);
    }
}
