package io.github.Plants_Vs_Zombies_2.network.matchmaking;

public final class Invitation {
    private final String invitationId;
    private final String inviterUsername;
    private final String recipientUsername;
    private final long creationTimeEpochMillis;
    private final long expirationTimeEpochMillis;
    private final InvitationStatus status;

    public Invitation(String invitationId, String inviterUsername,
            String recipientUsername, long creationTimeEpochMillis,
            long expirationTimeEpochMillis, InvitationStatus status) {
        this.invitationId = invitationId;
        this.inviterUsername = inviterUsername;
        this.recipientUsername = recipientUsername;
        this.creationTimeEpochMillis = creationTimeEpochMillis;
        this.expirationTimeEpochMillis = expirationTimeEpochMillis;
        this.status = status;
    }

    public String getInvitationId() { return invitationId; }
    public String getInviterUsername() { return inviterUsername; }
    public String getRecipientUsername() { return recipientUsername; }
    public long getCreationTimeEpochMillis() { return creationTimeEpochMillis; }
    public long getExpirationTimeEpochMillis() { return expirationTimeEpochMillis; }
    public InvitationStatus getStatus() { return status; }
}
