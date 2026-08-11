package view.roadmap;

import model.roadmap.Chapter;
import model.roadmap.Level;

/**
 * Builds adventure progress and unlock text.
 */
public final class AdventureView {
    private AdventureView() {
    }

    public static String buildLevelUnlockDescription(
            Chapter chapter, Level level) {
        StringBuilder description = new StringBuilder()
                .append(chapter.getDisplayName())
                .append(" level ")
                .append(level.getNumber())
                .append(" (\"")
                .append(level.getName())
                .append("\") is now available");
        if (level.getSpecialLevelType().isSpecial()) {
            description.append(". Special rules: ")
                    .append(level.getSpecialLevelType().getDisplayName());
        }
        description.append('.');
        return description.toString();
    }
}
