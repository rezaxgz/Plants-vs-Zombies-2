package io.github.Plants_Vs_Zombies_2.network.session;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;

/** Render-framework-independent orchestration for the graphical login form. */
public final class LoginFlowController {
    private final AccountSession session;
    private final UiDispatcher dispatcher;
    private final View view;
    private final AtomicBoolean submitting = new AtomicBoolean();

    public LoginFlowController(AccountSession session, UiDispatcher dispatcher, View view) {
        this.session = Objects.requireNonNull(session, "session");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.view = Objects.requireNonNull(view, "view");
    }

    public boolean submit(String username, String password) {
        if (!submitting.compareAndSet(false, true)) {
            return false;
        }
        view.setSubmitting(true, "Connecting and logging in...");
        session.login(username, password).whenComplete((profile, failure) ->
                dispatcher.dispatch(() -> {
                    submitting.set(false);
                    view.setSubmitting(false, failure == null ? "Connected." : "Ready to retry.");
                    if (failure == null) {
                        view.loginSucceeded(profile);
                    } else {
                        view.showError(AuthenticationErrorMessages.forFailure(failure));
                    }
                }));
        return true;
    }

    public boolean isSubmitting() {
        return submitting.get();
    }

    public interface View {
        void setSubmitting(boolean submitting, String message);

        void loginSucceeded(AccountProfile profile);

        void showError(String message);
    }
}
