package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.session.ProfileFlowController;

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
            addValue("Username", profile.getUsername());
            addValue("Nickname", profile.getNickname());
            addValue("Email", profile.getEmail());
            addValue("Gender", profile.getGender());
            addValue("Coins", profile.getCoins());
            addValue("Diamonds", profile.getDiamonds());
            addValue("Sprouts", profile.getSprouts());
            addValue("Plant Food", profile.getPlantFoodCount());
            addValue("Pot Inventory", profile.getPotCount());
            addValue("Last Completed Level", profile.getLastCompletedChapter()
                    + "-" + profile.getLastCompletedLevel());
            addValue("Completed Minigames", profile.getCompletedMinigames());
            addValue("Highest Score", profile.getHighestScore());
            addValue("Games Played", profile.getGamesPlayed());
        }
        statusLabel.setText(state.message() == null
                ? "Authenticated server profile." : state.message());
        refreshButton.setDisabled(state.loading());
        refreshButton.setText(state.loading() ? "Refreshing..."
                : state.retryAvailable() ? "Retry Server Refresh"
                        : "Refresh from Server");
    }

    private void addValue(String name, Object value) {
        profileValues.add(new Label(name + ":", skin, "medium_outline"))
                .width(235f);
        profileValues.add(new Label(String.valueOf(value), skin, "secondary"))
                .width(410f).row();
    }

    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        controller.close();
        super.dispose();
    }
}
