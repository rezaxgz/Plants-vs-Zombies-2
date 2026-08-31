package io.github.Plants_Vs_Zombies_2.model.auth;

import io.github.Plants_Vs_Zombies_2.model.user.User;

import java.util.List;
import java.util.Optional;

import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardEntry;

public interface UserRepository {
    Optional<User> findByUsername(String username);

    List<User> findAll();

    /** Immutable sanitized copies made under the repository state boundary. */
    List<LeaderboardEntry> snapshotLeaderboardEntries();

    boolean addIfUsernameAvailable(User user);

    Optional<GameplayStateSnapshot> findGameplayState(String username);

    GameplayStateSnapshot updateGameplayState(String username,
            long expectedRevision, GameplayState state)
            throws GameplayUpdateException;
}
