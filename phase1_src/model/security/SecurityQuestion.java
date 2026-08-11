package model.security;

public class SecurityQuestion {
    private final String question;
    private final String answerHash;

    public SecurityQuestion(String question, String answer) {
        this(question, Sha256.hash(answer), true);
    }

    private SecurityQuestion(String question, String answerHash, boolean storedHash) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question cannot be blank");
        }
        if (answerHash == null || answerHash.isBlank()) {
            throw new IllegalArgumentException("answer hash cannot be blank");
        }
        this.question = question;
        this.answerHash = answerHash;
    }

    public static SecurityQuestion fromStoredHash(String question, String answerHash) {
        return new SecurityQuestion(question, answerHash, true);
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswerHashForStorage() {
        return answerHash;
    }

    public boolean isAnswerCorrect(String answer) {
        return answer != null && Sha256.hash(answer).equals(answerHash);
    }
}
