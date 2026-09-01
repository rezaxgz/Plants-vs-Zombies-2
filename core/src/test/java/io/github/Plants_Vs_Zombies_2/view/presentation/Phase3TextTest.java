package io.github.Plants_Vs_Zombies_2.view.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchFinishReason;
import io.github.Plants_Vs_Zombies_2.network.session.ClientSessionState;

class Phase3TextTest {
    @Test
    void nullableWireAndProfileValuesAlwaysHaveReadableFallbacks() {
        assertEquals("Waiting for opponent...", Phase3Text.username(null));
        assertEquals("Waiting for assignment...", Phase3Text.role(null));
        assertEquals("Not provided", Phase3Text.optional("  "));
        assertEquals("Connection unavailable", Phase3Text.connection(null));
        assertEquals("Not completed yet", Phase3Text.levelProgress(0, 0));
        assertEquals("-", Phase3Text.rank(0));
    }

    @Test
    void wireEnumsAreConvertedToPlayerFacingLanguage() {
        assertEquals("Plants - defend the brains",
                Phase3Text.role(MatchRole.PLANTS));
        assertEquals("Signing in...",
                Phase3Text.connection(ClientSessionState.AUTHENTICATING));
        assertEquals("A player disconnected.",
                Phase3Text.finishReason(
                        MatchFinishReason.PLAYER_DISCONNECTED));
        String cancellation = Phase3Text.cancellationReason(
                "PLAYER_DISCONNECTED");
        assertEquals("A player disconnected.", cancellation);
        assertFalse(cancellation.contains("_"));
    }
}
