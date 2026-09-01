package io.github.Plants_Vs_Zombies_2.network.auth;

public final class PasswordResetLookup {
    private final String username;
    private final String email;

    public PasswordResetLookup(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
