package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.session.ProfileFlowController;
import io.github.Plants_Vs_Zombies_2.view.presentation.Phase3Text;

/** Complete graphical view of the authenticated server-owned profile. */
public final class ProfileScreen extends AbstractScreen {
    private final Table profileValues = new Table();
    private final Label statusLabel;
    private final TextButton refreshButton;
    private final ProfileFlowController controller;
    private boolean disposed;

    public ProfileScreen(ScreenNavigator navigator) {
        super(navigator, "Profile");
        addBackButton();

        statusLabel = new Label("", skin, "secondary");
        statusLabel.setWrap(true);
        refreshButton = new TextButton("Refresh from Server", skin, "green");

        Table panel = new Table();
        panel.setBackground(skin.get("brown", TextButton.TextButtonStyle.class).up);
        panel.pad(22f);
        panel.add(new Label("Server Profile", skin, "big"))
                .left().padBottom(12f).row();
        profileValues.defaults().left().pad(5f);
        panel.add(profileValues).width(660f).left().row();
        panel.add(statusLabel).width(660f).left().padTop(12f).row();
        panel.add(refreshButton).width(230f).height(48f).padTop(14f);
        content.add(panel).width(720f).expand().center();

        controller = new ProfileFlowController(navigator.getAccountSession(),
                navigator.getUiDispatcher(), this::applyState);
        refreshButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!disposed) controller.refresh();
            }
        });
        applyState(controller.getState());
        controller.refresh();
    }

    private void applyState(ProfileFlowController.State state) {
        if (disposed) return;
        profileValues.clearChildren();
        AccountProfile profile = state.profile();
        if (profile == null) {
            profileValues.add(new Label("No server profile is available.", skin,
                    "medium_outline")).colspan(2).left().row();
        } else {
            addTextValue("Username", Phase3Text.required(
                    profile.getUsername(), "Account unavailable"));
            addTextValue("Nickname", Phase3Text.optional(
                    profile.getNickname()));
            addTextValue("Email", Phase3Text.optional(profile.getEmail()));
            addTextValue("Gender", Phase3Text.prettyIdentifier(
                    profile.getGender(), "Not provided"));
            addNumberValue("Coins", profile.getCoins());
            addNumberValue("Diamonds", profile.getDiamonds());
            addNumberValue("Sprouts", profile.getSprouts());
            addNumberValue("Plant Food", profile.getPlantFoodCount());
            addNumberValue("Pot Inventory", profile.getPotCount());
            addTextValue("Last Completed Level", Phase3Text.levelProgress(
                    profile.getLastCompletedChapter(),
                    profile.getLastCompletedLevel()));
            addNumberValue("Completed Minigames",
                    profile.getCompletedMinigames());
            addNumberValue("Highest Score", profile.getHighestScore());
            addNumberValue("Games Played", profile.getGamesPlayed());
        }
        statusLabel.setText(Phase3Text.status(state.message(),
                "Authenticated server profile."));
        refreshButton.setDisabled(state.loading());
        refreshButton.setText(state.loading() ? "Refreshing..."
                : state.retryAvailable() ? "Retry Server Refresh"
                        : "Refresh from Server");
    }

    private void addNumberValue(String name, int value) {
        addTextValue(name, Integer.toString(value));
    }

    private void addTextValue(String name, String value) {
        profileValues.add(new Label(name + ":", skin, "medium_outline"))
                .width(235f);
        profileValues.add(new Label(Phase3Text.optional(value), skin,
                "secondary"))
                .width(410f).row();
    }

    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        controller.close();
        super.dispose();
    }
}
