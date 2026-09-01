package io.github.Plants_Vs_Zombies_2.network.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.user.User;
import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.auth.PersistentLoginToken;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayState;
import io.github.Plants_Vs_Zombies_2.network.gameplay.GameplayStateSnapshot;

class RemoteAccountSessionTest {
    @TempDir Path temporaryDirectory;
    private static final AccountProfile PROFILE = new AccountProfile(
            "remote-only-user-for-test", "Remote", "remote@example.com", "FEMALE",
            100, 5, 3, 2, 1, 2, 4, 6, 120, 9);

    @Test
    void simultaneousConnectCallsShareOneAttempt() {
        FakeTransport transport = new FakeTransport();
        RemoteAccountSession session = session(transport);

        CompletableFuture<Void> first = session.connect();
        CompletableFuture<Void> second = session.connect();

        assertSame(first, second);
        assertEquals(1, transport.connectCalls);
        transport.connected = true;
        transport.connectFuture.complete(null);
        assertEquals(ClientSessionState.CONNECTED, session.getState());
    }

    @Test
    void loginStoresProfileAndLogoutClearsItEvenWhenRequestFails() {
        FakeTransport transport = new FakeTransport();
        transport.connected = true;
        RemoteAccountSession session = session(transport);

        transport.loginFuture.complete(PROFILE);
        AccountProfile loggedIn = session.login(
                "remote-only-user-for-test", "secret").join();
        assertEquals(PROFILE.getUsername(), loggedIn.getUsername());
        assertEquals(PROFILE.getCoins(), session.getProfile().getCoins());
        assertNotNull(session.getGameplayStateSnapshot());
        assertEquals(ClientSessionState.AUTHENTICATED, session.getState());

        User compatibility = RemoteGameplayUserFactory.create(
                session.getProfile(), session.getGameplayStateSnapshot());
        compatibility.addCoins(10);
        GameplayStateSnapshot synchronizedState = session.synchronizeGameplayState(
                session.getGameplayStateSnapshot().getRevision(),
                GameplayState.fromUser(compatibility)).join();
        assertEquals(1L, synchronizedState.getRevision());
        assertEquals(PROFILE.getCoins() + 10, session.getProfile().getCoins());

        transport.logoutFuture.completeExceptionally(
                new IllegalStateException("Server disconnected"));
        session.logout().handle((ignored, failure) -> null).join();
        assertNull(session.getProfile());
        assertEquals(ClientSessionState.DISCONNECTED, session.getState());
    }

    @Test
    void stayLoggedInStoresAnOpaqueTokenWithoutThePassword() throws Exception {
        FakeTransport transport = new FakeTransport();
        transport.connected = true;
        transport.loginFuture.complete(PROFILE);
        Path sessionPath = temporaryDirectory.resolve("remote-session.json");
        RemoteAccountSession session = new RemoteAccountSession(transport,
                new RemoteSessionStore(sessionPath));

        session.login(PROFILE.getUsername(), "do-not-store-this", true).join();

        assertTrue(session.hasPersistentLogin());
        String stored = Files.readString(sessionPath);
        assertFalse(stored.contains("do-not-store-this"));
        assertTrue(stored.contains("opaque-token"));
    }

    @Test
    void compatibilityUserHasNoUsablePasswordAndIsNotManagedLocally() {
        List<User> before = UserManager.loadAllUsers();
        User compatibility = RemoteGameplayUserFactory.create(PROFILE);
        List<User> after = UserManager.loadAllUsers();

        assertEquals(before.size(), after.size());
        assertFalse(after.contains(compatibility));
        assertFalse(compatibility.doesMatchPassword("Password1!"));
        assertEquals(RemoteGameplayUserFactory.UNUSABLE_PASSWORD_HASH,
                compatibility.getPasswordHashForStorage());
        assertEquals(PROFILE.getCoins(), compatibility.getCoins());
        assertEquals(PROFILE.getHighestScore(),
                compatibility.getGameProgerss().getHighestScore());
    }

    @Test
    void graphicalScreensContainNoLocalAuthenticationCalls() throws IOException {
        Path screens = Path.of("src", "main", "java", "io", "github",
                "Plants_Vs_Zombies_2", "view", "screens");
        String login = Files.readString(screens.resolve("LoginScreen.java"));
        String signup = Files.readString(screens.resolve("SignUpScreen.java"));
        String profile = Files.readString(screens.resolve("ProfileScreen.java"));
        assertFalse(login.contains("UserManager"));
        assertFalse(login.contains("LoginMenuController"));
        assertFalse(signup.contains("UserManager"));
        assertFalse(signup.contains("SignupMenuController"));
        assertFalse(profile.contains("UserManager"));
        assertFalse(profile.contains("ProfileMenuController"));
        assertTrue(profile.contains("ProfileFlowController"));
    }

    private RemoteAccountSession session(FakeTransport transport) {
        return new RemoteAccountSession(transport, new RemoteSessionStore(
                temporaryDirectory.resolve("remote-session.json")));
    }

    private static final class FakeTransport implements RemoteAccountTransport {
        private final CompletableFuture<Void> connectFuture = new CompletableFuture<>();
        private final CompletableFuture<AccountProfile> loginFuture = new CompletableFuture<>();
        private final CompletableFuture<Void> logoutFuture = new CompletableFuture<>();
        private boolean connected;
        private int connectCalls;
        private Consumer<Throwable> disconnectListener = ignored -> { };

        @Override
        public CompletableFuture<Void> connect() {
            connectCalls++;
            return connectFuture;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public CompletableFuture<Void> register(RegistrationDetails details) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<AccountProfile> login(String username, String password) {
            return loginFuture;
        }

        @Override
        public CompletableFuture<PersistentLoginToken> createPersistentLogin() {
            return CompletableFuture.completedFuture(new PersistentLoginToken(
                    PROFILE.getUsername(), "opaque-token"));
        }

        @Override
        public CompletableFuture<AccountProfile> getProfile() {
            return CompletableFuture.completedFuture(PROFILE);
        }

        @Override
        public CompletableFuture<GameplayStateSnapshot> getGameplayState() {
            return CompletableFuture.completedFuture(new GameplayStateSnapshot(0,
                    GameplayState.fromUser(RemoteGameplayUserFactory.create(PROFILE))));
        }

        @Override
        public CompletableFuture<GameplayStateSnapshot> synchronizeGameplayState(
                long expectedRevision, GameplayState state) {
            return CompletableFuture.completedFuture(
                    new GameplayStateSnapshot(expectedRevision + 1, state));
        }

        @Override
        public CompletableFuture<Void> logout() {
            return logoutFuture;
        }

        @Override
        public void setDisconnectListener(Consumer<Throwable> listener) {
            disconnectListener = listener;
        }

        @Override
        public void disconnect() {
            connected = false;
            disconnectListener.accept(null);
        }

        @Override
        public void close() {
            connected = false;
        }
    }
}
