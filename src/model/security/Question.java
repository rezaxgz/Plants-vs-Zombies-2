package model.security;

public enum Question {
    Q1("dfjslkdfj?");

    private final String text;

    Question(String q) {
        this.text = q;
    }

    public String getText() {
        return this.text;
    }
}
