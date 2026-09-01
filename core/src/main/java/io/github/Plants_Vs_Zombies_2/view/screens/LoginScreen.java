package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.model.menu.SignUpMenu;
import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.auth.PasswordResetChallenge;
import io.github.Plants_Vs_Zombies_2.network.auth.PasswordResetRequest;
import io.github.Plants_Vs_Zombies_2.network.session.AuthenticationErrorMessages;
import io.github.Plants_Vs_Zombies_2.network.session.LoginFlowController;
import io.github.Plants_Vs_Zombies_2.view.presentation.Phase3Text;

/** Graphical login and password recovery backed by the account server. */
public final class LoginScreen extends AbstractScreen {
    private static final String TITLE_BACKGROUND =
            "pvz-assets/ATLASES/TITLESCREEN4_768_00.PNG";

    private boolean active = true;
    private String resetUsername;
    private String resetEmail;

    public LoginScreen(ScreenNavigator navigator) {
        super(navigator, "Login");
        // AbstractScreen draws this region across the complete framebuffer,
        // independently of the letterboxed UI FitViewport.
        setBackground(TITLE_BACKGROUND);
        showLoginForm(null);
        addMenuButton("Register", SignUpMenu::new);
    }

    private void showLoginForm(String initialMessage) {
        content.clearChildren();
        resetUsername = null;
        resetEmail = null;

        Table panel = createPanel();
        Label status = createStatusLabel();
        TextField username = field("Username");
        TextField password = passwordField("Password");
        CheckBox stayLoggedIn = new CheckBox(" Stay logged in", skin);
        stayLoggedIn.getImage().setScaling(Scaling.fit);
        stayLoggedIn.getImageCell().size(28f, 28f).padRight(8f);

        addField(panel, "Username", username);
        addField(panel, "Password", password);
        panel.add(stayLoggedIn).colspan(2).left().padLeft(145f)
                .padTop(2f).row();

        TextButton login = new TextButton("Login", skin, "green");
        TextButton forgot = new TextButton("Forgot Password", skin, "brown");
        Table buttons = new Table();
        buttons.add(login).width(220f).height(52f).pad(6f);
        buttons.add(forgot).width(240f).height(52f).pad(6f);
        panel.add(buttons).colspan(2).padTop(12f).row();
        panel.add(status).colspan(2).growX().width(520f).padTop(10f).row();

        LoginFlowController controller = new LoginFlowController(
                navigator.getAccountSession(), navigator.getUiDispatcher(),
                new LoginFlowController.View() {
                    @Override public void setSubmitting(boolean submitting,
                            String message) {
                        if (!active) return;
                        login.setDisabled(submitting);
                        forgot.setDisabled(submitting);
                        username.setDisabled(submitting);
                        password.setDisabled(submitting);
                        stayLoggedIn.setDisabled(submitting);
                        status.setText(Phase3Text.status(message,
                                "Contacting the server..."));
                        status.setColor(Color.LIGHT_GRAY);
                    }

                    @Override public void loginSucceeded(AccountProfile profile) {
                        if (!active) return;
                        password.setText("");
                        navigator.completeRemoteLogin(profile);
                    }

                    @Override public void showError(String message) {
                        if (active) LoginScreen.this.showError(status, message);
                    }
                });

        login.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (login.isDisabled()) return;
                if (username.getText().trim().isEmpty()
                        || password.getText().isEmpty()) {
                    showError(status, "Enter both username and password.");
                    return;
                }
                controller.submit(username.getText().trim(), password.getText(),
                        stayLoggedIn.isChecked());
            }
        });
        forgot.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!forgot.isDisabled()) showForgotPasswordLookup(null);
            }
        });

        content.add(panel).width(620f).pad(20f);
        if (initialMessage != null) showSuccess(status, initialMessage);
        String notice = navigator.consumeAuthenticationNotice();
        if (notice != null) {
            status.setText(notice);
            status.setColor(Color.GOLD);
        }
    }

    private void showForgotPasswordLookup(String initialMessage) {
        content.clearChildren();
        Table panel = createPanel();
        Label status = createStatusLabel();
        TextField username = field("Username");
        TextField email = field("Email");
        addField(panel, "Username", username);
        addField(panel, "Email", email);

        TextButton continueButton = new TextButton("Continue", skin, "green");
        TextButton cancel = new TextButton("Back to Login", skin, "brown");
        Table buttons = new Table();
        buttons.add(continueButton).width(220f).height(52f).pad(6f);
        buttons.add(cancel).width(220f).height(52f).pad(6f);
        panel.add(buttons).colspan(2).padTop(12f).row();
        panel.add(status).colspan(2).growX().width(520f).padTop(10f).row();

        continueButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                String requestedUsername = username.getText().trim();
                String requestedEmail = email.getText().trim();
                if (requestedUsername.isEmpty() || requestedEmail.isEmpty()) {
                    showError(status, "Enter both username and email.");
                    return;
                }
                setLookupSubmitting(true, continueButton, cancel,
                        username, email, status);
                navigator.getAccountSession().lookupPasswordReset(
                        requestedUsername, requestedEmail)
                        .whenComplete((challenge, failure) ->
                                navigator.getUiDispatcher().dispatch(() -> {
                                    if (!active) return;
                                    if (failure != null) {
                                        setLookupSubmitting(false, continueButton,
                                                cancel, username, email, status);
                                        showError(status,
                                                AuthenticationErrorMessages
                                                        .forFailure(failure));
                                        return;
                                    }
                                    resetUsername = requestedUsername;
                                    resetEmail = requestedEmail;
                                    showPasswordResetForm(challenge);
                                }));
            }
        });
        cancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!cancel.isDisabled()) showLoginForm(null);
            }
        });
        content.add(panel).width(620f).pad(20f);
        if (initialMessage != null) showError(status, initialMessage);
    }

    private void showPasswordResetForm(PasswordResetChallenge challenge) {
        content.clearChildren();
        Table panel = createPanel();
        Label status = createStatusLabel();
        Label question = new Label(Phase3Text.required(challenge.getQuestion(),
                "Security question unavailable"), skin, "medium");
        question.setWrap(true);
        panel.add(question).colspan(2).growX().width(520f)
                .padBottom(10f).row();

        TextField answer = field("Security answer");
        TextField password = passwordField("New password");
        TextField confirmation = passwordField("Confirm new password");
        addField(panel, "Answer", answer);
        addField(panel, "New password", password);
        addField(panel, "Confirm password", confirmation);

        TextButton change = new TextButton("Change Password", skin, "green");
        TextButton cancel = new TextButton("Cancel", skin, "brown");
        Table buttons = new Table();
        buttons.add(change).width(240f).height(52f).pad(6f);
        buttons.add(cancel).width(180f).height(52f).pad(6f);
        panel.add(buttons).colspan(2).padTop(12f).row();
        panel.add(status).colspan(2).growX().width(520f).padTop(10f).row();

        change.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (answer.getText().isBlank() || password.getText().isEmpty()
                        || confirmation.getText().isEmpty()) {
                    showError(status, "Complete every password recovery field.");
                    return;
                }
                setResetSubmitting(true, change, cancel, answer, password,
                        confirmation, status);
                navigator.getAccountSession().resetPassword(
                        new PasswordResetRequest(resetUsername, resetEmail,
                                answer.getText(), password.getText(),
                                confirmation.getText()))
                        .whenComplete((ignored, failure) ->
                                navigator.getUiDispatcher().dispatch(() -> {
                                    if (!active) return;
                                    if (failure != null) {
                                        setResetSubmitting(false, change, cancel,
                                                answer, password, confirmation,
                                                status);
                                        showError(status,
                                                AuthenticationErrorMessages
                                                        .forFailure(failure));
                                        return;
                                    }
                                    showLoginForm("Password changed successfully. You can now log in.");
                                }));
            }
        });
        cancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!cancel.isDisabled()) showLoginForm(null);
            }
        });
        content.add(panel).width(650f).pad(20f);
    }

    private void setLookupSubmitting(boolean submitting, TextButton submit,
            TextButton cancel, TextField username, TextField email, Label status) {
        submit.setDisabled(submitting);
        cancel.setDisabled(submitting);
        username.setDisabled(submitting);
        email.setDisabled(submitting);
        status.setText(submitting ? "Checking recovery details..." : "");
        status.setColor(Color.LIGHT_GRAY);
    }

    private void setResetSubmitting(boolean submitting, TextButton submit,
            TextButton cancel, TextField answer, TextField password,
            TextField confirmation, Label status) {
        submit.setDisabled(submitting);
        cancel.setDisabled(submitting);
        answer.setDisabled(submitting);
        password.setDisabled(submitting);
        confirmation.setDisabled(submitting);
        status.setText(submitting ? "Changing password..." : "");
        status.setColor(Color.LIGHT_GRAY);
    }

    private Table createPanel() {
        Table panel = new Table();
        panel.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"));
        panel.pad(24f);
        panel.defaults().pad(6f);
        return panel;
    }

    private Label createStatusLabel() {
        Label label = new Label("", skin, "secondary");
        label.setWrap(true);
        return label;
    }

    private TextField field(String hint) {
        TextField field = new TextField("", skin);
        field.setMessageText(hint);
        return field;
    }

    private TextField passwordField(String hint) {
        TextField field = field(hint);
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
        return field;
    }

    private void addField(Table table, String name, Actor actor) {
        table.add(new Label(name, skin)).right().padRight(12f);
        table.add(actor).width(360f).height(46f).left().row();
    }

    private void showError(Label status, String message) {
        status.setText(Phase3Text.status(message,
                "The account request failed."));
        status.setColor(Color.SCARLET);
    }

    private void showSuccess(Label status, String message) {
        status.setText(message);
        status.setColor(Color.GREEN);
    }

    @Override public void dispose() {
        active = false;
        super.dispose();
    }
}
