package io.github.Plants_Vs_Zombies_2.network.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ConnectException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.AuthenticationException;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolErrorCode;

class AuthenticationFlowControllerTest {
    private static final AccountProfile PROFILE = new AccountProfile(
            "remote-user", "Remote", "remote@example.com", "MALE",
            10, 2, 1, 3, 4, 1, 2, 3, 99, 8);

    @Test
    void successfulLoginIsDispatchedAndAuthenticatesView() {
        FakeSession session = new FakeSession();
        QueuedDispatcher dispatcher = new QueuedDispatcher();
        LoginView view = new LoginView();
        LoginFlowController controller = new LoginFlowController(session, dispatcher, view);

        assertTrue(controller.submit("remote-user", "secret"));
        session.loginFuture.complete(PROFILE);

        assertNull(view.profile, "network completion must not mutate UI directly");
        assertTrue(controller.isSubmitting());
        dispatcher.runNext();

        assertSame(PROFILE, view.profile);
        assertFalse(controller.isSubmitting());
        assertFalse(view.submitting);
    }

    @Test
    void failedLoginDoesNotAuthenticateAndCanRetry() {
        FakeSession session = new FakeSession();
        QueuedDispatcher dispatcher = new QueuedDispatcher();
        LoginView view = new LoginView();
        LoginFlowController controller = new LoginFlowController(session, dispatcher, view);

        controller.submit("remote-user", "wrong");
        session.loginFuture.completeExceptionally(new AuthenticationException(
                ProtocolErrorCode.INVALID_CREDENTIALS, "invalid"));
        dispatcher.runNext();

        assertNull(view.profile);
        assertEquals("Invalid username or password.", view.error);
        assertFalse(controller.isSubmitting());
    }

    @Test
    void repeatedLoginClickDoesNotCreateDuplicateRequest() {
        FakeSession session = new FakeSession();
        LoginFlowController controller = new LoginFlowController(
                session, Runnable::run, new LoginView());

        assertTrue(controller.submit("remote-user", "secret"));
        assertFalse(controller.submit("remote-user", "secret"));
        assertEquals(1, session.loginCalls);
    }

    @Test
    void successfulSignupAndServerValidationErrorArePropagated() {
        FakeSession successSession = new FakeSession();
        QueuedDispatcher dispatcher = new QueuedDispatcher();
        SignupView successView = new SignupView();
        SignupFlowController success = new SignupFlowController(
                successSession, dispatcher, successView);
        RegistrationDetails details = registration();

        assertTrue(success.submit(details));
        assertFalse(success.submit(details));
        successSession.registerFuture.complete(null);
        assertFalse(successView.succeeded);
        dispatcher.runNext();
        assertTrue(successView.succeeded);

        FakeSession failedSession = new FakeSession();
        SignupView failedView = new SignupView();
        SignupFlowController failed = new SignupFlowController(
                failedSession, Runnable::run, failedView);
        failed.submit(details);
        failedSession.registerFuture.completeExceptionally(new AuthenticationException(
                ProtocolErrorCode.USERNAME_EXISTS, "duplicate"));
        assertFalse(failedView.succeeded);
        assertEquals("That username is already registered.", failedView.error);
    }

    @Test
    void connectionFailureAndTimeoutLeaveLoginRecoverable() {
        assertRecoverableFailure(new IllegalStateException(
                "Could not connect", new ConnectException("refused")),
                "Server unavailable. Check that it is running, then retry.");
        assertRecoverableFailure(new TimeoutException("late"),
                "The server request timed out. You can retry.");
    }

    private static void assertRecoverableFailure(Throwable failure, String expected) {
        FakeSession session = new FakeSession();
        LoginView view = new LoginView();
        LoginFlowController controller = new LoginFlowController(
                session, Runnable::run, view);
        controller.submit("remote-user", "secret");
        session.loginFuture.completeExceptionally(failure);
        assertEquals(expected, view.error);
        assertFalse(view.submitting);
        assertFalse(controller.isSubmitting());
    }

    private static RegistrationDetails registration() {
        return new RegistrationDetails("remote-user", "Password1!",
                "Password1!", "Remote", "remote@example.com", "Male",
                1, "answer", "answer");
    }

    private static final class QueuedDispatcher implements UiDispatcher {
        private final Queue<Runnable> work = new ArrayDeque<>();

        @Override
        public void dispatch(Runnable runnable) {
            work.add(runnable);
        }

        void runNext() {
            work.remove().run();
        }
    }

    private static final class LoginView implements LoginFlowController.View {
        private boolean submitting;
        private AccountProfile profile;
        private String error;

        @Override
        public void setSubmitting(boolean submitting, String message) {
            this.submitting = submitting;
        }

        @Override
        public void loginSucceeded(AccountProfile profile) {
            this.profile = profile;
        }

        @Override
        public void showError(String message) {
            error = message;
        }
    }

    private static final class SignupView implements SignupFlowController.View {
        private boolean succeeded;
        private String error;

        @Override
        public void setSubmitting(boolean submitting, String message) {
        }

        @Override
        public void signupSucceeded() {
            succeeded = true;
        }

        @Override
        public void showError(String message) {
            error = message;
        }
    }

    private static final class FakeSession implements AccountSession {
        private final CompletableFuture<AccountProfile> loginFuture =
                new CompletableFuture<>();
        private final CompletableFuture<Void> registerFuture =
                new CompletableFuture<>();
        private int loginCalls;

        @Override
        public CompletableFuture<Void> connect() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> register(RegistrationDetails details) {
            return registerFuture;
        }

        @Override
        public CompletableFuture<AccountProfile> login(String username, String password) {
            loginCalls++;
            return loginFuture;
        }

        @Override
        public CompletableFuture<AccountProfile> refreshProfile() {
            return CompletableFuture.completedFuture(PROFILE);
        }

        @Override
        public CompletableFuture<Void> logout() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public ClientSessionState getState() {
            return ClientSessionState.CONNECTED;
        }

        @Override
        public AccountProfile getProfile() {
            return null;
        }

        @Override
        public Throwable getLastFailure() {
            return null;
        }

        @Override
        public void disconnect() {
        }

        @Override
        public void close() {
        }
    }
}
