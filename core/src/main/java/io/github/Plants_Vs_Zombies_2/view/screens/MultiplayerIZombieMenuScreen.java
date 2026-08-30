package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.github.Plants_Vs_Zombies_2.network.session.ClientSessionState;
import io.github.Plants_Vs_Zombies_2.view.multiplayer.ClientMatchmakingTransport;
import io.github.Plants_Vs_Zombies_2.view.multiplayer.MatchmakingFlowController;

/** Direct invitation and random matchmaking entry for graphical I, Zombie. */
public final class MultiplayerIZombieMenuScreen extends AbstractScreen {
    private final MatchmakingFlowController controller;
    private final TextField usernameField;
    private final Label connectionLabel;
    private final Label statusLabel;
    private final TextButton inviteButton;
    private final TextButton cancelInviteButton;
    private final TextButton joinQueueButton;
    private final TextButton leaveQueueButton;
    private final TextButton retryButton;
    private boolean disposed;

    public MultiplayerIZombieMenuScreen(ScreenNavigator navigator, String initialStatus) {
        super(navigator, "Multiplayer I, Zombie");

        if (navigator.getAccountSession().getMatchmakingClient() == null) {
            throw new IllegalStateException("Authenticated matchmaking client is unavailable");
        }

        connectionLabel = new Label("", skin, "medium_outline");
        statusLabel = new Label(initialStatus == null ? "" : initialStatus,
                skin, "medium_outline");
        statusLabel.setWrap(true);
        usernameField = new TextField("", skin);
        usernameField.setMessageText("Opponent username");

        inviteButton = new TextButton("Send Invitation", skin, "green");
        cancelInviteButton = new TextButton("Cancel Invitation", skin, "brown");
        joinQueueButton = new TextButton("Join Random Queue", skin, "green");
        leaveQueueButton = new TextButton("Leave Queue", skin, "brown");
        retryButton = new TextButton("Retry Last Request", skin, "green");

        Table panel = new Table();
        panel.defaults().pad(8f);
        panel.add(connectionLabel).colspan(2).left().row();
        panel.add(new Label("Play with a specific user", skin, "big"))
                .colspan(2).padTop(12f).row();
        panel.add(usernameField).width(360f).height(52f);
        panel.add(inviteButton).width(220f).height(52f).row();
        panel.add(cancelInviteButton).colspan(2).width(220f).height(48f).row();
        panel.add(new Label("Find a random opponent", skin, "big"))
                .colspan(2).padTop(22f).row();
        panel.add(joinQueueButton).width(260f).height(52f);
        panel.add(leaveQueueButton).width(220f).height(52f).row();
        panel.add(statusLabel).colspan(2).width(720f).left().padTop(18f).row();
        panel.add(retryButton).colspan(2).width(240f).height(46f).row();
        content.add(panel).expand().center();

        controller = new MatchmakingFlowController(
                new ClientMatchmakingTransport(navigator.getAccountSession().getMatchmakingClient()),
                navigator.getUiDispatcher(), this::applyState);

        inviteButton.addListener(click(() -> controller.invite(usernameField.getText())));
        cancelInviteButton.addListener(click(() -> controller.cancelInvitation()));
        joinQueueButton.addListener(click(() -> controller.joinQueue()));
        leaveQueueButton.addListener(click(() -> controller.leaveQueue()));
        retryButton.addListener(click(() -> controller.retryLast()));
        if (initialStatus != null && !initialStatus.isBlank()) {
            controller.showNotice(initialStatus);
        }
        addReturnToCurrentMenuButton("Back");
        refreshConnectionState();
    }

    private ClickListener click(Runnable action) {
        return new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!disposed) action.run();
            }
        };
    }

    private void applyState(MatchmakingFlowController.State state) {
        if (disposed) return;
        statusLabel.setText(state.status());
        boolean authenticated = navigator.getAccountSession().getState()
                == ClientSessionState.AUTHENTICATED;
        boolean busy = state.requestInFlight() || !authenticated;
        inviteButton.setDisabled(busy || state.pendingInvitationId() != null);
        usernameField.setDisabled(busy || state.pendingInvitationId() != null);
        cancelInviteButton.setDisabled(busy || state.pendingInvitationId() == null);
        joinQueueButton.setDisabled(busy || state.queued());
        leaveQueueButton.setDisabled(busy || !state.queued());
        retryButton.setDisabled(busy || !state.error());
        refreshConnectionState();
    }

    private void refreshConnectionState() {
        ClientSessionState state = navigator.getAccountSession().getState();
        String username = navigator.getAccountSession().getProfile() == null
                ? "not authenticated"
                : navigator.getAccountSession().getProfile().getUsername();
        connectionLabel.setText("Server: " + state + " | account: " + username);
    }

    @Override public void render(float delta) {
        refreshConnectionState();
        super.render(delta);
    }

    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        controller.close();
        super.dispose();
    }
}
