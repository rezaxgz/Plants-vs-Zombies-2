package io.github.Plants_Vs_Zombies_2.network.auth;

/** One revocable token returned only when a player enables Stay logged in. */
public final class PersistentLoginToken {
    private final String username;
    private final String token;

    public PersistentLoginToken(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public String getUsername() { return username; }
    public String getToken() { return token; }
}
