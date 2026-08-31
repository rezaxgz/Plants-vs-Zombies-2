package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardClient;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardEntry;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardFlowController;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardSortColumn;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardSortDirection;
import io.github.Plants_Vs_Zombies_2.network.leaderboard.LeaderboardTransport;
import io.github.Plants_Vs_Zombies_2.network.session.ClientSessionState;
import io.github.Plants_Vs_Zombies_2.network.session.SessionStateListener;

/** Graphical leaderboard backed exclusively by the authenticated server. */
public final class LeaderboardScreen extends AbstractScreen {
    private static final String SORT_UP = "IMAGE_UI_ALMANAC_SORT_BUTTON_UP";
    private static final String SORT_DOWN = "IMAGE_UI_ALMANAC_SORT_BUTTON_DOWN";
    private static final String ASCENDING_UP = "IMAGE_UI_ALMANAC_SORT_ASCENDING_UP";
    private static final String ASCENDING_DOWN = "IMAGE_UI_ALMANAC_SORT_ASCENDING_DOWN";
    private static final String DESCENDING_UP = "IMAGE_UI_ALMANAC_SORT_DESCENDING_UP";
    private static final String DESCENDING_DOWN = "IMAGE_UI_ALMANAC_SORT_DESCENDING_DOWN";

    private final LeaderboardFlowController controller;
    private final SessionStateListener sessionListener;
    private Table sortModal;

    public LeaderboardScreen(ScreenNavigator navigator) {
        super(navigator, "Leaderboard");
        addBackButton();
        LeaderboardClient client = navigator.getAccountSession()
                .getLeaderboardClient();
        LeaderboardTransport transport = client == null
                ? query -> java.util.concurrent.CompletableFuture.failedFuture(
                        new IllegalStateException(
                                "Leaderboard service is unavailable"))
                : client;
        controller = new LeaderboardFlowController(transport,
                navigator.getUiDispatcher(), this::rebuildLeaderboard);
        sessionListener = (previous, current, failure) -> {
            if (previous == ClientSessionState.AUTHENTICATED
                    && current != ClientSessionState.AUTHENTICATED) {
                controller.connectionLost(failure);
            }
        };
        navigator.getAccountSession().addStateListener(sessionListener);
        controller.load();
        rebuildLeaderboard(controller.getState());
    }

    private void rebuildLeaderboard(LeaderboardFlowController.State state) {
        content.clearChildren();

        Table panel = new Table();
        panel.setBackground(skin.get("brown", TextButtonStyle.class).up);
        panel.pad(18f);

        Table toolbar = new Table();
        toolbar.add(new Label("Global Leaderboard", skin, "big"))
                .left().expandX();
        toolbar.add(new Label(buildSortSummary(state), skin, "secondary"))
                .right().padRight(10f);
        ImageButton sortButton = assetImageButton(
                SORT_UP, SORT_DOWN, "Sort leaderboard");
        sortButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                showSortModal();
            }
        });
        toolbar.add(sortButton).size(58f);
        panel.add(toolbar).growX().padBottom(10f).row();

        Table table = new Table();
        table.top();
        addHeaderRow(table);
        if (state.entries().isEmpty()) {
            String text = state.loading() ? "Loading leaderboard..."
                    : state.message() != null ? state.message()
                    : "No players are registered on the server.";
            table.add(new Label(text, skin, "medium_outline"))
                    .colspan(8).pad(40f).row();
        } else {
            for (LeaderboardEntry entry : state.entries()) addUserRow(table, entry);
            if (state.loading()) {
                table.add(new Label("Refreshing leaderboard...", skin,
                        "secondary")).colspan(8).pad(8f).row();
            }
            if (state.message() != null) {
                table.add(new Label(state.message(), skin, "secondary"))
                        .colspan(8).pad(8f).row();
            }
        }

        ScrollPane scroll = new ScrollPane(table, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setScrollbarsOnTop(false);
        panel.add(scroll).width(1135f).height(420f).row();

        Table footer = new Table();
        String count = state.totalPlayers() + " server player"
                + (state.totalPlayers() == 1 ? "" : "s");
        if (state.totalPlayers() > state.entries().size()) {
            count += " (showing first " + state.entries().size() + ")";
        }
        footer.add(new Label(count, skin, "secondary")).left().expandX();
        if (state.authenticatedUserRank() != null) {
            footer.add(new Label("Your rank: #"
                    + state.authenticatedUserRank(), skin, "secondary"))
                    .padRight(12f);
        }
        if (state.retryAvailable()) {
            TextButton retry = new TextButton("Retry", skin, "green");
            retry.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    controller.retry();
                }
            });
            footer.add(retry).width(120f).height(42f);
        }
        panel.add(footer).growX().padTop(8f);
        content.add(panel).expand().center().width(1185f).height(555f);
    }

    private void addHeaderRow(Table table) {
        TextButtonStyle style = skin.get("green", TextButtonStyle.class);
        addHeaderCell(table, "#", 52f, style);
        addHeaderCell(table, "Username", 190f, style);
        addHeaderCell(table, "Last Level", 125f, style);
        addHeaderCell(table, "Minigames", 120f, style);
        addHeaderCell(table, "Daily", 110f, style);
        addHeaderCell(table, "Non-Daily", 125f, style);
        addHeaderCell(table, "Quests", 105f, style);
        addHeaderCell(table, "High Score", 130f, style);
        table.row();
    }

    private void addHeaderCell(Table table, String text, float width,
            TextButtonStyle style) {
        Table cell = new Table();
        cell.setBackground(style.up);
        Label label = new Label(text, skin, "medium_outline");
        label.setAlignment(Align.center);
        cell.add(label).grow();
        table.add(cell).width(width).height(48f).pad(2f);
    }

    private void addUserRow(Table table, LeaderboardEntry entry) {
        AccountProfile profile = navigator.getAccountSession().getProfile();
        boolean currentUser = profile != null
                && profile.getUsername().equals(entry.getUsername());
        TextButtonStyle style = skin.get(currentUser ? "green" : "brown",
                TextButtonStyle.class);
        Color color = currentUser ? Color.WHITE : Color.LIGHT_GRAY;
        addValueCell(table, Integer.toString(entry.getRank()), 52f, style, color);
        addValueCell(table, entry.getUsername(), 190f, style, color);
        addValueCell(table, entry.getLastCompletedChapter() + "-"
                + entry.getLastCompletedLevel(), 125f, style, color);
        addValueCell(table, Integer.toString(entry.getCompletedMinigames()),
                120f, style, color);
        addValueCell(table, Integer.toString(entry.getCompletedDailyQuests()),
                110f, style, color);
        addValueCell(table, Integer.toString(entry.getCompletedNonDailyQuests()),
                125f, style, color);
        addValueCell(table, Integer.toString(entry.getTotalCompletedQuests()),
                105f, style, color);
        addValueCell(table, Integer.toString(entry.getHighestScore()),
                130f, style, color);
        table.row();
    }

    private void addValueCell(Table table, String text, float width,
            TextButtonStyle style, Color color) {
        Table cell = new Table();
        cell.setBackground(style.up);
        Label label = new Label(text, skin, "secondary");
        label.setColor(color);
        label.setAlignment(Align.center);
        cell.add(label).growX().pad(4f);
        table.add(cell).width(width).height(46f).pad(2f);
    }

    private void showSortModal() {
        if (sortModal != null) return;
        LeaderboardFlowController.State state = controller.getState();
        sortModal = new Table();
        sortModal.setFillParent(true);
        sortModal.setTouchable(Touchable.enabled);
        Table panel = new Table();
        panel.setBackground(skin.get("brown", TextButtonStyle.class).up);
        panel.pad(22f);
        Table title = new Table();
        title.add(new Label("Sort Leaderboard", skin, "big")).left().expandX();
        ImageButton close = new ImageButton(skin, "generic_close_circle");
        close.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                closeSortModal();
            }
        });
        title.add(close).size(52f);
        panel.add(title).growX().padBottom(10f).row();
        panel.add(new Label("Choose a server leaderboard ordering.", skin,
                "secondary")).left().padBottom(12f).row();
        Table options = new Table();
        options.defaults().width(240f).height(50f).pad(5f);
        addSortOption(options, "Username", LeaderboardSortColumn.USERNAME, state);
        addSortOption(options, "Last Level", LeaderboardSortColumn.LAST_LEVEL, state);
        options.row();
        addSortOption(options, "Minigames", LeaderboardSortColumn.MINIGAMES, state);
        addSortOption(options, "Daily Quests", LeaderboardSortColumn.DAILY_QUESTS, state);
        options.row();
        addSortOption(options, "Non-Daily Quests",
                LeaderboardSortColumn.NON_DAILY_QUESTS, state);
        addSortOption(options, "All Quests", LeaderboardSortColumn.QUESTS, state);
        options.row();
        addSortOption(options, "High Score", LeaderboardSortColumn.HIGH_SCORE, state);
        panel.add(options).row();
        Table direction = new Table();
        direction.defaults().pad(10f);
        direction.add(new Label("Order", skin, "medium_outline")).padRight(8f);
        ImageButton ascending = assetImageButton(
                ASCENDING_UP, ASCENDING_DOWN, "Ascending");
        ascending.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                closeSortModal();
                controller.selectSort(controller.getState().sortColumn(),
                        LeaderboardSortDirection.ASCENDING);
            }
        });
        direction.add(ascending).size(58f);
        ImageButton descending = assetImageButton(
                DESCENDING_UP, DESCENDING_DOWN, "Descending");
        descending.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                closeSortModal();
                controller.selectSort(controller.getState().sortColumn(),
                        LeaderboardSortDirection.DESCENDING);
            }
        });
        direction.add(descending).size(58f);
        panel.add(direction).padTop(8f).row();
        panel.add(new Label("Current: " + buildSortSummary(state), skin,
                "secondary")).padTop(6f);
        sortModal.add(panel).width(590f).height(520f);
        root.setTouchable(Touchable.disabled);
        stage.addActor(sortModal);
    }

    private void addSortOption(Table options, String label,
            LeaderboardSortColumn column, LeaderboardFlowController.State state) {
        TextButton button = new TextButton(label, skin,
                state.sortColumn() == column ? "green" : "brown");
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                closeSortModal();
                controller.selectSort(column,
                        controller.getState().sortDirection());
            }
        });
        options.add(button);
    }

    private String buildSortSummary(LeaderboardFlowController.State state) {
        return displayName(state.sortColumn()) + " - "
                + (state.sortDirection() == LeaderboardSortDirection.ASCENDING
                        ? "Ascending" : "Descending");
    }

    private String displayName(LeaderboardSortColumn column) {
        return switch (column) {
            case USERNAME -> "Username";
            case LAST_LEVEL -> "Last Level";
            case MINIGAMES -> "Minigames";
            case DAILY_QUESTS -> "Daily Quests";
            case NON_DAILY_QUESTS -> "Non-Daily Quests";
            case QUESTS -> "All Quests";
            case HIGH_SCORE -> "High Score";
        };
    }

    private ImageButton assetImageButton(String normalAsset,
            String pressedAsset, String tooltip) {
        ImageButtonStyle style = new ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(requireAssetRegion(normalAsset));
        style.imageDown = new TextureRegionDrawable(requireAssetRegion(pressedAsset));
        style.imageOver = style.imageDown;
        ImageButton button = new ImageButton(style);
        button.getImage().setScaling(Scaling.fit);
        button.addListener(new com.badlogic.gdx.scenes.scene2d.ui.TextTooltip(
                tooltip, skin));
        return button;
    }

    private void closeSortModal() {
        if (sortModal == null) return;
        sortModal.remove();
        sortModal = null;
        root.setTouchable(Touchable.enabled);
    }

    @Override public void dispose() {
        navigator.getAccountSession().removeStateListener(sessionListener);
        controller.close();
        super.dispose();
    }
}
