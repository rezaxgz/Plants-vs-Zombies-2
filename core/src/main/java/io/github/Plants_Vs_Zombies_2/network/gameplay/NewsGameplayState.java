package io.github.Plants_Vs_Zombies_2.network.gameplay;

import java.util.Objects;

/** Immutable non-secret representation of one account news entry. */
public final class NewsGameplayState {
    private final long timestampMillis;
    private final String title;
    private final String description;
    private final boolean read;

    public NewsGameplayState(long timestampMillis, String title,
            String description, boolean read) {
        this.timestampMillis = timestampMillis;
        this.title = title;
        this.description = description;
        this.read = read;
    }

    public long getTimestampMillis() { return timestampMillis; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isRead() { return read; }

    @Override public boolean equals(Object other) {
        if (!(other instanceof NewsGameplayState value)) return false;
        return timestampMillis == value.timestampMillis && read == value.read
                && Objects.equals(title, value.title)
                && Objects.equals(description, value.description);
    }

    @Override public int hashCode() {
        return Objects.hash(timestampMillis, title, description, read);
    }
}
