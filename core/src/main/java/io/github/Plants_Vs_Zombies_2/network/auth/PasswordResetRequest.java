package io.github.Plants_Vs_Zombies_2.network.auth;

public final class PasswordResetRequest {
    private final String username;
    private final String email;
    private final String answer;
    private final String password;
    private final String passwordConfirmation;

    public PasswordResetRequest(String username, String email, String answer,
            String password, String passwordConfirmation) {
        this.username = username;
        this.email = email;
        this.answer = answer;
        this.password = password;
        this.passwordConfirmation = passwordConfirmation;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getAnswer() { return answer; }
    public String getPassword() { return password; }
    public String getPasswordConfirmation() { return passwordConfirmation; }
}
