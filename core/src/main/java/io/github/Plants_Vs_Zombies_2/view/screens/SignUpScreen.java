package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.github.Plants_Vs_Zombies_2.model.menu.LoginMenu;
import io.github.Plants_Vs_Zombies_2.model.security.Question;
import io.github.Plants_Vs_Zombies_2.network.auth.RegistrationDetails;
import io.github.Plants_Vs_Zombies_2.network.session.SignupFlowController;
import io.github.Plants_Vs_Zombies_2.view.presentation.Phase3Text;

/** Two-step graphical registration backed exclusively by the remote server. */
public final class SignUpScreen extends AbstractScreen {
    private static final String TITLE_BACKGROUND =
            "pvz-assets/ATLASES/TITLESCREEN4_768_00.PNG";
    private PendingRegistration pendingRegistration;
    private boolean active = true;

    public SignUpScreen(ScreenNavigator navigator) {
        super(navigator, "Register");
        setBackground(TITLE_BACKGROUND);
        showRegistrationForm(null);
        addMenuButton("Login", LoginMenu::new);
        addActionButton("Exit", navigator::exitApplication);
    }

    private void showRegistrationForm(String initialMessage) {
        content.clearChildren();
        pendingRegistration = null;
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
        Label hint = new Label(
                "Password: 8+ chars with lowercase, uppercase, digit and special character.",
                skin, "secondary");
        hint.setWrap(true);
        panel.add(hint).colspan(2).width(520f).padTop(4f).row();

        TextButton continueButton = new TextButton("Continue", skin, "green");
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String trimmedNickname = nickname.getText().trim();
                String validation = SignupFlowController.validateAccountDetails(
                        username.getText().trim(), password.getText(),
                        passwordConfirm.getText(), trimmedNickname,
                        email.getText().trim(), gender.getSelected());
                if (validation != null) {
                    showError(status, validation);
                    return;
                }
                pendingRegistration = new PendingRegistration(
                        username.getText().trim(), password.getText(),
                        passwordConfirm.getText(), trimmedNickname,
                        email.getText().trim(), gender.getSelected());
                showSecurityQuestionForm("Account details are valid. Choose a security question.");
            }
        });
        panel.add(continueButton).colspan(2).width(240f).height(52f)
                .padTop(14f).row();
        panel.add(status).colspan(2).growX().width(540f).padTop(10f).row();
        content.add(panel).width(620f).pad(20f);
        if (initialMessage != null) {
            status.setText(initialMessage);
            status.setColor(Color.GOLD);
        }
    }

    private void showSecurityQuestionForm(String initialMessage) {
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

        TextButton createAccount = new TextButton("Create Account", skin, "green");
        TextButton cancel = new TextButton("Start Over", skin, "brown");
        Table buttons = new Table();
        buttons.add(createAccount).width(240f).height(52f).pad(6f);
        buttons.add(cancel).width(200f).height(52f).pad(6f);
        panel.add(buttons).colspan(2).padTop(12f).row();
        panel.add(status).colspan(2).growX().width(540f).padTop(10f).row();

        SignupFlowController controller = new SignupFlowController(
                navigator.getAccountSession(), navigator.getUiDispatcher(),
                new SignupFlowController.View() {
                    @Override
                    public void setSubmitting(boolean submitting, String message) {
                        if (!active) {
                            return;
                        }
                        createAccount.setDisabled(submitting);
                        cancel.setDisabled(submitting);
                        questionBox.setDisabled(submitting);
                        answer.setDisabled(submitting);
                        answerConfirm.setDisabled(submitting);
                        status.setText(Phase3Text.status(message,
                                "Creating your account..."));
                        status.setColor(Color.LIGHT_GRAY);
                    }

                    @Override
                    public void signupSucceeded() {
                        if (active) {
                            pendingRegistration = null;
                            answer.setText("");
                            answerConfirm.setText("");
                            navigator.showLoginAfterRegistration();
                        }
                    }

                    @Override
                    public void showError(String message) {
                        if (active) {
                            SignUpScreen.this.showError(status, message);
                        }
                    }
                });

        createAccount.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (createAccount.isDisabled() || pendingRegistration == null) {
                    return;
                }
                int questionNumber = Question.values()[
                        questionBox.getSelectedIndex()].getNumber();
                String validation = SignupFlowController.validateSecurityAnswer(
                        questionNumber, answer.getText(), answerConfirm.getText());
                if (validation != null) {
                    showError(status, validation);
                    return;
                }
                controller.submit(pendingRegistration.toDetails(
                        questionNumber, answer.getText(), answerConfirm.getText()));
            }
        });
        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!cancel.isDisabled()) {
                    showRegistrationForm(null);
                }
            }
        });
        content.add(panel).width(650f).pad(20f);
        status.setText(Phase3Text.status(initialMessage,
                "Choose a security question."));
        status.setColor(Color.GOLD);
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

    private void showError(Label status, String message) {
        status.setText(Phase3Text.status(message,
                "The account request failed."));
        status.setColor(Color.SCARLET);
    }

    @Override
    public void dispose() {
        active = false;
        pendingRegistration = null;
        super.dispose();
    }

    private record PendingRegistration(String username, String password,
            String passwordConfirmation, String nickname, String email,
            String gender) {
        RegistrationDetails toDetails(int questionNumber, String answer,
                String answerConfirmation) {
            return new RegistrationDetails(username, password,
                    passwordConfirmation, nickname, email, gender,
                    questionNumber, answer, answerConfirmation);
        }
    }
}
