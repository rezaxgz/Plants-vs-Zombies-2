package io.github.Plants_Vs_Zombies_2.network.leaderboard;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.Plants_Vs_Zombies_2.network.client.NetworkClient;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolCodec;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolException;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessages;

/** Typed leaderboard API sharing the application account session's socket. */
public final class LeaderboardClient implements LeaderboardTransport {
    private final NetworkClient networkClient;
    private final ProtocolCodec codec = new ProtocolCodec();

    public LeaderboardClient(NetworkClient networkClient) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient");
    }

    @Override
    public CompletableFuture<LeaderboardPage> load(LeaderboardQuery query) {
        Objects.requireNonNull(query, "query");
        ProtocolMessage request = ProtocolMessages.withPayload(
                MessageType.GET_LEADERBOARD_REQUEST,
                ProtocolMessages.newRequestId(), query);
        CompletableFuture<ProtocolMessage> exchange = networkClient.sendRequest(request);
        CompletableFuture<LeaderboardPage> result = exchange.thenApply(response -> {
            if (response.getType() == MessageType.ERROR) throw readError(response);
            if (response.getType() != MessageType.GET_LEADERBOARD_RESPONSE) {
                throw new LeaderboardException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                        "The server returned an unexpected leaderboard response");
            }
            try {
                LeaderboardPage page = codec.deserializePayload(
                        response, LeaderboardPage.class);
                if (page == null || page.getEntries() == null
                        || page.getTotalPlayers() < 0 || page.getOffset() < 0
                        || page.getLimit() <= 0
                        || page.getEntries().size() > page.getLimit()
                        || !validPage(page)) {
                    throw new ProtocolException(
                            ProtocolErrorCode.MALFORMED_PAYLOAD.name(),
                            response.getRequestId(), "Leaderboard page is incomplete");
                }
                return page;
            } catch (ProtocolException | RuntimeException exception) {
                if (exception instanceof LeaderboardException leaderboard) {
                    throw leaderboard;
                }
                throw new LeaderboardException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                        "The server returned malformed leaderboard data");
            }
        });
        result.whenComplete((page, failure) -> {
            if (result.isCancelled()) exchange.cancel(false);
        });
        return result;
    }

    private static boolean validPage(LeaderboardPage page) {
        Integer userRank = page.getAuthenticatedUserRank();
        if (userRank != null && (userRank < 1
                || userRank > page.getTotalPlayers())) return false;
        for (LeaderboardEntry entry : page.getEntries()) {
            if (entry == null || entry.getRank() < 1
                    || entry.getRank() > page.getTotalPlayers()
                    || entry.getUsername() == null || entry.getUsername().isBlank()
                    || entry.getLastCompletedChapter() < 0
                    || entry.getLastCompletedLevel() < 0
                    || entry.getCompletedMinigames() < 0
                    || entry.getCompletedDailyQuests() < 0
                    || entry.getCompletedNonDailyQuests() < 0
                    || entry.getHighestScore() < 0
                    || entry.getTotalCompletedQuests()
                            != entry.getCompletedDailyQuests()
                                    + entry.getCompletedNonDailyQuests()) return false;
        }
        return true;
    }

    private static LeaderboardException readError(ProtocolMessage response) {
        JsonElement payload = response.getPayload();
        if (!payload.isJsonObject()) return malformedError();
        JsonObject object = payload.getAsJsonObject();
        JsonElement code = object.get("code");
        JsonElement message = object.get("message");
        if (code == null || message == null || !code.isJsonPrimitive()
                || !message.isJsonPrimitive()
                || !code.getAsJsonPrimitive().isString()
                || !message.getAsJsonPrimitive().isString()) return malformedError();
        try {
            return new LeaderboardException(
                    ProtocolErrorCode.valueOf(code.getAsString()),
                    message.getAsString());
        } catch (IllegalArgumentException exception) {
            return malformedError();
        }
    }

    private static LeaderboardException malformedError() {
        return new LeaderboardException(ProtocolErrorCode.UNEXPECTED_RESPONSE,
                "The server returned a malformed error response");
    }
}
