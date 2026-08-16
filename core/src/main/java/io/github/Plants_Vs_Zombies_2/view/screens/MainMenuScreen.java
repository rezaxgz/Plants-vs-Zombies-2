package io.github.Plants_Vs_Zombies_2.view.screens;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
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
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.menu.CollectionMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.SettingsMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.ShopMenu;
import io.github.Plants_Vs_Zombies_2.model.news.News;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/** Main menu controls requested by the phase-two GUI specification. */
public final class MainMenuScreen extends AbstractScreen {
    private static final String MAIN_MENU_LOGO =
            "IMAGE_UI_MAINMENU_PVZ2_LOGO_HORIZONTAL";
    private static final String COLLECTION_BUTTON_UP =
            "IMAGE_UI_HUD_ALMANACBUTTON_BUTTONS_HUD_ALMANAC_NORMAL";
    private static final String COLLECTION_BUTTON_DOWN =
            "IMAGE_UI_HUD_ALMANACBUTTON_BUTTONS_HUD_ALMANAC_SELECTED";
    private static final String SHOP_BUTTON_UP =
            "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_NORMAL";
    private static final String SHOP_BUTTON_DOWN =
            "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_SELECTED";

    private static final String[] NEWS_CARD_STYLES = {
            "green", "brown"
    };

    private final Label unreadNewsBadge;
    private Table newsModal;

    public MainMenuScreen(ScreenNavigator navigator) {
        super(navigator, "Main Menu");

        ImageButton logout = imageButton("previous", "Logout");
        logout.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.logout();
            }
        });
        headerLeading.add(logout).size(64f);

        buildMainContent();

        ImageButton news = imageButton("hud_quests", "News");
        news.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showNewsModal();
            }
        });

        unreadNewsBadge = new Label("", skin, "medium_outline");
        unreadNewsBadge.setColor(Color.GOLD);
        refreshUnreadNewsBadge();

        Table newsControl = new Table();
        newsControl.add(unreadNewsBadge).right().height(28f).row();
        newsControl.add(news).size(76f);

        ImageButton collection = assetImageButton(
                COLLECTION_BUTTON_UP, COLLECTION_BUTTON_DOWN, "Collection");
        collection.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.navigate(new CollectionMenu());
            }
        });

        ImageButton shop = assetImageButton(
                SHOP_BUTTON_UP, SHOP_BUTTON_DOWN, "Shop");
        shop.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.navigate(new ShopMenu());
            }
        });

        ImageButton settings = imageButton("settings", "Settings");
        settings.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.navigate(new SettingsMenu());
            }
        });

        // Keep the two content shortcuts together at the lower-left, like the
        // PvZ2 HUD, while Settings remains isolated on the lower-right.
        navigation.add(newsControl).width(96f).height(112f).left().bottom();
        navigation.add(collection).size(82f).left().bottom();
        navigation.add(shop).size(82f).left().bottom();
        navigation.add().expandX();
        navigation.add(settings).size(78f).right().bottom();
    }

    private void buildMainContent() {
        Table center = new Table();
        center.defaults().pad(8f);

        Image logo = createAssetImage(MAIN_MENU_LOGO);
        logo.setScaling(Scaling.fit);
        center.add(logo).width(700f).height(220f).row();

        TextButton playButton = new TextButton("Play", skin, "green");
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.showAdventureScreen();
            }
        });
        center.add(playButton).width(260f).height(70f).padTop(18f);

        content.add(center).expand().center();
    }

    private ImageButton imageButton(String style, String tooltip) {
        ImageButton button = new ImageButton(skin, style);
        button.addListener(new TextTooltip(tooltip, skin));
        return button;
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

    private void refreshUnreadNewsBadge() {
        User user = App.getInstance().getLoggedInUser();
        int unread = user == null ? 0 : user.getNewsPanel().getUnreadCount();
        unreadNewsBadge.setVisible(unread > 0);
        unreadNewsBadge.setText(unread > 0 ? Integer.toString(unread) : "");
    }

    private void showNewsModal() {
        if (newsModal != null) {
            return;
        }

        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return;
        }

        newsModal = new Table();
        newsModal.setFillParent(true);
        newsModal.setTouchable(Touchable.enabled);

        Table panel = new Table();
        panel.setBackground(
                skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        panel.pad(22f);

        Table titleBar = new Table();
        titleBar.add(new Label("News", skin, "big")).left().expandX();
        ImageButton close = imageButton("generic_close_circle", "Close");
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeNewsModal();
            }
        });
        titleBar.add(close).size(58f).right();
        panel.add(titleBar).growX().row();

        List<News> allNews = new ArrayList<>(user.getNewsPanel().getAllNews());
        allNews.sort(Comparator.comparingLong(News::getTimestampMillis).reversed());

        Table cards = new Table();
        cards.top();
        if (allNews.isEmpty()) {
            Label empty = new Label("No news available.", skin, "medium");
            cards.add(empty).pad(30f);
        } else {
            int index = 0;
            for (News item : allNews) {
                boolean wasUnread = !item.isHasRead();
                cards.add(createNewsCard(item, wasUnread, index++))
                        .growX().width(820f).pad(7f).row();
            }
        }

        ScrollPane scroll = new ScrollPane(cards, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        panel.add(scroll).width(880f).height(500f).padTop(10f).row();

        newsModal.add(panel).width(940f).height(600f);
        root.setTouchable(Touchable.disabled);
        stage.addActor(newsModal);

        // Opening the panel is the graphical equivalent of viewing the news.
        // Preserve the old persistent read/unread semantics immediately.
        user.getNewsPanel().markAllAsRead();
        UserManager.saveAllUsers();
        refreshUnreadNewsBadge();
    }

    private Table createNewsCard(News item, boolean wasUnread, int index) {
        Table card = new Table();
        String styleName = NEWS_CARD_STYLES[
                index % NEWS_CARD_STYLES.length];
        TextButtonStyle style = skin.get(styleName, TextButtonStyle.class);
        card.setBackground(style.up);
        card.pad(14f);

        String titleText = wasUnread
                ? "NEW  " + item.getTitle()
                : item.getTitle();
        Label title = new Label(titleText, skin, "medium_outline");
        Label date = new Label(formatDate(item.getTimestampMillis()),
                skin, "secondary");
        Label description = new Label(item.getDescription(), skin);
        description.setWrap(true);

        card.add(title).left().expandX();
        card.add(date).right().padLeft(14f).row();
        card.add(description).colspan(2).growX().width(760f)
                .left().padTop(8f).row();
        return card;
    }

    private static String formatDate(long timestampMillis) {
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm", Locale.ROOT);
        return format.format(new Date(timestampMillis));
    }

    private void closeNewsModal() {
        if (newsModal == null) {
            return;
        }
        newsModal.remove();
        newsModal = null;
        root.setTouchable(Touchable.enabled);
        refreshUnreadNewsBadge();
    }
}
