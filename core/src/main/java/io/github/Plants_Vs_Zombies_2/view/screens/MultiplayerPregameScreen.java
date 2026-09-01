package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.view.multiplayer.ClientMultiplayerTransport;
import io.github.Plants_Vs_Zombies_2.view.multiplayer.PregameController;
import io.github.Plants_Vs_Zombies_2.view.presentation.Phase3Text;

/** Server-assigned role and readiness screen shown after MATCH_FOUND. */
public final class MultiplayerPregameScreen extends AbstractScreen {
    private final MatchAssignment assignment;
    private final PregameController controller;
    private final Label roleLabel;
    private final Label localReadyLabel;
    private final Label opponentReadyLabel;
    private final Label connectionLabel;
    private final Label statusLabel;
    private final TextButton readyButton;
    private final TextButton leaveButton;
    private boolean disposed;

    public MultiplayerPregameScreen(ScreenNavigator navigator,
            MatchAssignment assignment) {
        super(navigator, "Multiplayer I, Zombie - Pre-game");
        this.assignment = assignment;
        if (navigator.getAccountSession().getMultiplayerGameClient() == null) {
            throw new IllegalStateException("Authoritative multiplayer client is unavailable");
        }

        roleLabel = new Label("", skin, "big");
        localReadyLabel = new Label("", skin, "medium_outline");
        opponentReadyLabel = new Label("", skin, "medium_outline");
        connectionLabel = new Label("", skin, "medium_outline");
        statusLabel = new Label("", skin, "medium_outline");
        statusLabel.setWrap(true);
        readyButton = new TextButton("Ready", skin, "green");
        leaveButton = new TextButton("Leave Match", skin, "brown");

        Table panel = new Table();
        panel.defaults().pad(8f).left();
        panel.add(new Label("Preparing your multiplayer match", skin, "big"))
                .colspan(2).center().padBottom(12f).row();
        panel.add(new Label("Opponent:", skin));
        panel.add(new Label(Phase3Text.username(
                assignment.getOpponentUsername()), skin)).row();
        panel.add(new Label("Your role:", skin));
        Table rolePresentation = new Table();
        rolePresentation.add(createRoleVisual()).size(88f, 74f)
                .padRight(10f);
        rolePresentation.add(roleLabel).left();
        panel.add(rolePresentation).row();
        panel.add(new Label("Connection:", skin));
        panel.add(connectionLabel).row();
        panel.add(new Label("You:", skin));
        panel.add(localReadyLabel).row();
        panel.add(new Label("Opponent:", skin));
        panel.add(opponentReadyLabel).row();
        panel.add(statusLabel).colspan(2).width(760f).padTop(14f).row();
        panel.add(readyButton).width(220f).height(54f).padTop(18f);
        panel.add(leaveButton).width(220f).height(54f).padTop(18f).row();
        content.add(panel).expand().center();

        controller = new PregameController(
                new ClientMultiplayerTransport(navigator.getAccountSession().getMultiplayerGameClient()),
                navigator.getUiDispatcher(), assignment, new PregameController.Observer() {
                    @Override public void changed(PregameController.State state) {
                        applyState(state);
                    }
                    @Override public void matchStarted(MatchStateSnapshot snapshot) {
                        if (!disposed) navigator.showMultiplayerIZombieGame(assignment, snapshot);
                    }
                    @Override public void leaveCompleted() {
                        if (!disposed) navigator.showMultiplayerIZombieMenu("Left the match.");
                    }
                });

        readyButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!disposed) controller.ready();
            }
        });
        leaveButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!disposed) controller.leave();
            }
        });
    }

    private void applyState(PregameController.State state) {
        if (disposed) return;
        roleLabel.setText(Phase3Text.role(state.role()));
        localReadyLabel.setText(state.localReady() ? "Ready" : "Not ready");
        opponentReadyLabel.setText(state.opponentReady()
                ? "Ready" : "Waiting for opponent...");
        connectionLabel.setText(Phase3Text.connection(
                navigator.getAccountSession().getState()));
        statusLabel.setText(Phase3Text.status(state.status(),
                "Waiting for both players..."));
        readyButton.setDisabled(state.requestInFlight() || state.localReady()
                || state.cancelled());
        leaveButton.setDisabled(state.requestInFlight());
        if (state.cancelled()) {
            navigator.showMultiplayerIZombieMenu(state.status());
        }
    }

    private Actor createRoleVisual() {
        String asset = MultiplayerVisualCatalog.roleIconAsset(
                assignment.getRole());
        TextureRegion region = navigator.getTextureBank().region(asset);
        if (region == null) {
            region = navigator.getTextureBank().region(
                    MultiplayerVisualCatalog.MISSING_ASSET);
        }
        if (region == null) {
            return new Label(Phase3Text.roleShort(assignment.getRole()), skin,
                    "medium_outline");
        }
        Image image = new Image(region);
        image.setScaling(Scaling.fit);
        image.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        return image;
    }

    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        controller.close();
        super.dispose();
    }
}
