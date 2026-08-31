package io.github.Plants_Vs_Zombies_2.network.session;

import java.util.HashMap;

import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.user.GameProgerss;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;

/**
 * Creates a temporary gameplay snapshot for legacy App.getLoggedInUser() call
 * sites. The snapshot is never registered with UserManager and contains an
 * intentionally non-SHA-256 password marker, so no password can match it.
 */
public final class RemoteGameplayUserFactory {
    static final String UNUSABLE_PASSWORD_HASH = "REMOTE-AUTHENTICATION-ONLY";

    private RemoteGameplayUserFactory() {
    }

    public static User create(AccountProfile profile) {
        return create(profile, null);
    }

    public static User create(AccountProfile profile,
            GameplayStateSnapshot gameplaySnapshot) {
        Gender gender = profile.getGender() == null
                ? Gender.MALE : Gender.getByName(profile.getGender());
        if (gender == null) {
            gender = Gender.MALE;
        }
        GameProgerss progress = GameProgerss.fromStoredData(
                profile.getLastCompletedChapter(),
                profile.getLastCompletedLevel(),
                profile.getCompletedMinigames(),
                profile.getHighestScore(),
                profile.getGamesPlayed());
        User user = User.fromStoredData(
                profile.getUsername(), UNUSABLE_PASSWORD_HASH,
                profile.getNickname(), profile.getEmail(), gender,
                null, profile.getCoins(), profile.getDiamonds(),
                0, profile.getPlantFoodCount(), null, new HashMap<>(),
                null, null, null, null, progress);
        user.setSprouts(profile.getSprouts());
        user.setPotCount(profile.getPotCount());
        if (gameplaySnapshot != null && gameplaySnapshot.getState() != null) {
            user.applyGameplayState(gameplaySnapshot.getState());
            user.setGameplayRevisionForStorage(gameplaySnapshot.getRevision());
        }
        return user;
    }
}
