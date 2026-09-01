package io.github.Plants_Vs_Zombies_2.network.auth;

/** Opaque credential used to restore a server-owned account session. */
public final class PersistentLoginCredentials {
    private final String username;
    private final String token;

    public PersistentLoginCredentials(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public String getUsername() { return username; }
    public String getToken() { return token; }
}
