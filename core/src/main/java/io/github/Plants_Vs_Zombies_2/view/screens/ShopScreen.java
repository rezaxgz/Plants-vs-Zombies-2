package io.github.Plants_Vs_Zombies_2.view.screens;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.controller.ShopMenuController;
import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.CommandResult;
import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.enums.CurrencyType;
import io.github.Plants_Vs_Zombies_2.model.shop.Shop;
import io.github.Plants_Vs_Zombies_2.model.shop.item.ItemType;
import io.github.Plants_Vs_Zombies_2.model.shop.item.ShopItem;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/**
 * PvZ2-style graphical shop. Permanent goods and the daily offer intentionally
 * share one shelf, matching the phase-two specification.
 */
public final class ShopScreen extends AbstractScreen {
    private static final String STORE_PANEL = "IMAGE_UI_STORE_MINISTORE_BG";
    private static final String STORE_CLOSE = "IMAGE_UI_STORE_CLOSE_BTN";
    private static final String STORE_CLOSE_DOWN = "IMAGE_UI_STORE_CLOSE_DOWN";
    private static final String PROMO_RIBBON =
            "IMAGE_UI_CARDS_STORE_PROMO_RIBBON_SMALL";

    private static final String SEED_PACKET_ICON =
            "IMAGE_UI_STOREMULTI_SEEDPACKETICON";
    private static final String PLANT_FOOD_ICON =
            "IMAGE_EFFECTS_PLANTFOOD_PICKUP_PLANTFOOD_PICKUP_146X146";
    private static final String POT_ICON =
            "IMAGE_ZEN_GARDEN_ZEN_POT_WATER_ZEN_POT_WATER_160X97";
    private static final String COIN_ICON =
            "IMAGE_UI_THYMED_EVENTS_ECS_CONVRT_COIN";

    private static final float CARD_WIDTH = 184f;
    private static final float CARD_HEIGHT = 388f;

    private final Label feedbackLabel;
    private final Table shelfHolder;
    private Table modalOverlay;

    public ShopScreen(ScreenNavigator navigator) {
        super(navigator, "Shop");

        User user = App.getInstance().getLoggedInUser();
        ShopMenuController.prepareShop(user);

        Table store = new Table();
        store.setBackground(requireAssetDrawable(STORE_PANEL));
        store.pad(18f, 20f, 18f, 20f);

        Table top = new Table();
        Label title = new Label("STORE", skin, "big");
        top.add(title).expandX().center().padLeft(70f);
        ImageButton close = assetImageButton(
                STORE_CLOSE, STORE_CLOSE_DOWN, "Close Store");
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.back();
            }
        });
        top.add(close).size(54f).right();
        store.add(top).growX().row();

        Label subtitle = new Label(
                "Plants and upgrades for your current profile",
                skin, "secondary");
        store.add(subtitle).center().padBottom(6f).row();

        shelfHolder = new Table();
        store.add(shelfHolder).growX().height(410f).row();

        feedbackLabel = new Label("", skin, "medium_outline");
        feedbackLabel.setWrap(true);
        store.add(feedbackLabel).growX().height(42f).padTop(4f).row();

        content.add(store).expand().center().width(1180f).height(555f);
        rebuildShelf();
    }

    private void rebuildShelf() {
        shelfHolder.clearChildren();

        User user = App.getInstance().getLoggedInUser();
        ShopMenuController.prepareShop(user);

        Table row = new Table();
        row.defaults().pad(5f);
        for (ShopItem item : Shop.PERMANENT_ITEMS) {
            row.add(createPermanentCard(item)).size(CARD_WIDTH, CARD_HEIGHT);
        }
        row.add(createDailyOfferCard()).size(CARD_WIDTH, CARD_HEIGHT);

        ScrollPane scroll = new ScrollPane(row, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(false, true);
        scroll.setOverscroll(false, false);
        scroll.setScrollbarsOnTop(false);
        shelfHolder.add(scroll).grow();
    }

    private Table createPermanentCard(ShopItem item) {
        Table card = createCardShell(cardBackgroundFor(item));

        Label name = new Label(item.getName(), skin, "medium_outline");
        name.setWrap(true);
        card.add(name).width(154f).height(44f).center().row();

        Image icon = createAssetImage(itemImageFor(item));
        icon.setScaling(Scaling.fit);
        card.add(icon).size(128f, 120f).padTop(4f).row();

        Label description = new Label(itemDescription(item), skin, "secondary");
        description.setWrap(true);
        card.add(description).width(150f).height(76f).top().padTop(4f).row();

        Label status = new Label(itemStatus(item), skin, "secondary");
        status.setWrap(true);
        card.add(status).width(150f).height(38f).center().row();

        Label price = new Label(formatPrice(item), skin, "medium_outline");
        card.add(price).padTop(2f).row();

        TextButton buy = new TextButton("BUY", skin, "green");
        buy.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                beginPurchase(item, null);
            }
        });
        card.add(buy).width(118f).height(42f).padTop(4f);
        return card;
    }

    private Table createDailyOfferCard() {
        User user = App.getInstance().getLoggedInUser();
        Table card = createCardShell(
                "IMAGE_UI_CARDS_STORE_STORE_CARD_YELLOW");

        Stack ribbon = new Stack();
        Image ribbonImage = createAssetImage(PROMO_RIBBON);
        ribbonImage.setScaling(Scaling.stretch);
        ribbon.add(ribbonImage);
        Table ribbonText = new Table();
        ribbonText.add(new Label("DAILY OFFER", skin, "medium_outline"));
        ribbon.add(ribbonText);
        card.add(ribbon).width(166f).height(44f).row();

        String plantName = user == null ? "" : user.getDailyOfferPlant();
        Label target = new Label(
                plantName == null || plantName.isBlank()
                        ? "No offer"
                        : plantName,
                skin, "medium_outline");
        target.setWrap(true);
        card.add(target).width(154f).height(42f).center().row();

        Image packet = createAssetImage(
                plantName == null || plantName.isBlank()
                        ? SEED_PACKET_ICON
                        : PlantPacketCard.packetAssetFor(plantName));
        packet.setScaling(Scaling.fit);
        card.add(packet).size(124f, 112f).padTop(2f).row();

        Label amount = new Label(
                ShopMenuController.getDailyOfferSeeds()
                        + " seed packets\n20% daily discount",
                skin, "secondary");
        amount.setWrap(true);
        card.add(amount).width(150f).height(62f).center().row();

        String state = user != null && user.isDailyOfferPurchased()
                ? "Purchased today"
                : "Resets in " + dailyTimeRemaining();
        Label status = new Label(state, skin, "secondary");
        status.setWrap(true);
        card.add(status).width(150f).height(38f).center().row();

        card.add(new Label(
                formatNumber(ShopMenuController.getDailyOfferPrice())
                        + " COINS",
                skin, "medium_outline")).row();

        TextButton buy = new TextButton("BUY", skin, "green");
        buy.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                beginDailyPurchase();
            }
        });
        card.add(buy).width(118f).height(42f).padTop(4f);
        return card;
    }

    private Table createCardShell(String backgroundAsset) {
        Table card = new Table();
        card.setBackground(requireAssetDrawable(backgroundAsset));
        card.pad(14f, 10f, 12f, 10f);
        return card;
    }

    private void beginPurchase(ShopItem item, String plantName) {
        if (item.getType() == ItemType.SELECTIVE_SEED_PACK
                && (plantName == null || plantName.isBlank())) {
            showPlantSelectionModal(item);
            return;
        }

        CommandResult validation = ShopMenuController.validatePurchase(
                item.getId(), 1, plantName);
        if (!validation.isSuccsesful()) {
            showFeedback(validation.getMessage(), true);
            rebuildShelf();
            return;
        }
        showConfirmationModal(item.getId(), item.getName(),
                item.getPrice().getAmount(), item.getPrice().getType(),
                itemDescription(item), plantName);
    }

    private void beginDailyPurchase() {
        CommandResult validation = ShopMenuController.validatePurchase(
                "daily_offer", 1, null);
        if (!validation.isSuccsesful()) {
            showFeedback(validation.getMessage(), true);
            rebuildShelf();
            return;
        }

        User user = App.getInstance().getLoggedInUser();
        String plantName = user == null ? "" : user.getDailyOfferPlant();
        showConfirmationModal(
                "daily_offer",
                "Daily Seed Offer",
                ShopMenuController.getDailyOfferPrice(),
                CurrencyType.COIN,
                ShopMenuController.getDailyOfferSeeds()
                        + " seed packets for " + plantName,
                null);
    }

    private void showConfirmationModal(String itemId, String itemName,
            int price, CurrencyType currency, String description,
            String plantName) {
        closeModal();
        modalOverlay = new Table();
        modalOverlay.setFillParent(true);
        modalOverlay.setTouchable(Touchable.enabled);

        Table panel = new Table();
        panel.setBackground(
                skin.get("brown", TextButtonStyle.class).up);
        panel.pad(22f);

        Table titleRow = new Table();
        titleRow.add(new Label("Purchase Confirmation", skin, "big"))
                .expandX().center().padLeft(48f);
        ImageButton close = imageButton("generic_close_circle", "Close");
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeModal();
            }
        });
        titleRow.add(close).size(48f).right();
        panel.add(titleRow).growX().row();

        Label prompt = new Label(
                "Would you like to purchase " + itemName + "?",
                skin, "medium_outline");
        prompt.setWrap(true);
        panel.add(prompt).width(500f).center().padTop(16f).row();

        Label details = new Label(description
                + (plantName == null ? "" : "\nPlant: " + plantName)
                + "\nPrice: " + formatNumber(price) + " "
                + currencyName(currency),
                skin, "secondary");
        details.setWrap(true);
        panel.add(details).width(500f).center().padTop(8f).row();

        Label modalFeedback = new Label("", skin, "medium_outline");
        modalFeedback.setWrap(true);
        panel.add(modalFeedback).width(500f).height(38f).padTop(6f).row();

        Table buttons = new Table();
        TextButton cancel = new TextButton("Cancel", skin, "brown");
        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeModal();
            }
        });
        TextButton confirm = new TextButton("Confirm", skin, "green");
        confirm.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                confirm.setDisabled(true);
                confirm.setTouchable(Touchable.disabled);
                CommandResult result = ShopMenuController.purchase(
                        itemId, 1, plantName);
                if (!result.isSuccsesful()) {
                    modalFeedback.setColor(Color.SCARLET);
                    modalFeedback.setText(result.getMessage());
                    confirm.setDisabled(false);
                    confirm.setTouchable(Touchable.enabled);
                    return;
                }
                closeModal();
                showFeedback(result.getMessage(), false);
                rebuildShelf();
            }
        });
        buttons.add(cancel).width(150f).height(48f).pad(8f);
        buttons.add(confirm).width(150f).height(48f).pad(8f);
        panel.add(buttons).padTop(8f);

        modalOverlay.add(panel).width(610f).height(360f);
        root.setTouchable(Touchable.disabled);
        stage.addActor(modalOverlay);
    }

    private void showPlantSelectionModal(ShopItem item) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            showFeedback("You must be logged in.", true);
            return;
        }
        List<PlantCollectionItem> unlocked =
                user.getPlantCollection().getUnlockedPlants();
        if (unlocked.isEmpty()) {
            showFeedback("Unlock a plant before buying selective seeds.", true);
            return;
        }

        closeModal();
        modalOverlay = new Table();
        modalOverlay.setFillParent(true);
        modalOverlay.setTouchable(Touchable.enabled);

        Table panel = new Table();
        panel.setBackground(
                skin.get("brown", TextButtonStyle.class).up);
        panel.pad(18f);

        Table titleRow = new Table();
        titleRow.add(new Label("Choose a Plant", skin, "big"))
                .expandX().center().padLeft(48f);
        ImageButton close = imageButton("generic_close_circle", "Close");
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeModal();
            }
        });
        titleRow.add(close).size(48f).right();
        panel.add(titleRow).growX().row();

        Label info = new Label(
                "Selective Seed Packet: choose one unlocked plant to receive "
                        + item.getUnit() + " seed packets.",
                skin, "secondary");
        info.setWrap(true);
        panel.add(info).width(820f).center().padBottom(8f).row();

        Table grid = new Table();
        grid.top().left();
        grid.defaults().pad(5f);
        int column = 0;
        for (PlantCollectionItem plant : unlocked) {
            PlantPacketCard packet = new PlantPacketCard(navigator, plant);
            packet.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    closeModal();
                    beginPurchase(item, plant.getName());
                }
            });
            grid.add(packet).size(
                    PlantPacketCard.WIDTH, PlantPacketCard.TOTAL_HEIGHT);
            column++;
            if (column == 7) {
                grid.row();
                column = 0;
            }
        }

        ScrollPane scroll = new ScrollPane(grid, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        panel.add(scroll).width(850f).height(390f).row();

        modalOverlay.add(panel).width(920f).height(535f);
        root.setTouchable(Touchable.disabled);
        stage.addActor(modalOverlay);
    }

    private void closeModal() {
        if (modalOverlay == null) {
            return;
        }
        modalOverlay.remove();
        modalOverlay = null;
        root.setTouchable(Touchable.enabled);
    }

    private void showFeedback(String message, boolean error) {
        feedbackLabel.setColor(error ? Color.SCARLET : Color.GREEN);
        feedbackLabel.setText(message == null ? "" : message);
    }

    private String itemStatus(ShopItem item) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return "";
        }
        if (item.getType() == ItemType.POT) {
            return user.getGreenHouse().getBoard().getUnlockedPotCount()
                    + "/" + ShopMenuController.getMaximumPots()
                    + " pots unlocked";
        }
        if (item.getType() == ItemType.PLANT_FOOD) {
            return user.getPlantFoodCount() + "/"
                    + ShopMenuController.getMaximumPlantFood()
                    + " stored";
        }
        if (item.getType() == ItemType.RANDOM_SEED_PACK) {
            return "+" + item.getUnit() + " seeds";
        }
        if (item.getType() == ItemType.SELECTIVE_SEED_PACK) {
            return "+" + item.getUnit() + " chosen seeds";
        }
        return "+" + item.getUnit() + " coins";
    }

    private String itemDescription(ShopItem item) {
        switch (item.getType()) {
            case POT:
                return "Unlock one additional Greenhouse pot.";
            case PLANT_FOOD:
                return "Start a later level with one extra Plant Food.";
            case RANDOM_SEED_PACK:
                return "Five seed packets for a random unlocked plant.";
            case SELECTIVE_SEED_PACK:
                return "Ten seed packets for one unlocked plant you choose.";
            case CURRENCY_EXCHANGE:
                return "Convert 5 diamonds into 500 coins.";
            default:
                return item.getName();
        }
    }

    private String itemImageFor(ShopItem item) {
        switch (item.getType()) {
            case POT:
                return POT_ICON;
            case PLANT_FOOD:
                return PLANT_FOOD_ICON;
            case RANDOM_SEED_PACK:
            case SELECTIVE_SEED_PACK:
                return SEED_PACKET_ICON;
            case CURRENCY_EXCHANGE:
                return COIN_ICON;
            default:
                return "IMAGE_MISSING_IMAGE";
        }
    }

    private String cardBackgroundFor(ShopItem item) {
        if (item.getType() == ItemType.CURRENCY_EXCHANGE) {
            return "IMAGE_UI_CARDS_STORE_STORE_CARD_BLUE";
        }
        return item.getPrice().getType() == CurrencyType.DIAMOND
                ? "IMAGE_UI_CARDS_STORE_STORE_CARD_PURPLE"
                : "IMAGE_UI_CARDS_STORE_STORE_CARD_GREEN";
    }

    private String formatPrice(ShopItem item) {
        return formatNumber(item.getPrice().getAmount()) + " "
                + currencyName(item.getPrice().getType());
    }

    private static String currencyName(CurrencyType type) {
        return type == CurrencyType.COIN ? "COINS" : "DIAMONDS";
    }

    private static String formatNumber(int number) {
        return String.format(Locale.US, "%,d", number);
    }

    private static String dailyTimeRemaining() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = LocalDate.now().plusDays(1).atStartOfDay();
        Duration duration = Duration.between(now, next);
        long hours = Math.max(0, duration.toHours());
        long minutes = Math.max(0, duration.toMinutesPart());
        return String.format(Locale.US, "%02dh %02dm", hours, minutes);
    }

    private ImageButton assetImageButton(
            String normalAsset, String pressedAsset, String tooltip) {
        ImageButtonStyle style = new ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(
                requireAssetRegion(normalAsset));
        style.imageDown = new TextureRegionDrawable(
                requireAssetRegion(pressedAsset));
        style.imageOver = style.imageDown;
        ImageButton button = new ImageButton(style);
        button.getImage().setScaling(Scaling.fit);
        button.addListener(new TextTooltip(tooltip, skin));
        return button;
    }

    private ImageButton imageButton(String style, String tooltip) {
        ImageButton button = new ImageButton(skin, style);
        button.addListener(new TextTooltip(tooltip, skin));
        return button;
    }
}
