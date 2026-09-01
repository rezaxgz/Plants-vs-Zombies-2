package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.Settings;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/** Graphical settings menu. */
public final class SettingsScreen extends AbstractScreen {
    private final Label difficultyValue;
    private final Label gameSpeedValue;
    private final Label gridValue;
    private final Label debugValue;

    public SettingsScreen(ScreenNavigator navigator) {
        super(navigator, "Settings");

        Table panel = new Table();
        panel.setBackground(
                skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        panel.pad(24f);
        panel.defaults().pad(8f);

        panel.add(new Label("Difficulty", skin, "medium_outline")).left();
        difficultyValue = new Label("", skin, "medium_outline");
        panel.add(difficultyValue).width(120f).center();
        panel.add(createDifficultyButton("-", -1)).size(56f, 48f);
        panel.add(createDifficultyButton("+", 1)).size(56f, 48f).row();

        panel.add(new Label("Game Speed", skin, "medium_outline")).left();
        gameSpeedValue = new Label("", skin, "medium_outline");
        panel.add(gameSpeedValue).width(120f).center();
        panel.add(createGameSpeedButton("-", -1)).size(56f, 48f);
        panel.add(createGameSpeedButton("+", 1)).size(56f, 48f).row();

        panel.add(new Label("Show Map Grid", skin, "medium_outline")).left();
        gridValue = new Label("", skin, "medium_outline");
        panel.add(gridValue).width(120f).center();
        panel.add(createGridToggle()).colspan(2).width(140f).height(50f).row();

        panel.add(new Label("Debug Mode", skin, "medium_outline")).left();
        debugValue = new Label("", skin, "medium_outline");
        panel.add(debugValue).width(120f).center();
        panel.add(createDebugToggle()).colspan(2).width(140f).height(50f).row();

        content.add(panel).expand().center().width(620f);
        addBackButton();
        refreshSettingsState();
    }

    private TextButton createDifficultyButton(String text, int delta) {
        TextButton button = new TextButton(text, skin, "brown");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                User user = App.getInstance().getLoggedInUser();
                if (user == null) {
                    return;
                }
                Settings settings = user.getSettings();
                int next = Math.max(Settings.MIN_DIFFICULTY,
                        Math.min(Settings.MAX_DIFFICULTY,
                                settings.getDifficultyLevel() + delta));
                settings.setDifficultyLevel(next);
                saveAndRefresh();
            }
        });
        return button;
    }

    private TextButton createGameSpeedButton(String text, int delta) {
        TextButton button = new TextButton(text, skin, "brown");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                User user = App.getInstance().getLoggedInUser();
                if (user == null) {
                    return;
                }
                Settings settings = user.getSettings();
                int next = Math.max(Settings.MIN_GAME_SPEED,
                        Math.min(Settings.MAX_GAME_SPEED,
                                settings.getGameSpeed() + delta));
                settings.setGameSpeed(next);
                saveAndRefresh();
            }
        });
        return button;
    }

    private TextButton createGridToggle() {
        TextButton button = new TextButton("Toggle", skin, "green");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                User user = App.getInstance().getLoggedInUser();
                if (user == null) {
                    return;
                }
                Settings settings = user.getSettings();
                settings.setShowGameMapGrid(!settings.isShowGameMapGrid());
                saveAndRefresh();
            }
        });
        return button;
    }

    private TextButton createDebugToggle() {
        TextButton button = new TextButton("Toggle", skin, "green");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                User user = App.getInstance().getLoggedInUser();
                if (user == null) {
                    return;
                }
                Settings settings = user.getSettings();
                settings.setDebugMode(!settings.isDebugMode());
                saveAndRefresh();
            }
        });
        return button;
    }

    private void saveAndRefresh() {
        UserManager.saveAllUsers();
        navigator.getGameplaySync().markDirty();
        refreshSettingsState();
    }

    private void refreshSettingsState() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            difficultyValue.setText("--");
            gameSpeedValue.setText("--");
            gridValue.setText("OFF");
            debugValue.setText("OFF");
            return;
        }
        Settings settings = user.getSettings();
        difficultyValue.setText(Integer.toString(settings.getDifficultyLevel()));
        gameSpeedValue.setText(Integer.toString(settings.getGameSpeed()));
        gridValue.setText(settings.isShowGameMapGrid() ? "ON" : "OFF");
        debugValue.setText(settings.isDebugMode() ? "ON" : "OFF");
    }

    @Override
    public void render(float delta) {
        refreshSettingsState();
        super.render(delta);
    }
}
