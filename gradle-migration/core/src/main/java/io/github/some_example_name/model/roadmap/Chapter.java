package io.github.some_example_name.model.roadmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Immutable adventure chapter containing its ordered levels.
 */
public final class Chapter {
    private final String id;
    private final String displayName;
    private final List<String> aliases;
    private final List<Level> levels;

    public Chapter(String id, String displayName,
            List<String> aliases, List<Level> levels) {
        if (id == null || id.isBlank()
                || displayName == null
                || displayName.isBlank()) {
            throw new IllegalArgumentException(
                    "chapter id and name cannot be blank");
        }
        if (levels == null || levels.isEmpty()) {
            throw new IllegalArgumentException(
                    "chapter levels cannot be empty");
        }

        this.id = id;
        this.displayName = displayName;
        this.aliases = immutableAliases(aliases);
        this.levels = Collections.unmodifiableList(
                new ArrayList<>(levels));
        validateLevelNumbers();
    }

    private static List<String> immutableAliases(
            List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(
                new ArrayList<>(values));
    }

    private void validateLevelNumbers() {
        for (int index = 0; index < levels.size(); index++) {
            if (levels.get(index).getNumber() != index + 1) {
                throw new IllegalArgumentException(
                        "chapter level numbers must be sequential");
            }
        }
    }

    public boolean matches(String rawName) {
        String normalized = normalize(rawName);
        if (normalize(id).equals(normalized)
                || normalize(displayName)
                        .equals(normalized)) {
            return true;
        }
        for (String alias : aliases) {
            if (normalize(alias).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    public Level getLevel(int number) {
        if (number < 1 || number > levels.size()) {
            return null;
        }
        return levels.get(number - 1);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public List<Level> getLevels() {
        return levels;
    }

    public int getLevelCount() {
        return levels.size();
    }
}
