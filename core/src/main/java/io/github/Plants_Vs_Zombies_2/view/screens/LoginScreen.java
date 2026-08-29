package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.github.Plants_Vs_Zombies_2.model.menu.SignUpMenu;
import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.session.LoginFlowController;

/** Graphical login backed exclusively by the remote authentication server. */
public final class LoginScreen extends AbstractScreen {
    private static final String TITLE_BACKGROUND =
            "pvz-assets/ATLASES/TITLESCREEN4_768_00.PNG";
    private boolean active = true;

    public LoginScreen(ScreenNavigator navigator) {
        super(navigator, "Login");
        setBackground(TITLE_BACKGROUND);
        buildLoginForm();
        addMenuButton("Register", SignUpMenu::new);
    }

    private void buildLoginForm() {
        Table panel = createPanel();
        Label status = createStatusLabel();
        TextField username = field("Username");
        TextField password = passwordField("Password");

        addField(panel, "Username", username);
        addField(panel, "Password", password);

        Label persistenceNotice = new Label(
                "Stay logged in is not available in online mode yet.",
                skin, "secondary");
        persistenceNotice.setWrap(true);
        panel.add(persistenceNotice).colspan(2).width(520f).left()
                .padTop(4f).row();

        TextButton login = new TextButton("Login", skin, "green");
        TextButton forgot = new TextButton("Forgot Password", skin, "brown");
        forgot.setDisabled(true);
        Table buttons = new Table();
        buttons.add(login).width(220f).height(52f).pad(6f);
        buttons.add(forgot).width(240f).height(52f).pad(6f);
        panel.add(buttons).colspan(2).padTop(12f).row();

        Label passwordResetNotice = new Label(
                "Remote password recovery is not available yet.",
                skin, "secondary");
        panel.add(passwordResetNotice).colspan(2).row();
        panel.add(status).colspan(2).growX().width(520f).padTop(10f).row();

        LoginFlowController controller = new LoginFlowController(
                navigator.getAccountSession(), navigator.getUiDispatcher(),
                new LoginFlowController.View() {
                    @Override
                    public void setSubmitting(boolean submitting, String message) {
                        if (!active) {
                            return;
                        }
                        login.setDisabled(submitting);
                        username.setDisabled(submitting);
                        password.setDisabled(submitting);
                        status.setText(message);
                        status.setColor(Color.LIGHT_GRAY);
                    }

                    @Override
                    public void loginSucceeded(AccountProfile profile) {
                        if (active) {
                            password.setText("");
                            navigator.completeRemoteLogin(profile);
                        }
                    }

                    @Override
                    public void showError(String message) {
                        if (active) {
                            showErrorMessage(status, message);
                        }
                    }
                });

        login.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (login.isDisabled()) {
                    return;
                }
                if (username.getText().trim().isEmpty()
                        || password.getText().isEmpty()) {
                    showErrorMessage(status, "Enter both username and password.");
                    return;
                }
                controller.submit(username.getText().trim(), password.getText());
            }
        });

        content.add(panel).width(620f).pad(20f);
        String notice = navigator.consumeAuthenticationNotice();
        if (notice != null) {
            status.setText(notice);
            status.setColor(Color.GOLD);
        }
    }

    private Table createPanel() {
        Table panel = new Table();
        panel.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
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

    private void showErrorMessage(Label status, String message) {
        status.setText(message);
        status.setColor(Color.SCARLET);
    }

    @Override
    public void dispose() {
        active = false;
        super.dispose();
    }
}
