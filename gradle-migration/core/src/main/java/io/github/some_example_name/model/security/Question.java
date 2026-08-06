package io.github.some_example_name.model.security;

import io.github.some_example_name.view.security.QuestionView;

public enum Question {
    FIRST_SCHOOL(1, "What was the name of your first school?"),
    CHILDHOOD_FRIEND(2, "What was the name of your childhood best friend?"),
    FAVORITE_BOOK(3, "What was your favorite childhood book?"),
    FIRST_PET(4, "What was the name of your first pet?"),
    BIRTH_CITY(5, "In which city were you born?");

    private final String text;
    private final int number;

    Question(int number, String text) {
        this.number = number;
        this.text = text;
    }

    public int getNumber() {
        return number;
    }

    public String getText() {
        return text;
    }

    public static Question getByNumber(int number) {
        for (Question question : values()) {
            if (question.number == number) {
                return question;
            }
        }
        return null;
    }

    public static String getAllQuestions() {
        return QuestionView.formatQuestions(values());
    }
}
