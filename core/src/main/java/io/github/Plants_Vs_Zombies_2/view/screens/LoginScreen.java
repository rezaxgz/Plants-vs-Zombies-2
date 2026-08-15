package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.github.Plants_Vs_Zombies_2.controller.LoginMenuController;
import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.menu.LoginMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.SignUpMenu;

/** Graphical login and password-recovery flow. */
public final class LoginScreen extends AbstractScreen {
    private static final String TITLE_BACKGROUND =
            "pvz-assets/ATLASES/TITLESCREEN4_768_00.PNG";

    public LoginScreen(ScreenNavigator navigator) {
        super(navigator, "Login");
        setBackground(TITLE_BACKGROUND);
        showLoginForm(null);

        addMenuButton("Register", SignUpMenu::new);
    }

    private void showLoginForm(CommandResult initialResult) {
        content.clearChildren();
        LoginMenuController.cancelPasswordReset();

        Table panel = createPanel();
        Label status = createStatusLabel();
        TextField username = field("Username");
        TextField password = passwordField("Password");
        CheckBox stayLoggedIn = new CheckBox(" Stay logged in", skin);
        stayLoggedIn.getImage().setScaling(Scaling.fit);
        stayLoggedIn.getImageCell().size(28f, 28f).padRight(8f);

        addField(panel, "Username", username);
        addField(panel, "Password", password);
        panel.add(stayLoggedIn).colspan(2).left().padLeft(145f).padTop(2f).row();

        Table buttons = new Table();
        TextButton login = new TextButton("Login", skin, "green");
        TextButton forgot = new TextButton("Forgot Password", skin, "brown");
        buttons.add(login).width(220f).height(52f).pad(6f);
        buttons.add(forgot).width(240f).height(52f).pad(6f);
        panel.add(buttons).colspan(2).padTop(12f).row();
        panel.add(status).colspan(2).growX().width(520f).padTop(10f).row();

        login.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                CommandResult result = LoginMenuController.login(
                        username.getText().trim(),
                        password.getText(),
                        stayLoggedIn.isChecked());
                showResult(status, result);
                if (result.isSuccsesful()) {
                    navigator.showCurrentMenu();
                }
            }
        });

        forgot.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showForgotPasswordLookup(null);
            }
        });

        content.add(panel).width(620f).pad(20f);
        if (initialResult != null) {
            showResult(status, initialResult);
        }
    }

    private void showForgotPasswordLookup(CommandResult initialResult) {
        content.clearChildren();
        LoginMenuController.cancelPasswordReset();

        Table panel = createPanel();
        Label status = createStatusLabel();
        TextField username = field("Username");
        TextField email = field("Email");

        addField(panel, "Username", username);
        addField(panel, "Email", email);

        Table buttons = new Table();
        TextButton continueButton = new TextButton("Continue", skin, "green");
        TextButton cancel = new TextButton("Back to Login", skin, "brown");
        buttons.add(continueButton).width(220f).height(52f).pad(6f);
        buttons.add(cancel).width(220f).height(52f).pad(6f);
        panel.add(buttons).colspan(2).padTop(12f).row();
        panel.add(status).colspan(2).growX().width(520f).padTop(10f).row();

        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                CommandResult result = LoginMenuController.beginPasswordReset(
                        username.getText().trim(), email.getText().trim());
                showResult(status, result);
                if (result.isSuccsesful()) {
                    showPasswordResetForm(null);
                }
            }
        });
        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showLoginForm(null);
            }
        });

        content.add(panel).width(620f).pad(20f);
        if (initialResult != null) {
            showResult(status, initialResult);
        }
    }

    private void showPasswordResetForm(CommandResult initialResult) {
        content.clearChildren();

        LoginMenu menu = (LoginMenu) App.getInstance().getCurrentMenu();
        String question = menu.getTempUser() == null
                ? "Security question unavailable"
                : menu.getTempUser().getSecurityQuestion();

        Table panel = createPanel();
        Label status = createStatusLabel();
        Label questionLabel = new Label(question, skin, "medium");
        questionLabel.setWrap(true);
        panel.add(questionLabel).colspan(2).growX().width(520f).padBottom(10f).row();

        TextField answer = field("Security answer");
        TextField newPassword = passwordField("New password");
        TextField confirmPassword = passwordField("Confirm new password");
        addField(panel, "Answer", answer);
        addField(panel, "New password", newPassword);
        addField(panel, "Confirm password", confirmPassword);

        Table buttons = new Table();
        TextButton changePassword = new TextButton(
                "Change Password", skin, "green");
        TextButton cancel = new TextButton("Cancel", skin, "brown");
        buttons.add(changePassword).width(240f).height(52f).pad(6f);
        buttons.add(cancel).width(180f).height(52f).pad(6f);
        panel.add(buttons).colspan(2).padTop(12f).row();
        panel.add(status).colspan(2).growX().width(520f).padTop(10f).row();

        changePassword.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                CommandResult result = LoginMenuController.completePasswordReset(
                        answer.getText(),
                        newPassword.getText(),
                        confirmPassword.getText());
                showResult(status, result);
                if (result.isSuccsesful()) {
                    showLoginForm(result);
                } else if (menu.getTempUser() == null) {
                    showForgotPasswordLookup(result);
                }
            }
        });
        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                LoginMenuController.cancelPasswordReset();
                showLoginForm(null);
            }
        });

        content.add(panel).width(650f).pad(20f);
        if (initialResult != null) {
            showResult(status, initialResult);
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

    private void showResult(Label status, CommandResult result) {
        status.setText(result.getMessage());
        status.setColor(result.isSuccsesful() ? Color.GREEN : Color.SCARLET);
    }
}
