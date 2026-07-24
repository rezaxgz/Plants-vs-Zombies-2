package view.security;

import model.security.Question;

/**
 * Builds the security-question list shown during sign-up.
 */
public final class QuestionView {
    private QuestionView() {
    }

    public static String formatQuestions(Question[] questions) {
        StringBuilder output = new StringBuilder();
        for (Question question : questions) {
            if (output.length() > 0) {
                output.append(System.lineSeparator());
            }
            output.append(question.getNumber())
                    .append(". ")
                    .append(question.getText());
        }
        return output.toString();
    }
}
