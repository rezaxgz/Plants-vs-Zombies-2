package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.List;

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

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.leaderboard.LeaderBoard;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/** Graphical leaderboard backed by the phase-one leaderboard model. */
public final class LeaderboardScreen extends AbstractScreen {
    private static final String SORT_UP =
            "IMAGE_UI_ALMANAC_SORT_BUTTON_UP";
    private static final String SORT_DOWN =
            "IMAGE_UI_ALMANAC_SORT_BUTTON_DOWN";
    private static final String ASCENDING_UP =
            "IMAGE_UI_ALMANAC_SORT_ASCENDING_UP";
    private static final String ASCENDING_DOWN =
            "IMAGE_UI_ALMANAC_SORT_ASCENDING_DOWN";
    private static final String DESCENDING_UP =
            "IMAGE_UI_ALMANAC_SORT_DESCENDING_UP";
    private static final String DESCENDING_DOWN =
            "IMAGE_UI_ALMANAC_SORT_DESCENDING_DOWN";

    private LeaderBoard.SortColumn sortColumn = LeaderBoard.SortColumn.HIGH_SCORE;
    private boolean ascending;
    private Table sortModal;
    private Label sortSummary;

    public LeaderboardScreen(ScreenNavigator navigator) {
        super(navigator, "Leaderboard");
        addBackButton();
        rebuildLeaderboard();
    }

    private void rebuildLeaderboard() {
        content.clearChildren();

        Table panel = new Table();
        panel.setBackground(skin.get("brown", TextButtonStyle.class).up);
        panel.pad(18f);

        Table toolbar = new Table();
        Label heading = new Label("Global Leaderboard", skin, "big");
        toolbar.add(heading).left().expandX();

        sortSummary = new Label(buildSortSummary(), skin, "secondary");
        toolbar.add(sortSummary).right().padRight(10f);

        ImageButton sortButton = assetImageButton(
                SORT_UP, SORT_DOWN, "Sort leaderboard");
        sortButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showSortModal();
            }
        });
        toolbar.add(sortButton).size(58f);
        panel.add(toolbar).growX().padBottom(10f).row();

        Table table = new Table();
        table.top();
        addHeaderRow(table);

        List<User> users = LeaderBoard.getSortedLeaderboard(sortColumn, ascending);
        if (users.isEmpty()) {
            Label empty = new Label("No registered users were found.", skin,
                    "medium_outline");
            table.add(empty).colspan(8).pad(40f);
        } else {
            for (int index = 0; index < users.size(); index++) {
                addUserRow(table, index + 1, users.get(index));
            }
        }

        ScrollPane scroll = new ScrollPane(table, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setScrollbarsOnTop(false);
        panel.add(scroll).width(1135f).height(455f).row();

        Label footer = new Label(users.size() + " registered player"
                + (users.size() == 1 ? "" : "s"), skin, "secondary");
        panel.add(footer).right().padTop(8f);

        content.add(panel).expand().center().width(1185f).height(555f);
    }

    private void addHeaderRow(Table table) {
        TextButtonStyle headerStyle = skin.get("green", TextButtonStyle.class);
        addHeaderCell(table, "#", 52f, headerStyle);
        addHeaderCell(table, "Username", 190f, headerStyle);
        addHeaderCell(table, "Last Level", 125f, headerStyle);
        addHeaderCell(table, "Minigames", 120f, headerStyle);
        addHeaderCell(table, "Daily", 110f, headerStyle);
        addHeaderCell(table, "Non-Daily", 125f, headerStyle);
        addHeaderCell(table, "Quests", 105f, headerStyle);
        addHeaderCell(table, "High Score", 130f, headerStyle);
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

    private void addUserRow(Table table, int rank, User user) {
        boolean currentUser = App.getInstance().getLoggedInUser() == user
                || App.getInstance().getLoggedInUser() != null
                && App.getInstance().getLoggedInUser().getUsername()
                        .equals(user.getUsername());

        TextButtonStyle rowStyle = skin.get(
                currentUser ? "green" : "brown", TextButtonStyle.class);
        Color textColor = currentUser ? Color.WHITE : Color.LIGHT_GRAY;

        addValueCell(table, Integer.toString(rank), 52f, rowStyle, textColor);
        addValueCell(table, user.getUsername(), 190f, rowStyle, textColor);
        addValueCell(table,
                user.getGameProgerss().getLastCompletedChapter() + "-"
                        + user.getGameProgerss().getLastCompletedLevel(),
                125f, rowStyle, textColor);
        addValueCell(table,
                Integer.toString(user.getGameProgerss().getCompletedMinigames()),
                120f, rowStyle, textColor);
        addValueCell(table,
                Integer.toString(user.getQuestProgress().getCompletedDailyQuests()),
                110f, rowStyle, textColor);
        addValueCell(table,
                Integer.toString(user.getQuestProgress().getCompletedNonDailyQuests()),
                125f, rowStyle, textColor);
        addValueCell(table,
                Integer.toString(user.getQuestProgress().getTotalCompletedQuests()),
                105f, rowStyle, textColor);
        addValueCell(table,
                Integer.toString(user.getGameProgerss().getHighestScore()),
                130f, rowStyle, textColor);
        table.row();
    }

    private void addValueCell(Table table, String text, float width,
            TextButtonStyle style, Color textColor) {
        Table cell = new Table();
        cell.setBackground(style.up);
        Label label = new Label(text, skin, "secondary");
        label.setColor(textColor);
        label.setAlignment(Align.center);
        cell.add(label).growX().pad(4f);
        table.add(cell).width(width).height(46f).pad(2f);
    }

    private void showSortModal() {
        if (sortModal != null) {
            return;
        }

        sortModal = new Table();
        sortModal.setFillParent(true);
        sortModal.setTouchable(Touchable.enabled);

        Table panel = new Table();
        panel.setBackground(skin.get("brown", TextButtonStyle.class).up);
        panel.pad(22f);

        Table titleBar = new Table();
        titleBar.add(new Label("Sort Leaderboard", skin, "big"))
                .left().expandX();
        ImageButton close = new ImageButton(skin, "generic_close_circle");
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeSortModal();
            }
        });
        titleBar.add(close).size(52f);
        panel.add(titleBar).growX().padBottom(10f).row();

        Label helper = new Label(
                "Choose any sorting option available in the phase-one leaderboard.",
                skin, "secondary");
        panel.add(helper).left().padBottom(12f).row();

        Table options = new Table();
        options.defaults().width(240f).height(50f).pad(5f);
        addSortOption(options, "Username", LeaderBoard.SortColumn.USERNAME);
        addSortOption(options, "Last Level", LeaderBoard.SortColumn.LAST_LEVEL);
        options.row();
        addSortOption(options, "Minigames", LeaderBoard.SortColumn.MINIGAMES);
        addSortOption(options, "Daily Quests", LeaderBoard.SortColumn.DAILY_QUESTS);
        options.row();
        addSortOption(options, "Non-Daily Quests",
                LeaderBoard.SortColumn.NON_DAILY_QUESTS);
        addSortOption(options, "All Quests", LeaderBoard.SortColumn.QUESTS);
        options.row();
        addSortOption(options, "High Score", LeaderBoard.SortColumn.HIGH_SCORE);
        panel.add(options).row();

        Table direction = new Table();
        direction.defaults().pad(10f);
        Label directionLabel = new Label("Order", skin, "medium_outline");
        direction.add(directionLabel).padRight(8f);

        ImageButton ascendingButton = assetImageButton(
                ASCENDING_UP, ASCENDING_DOWN, "Ascending");
        ascendingButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ascending = true;
                closeSortModal();
                rebuildLeaderboard();
            }
        });
        direction.add(ascendingButton).size(58f);

        ImageButton descendingButton = assetImageButton(
                DESCENDING_UP, DESCENDING_DOWN, "Descending");
        descendingButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ascending = false;
                closeSortModal();
                rebuildLeaderboard();
            }
        });
        direction.add(descendingButton).size(58f);
        panel.add(direction).padTop(8f).row();

        Label note = new Label(
                "Current: " + buildSortSummary(), skin, "secondary");
        panel.add(note).padTop(6f);

        sortModal.add(panel).width(590f).height(520f);
        root.setTouchable(Touchable.disabled);
        stage.addActor(sortModal);
    }

    private void addSortOption(Table options, String label,
            LeaderBoard.SortColumn column) {
        TextButton button = new TextButton(label, skin,
                sortColumn == column ? "green" : "brown");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sortColumn = column;
                closeSortModal();
                rebuildLeaderboard();
            }
        });
        options.add(button);
    }

    private String buildSortSummary() {
        return displayName(sortColumn) + " - "
                + (ascending ? "Ascending" : "Descending");
    }

    private String displayName(LeaderBoard.SortColumn column) {
        switch (column) {
            case USERNAME:
                return "Username";
            case LAST_LEVEL:
                return "Last Level";
            case MINIGAMES:
                return "Minigames";
            case DAILY_QUESTS:
                return "Daily Quests";
            case NON_DAILY_QUESTS:
                return "Non-Daily Quests";
            case QUESTS:
                return "All Quests";
            case HIGH_SCORE:
            default:
                return "High Score";
        }
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
        if (sortModal == null) {
            return;
        }
        sortModal.remove();
        sortModal = null;
        root.setTouchable(Touchable.enabled);
    }
}
