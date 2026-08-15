package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.github.Plants_Vs_Zombies_2.controller.SignupMenuController;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.menu.LoginMenu;
import io.github.Plants_Vs_Zombies_2.model.security.Question;

/** Graphical registration flow for every signup-menu command. */
public final class SignUpScreen extends AbstractScreen {
    private static final String TITLE_BACKGROUND =
            "pvz-assets/ATLASES/TITLESCREEN4_768_00.PNG";

    public SignUpScreen(ScreenNavigator navigator) {
        super(navigator, "Register");
        setBackground(TITLE_BACKGROUND);
        showRegistrationForm(null);

        addMenuButton("Login", LoginMenu::new);
        addActionButton("Exit", navigator::exitApplication);
    }

    private void showRegistrationForm(CommandResult initialResult) {
        content.clearChildren();

        Table panel = createPanel();
        Label status = createStatusLabel();

        TextField username = field("Username");
        TextField password = passwordField("Password");
        TextField passwordConfirm = passwordField("Confirm password");
        TextField nickname = field("Nickname");
        TextField email = field("Email");
        SelectBox<String> gender = new SelectBox<>(skin);
        gender.setItems("Male", "Female");

        addField(panel, "Username", username);
        addField(panel, "Password", password);
        addField(panel, "Confirm password", passwordConfirm);
        addField(panel, "Nickname", nickname);
        addField(panel, "Email", email);
        addField(panel, "Gender", gender);

        Label passwordHint = new Label(
                "Password: 8+ chars with lowercase, uppercase, digit and special character.",
                skin, "secondary");
        passwordHint.setWrap(true);
        panel.add(passwordHint).colspan(2).growX().width(520f).padTop(4f).row();

        TextButton continueButton = new TextButton(
                "Continue", skin, "green");
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                CommandResult result = SignupMenuController.register(
                        username.getText().trim(),
                        password.getText(),
                        passwordConfirm.getText(),
                        nickname.getText(),
                        email.getText().trim(),
                        gender.getSelected());
                showResult(status, result);
                if (result.isSuccsesful()) {
                    showSecurityQuestionForm(CommandResult.success(
                            "User data is valid. Choose a security question."));
                }
            }
        });
        panel.add(continueButton).colspan(2).width(240f).height(52f).padTop(14f).row();
        panel.add(status).colspan(2).growX().width(540f).padTop(10f).row();

        content.add(panel).width(620f).pad(20f);
        if (initialResult != null) {
            showResult(status, initialResult);
        }
    }

    private void showSecurityQuestionForm(CommandResult initialResult) {
        content.clearChildren();

        Table panel = createPanel();
        Label status = createStatusLabel();

        String[] questions = new String[Question.values().length];
        for (int i = 0; i < Question.values().length; i++) {
            Question question = Question.values()[i];
            questions[i] = question.getNumber() + ". " + question.getText();
        }

        SelectBox<String> questionBox = new SelectBox<>(skin);
        questionBox.setItems(questions);
        TextField answer = field("Security answer");
        TextField answerConfirm = field("Confirm answer");

        addField(panel, "Security question", questionBox);
        addField(panel, "Answer", answer);
        addField(panel, "Confirm answer", answerConfirm);

        Table buttons = new Table();
        TextButton createAccount = new TextButton(
                "Create Account", skin, "green");
        TextButton cancel = new TextButton("Start Over", skin, "brown");
        buttons.add(createAccount).width(240f).height(52f).pad(6f);
        buttons.add(cancel).width(200f).height(52f).pad(6f);
        panel.add(buttons).colspan(2).padTop(12f).row();
        panel.add(status).colspan(2).growX().width(540f).padTop(10f).row();

        createAccount.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int questionNumber = questionBox.getSelectedIndex() + 1;
                CommandResult result = SignupMenuController.pickQuestion(
                        questionNumber, answer.getText(), answerConfirm.getText());
                showResult(status, result);
                if (result.isSuccsesful()) {
                    navigator.showCurrentMenu();
                }
            }
        });

        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                SignupMenuController.cancelPendingRegistration();
                showRegistrationForm(null);
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

    private void addField(Table table, String name, com.badlogic.gdx.scenes.scene2d.Actor actor) {
        table.add(new Label(name, skin)).right().padRight(12f);
        table.add(actor).width(360f).height(46f).left().row();
    }

    private void showResult(Label status, CommandResult result) {
        status.setText(result.getMessage());
        status.setColor(result.isSuccsesful() ? Color.GREEN : Color.SCARLET);
    }
}
