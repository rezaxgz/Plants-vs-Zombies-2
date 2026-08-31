package io.github.Plants_Vs_Zombies_2.network.session;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;

/** Render-thread-safe server profile refresh and retry state. */
public final class ProfileFlowController implements AutoCloseable {
    public record State(AccountProfile profile, boolean loading,
            String message, boolean retryAvailable) { }

    private final AccountSession session;
    private final UiDispatcher ui;
    private final Consumer<State> observer;
    private State state;
    private CompletableFuture<AccountProfile> activeRequest;
    private long generation;
    private boolean closed;

    public ProfileFlowController(AccountSession session, UiDispatcher ui,
            Consumer<State> observer) {
        this.session = Objects.requireNonNull(session, "session");
        this.ui = Objects.requireNonNull(ui, "ui");
        this.observer = Objects.requireNonNull(observer, "observer");
        AccountProfile current = session.getProfile();
        state = new State(current, false,
                current == null ? "No authenticated server profile." : null,
                current == null);
    }

    public synchronized State getState() {
        return state;
    }

    public void refresh() {
        State loading;
        CompletableFuture<AccountProfile> request;
        long requestGeneration;
        synchronized (this) {
            if (closed || state.loading()) return;
            generation++;
            requestGeneration = generation;
            state = new State(state.profile(), true,
                    "Refreshing profile from the server...", false);
            loading = state;
            try {
                request = session.refreshProfile();
            } catch (RuntimeException failure) {
                request = CompletableFuture.failedFuture(failure);
            }
            activeRequest = request;
        }
        dispatch(loading);
        request.whenComplete((profile, failure) -> ui.dispatch(() ->
                complete(requestGeneration, profile, failure)));
    }

    private void complete(long requestGeneration, AccountProfile profile,
            Throwable failure) {
        State completed;
        synchronized (this) {
            if (closed || generation != requestGeneration) return;
            activeRequest = null;
            if (failure == null && profile != null) {
                state = new State(profile, false,
                        "Profile is synchronized with the server.", false);
            } else {
                state = new State(state.profile(), false,
                        readableFailure(failure), true);
            }
            completed = state;
        }
        observer.accept(completed);
    }

    private void dispatch(State expected) {
        ui.dispatch(() -> {
            synchronized (ProfileFlowController.this) {
                if (closed || state != expected) return;
            }
            observer.accept(expected);
        });
    }

    private static String readableFailure(Throwable failure) {
        Throwable cause = failure;
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause == null ? null : cause.getMessage();
        return message == null || message.isBlank()
                ? "Could not refresh the server profile. Please retry."
                : "Could not refresh the server profile: " + message;
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        generation++;
        if (activeRequest != null && !activeRequest.isDone()) {
            activeRequest.cancel(false);
        }
        activeRequest = null;
    }
}
