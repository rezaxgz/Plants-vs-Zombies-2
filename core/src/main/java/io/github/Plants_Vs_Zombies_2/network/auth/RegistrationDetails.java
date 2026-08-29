package io.github.Plants_Vs_Zombies_2.network.auth;

public final class RegistrationDetails {
    private final String username;
    private final String password;
    private final String passwordConfirmation;
    private final String nickname;
    private final String email;
    private final String gender;
    private final int securityQuestionNumber;
    private final String securityAnswer;
    private final String securityAnswerConfirmation;

    public RegistrationDetails(
            String username,
            String password,
            String passwordConfirmation,
            String nickname,
            String email,
            String gender,
            int securityQuestionNumber,
            String securityAnswer,
            String securityAnswerConfirmation) {
        this.username = username;
        this.password = password;
        this.passwordConfirmation = passwordConfirmation;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.securityQuestionNumber = securityQuestionNumber;
        this.securityAnswer = securityAnswer;
        this.securityAnswerConfirmation = securityAnswerConfirmation;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPasswordConfirmation() {
        return passwordConfirmation;
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

    public int getSecurityQuestionNumber() {
        return securityQuestionNumber;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public String getSecurityAnswerConfirmation() {
        return securityAnswerConfirmation;
    }
}
