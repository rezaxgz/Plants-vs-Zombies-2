package io.github.Plants_Vs_Zombies_2.network.multiplayer;

/** Fixed server-authoritative reaction catalog. Enum names are stable wire IDs. */
public enum MatchReactionType {
    GOOD_LUCK(MatchReactionKind.TEXT, "Good luck!"),
    NICE_MOVE(MatchReactionKind.TEXT, "Nice move!"),
    WELL_PLAYED(MatchReactionKind.TEXT, "Well played!"),
    SMILE(MatchReactionKind.EMOJI, "Smile"),
    LAUGH(MatchReactionKind.EMOJI, "Laugh"),
    ANGRY(MatchReactionKind.EMOJI, "Angry");

    private final MatchReactionKind kind;
    private final String displayText;

    MatchReactionType(MatchReactionKind kind, String displayText) {
        this.kind = kind;
        this.displayText = displayText;
    }

    public MatchReactionKind getKind() { return kind; }

    /** Readable fallback used when an emoji-capable font is unavailable. */
    public String getDisplayText() { return displayText; }
}
