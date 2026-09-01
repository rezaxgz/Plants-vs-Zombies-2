package io.github.Plants_Vs_Zombies_2.network.auth;

/** Public part of an account's recovery data. */
public final class PasswordResetChallenge {
    private final String username;
    private final String question;

    public PasswordResetChallenge(String username, String question) {
        this.username = username;
        this.question = question;
    }

    public String getUsername() { return username; }
    public String getQuestion() { return question; }
}
