package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.controller.CollectionMenuController;
import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.collections.zombies.ZombieCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantCategory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantTag;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/**
 * PvZ2-style collection screen. Plant cards open an Almanac-like detail modal
 * and the footer exposes collection filtering without leaving the screen.
 */
public final class CollectionScreen extends AbstractScreen {
    private static final int CARDS_PER_ROW = 10;
    // private static final String DETAIL_BACKGROUND =
    //         "IMAGE_UI_ALMANAC_GENERIC_RIFT_IMAGE";
    private static final String FILTER_UP =
            "IMAGE_UI_ALMANAC_FILTER_BUTTON_UP";
    private static final String FILTER_DOWN =
            "IMAGE_UI_ALMANAC_FILTER_BUTTON_DOWN";

    private enum PlantFilter {
        ALL,
        UNLOCKED,
        LOCKED,
        UPGRADABLE,
        CATEGORY
    }

    private final Table tabContent;
    private final TextButton plantsTab;
    private final TextButton zombiesTab;

    private PlantFilter activeFilter = PlantFilter.ALL;
    private PlantCategory activeCategory;
    private Table activeModal;

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
                showZombiesTab();
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

        List<PlantCollectionItem> allPlants =
                user.getPlantCollection().getAllPlants();
        List<PlantCollectionItem> plants = filterPlants(allPlants, user);

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
            PlantPacketCard card = new PlantPacketCard(navigator, plant);
            card.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showPlantDetailModal(plant, null);
                }
            });
            grid.add(card);
            column++;
            if (column == CARDS_PER_ROW) {
                grid.row();
                column = 0;
            }
        }
        if (plants.isEmpty()) {
            grid.add(new Label(
                    "No plants match this filter.",
                    skin, "medium_outline")).pad(40f);
        }

        ScrollPane scroll = new ScrollPane(grid, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollbarsVisible(true);
        scroll.setScrollBarPositions(false, true);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        panel.add(scroll).grow().row();

        Table footerRow = new Table();
        ImageButton filterButton = createAssetImageButton(FILTER_UP, FILTER_DOWN);
        filterButton.addListener(new TextTooltip("Filter plants", skin));
        filterButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showFilterModal();
            }
        });
        footerRow.add(filterButton).size(42f).left().padRight(6f);

        Label filterLabel = new Label(
                filterDisplayName(), skin, "medium_outline");
        filterLabel.setColor(Color.WHITE);
        filterLabel.setFontScale(0.72f);
        footerRow.add(filterLabel).left();
        footerRow.add().expandX();

        int unlocked = user.getPlantCollection().getUnlockedPlants().size();
        Label footer = new Label(
                "Plants Collected: " + unlocked + " of " + allPlants.size(),
                skin, "medium_outline");
        footer.setColor(Color.WHITE);
        footer.setFontScale(0.72f);
        footerRow.add(footer).right().padRight(6f);

        panel.add(footerRow).growX().padTop(3f);

        tabContent.add(panel).grow().width(1210f).height(530f);
    }

    private List<PlantCollectionItem> filterPlants(
            List<PlantCollectionItem> plants, User user) {
        List<PlantCollectionItem> filtered = new ArrayList<>();
        for (PlantCollectionItem plant : plants) {
            if (matchesActiveFilter(plant, user)) {
                filtered.add(plant);
            }
        }
        return filtered;
    }

    private boolean matchesActiveFilter(PlantCollectionItem plant, User user) {
        switch (activeFilter) {
            case UNLOCKED:
                return plant.isUnlocked();
            case LOCKED:
                return !plant.isUnlocked();
            case UPGRADABLE:
                return plant.isUnlocked()
                        && !plant.isAtMaximumLevel()
                        && plant.getTotalCardsCollected()
                                >= plant.getCardsNeededForNextLevel()
                        && user != null
                        && user.getCoins() >= plant.getCoinsNeededForNextLevel();
            case CATEGORY:
                return activeCategory != null
                        && plant.getCategory() == activeCategory;
            case ALL:
            default:
                return true;
        }
    }

    private String filterDisplayName() {
        switch (activeFilter) {
            case UNLOCKED:
                return "Unlocked Plants";
            case LOCKED:
                return "Locked Plants";
            case UPGRADABLE:
                return "Upgradable Plants";
            case CATEGORY:
                return activeCategory == null
                        ? "All Plants"
                        : PlantPacketCard.prettyCategory(activeCategory);
            case ALL:
            default:
                return "All Plants";
        }
    }

    private void showPlantDetailModal(PlantCollectionItem plant,
            CommandResult result) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null || plant == null) {
            return;
        }

        Table frame = new Table();
        frame.setBackground(
                skin.get("brown", TextButton.TextButtonStyle.class).up);
        frame.pad(8f);

        Stack bodyStack = new Stack();
        // Image blueBackground = createAssetImage(DETAIL_BACKGROUND);
        // blueBackground.setScaling(Scaling.fill);
        // blueBackground.setColor(0.48f, 0.68f, 1f, 0.95f);
        // bodyStack.add(blueBackground);

        Table body = new Table();
        body.pad(16f, 22f, 16f, 22f);

        Table header = new Table();
        header.add().width(58f);
        Label title = new Label(plant.getName(), skin, "big");
        title.setColor(Color.WHITE);
        header.add(title).expandX().center();
        ImageButton close = new ImageButton(skin, "generic_close_circle");
        close.addListener(new TextTooltip("Close", skin));
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeActiveModal();
            }
        });
        header.add(close).size(58f).right();
        body.add(header).growX().colspan(2).row();

        Table left = buildPlantPreviewColumn(plant, user);
        Table right = buildPlantInformationColumn(plant);
        body.add(left).width(390f).height(445f).top().padRight(20f);
        body.add(right).width(555f).height(445f).top().row();

        Label status = new Label(
                result == null ? "" : result.getMessage(),
                skin, "medium_outline");
        status.setColor(result == null || result.isSuccsesful()
                ? Color.GREEN : Color.RED);
        status.setWrap(true);
        body.add(status).colspan(2).width(900f).height(35f)
                .center().padTop(2f);

        bodyStack.add(body);
        frame.add(bodyStack).grow();
        openModal(frame, 1040f, 610f);
    }

    private Table buildPlantPreviewColumn(PlantCollectionItem plant,
            User user) {
        Table left = new Table();
        left.top();

        Stack preview = new Stack();
        // Image previewBackground = createAssetImage(DETAIL_BACKGROUND);
        // previewBackground.setScaling(Scaling.fill);
        // previewBackground.setColor(0.55f, 0.65f, 0.72f, 0.95f);
        // preview.add(previewBackground);

        Actor animation = createPlantPreviewActor(plant);
        if (!plant.isUnlocked()) {
            animation.setColor(0.62f, 0.62f, 0.62f, 0.72f);
        }
        Table animationLayer = new Table();
        animationLayer.add(animation).size(300f, 280f);
        preview.add(animationLayer);

        left.add(preview).width(340f).height(305f).row();

        Label level = new Label(
                plant.isUnlocked()
                        ? "Level " + plant.getCurrentLevel()
                        : "Locked",
                skin, "medium_outline");
        level.setColor(Color.WHITE);
        left.add(level).padTop(4f).row();

        left.add(PlantPacketCard.createSeedProgress(skin, plant, 0.7f))
                .width(310f).height(26f).padTop(2f).row();

        TextButton actionButton = createPlantActionButton(plant, user);
        left.add(actionButton).width(265f).height(50f).padTop(9f);
        return left;
    }

    private Actor createPlantPreviewActor(PlantCollectionItem plant) {
        PlantAnimationCatalog.Preview preview =
                PlantAnimationCatalog.find(plant.getName());
        if (preview != null) {
            try {
                return new PamAnimationActor(
                        navigator.getPamPlayer(),
                        preview.getPath(), preview.getClip());
            } catch (RuntimeException ignored) {
                // Some extracted asset packs omit optional PAM groups. Keep
                // the modal usable by falling back to the plant packet art.
            }
        }
        Image fallback = createAssetImage(
                PlantPacketCard.packetAssetFor(plant.getName()));
        fallback.setScaling(Scaling.fit);
        return fallback;
    }

    private TextButton createPlantActionButton(PlantCollectionItem plant,
            User user) {
        String text;
        if (!plant.isUnlocked()) {
            text = "Buy - " + PlantCollectionItem.PLANT_PRICE_IN_COINS
                    + " Coins";
        } else if (plant.isAtMaximumLevel()) {
            text = "Maximum Level";
        } else {
            text = "Upgrade - " + plant.getCoinsNeededForNextLevel()
                    + " Coins";
        }

        TextButton button = new TextButton(text, skin, "green");
        if (plant.isAtMaximumLevel()) {
            button.setDisabled(true);
            button.setTouchable(Touchable.disabled);
            button.getColor().a = 0.55f;
            return button;
        }

        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                CommandResult result = plant.isUnlocked()
                        ? CollectionMenuController.upgradePlant(user, plant)
                        : CollectionMenuController.purchasePlant(user, plant);
                refreshAfterPlantMutation(plant, result);
            }
        });
        return button;
    }

    private void refreshAfterPlantMutation(PlantCollectionItem plant,
            CommandResult result) {
        if (activeModal != null) {
            activeModal.remove();
            activeModal = null;
        }
        showPlantsTab();
        showPlantDetailModal(plant, result);
    }

    private Table buildPlantInformationColumn(PlantCollectionItem plant) {
        Table right = new Table();
        right.top().left();
        right.defaults().pad(5f);

        Table stats = new Table();
        stats.defaults().width(260f).height(62f).pad(5f);
        stats.add(createStat(
                "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_SUNCOST",
                "SUN COST", Integer.toString(plant.getCost())));
        stats.add(createStat(
                "IMAGE_UI_ALMANAC_PLANTS_RECHARGE_ICON",
                "RECHARGE", formatSeconds(plant.getRechargeSeconds()))).row();
        stats.add(createStat(
                "IMAGE_UI_ALMANAC_PLANTS_TOUGHNESS_ICON",
                "TOUGHNESS", Integer.toString(plant.getBaseHP())));
        stats.add(createStat(
                "IMAGE_UI_ALMANAC_PLANTS_DAMAGE_ICON",
                "DAMAGE", Integer.toString(plant.getDamage()))).row();
        stats.add(createStat(
                "IMAGE_UI_ALMANAC_PLANTS_RANGE_ICON",
                "ACTION", formatActionInterval(plant)));
        stats.add(createStat(
                "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_SPECIAL",
                "SPECIAL", shortAbility(plant.getBaseAbility()))).row();
        right.add(stats).growX().row();

        Table family = new Table();
        Image familyIcon = createAssetImage(
                "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_FAMILY");
        familyIcon.setScaling(Scaling.fit);
        family.add(familyIcon).size(42f).padRight(8f);
        Label familyLabel = new Label(
                PlantPacketCard.prettyCategory(plant.getCategory()),
                skin, "medium_outline");
        familyLabel.setColor(Color.WHITE);
        family.add(familyLabel).left();
        family.add().width(20f);
        Label tags = new Label(formatTags(plant.getTags()),
                skin, "secondary");
        tags.setColor(Color.WHITE);
        tags.setWrap(true);
        family.add(tags).width(275f).left();
        right.add(family).growX().left().padTop(3f).row();

        Table descriptions = new Table();
        descriptions.top().left();
        descriptions.defaults().growX().left().pad(3f);
        descriptions.add(wrappedDetail(
                "Ability: " + plant.getBaseAbility(), 505f)).row();
        descriptions.add(wrappedDetail(
                "Plant Food: " + plant.getPlantFoodEffect(), 505f)).row();
        descriptions.add(wrappedDetail(
                upgradeSummary(plant), 505f)).row();

        ScrollPane detailsScroll = new ScrollPane(descriptions, skin);
        detailsScroll.setFadeScrollBars(false);
        detailsScroll.setScrollingDisabled(true, false);
        detailsScroll.setOverscroll(false, false);
        right.add(detailsScroll).width(525f).height(150f).padTop(4f);
        return right;
    }

    private Table createStat(String iconId, String heading, String value) {
        Table stat = new Table();
        stat.setBackground(
                skin.get("green", TextButton.TextButtonStyle.class).up);
        stat.pad(5f, 8f, 5f, 8f);

        Image icon = createAssetImage(iconId);
        icon.setScaling(Scaling.fit);
        stat.add(icon).size(42f).padRight(7f);

        Table labels = new Table();
        Label title = new Label(heading, skin, "secondary");
        title.setColor(Color.WHITE);
        Label amount = new Label(value, skin, "medium_outline");
        amount.setColor(Color.WHITE);
        amount.setFontScale(0.72f);
        labels.add(title).left().row();
        labels.add(amount).left();
        stat.add(labels).growX().left();
        return stat;
    }

    private Label wrappedDetail(String text, float width) {
        Label label = new Label(text, skin, "secondary");
        label.setColor(Color.WHITE);
        label.setWrap(true);
        label.setWidth(width);
        return label;
    }

    private void showFilterModal() {
        final PlantFilter[] pendingFilter = {activeFilter};
        final PlantCategory[] pendingCategory = {activeCategory};
        final List<TextButton> options = new ArrayList<>();

        Table frame = new Table();
        frame.setBackground(
                skin.get("brown", TextButton.TextButtonStyle.class).up);
        frame.pad(8f);

        Stack bodyStack = new Stack();
        // Image background = createAssetImage(DETAIL_BACKGROUND);
        // background.setScaling(Scaling.fill);
        // background.setColor(0.48f, 0.68f, 1f, 0.96f);
        // bodyStack.add(background);

        Table panel = new Table();
        panel.pad(18f);

        Table header = new Table();
        header.add().width(58f);
        Label title = new Label("Filter Plants", skin, "big");
        title.setColor(Color.WHITE);
        header.add(title).expandX().center();
        ImageButton close = new ImageButton(skin, "generic_close_circle");
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeActiveModal();
            }
        });
        header.add(close).size(58f);
        panel.add(header).growX().colspan(3).row();

        Label statusHeading = new Label("Collection", skin, "medium_outline");
        statusHeading.setColor(Color.WHITE);
        panel.add(statusHeading).colspan(3).padBottom(6f).row();

        TextButton all = addFilterOption(panel, options, "All Plants", 0, 0,
                () -> {
                    pendingFilter[0] = PlantFilter.ALL;
                    pendingCategory[0] = null;
                });
        TextButton unlocked = addFilterOption(panel, options,
                "Unlocked Plants", 0, 1,
                () -> {
                    pendingFilter[0] = PlantFilter.UNLOCKED;
                    pendingCategory[0] = null;
                });
        TextButton locked = addFilterOption(panel, options,
                "Locked Plants", 0, 2,
                () -> {
                    pendingFilter[0] = PlantFilter.LOCKED;
                    pendingCategory[0] = null;
                });
        panel.row();
        TextButton upgradable = addFilterOption(panel, options,
                "Upgradable Plants", 1, 0,
                () -> {
                    pendingFilter[0] = PlantFilter.UPGRADABLE;
                    pendingCategory[0] = null;
                });
        panel.add().colspan(2);
        panel.row();

        Label categoryHeading = new Label("Plant Category",
                skin, "medium_outline");
        categoryHeading.setColor(Color.WHITE);
        panel.add(categoryHeading).colspan(3).padTop(12f)
                .padBottom(5f).row();

        TextButton activeOption = null;
        if (activeFilter == PlantFilter.ALL) {
            activeOption = all;
        } else if (activeFilter == PlantFilter.UNLOCKED) {
            activeOption = unlocked;
        } else if (activeFilter == PlantFilter.LOCKED) {
            activeOption = locked;
        } else if (activeFilter == PlantFilter.UPGRADABLE) {
            activeOption = upgradable;
        }

        int categoryColumn = 0;
        for (PlantCategory category : PlantCategory.values()) {
            TextButton categoryButton = new TextButton(
                    PlantPacketCard.prettyCategory(category),
                    skin, "brown");
            options.add(categoryButton);
            categoryButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    pendingFilter[0] = PlantFilter.CATEGORY;
                    pendingCategory[0] = category;
                    selectFilterButton(categoryButton, options);
                }
            });
            panel.add(categoryButton).width(235f).height(44f).pad(4f);
            if (activeFilter == PlantFilter.CATEGORY
                    && activeCategory == category) {
                activeOption = categoryButton;
            }
            categoryColumn++;
            if (categoryColumn == 3) {
                panel.row();
                categoryColumn = 0;
            }
        }
        if (categoryColumn != 0) {
            panel.row();
        }

        if (activeOption != null) {
            selectFilterButton(activeOption, options);
        }

        TextButton apply = new TextButton("Apply Filter", skin, "green");
        apply.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activeFilter = pendingFilter[0];
                activeCategory = pendingCategory[0];
                closeActiveModal();
                showPlantsTab();
            }
        });
        panel.add(apply).colspan(3).width(260f).height(52f).padTop(12f);

        bodyStack.add(panel);
        frame.add(bodyStack).grow();
        openModal(frame, 820f, 610f);
    }

    private TextButton addFilterOption(Table panel,
            List<TextButton> options, String label,
            int ignoredRow, int ignoredColumn, Runnable selection) {
        TextButton button = new TextButton(label, skin, "brown");
        options.add(button);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selection.run();
                selectFilterButton(button, options);
            }
        });
        panel.add(button).width(235f).height(44f).pad(4f);
        return button;
    }

    private void selectFilterButton(TextButton selected,
            List<TextButton> options) {
        for (TextButton option : options) {
            option.setStyle(skin.get(
                    option == selected ? "green" : "brown",
                    TextButton.TextButtonStyle.class));
        }
    }

    private ImageButton createAssetImageButton(String upId, String downId) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(requireAssetRegion(upId));
        style.imageDown = new TextureRegionDrawable(requireAssetRegion(downId));
        return new ImageButton(style);
    }

    private void openModal(Table panel, float width, float height) {
        if (activeModal != null) {
            return;
        }
        activeModal = new Table();
        activeModal.setFillParent(true);
        activeModal.setTouchable(Touchable.enabled);
        activeModal.add(panel).width(width).height(height);
        root.setTouchable(Touchable.disabled);
        stage.addActor(activeModal);
    }

    private void closeActiveModal() {
        if (activeModal == null) {
            return;
        }
        activeModal.remove();
        activeModal = null;
        root.setTouchable(Touchable.enabled);
    }

    private void showZombiesTab() {
        setSelectedTab(false);
        tabContent.clearChildren();

        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            tabContent.add(new Label(
                    "Log in to view the zombie collection.",
                    skin, "medium_outline"));
            return;
        }

        List<ZombieCollectionItem> zombies =
                user.getZombieCollection().getAllZombies();

        Table panel = new Table();
        panel.setBackground(
                skin.get("brown", TextButton.TextButtonStyle.class).up);
        panel.pad(12f, 18f, 8f, 18f);

        Table grid = new Table();
        grid.top();
        grid.defaults().width(ZombiePacketCard.WIDTH)
                .height(ZombiePacketCard.HEIGHT)
                .pad(8f, 13f, 8f, 13f);

        int column = 0;
        int discovered = 0;
        final int cardsPerRow = 9;
        for (ZombieCollectionItem zombie : zombies) {
            ZombiePacketCard card = new ZombiePacketCard(navigator, zombie);
            if (zombie.isUnlocked()) {
                discovered++;
                card.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        showZombieDetailModal(zombie);
                    }
                });
            }
            grid.add(card);
            column++;
            if (column == cardsPerRow) {
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

        Label footer = new Label(
                "Zombies Discovered: " + discovered + " of " + zombies.size(),
                skin, "medium_outline");
        footer.setColor(Color.WHITE);
        footer.setFontScale(0.72f);
        panel.add(footer).right().padTop(4f).padRight(8f);

        tabContent.add(panel).grow().width(1210f).height(530f);
    }

    private void showZombieDetailModal(ZombieCollectionItem zombie) {
        if (zombie == null || !zombie.isUnlocked()) {
            return;
        }

        Table frame = new Table();
        frame.setBackground(
                skin.get("brown", TextButton.TextButtonStyle.class).up);
        frame.pad(8f);

        Stack bodyStack = new Stack();
        // Image background = createAssetImage(DETAIL_BACKGROUND);
        // background.setScaling(Scaling.fill);
        // background.setColor(0.43f, 0.62f, 1f, 0.97f);
        // bodyStack.add(background);

        Table body = new Table();
        body.pad(18f, 24f, 18f, 24f);

        Table header = new Table();
        header.add().width(58f);
        Label title = new Label(
                ZombiePacketCard.prettyZombieName(zombie), skin, "big");
        title.setColor(Color.WHITE);
        header.add(title).expandX().center();
        ImageButton close = new ImageButton(skin, "generic_close_circle");
        close.addListener(new TextTooltip("Close", skin));
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeActiveModal();
            }
        });
        header.add(close).size(58f).right();
        body.add(header).growX().colspan(2).row();

        Table previewColumn = buildZombiePreviewColumn(zombie);
        Table detailsColumn = buildZombieInformationColumn(zombie);
        body.add(previewColumn).width(385f).height(455f)
                .top().padRight(26f);
        body.add(detailsColumn).width(560f).height(455f).top();

        bodyStack.add(body);
        frame.add(bodyStack).grow();
        openModal(frame, 1040f, 610f);
    }

    private Table buildZombiePreviewColumn(ZombieCollectionItem zombie) {
        Table left = new Table();
        left.top();

        Stack preview = new Stack();
        // Image previewBackground = createAssetImage(DETAIL_BACKGROUND);
        // previewBackground.setScaling(Scaling.fill);
        // previewBackground.setColor(0.42f, 0.64f, 0.34f, 0.95f);
        // preview.add(previewBackground);

        Actor animation = createZombiePreviewActor(zombie);
        Table animationLayer = new Table();
        float animationSize = zombie.isLarge() ? 330f : 285f;
        animationLayer.add(animation).size(animationSize, animationSize);
        preview.add(animationLayer);

        left.add(preview).width(350f).height(380f).row();

        Label alias = new Label(zombie.getName(), skin, "secondary");
        alias.setColor(Color.WHITE);
        alias.setFontScale(0.78f);
        left.add(alias).padTop(8f);
        return left;
    }

    private Actor createZombiePreviewActor(ZombieCollectionItem zombie) {
        ZombieVisualCatalog.Visual visual = ZombieVisualCatalog.find(zombie);
        if (visual != null) {
            try {
                return new PamAnimationActor(
                        navigator.getPamPlayer(),
                        visual.getPamPath(), visual.getIdleClip());
            } catch (RuntimeException ignored) {
                // Keep Almanac details usable even if a locally extracted
                // asset pack happens to omit one optional PAM group.
            }
        }

        Image fallback = createAssetImage(
                ZombieVisualCatalog.packetAssetFor(zombie));
        fallback.setScaling(Scaling.fit);
        return fallback;
    }

    private Table buildZombieInformationColumn(ZombieCollectionItem zombie) {
        Table right = new Table();
        right.top().left();

        Table primaryStats = new Table();
        primaryStats.defaults().width(255f).height(72f).pad(6f);
        primaryStats.add(createZombieStat(
                "IMAGE_UI_ALMANAC_ZOMBIES_ZOMBIETOUGHNESS_ICON",
                "TOUGHNESS", zombieToughnessText(zombie)));
        primaryStats.add(createZombieStat(
                "IMAGE_UI_ALMANAC_ZOMBIES_ZOMBIESPEED_ICON",
                "SPEED", zombieSpeedText(zombie)));
        right.add(primaryStats).growX().row();

        Table facts = new Table();
        facts.top().left();
        facts.defaults().growX().left().pad(4f);
        facts.add(zombieFact("Health", Integer.toString(zombie.getHitpoints())))
                .row();

        String armor = zombie.getDefaultArmor().getDisplayName();
        if (zombie.getDefaultArmor().getBaseHealth() > 0) {
            armor += " (" + zombie.getDefaultArmor().getBaseHealth()
                    + " armor HP)";
        }
        facts.add(zombieFact("Armor", armor)).row();
        facts.add(zombieFact("Eat DPS", Integer.toString(zombie.getEatDPS())))
                .row();
        facts.add(zombieFact("Wave Point Cost",
                Integer.toString(zombie.getWavePointCost()))).row();
        facts.add(zombieFact("Weight", Integer.toString(zombie.getWeight())))
                .row();
        facts.add(zombieFact("Size",
                zombie.isBoss() ? "Boss"
                        : zombie.isLarge() ? "Large" : "Normal")).row();

        Label abilitiesHeading = new Label("ABILITIES", skin, "medium_outline");
        abilitiesHeading.setColor(Color.GOLD);
        facts.add(abilitiesHeading).padTop(12f).row();
        facts.add(wrappedDetail(formatZombieAbilities(zombie), 505f)).row();

        Label discovered = new Label(
                "Encountered - full Almanac entry unlocked",
                skin, "secondary");
        discovered.setColor(Color.WHITE);
        facts.add(discovered).padTop(10f).row();

        ScrollPane scroll = new ScrollPane(facts, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        right.add(scroll).width(535f).height(340f).padTop(10f);
        return right;
    }

    private Table createZombieStat(String iconId,
            String heading, String value) {
        Table stat = new Table();
        stat.setBackground(new TextureRegionDrawable(requireAssetRegion(
                "IMAGE_UI_ALMANAC_ALMANAC_STAT_BACKGROUND")));
        stat.pad(7f, 9f, 7f, 9f);

        Image icon = createAssetImage(iconId);
        icon.setScaling(Scaling.fit);
        stat.add(icon).size(50f).padRight(8f);

        Table labels = new Table();
        Label title = new Label(heading, skin, "secondary");
        title.setColor(Color.WHITE);
        Label amount = new Label(value, skin, "medium_outline");
        amount.setColor(Color.WHITE);
        amount.setFontScale(0.72f);
        labels.add(title).left().row();
        labels.add(amount).left();
        stat.add(labels).growX().left();
        return stat;
    }

    private Table zombieFact(String heading, String value) {
        Table fact = new Table();
        Label title = new Label(heading + ":", skin, "medium_outline");
        title.setColor(Color.GOLD);
        title.setFontScale(0.72f);
        fact.add(title).width(145f).left();

        Label amount = new Label(value, skin, "medium_outline");
        amount.setColor(Color.WHITE);
        amount.setFontScale(0.72f);
        fact.add(amount).growX().left();
        return fact;
    }

    private static String zombieToughnessText(ZombieCollectionItem zombie) {
        int total = zombie.getHitpoints()
                + zombie.getDefaultArmor().getBaseHealth();
        if (zombie.isBoss()) {
            return "Boss - " + total + " HP";
        }
        if (total >= 3000) {
            return "Formidable - " + total + " HP";
        }
        if (total >= 1200) {
            return "Tough - " + total + " HP";
        }
        if (zombie.getDefaultArmor().getBaseHealth() > 0) {
            return "Protected - " + total + " HP";
        }
        if (total >= 450) {
            return "Hardy - " + total + " HP";
        }
        return "Basic - " + total + " HP";
    }

    private static String zombieSpeedText(ZombieCollectionItem zombie) {
        double speed = zombie.getSpeed();
        String label;
        if (speed >= 0.35) {
            label = "Very Fast";
        } else if (speed >= 0.25) {
            label = "Fast";
        } else if (speed >= 0.16) {
            label = "Basic";
        } else {
            label = "Slow";
        }
        return label + " - "
                + String.format(Locale.ROOT, "%.3f", speed);
    }

    private static String formatZombieAbilities(ZombieCollectionItem zombie) {
        List<String> abilities = zombie.getAbilities();
        if (abilities == null || abilities.isEmpty()) {
            return "No special ability.";
        }
        StringBuilder result = new StringBuilder();
        for (String ability : abilities) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append("- ").append(formatAbilitySpec(ability));
        }
        return result.toString();
    }

    private static String formatAbilitySpec(String ability) {
        if (ability == null || ability.isBlank()) {
            return "Unknown ability";
        }
        String[] parts = ability.split(":");
        StringBuilder name = new StringBuilder();
        String rawName = parts[0].replace("Ability", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2");
        name.append(rawName);
        if (parts.length > 1) {
            name.append(" (");
            for (int index = 1; index < parts.length; index++) {
                if (index > 1) {
                    name.append(", ");
                }
                name.append(parts[index]);
            }
            name.append(')');
        }
        return name.toString();
    }

    private void setSelectedTab(boolean plantsSelected) {
        plantsTab.setStyle(skin.get(
                plantsSelected ? "green" : "brown",
                TextButton.TextButtonStyle.class));
        zombiesTab.setStyle(skin.get(
                plantsSelected ? "brown" : "green",
                TextButton.TextButtonStyle.class));
    }

    private static String formatSeconds(float seconds) {
        return String.format(Locale.ROOT, "%.1f s", seconds);
    }

    private static String formatActionInterval(PlantCollectionItem plant) {
        float interval = plant.getActionIntervalSeconds();
        if (Float.isNaN(interval)) {
            return "Passive";
        }
        return formatSeconds(interval);
    }

    private static String shortAbility(String ability) {
        if (ability == null || ability.isBlank()) {
            return "-";
        }
        String trimmed = ability.trim();
        return trimmed.length() <= 22
                ? trimmed
                : trimmed.substring(0, 20) + "...";
    }

    private static String formatTags(List<PlantTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return "No tags";
        }
        StringBuilder result = new StringBuilder();
        for (PlantTag tag : tags) {
            if (result.length() > 0) {
                result.append("  |  ");
            }
            result.append(prettyEnum(tag.name()));
        }
        return result.toString();
    }

    private static String upgradeSummary(PlantCollectionItem plant) {
        if (plant.isAtMaximumLevel()) {
            return "Upgrade: Maximum level reached.";
        }
        String nextUpgrade;
        if (plant.getCurrentLevel() == 1) {
            nextUpgrade = plant.getLevelTwoUpgrade();
        } else if (plant.getCurrentLevel() == 2) {
            nextUpgrade = plant.getLevelThreeUpgrade();
        } else {
            nextUpgrade = plant.getLevelFourUpgrade();
        }
        return "Next level: " + plant.getCardsNeededForNextLevel()
                + " seed packets + " + plant.getCoinsNeededForNextLevel()
                + " coins.  " + nextUpgrade;
    }

    private static String prettyEnum(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String raw = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder result = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }
        return result.toString();
    }
}
