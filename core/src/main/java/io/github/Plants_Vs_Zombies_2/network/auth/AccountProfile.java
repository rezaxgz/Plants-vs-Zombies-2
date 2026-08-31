package io.github.Plants_Vs_Zombies_2.network.auth;

import io.github.Plants_Vs_Zombies_2.model.user.GameProgerss;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;

public final class AccountProfile {
    private final String username;
    private final String nickname;
    private final String email;
    private final String gender;
    private final int coins;
    private final int diamonds;
    private final int sprouts;
    private final int plantFoodCount;
    private final int potCount;
    private final int lastCompletedChapter;
    private final int lastCompletedLevel;
    private final int completedMinigames;
    private final int highestScore;
    private final int gamesPlayed;

    public AccountProfile(
            String username,
            String nickname,
            String email,
            String gender,
            int coins,
            int diamonds,
            int sprouts,
            int plantFoodCount,
            int potCount,
            int lastCompletedChapter,
            int lastCompletedLevel,
            int completedMinigames,
            int highestScore,
            int gamesPlayed) {
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.coins = coins;
        this.diamonds = diamonds;
        this.sprouts = sprouts;
        this.plantFoodCount = plantFoodCount;
        this.potCount = potCount;
        this.lastCompletedChapter = lastCompletedChapter;
        this.lastCompletedLevel = lastCompletedLevel;
        this.completedMinigames = completedMinigames;
        this.highestScore = highestScore;
        this.gamesPlayed = gamesPlayed;
    }

    public static AccountProfile fromUser(User user) {
        GameProgerss progress = user.getGameProgerss();
        return new AccountProfile(
                user.getUsername(),
                user.getNickName(),
                user.getEmail(),
                user.getGender().name(),
                user.getCoins(),
                user.getDiamonds(),
                user.getSprouts(),
                user.getPlantFoodCount(),
                user.getPotCount(),
                progress.getLastCompletedChapter(),
                progress.getLastCompletedLevel(),
                progress.getCompletedMinigames(),
                progress.getHighestScore(),
                progress.getGamesPlayed());
    }

    public AccountProfile withGameplayState(GameplayState state) {
        return new AccountProfile(username, nickname, email, gender,
                state.getCoins(), state.getDiamonds(), state.getSprouts(),
                state.getPlantFoodCount(), state.getPotCount(),
                state.getLastCompletedChapter(), state.getLastCompletedLevel(),
                state.getCompletedMinigames(), state.getHighestScore(),
                state.getGamesPlayed());
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getGender() {
        return gender;
    }

    public int getCoins() {
        return coins;
    }

    public int getDiamonds() {
        return diamonds;
    }

    public int getSprouts() {
        return sprouts;
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public int getPotCount() {
        return potCount;
    }

    public int getLastCompletedChapter() {
        return lastCompletedChapter;
    }

    public int getLastCompletedLevel() {
        return lastCompletedLevel;
    }

    public int getCompletedMinigames() {
        return completedMinigames;
    }

    public int getHighestScore() {
        return highestScore;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }
}
