package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/**
 * Collection menu. The plants tab follows image 4 of the phase-two handout:
 * a dense grid of seed-packet cards with family/category badges, level/lock
 * state and seed progress directly underneath each packet.
 */
public final class CollectionScreen extends AbstractScreen {
    private static final int CARDS_PER_ROW = 10;

    private final Table tabContent;
    private final TextButton plantsTab;
    private final TextButton zombiesTab;

    public CollectionScreen(ScreenNavigator navigator) {
        super(navigator, "Collection");

        Table screen = new Table();
        screen.top();

        Table tabs = new Table();
        plantsTab = new TextButton("Plants", skin, "green");
        zombiesTab = new TextButton("Zombies", skin, "brown");
        tabs.add(plantsTab).width(150f).height(44f).padRight(6f);
        tabs.add(zombiesTab).width(150f).height(44f);
        tabs.add().expandX();
        screen.add(tabs).growX().left().padBottom(5f).row();

        tabContent = new Table();
        screen.add(tabContent).grow();
        content.add(screen).grow();

        plantsTab.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showPlantsTab();
            }
        });
        zombiesTab.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showZombiePlaceholder();
            }
        });

        addBackButton();
        showPlantsTab();
    }

    private void showPlantsTab() {
        setSelectedTab(true);
        tabContent.clearChildren();

        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            tabContent.add(new Label(
                    "Log in to view the plant collection.",
                    skin, "medium_outline"));
            return;
        }

        List<PlantCollectionItem> plants =
                user.getPlantCollection().getAllPlants();

        Table panel = new Table();
        panel.setBackground(
                skin.get("brown", TextButton.TextButtonStyle.class).up);
        panel.pad(9f, 12f, 6f, 12f);

        Table grid = new Table();
        grid.top().left();
        grid.defaults().width(PlantPacketCard.WIDTH)
                .height(PlantPacketCard.TOTAL_HEIGHT)
                .pad(3f, 4f, 3f, 4f);

        int column = 0;
        for (PlantCollectionItem plant : plants) {
            grid.add(new PlantPacketCard(navigator, plant));
            column++;
            if (column == CARDS_PER_ROW) {
                grid.row();
                column = 0;
            }
        }

        ScrollPane scroll = new ScrollPane(grid, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollbarsVisible(true);
        scroll.setScrollBarPositions(false, true);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        panel.add(scroll).grow().row();

        int unlocked = user.getPlantCollection().getUnlockedPlants().size();
        Label footer = new Label(
                "Plants Collected: " + unlocked + " of " + plants.size(),
                skin, "secondary");
        footer.setColor(Color.WHITE);
        panel.add(footer).right().padTop(3f).padRight(6f);

        // Leave enough room for the tenth packet and the vertical scrollbar.
        // 1210 still fits comfortably inside the 1280-wide virtual viewport.
        tabContent.add(panel).grow().width(1210f).height(530f);
    }

    private void showZombiePlaceholder() {
        setSelectedTab(false);
        tabContent.clearChildren();

        Table panel = new Table();
        panel.setBackground(
                skin.get("brown", TextButton.TextButtonStyle.class).up);
        panel.add(new Label(
                "Zombie collection graphics are unchanged by this patch.",
                skin, "medium_outline")).pad(40f);
        tabContent.add(panel).grow();
    }

    private void setSelectedTab(boolean plantsSelected) {
        plantsTab.setStyle(skin.get(
                plantsSelected ? "green" : "brown",
                TextButton.TextButtonStyle.class));
        zombiesTab.setStyle(skin.get(
                plantsSelected ? "brown" : "green",
                TextButton.TextButtonStyle.class));
    }
}
