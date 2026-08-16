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
import io.github.Plants_Vs_Zombies_2.model.menu.GreenhouseMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.LeaderboardMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.SettingsMenu;
import io.github.Plants_Vs_Zombies_2.model.menu.ShopMenu;
import io.github.Plants_Vs_Zombies_2.model.news.News;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/** Main menu controls requested by the phase-two GUI specification. */
public final class MainMenuScreen extends AbstractScreen {
    private static final String MAIN_MENU_BACKGROUND =
            "Images/Backgrounds/MAIN_MENU_BACKGROUND.png";
    private static final String MAIN_MENU_LOGO =
            "IMAGE_UI_MAINMENU_PVZ2_LOGO_HORIZONTAL";
    private static final String NEWS_BUTTON_UP =
            "IMAGE_UI_HUD_NEWSBUTTON_BUTTONS_HUD_NEWS_NORMAL";
    private static final String NEWS_BUTTON_DOWN =
            "IMAGE_UI_HUD_NEWSBUTTON_BUTTONS_HUD_NEWS_SELECTED";
    private static final String COLLECTION_BUTTON_UP =
            "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_ALMANAC_NORMAL";
    private static final String COLLECTION_BUTTON_DOWN =
            "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_ALMANAC_SELECTED";
    private static final String GREENHOUSE_BUTTON_UP =
            "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_NORMAL";
    private static final String GREENHOUSE_BUTTON_DOWN =
            "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_SELECTED";
    private static final String SHOP_BUTTON_UP =
            "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_NORMAL";
    private static final String SHOP_BUTTON_DOWN =
            "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_SELECTED";
    private static final String LEADERBOARD_BUTTON_UP =
            "IMAGE_UI_GAMECENTER_ANDROID_LEADERBOARD";
    private static final String LEADERBOARD_BUTTON_DOWN =
            "IMAGE_UI_GAMECENTER_ANDROID_LEADERBOARD_SELECT";

    private static final String[] NEWS_CARD_STYLES = {
            "green", "brown"
    };

    private final Label unreadNewsBadge;
    private Table newsModal;

    public MainMenuScreen(ScreenNavigator navigator) {
        super(navigator, "Main Menu");
        setBackground(MAIN_MENU_BACKGROUND);

        ImageButton logout = imageButton("previous", "Logout");
        logout.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.logout();
            }
        });
        headerLeading.add(logout).size(64f);

        buildMainContent();

        ImageButton news = assetImageButton(
                NEWS_BUTTON_UP, NEWS_BUTTON_DOWN, "News");
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

        ImageButton leaderboard = assetImageButton(
                LEADERBOARD_BUTTON_UP, LEADERBOARD_BUTTON_DOWN, "Leaderboard");
        leaderboard.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.navigate(new LeaderboardMenu());
            }
        });

        ImageButton greenhouse = assetImageButton(
                GREENHOUSE_BUTTON_UP, GREENHOUSE_BUTTON_DOWN, "Greenhouse");
        greenhouse.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.navigate(new GreenhouseMenu());
            }
        });

        ImageButton settings = imageButton("settings", "Settings");
        settings.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.navigate(new SettingsMenu());
            }
        });

        // Keep the feature shortcuts on the lower-left. Leaderboard belongs
        // with Settings on the lower-right so the utility buttons form one
        // compact group instead of shifting the feature shortcuts.
        navigation.add(newsControl).width(96f).height(112f).left().bottom();
        navigation.add(collection).size(82f).left().bottom();
        navigation.add(greenhouse).size(82f).left().bottom();
        navigation.add(shop).size(82f).left().bottom();
        navigation.add().expandX();
        navigation.add(leaderboard).size(82f).right().bottom();
        navigation.add(settings).size(78f).right().bottom();
    }

    private void buildMainContent() {
        Table center = new Table();
        center.top();

        Image logo = createAssetImage(MAIN_MENU_LOGO);
        logo.setScaling(Scaling.fit);

        center.add(logo).width(560f).height(176f).padTop(32f).row();
        center.add().height(150f).row();

        TextButton playButton = new TextButton("Play", skin, "purple");
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.showAdventureScreen();
            }
        });
        center.add(playButton).width(260f).height(70f);

        content.add(center).grow().top();
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
