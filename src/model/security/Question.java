package model.security;

public enum Question {
    Q1(1, "dfjslkdfj?");

    private final String text;
    private final int n;

    Question(int n, String q) {
        this.n = n;
        this.text = q;
    }

    public String getText() {
        return this.text;
    }

    public static Question getByNumber(int n) {
        for (Question q : values()) {
            if (q.n == n)
                return q;
        }
        return null;
    }

    public static String getAllQuestions() {
        StringBuilder sb = new StringBuilder();
        for (Question q : values()) {
            sb.append("\n");
            sb.append(q.n);
            sb.append(". ");
            sb.append(q.text);
        }
        return sb.toString().trim();
    }
}
