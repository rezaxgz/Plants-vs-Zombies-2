package io.github.Plants_Vs_Zombies_2.model.auth;

public final class GameplayUpdateException extends Exception {
    private final GameplayUpdateFailure failure;

    public GameplayUpdateException(GameplayUpdateFailure failure, String message) {
        super(message);
        this.failure = failure;
    }

    public GameplayUpdateFailure getFailure() { return failure; }
}
