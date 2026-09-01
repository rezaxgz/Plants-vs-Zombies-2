package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.view.multiplayer.MultiplayerPlantLoadout;

/** PvZ-style eight-plant chooser used before the plant player readies up. */
final class MultiplayerPlantSelectionModal {
    private static final int PLANTS_PER_ROW = 6;
    private static final String SUN_ICON = "IMAGE_EFFECTS_SUN_SUN_78X78";

    private final ScreenNavigator navigator;
    private final MultiplayerPlantLoadout loadout;
    private final Consumer<List<String>> confirmed;
    private final Runnable cancelled;
    private final Group root = new Group();
    private final Table grid = new Table();
    private final Table detail = new Table();
    private final Label count;
    private final Label feedback;
    private final TextButton ready;
    private PlantCollectionItem focused;

    MultiplayerPlantSelectionModal(ScreenNavigator navigator,
            List<PlantCollectionItem> plants,
            Consumer<List<String>> confirmed, Runnable cancelled) {
        if (navigator == null || plants == null || confirmed == null
                || cancelled == null) {
            throw new IllegalArgumentException(
                    "navigator, plants and callbacks are required");
        }
        this.navigator = navigator;
        this.loadout = new MultiplayerPlantLoadout(plants);
        this.confirmed = confirmed;
        this.cancelled = cancelled;
        this.focused = firstUnlocked(plants);

        root.setBounds(0f, 0f, AbstractScreen.VIRTUAL_WIDTH,
                AbstractScreen.VIRTUAL_HEIGHT);
        root.setTouchable(Touchable.enabled);

        Table panel = new Table();
        panel.setBackground(navigator.getSkin().getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"));
        panel.pad(12f);
        panel.setBounds(144f, 92f, 830f, 520f);

        Table heading = new Table();
        heading.add(new Label("CHOOSE YOUR PLANTS", navigator.getSkin(),
                "big_outline")).left().expandX();
        count = new Label("", navigator.getSkin(), "medium_outline");
        heading.add(count).right();
        panel.add(heading).growX().height(42f).row();

        panel.add(detail).growX().height(116f).padTop(4f).row();
        grid.top().left();
        ScrollPane scroll = new ScrollPane(grid, navigator.getSkin());
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        panel.add(scroll).growX().height(302f).padTop(6f).row();

        feedback = new Label("Select exactly eight unlocked plants.",
                navigator.getSkin(), "secondary");
        feedback.setAlignment(Align.center);
        feedback.setWrap(true);
        panel.add(feedback).growX().height(32f).padTop(2f);
        root.addActor(panel);

        ready = new TextButton("READY FOR MATCH", navigator.getSkin(),
                "purple");
        ready.setBounds(988f, 34f, 238f, 58f);
        ready.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!loadout.isComplete()) {
                    setFeedback("Choose all eight plants before readying up.",
                            Color.SCARLET);
                    return;
                }
                List<String> selection = loadout.selectedNames();
                close();
                MultiplayerPlantSelectionModal.this.confirmed.accept(selection);
            }
        });
        root.addActor(ready);

        TextButton back = new TextButton("BACK", navigator.getSkin(), "brown");
        back.setBounds(54f, 34f, 170f, 58f);
        back.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                close();
                MultiplayerPlantSelectionModal.this.cancelled.run();
            }
        });
        root.addActor(back);
        refresh();
    }

    void show(Stage stage) {
        if (stage == null) throw new IllegalArgumentException("stage is required");
        stage.addActor(root);
        root.toFront();
    }

    void close() {
        root.remove();
    }

    private void refresh() {
        count.setText("Selected " + loadout.selectedCount() + "/"
                + MultiplayerPlantLoadout.SLOT_COUNT);
        ready.setDisabled(!loadout.isComplete());
        rebuildGrid();
        rebuildDetail();
    }

    private void rebuildGrid() {
        grid.clearChildren();
        grid.defaults().pad(3f);
        int column = 0;
        for (PlantCollectionItem plant : loadout.plants()) {
            grid.add(choice(plant)).width(116f).height(118f);
            if (++column == PLANTS_PER_ROW) {
                grid.row();
                column = 0;
            }
        }
    }

    private Actor choice(PlantCollectionItem plant) {
        Table wrapper = new Table();
        wrapper.pad(3f);
        boolean selected = loadout.isSelected(plant);
        if (selected) {
            wrapper.setBackground(navigator.getSkin().get(
                    "green", TextButtonStyle.class).up);
        }
        if (!plant.isUnlocked()) wrapper.getColor().a = 0.58f;

        wrapper.add(new PlantPacketCard(navigator, plant))
                .width(PlantPacketCard.WIDTH)
                .height(PlantPacketCard.TOTAL_HEIGHT).row();
        Label state = new Label(selected ? "SELECTED"
                : plant.isUnlocked() ? "" : "LOCKED",
                navigator.getSkin(), "secondary");
        state.setFontScale(0.58f);
        wrapper.add(state).height(18f);
        wrapper.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                focused = plant;
                MultiplayerPlantLoadout.ToggleResult result =
                        loadout.toggle(plant);
                switch (result) {
                    case SELECTED -> setFeedback(
                            plant.getName() + " added to the match loadout.",
                            Color.FOREST);
                    case REMOVED -> setFeedback(
                            plant.getName() + " removed from the loadout.",
                            Color.WHITE);
                    case FULL -> setFeedback(
                            "The multiplayer loadout already has eight plants.",
                            Color.SCARLET);
                    case LOCKED -> setFeedback(
                            "Unlock this plant before selecting it.",
                            Color.SCARLET);
                }
                refresh();
            }
        });
        return wrapper;
    }

    private void rebuildDetail() {
        detail.clearChildren();
        detail.setBackground(navigator.getSkin().get(
                "green", TextButtonStyle.class).up);
        detail.pad(8f, 12f, 8f, 12f);
        if (focused == null) {
            detail.add(new Label("No unlocked plants are available.",
                    navigator.getSkin(), "medium_outline"));
            return;
        }

        detail.add(preview(focused)).width(122f).height(98f).padRight(10f);
        Table info = new Table();
        info.left();
        Label name = new Label(focused.getName(), navigator.getSkin(),
                "medium_outline");
        name.setFontScale(1.05f);
        info.add(name).left().row();
        Table meta = new Table();
        Image sun = new Image(navigator.getTextureBank().region(SUN_ICON));
        sun.setScaling(Scaling.fit);
        meta.add(sun).size(22f).padRight(1f);
        meta.add(new Label(Integer.toString(focused.getCost()),
                navigator.getSkin(), "medium_outline")).padRight(14f);
        meta.add(new Label("Lv " + focused.getCurrentLevel(),
                navigator.getSkin(), "medium_outline"));
        info.add(meta).left().padTop(2f).row();
        Label description = new Label(focused.getBaseAbility(),
                navigator.getSkin(), "secondary");
        description.setWrap(true);
        info.add(description).width(500f).left().padTop(3f);
        detail.add(info).expandX().left();
    }

    private Actor preview(PlantCollectionItem plant) {
        PlantAnimationCatalog.Preview preview =
                PlantAnimationCatalog.find(plant.getName());
        if (preview != null) {
            try {
                return new PamAnimationActor(navigator.getPamPlayer(),
                        preview.getPath(), preview.getClip());
            } catch (RuntimeException ignored) {
                // The packet card remains a complete graphical fallback.
            }
        }
        Image fallback = new Image(navigator.getTextureBank().region(
                PlantPacketCard.packetAssetFor(plant.getName())));
        fallback.setScaling(Scaling.fit);
        return fallback;
    }

    private void setFeedback(String message, Color color) {
        feedback.setText(message);
        feedback.setColor(color);
    }

    private static PlantCollectionItem firstUnlocked(
            List<PlantCollectionItem> plants) {
        for (PlantCollectionItem plant : plants) {
            if (plant != null && plant.isUnlocked()) return plant;
        }
        return null;
    }
}
