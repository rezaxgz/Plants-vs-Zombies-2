package model.security;

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
        StringBuilder output = new StringBuilder();
        for (Question question : values()) {
            if (output.length() > 0) {
                output.append(System.lineSeparator());
            }
            output.append(question.number).append(". ").append(question.text);
        }
        return output.toString();
    }
}
