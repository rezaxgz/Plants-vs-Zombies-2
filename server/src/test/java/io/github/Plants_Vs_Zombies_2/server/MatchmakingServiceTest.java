package io.github.Plants_Vs_Zombies_2.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.Invitation;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.InvitationStatus;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.PlayerMatchmakingState;
import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

class MatchmakingServiceTest {
    @Test
    void invitationExpirationUsesInjectedClockWithoutSleeping() throws Exception {
        MutableClock clock = new MutableClock(1_000L);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        List<MatchmakingEvent> published = new ArrayList<>();
        MatchmakingService service = new MatchmakingService(
                ignored -> true, ignored -> true, published::addAll,
                Duration.ofSeconds(30), clock, scheduler);
        try {
            Invitation invitation = service.invite("alice", "bob");
            assertEquals(InvitationStatus.PENDING, invitation.getStatus());

            clock.advance(Duration.ofSeconds(30));
            service.expireInvitation(invitation.getInvitationId());

            List<Invitation> results = published.stream()
                    .filter(event -> event.type() == MessageType.INVITATION_RESULT)
                    .map(event -> (Invitation) event.payload())
                    .toList();
            assertEquals(2, results.size());
            assertEquals(InvitationStatus.EXPIRED, results.get(0).getStatus());
            assertEquals(PlayerMatchmakingState.AVAILABLE,
                    service.stateOfPlayer("alice"));
            MatchmakingServiceException failure = assertThrows(
                    MatchmakingServiceException.class,
                    () -> service.respond("bob", invitation.getInvitationId(), true));
            assertEquals(ProtocolErrorCode.INVITATION_EXPIRED,
                    failure.getErrorCode());
        } finally {
            service.close();
        }
    }

    private static final class MutableClock extends Clock {
        private long epochMillis;

        private MutableClock(long epochMillis) {
            this.epochMillis = epochMillis;
        }

        void advance(Duration duration) {
            epochMillis += duration.toMillis();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new IllegalArgumentException("Only UTC is supported by this test clock");
            }
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(epochMillis);
        }

        @Override
        public long millis() {
            return epochMillis;
        }
    }
}
