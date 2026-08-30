package io.github.Plants_Vs_Zombies_2.model.game.minigame.multiplayer;

public final class MultiplayerRuleException extends Exception {
    private final MultiplayerRuleError error;

    MultiplayerRuleException(MultiplayerRuleError error, String message) {
        super(message);
        this.error = error;
    }

    public MultiplayerRuleError getError() { return error; }
}
