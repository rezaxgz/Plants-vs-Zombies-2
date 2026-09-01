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
import io.github.Plants_Vs_Zombies_2.network.auth.AccountProfile;
import io.github.Plants_Vs_Zombies_2.view.presentation.Phase3Text;

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
    private static final String PROFILE_BUTTON_ICON =
            "IMAGE_UI_MAINMENU_MM_PLAYERICON";
    private static final String PROFILE_COIN_ICON =
            "IMAGE_UI_THYMED_EVENTS_ECS_CONVRT_COIN";
    private static final String PROFILE_DIAMOND_ICON =
            "IMAGE_EFFECTS_COIN_DIAMOND_COIN_DIAMOND_141X146";

    private static final String[] NEWS_CARD_STYLES = {
            "green", "brown"
    };

    private final Label unreadNewsBadge;
    private Table newsModal;
    private Table profileModal;
    private boolean active = true;

    public MainMenuScreen(ScreenNavigator navigator) {
        super(navigator, welcomeTitle());
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

        ImageButton profile = profileImageButton("Profile");
        profile.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showProfileModal();
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
        navigation.add(profile).size(72f).right().bottom();
        navigation.add(settings).size(78f).right().bottom();
    }

    private static String welcomeTitle() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null || user.getNickName() == null
                || user.getNickName().isBlank()) {
            return "Welcome!";
        }
        return "Welcome " + user.getNickName() + "!";
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
        center.add(playButton).width(260f).height(70f).row();

        TextButton multiplayerButton = new TextButton(
                "Multiplayer I, Zombie", skin, "green");
        multiplayerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                navigator.showMultiplayerIZombieMenu();
            }
        });
        center.add(multiplayerButton).width(310f).height(64f).padTop(12f);

        content.add(center).grow().top();
    }

    private ImageButton imageButton(String style, String tooltip) {
        ImageButton button = new ImageButton(skin, style);
        button.addListener(new TextTooltip(tooltip, skin));
        return button;
    }

    private ImageButton profileImageButton(String tooltip) {
        TextButtonStyle brown = skin.get("brown", TextButtonStyle.class);
        ImageButtonStyle style = new ImageButtonStyle();
        style.up = brown.up;
        style.down = brown.down;
        style.over = brown.over != null ? brown.over : brown.down;

        TextureRegionDrawable icon = new TextureRegionDrawable(
                requireAssetRegion(PROFILE_BUTTON_ICON));
        style.imageUp = icon;
        style.imageDown = icon;
        style.imageOver = icon;

        ImageButton button = new ImageButton(style);
        button.getImage().setScaling(Scaling.fit);
        button.getImageCell().size(48f, 48f);
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

    private void showProfileModal() {
        if (profileModal != null || newsModal != null) {
            return;
        }
        AccountProfile profile = navigator.getAccountSession().getProfile();
        if (profile == null) {
            return;
        }
        renderProfileModal(profile, true);
    }

    private void renderProfileModal(AccountProfile profile, boolean refresh) {
        if (profile == null) {
            return;
        }
        profileModal = new Table();
        profileModal.setFillParent(true);
        profileModal.setTouchable(Touchable.enabled);

        Table panel = new Table();
        panel.setBackground(
                skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        panel.pad(22f);

        TextButtonStyle brownStyle = skin.get("brown", TextButtonStyle.class);
        Table titleBar = new Table();
        titleBar.setBackground(brownStyle.up);
        titleBar.pad(7f, 12f, 7f, 12f);
        Image avatar = createAssetImage(PROFILE_BUTTON_ICON);
        avatar.setScaling(Scaling.fit);
        titleBar.add(avatar).size(46f).padRight(10f);
        Label title = new Label("Profile", skin, "big");
        title.setColor(Color.WHITE);
        titleBar.add(title).left().expandX();
        ImageButton close = imageButton("generic_close_circle", "Close");
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeProfileModal();
            }
        });
        titleBar.add(close).size(52f).right();
        panel.add(titleBar).growX().height(68f).padBottom(12f).row();

        Table accountRows = new Table();
        accountRows.defaults().growX().height(49f).pad(3f);
        accountRows.add(profileInfoRow("Username", Phase3Text.required(
                profile.getUsername(), "Account unavailable"))).row();
        accountRows.add(profileInfoRow("Nickname", Phase3Text.optional(
                profile.getNickname()))).row();
        accountRows.add(profileInfoRow("Email", Phase3Text.optional(
                profile.getEmail()))).row();
        accountRows.add(profileInfoRow("Gender", Phase3Text.prettyIdentifier(
                profile.getGender(), "Not provided"))).row();
        panel.add(accountRows).width(820f).top().row();

        Label statsHeading = new Label("Player Statistics", skin,
                "medium_outline");
        statsHeading.setColor(new Color(0.38f, 0.22f, 0.08f, 1f));
        panel.add(statsHeading).left().padTop(10f).padBottom(4f).row();

        Table stats = new Table();
        stats.center();

        Table primaryStats = new Table();
        primaryStats.defaults().pad(5f);
        primaryStats.add(profileStatCard("Games Played",
                Integer.toString(profile.getGamesPlayed()),
                null)).width(220f).height(68f);
        primaryStats.add(profileStatCard("Last Completed",
                Phase3Text.levelProgress(profile.getLastCompletedChapter(),
                        profile.getLastCompletedLevel()), null))
                .width(220f).height(68f);
        primaryStats.add(profileStatCard("Highest Mew Point",
                Integer.toString(profile.getHighestScore()),
                null)).width(220f).height(68f);

        Table currencyStats = new Table();
        currencyStats.defaults().pad(5f);
        currencyStats.add(profileStatCard("Coins",
                String.format(Locale.US, "%,d", profile.getCoins()),
                PROFILE_COIN_ICON)).width(285f).height(72f);
        currencyStats.add(profileStatCard("Diamonds",
                String.format(Locale.US, "%,d", profile.getDiamonds()),
                PROFILE_DIAMOND_ICON)).width(285f).height(72f);

        stats.add(primaryStats).center().row();
        stats.add(currencyStats).center().padTop(2f).row();

        panel.add(stats).width(760f).center().top().row();
        Label refreshStatus = new Label(refresh
                ? "Refreshing profile from server..."
                : "Profile loaded from server.", skin, "secondary");
        panel.add(refreshStatus).width(760f).center().padTop(4f).row();
        profileModal.add(panel).width(900f).height(600f);
        root.setTouchable(Touchable.disabled);
        stage.addActor(profileModal);

        if (refresh) {
            navigator.getGameplaySync().refresh().whenComplete(
                    (updated, failure) -> navigator.getUiDispatcher().dispatch(() -> {
                        if (!active || profileModal == null) {
                            return;
                        }
                        if (failure != null) {
                            refreshStatus.setText(
                                    "Could not refresh; showing the last valid server profile.");
                            refreshStatus.setColor(Color.SCARLET);
                            return;
                        }
                        closeProfileModal();
                        renderProfileModal(
                                navigator.getAccountSession().getProfile(), false);
                    }));
        }
    }

    private Table profileInfoRow(String name, String value) {
        Table row = new Table();
        TextButtonStyle rowStyle = skin.get("brown", TextButtonStyle.class);
        row.setBackground(rowStyle.up);
        row.pad(5f, 14f, 5f, 12f);

        Label.LabelStyle nameStyle = new Label.LabelStyle(
                skin.get("secondary", Label.LabelStyle.class));
        nameStyle.fontColor = new Color(1f, 0.88f, 0.58f, 1f);
        Label nameLabel = new Label(name, nameStyle);
        nameLabel.setFontScale(0.82f);

        Label valueLabel = new Label(Phase3Text.optional(value),
                skin, "medium_outline");
        valueLabel.setFontScale(0.72f);
        valueLabel.setColor(Color.WHITE);

        row.add(nameLabel).width(195f).left();
        row.add(valueLabel).growX().left().padLeft(12f);
        row.add().width(38f);
        return row;
    }

    private Table profileStatCard(String name, String value,
            String iconAsset) {
        Table card = new Table();
        TextButtonStyle cardStyle = skin.get("green", TextButtonStyle.class);
        card.setBackground(cardStyle.up);
        card.pad(5f, 10f, 5f, 10f);

        Label.LabelStyle titleStyle = new Label.LabelStyle(
                skin.get("secondary", Label.LabelStyle.class));
        titleStyle.fontColor = new Color(1f, 0.92f, 0.68f, 1f);
        Label nameLabel = new Label(name, titleStyle);
        nameLabel.setFontScale(0.80f);
        nameLabel.setAlignment(com.badlogic.gdx.utils.Align.center);

        Label valueLabel = new Label(Phase3Text.optional(value),
                skin, "medium_outline");
        valueLabel.setFontScale(0.96f);
        valueLabel.setColor(Color.WHITE);
        valueLabel.setAlignment(com.badlogic.gdx.utils.Align.center);

        card.add(nameLabel).colspan(iconAsset == null ? 1 : 2)
                .growX().center().padBottom(3f).row();
        if (iconAsset == null) {
            card.add(valueLabel).growX().center();
        } else {
            Image icon = createAssetImage(iconAsset);
            icon.setScaling(Scaling.fit);
            card.add(icon).size(30f).right().padRight(7f);
            card.add(valueLabel).left();
        }
        return card;
    }

    private void closeProfileModal() {
        if (profileModal == null) {
            return;
        }
        profileModal.remove();
        profileModal = null;
        root.setTouchable(Touchable.enabled);
    }

    private void refreshUnreadNewsBadge() {
        User user = App.getInstance().getLoggedInUser();
        int unread = user == null ? 0 : user.getNewsPanel().getUnreadCount();
        unreadNewsBadge.setVisible(unread > 0);
        unreadNewsBadge.setText(unread > 0 ? Integer.toString(unread) : "");
    }

    private void showNewsModal() {
        if (newsModal != null || profileModal != null) {
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

    @Override
    public void dispose() {
        active = false;
        profileModal = null;
        newsModal = null;
        super.dispose();
    }
}
