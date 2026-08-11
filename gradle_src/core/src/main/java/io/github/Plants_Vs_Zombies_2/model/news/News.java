package io.github.Plants_Vs_Zombies_2.model.news;

public class News {
    private long timestampMillis;
    private String title;
    private String description;
    private boolean hasRead;

    public News(long timestampMillis, String title, String description, boolean hasRead) {
        this.timestampMillis = timestampMillis;
        this.title = title;
        this.description = description;
        this.hasRead = hasRead;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHasRead() {
        return hasRead;
    }

    public void setHasRead(boolean hasRead) {
        this.hasRead = hasRead;
    }
}