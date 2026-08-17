package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.model.App;
import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.quest.Quest;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestCondition;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestRewardType;
import io.github.Plants_Vs_Zombies_2.model.quest.QuestType;
import io.github.Plants_Vs_Zombies_2.model.roadmap.AdventureProgress;
import io.github.Plants_Vs_Zombies_2.model.roadmap.AdventureSession;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Chapter;
import io.github.Plants_Vs_Zombies_2.model.roadmap.ChapterCatalog;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Level;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/**
 * Adventure screens inspired by images 2 and 3 in the phase-two handout:
 * floating chapter islands first, then a staggered floating-island level map.
 */
public final class AdventureScreen extends AbstractScreen {
    private static final String LEVEL_ISLAND =
            "IMAGE_WORLDMAP_EGYPT_ISLAND26";
    private static final String LOCK_PAM =
            "768/INITIAL/UI/UNIVERSE/WORLD_LOCK/WORLD_LOCK.PAM";
    private static final String LOCK_CLIP = "idle";

    private static final String BACK_BUTTON_UP =
            "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_BACK_NORMAL";
    private static final String BACK_BUTTON_DOWN =
            "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_BACK_SELECTED";

    private static final String QUEST_BUTTON_UP =
            "IMAGE_UI_HUD_QUESTBUTTON_QUEST_ICON_UP";
    private static final String QUEST_BUTTON_DOWN =
            "IMAGE_UI_HUD_QUESTBUTTON_QUEST_ICON_DOWN";
    private static final String QUEST_TRAVEL_LOG_CORNER =
            "IMAGE_UI_QUESTS_TRAVEL_LOG_CORNER_NORANK";
    private static final String QUEST_LIST_BACKGROUND =
            "IMAGE_UI_QUESTS_QUEST_LIST_BG";
    private static final String QUEST_PANEL_DEFAULT =
            "IMAGE_UI_QUESTS_QUEST_PANEL_DEFAULT";
    private static final String QUEST_PANEL_DAILY =
            "IMAGE_UI_QUESTS_QUEST_PANEL_DAILY";
    private static final String QUEST_PANEL_EPIC =
            "IMAGE_UI_QUESTS_QUEST_PANEL_EPIC";
    private static final String QUEST_PANEL_COMPLETE =
            "IMAGE_UI_QUESTS_QUEST_PANEL_COMPLETE";
    private static final String QUEST_PANEL_EPIC_COMPLETE =
            "IMAGE_UI_QUESTS_QUEST_PANEL_EPIC_COMPLETE";
    private static final String QUEST_CLOSE_BUTTON =
            "IMAGE_UI_QUESTS_CLOSE_BTN";
    private static final String QUEST_PROGRESS_BACKGROUND =
            "IMAGE_UI_QUESTS_QUEST_POINTS_FILLBAR_BG";
    private static final String QUEST_PROGRESS_FILL =
            "IMAGE_UI_QUESTS_QUEST_POINTS_FILLBAR_FILL_GREEN";
    private static final String QUEST_REWARD_COINS =
            "IMAGE_EFFECTS_PRIZE_COINS_LARGE_PRIZE_COINS_LARGE_581X453";
    private static final String QUEST_REWARD_GEMS =
            "IMAGE_EFFECTS_PRIZE_GEMS_LARGE_PRIZE_GEMS_LARGE_511X558";
    private static final String QUEST_REWARD_PINATA =
            "IMAGE_UI_QUESTS_EPIC_REWARD_PINATA";
    private static final String QUEST_ICON_PLANT =
            "IMAGE_UI_QUESTS_QUESTICONS_PLANT";
    private static final String QUEST_ICON_ZOMBIE =
            "IMAGE_UI_QUESTS_QUESTICONS_ZOMBIE";
    private static final String QUEST_ICON_POWERUPS =
            "IMAGE_UI_QUESTS_QUESTICONS_POWERUPS";
    private static final String QUEST_ICON_LEVELUP =
            "IMAGE_UI_QUESTS_QUESTICONS_LEVELUP";
    private static final String QUEST_ICON_DARK_AGES =
            "IMAGE_UI_QUESTS_QUESTICONS_DARKAGES";

    private static final float LEVEL_MAP_WIDTH = 1080f;
    private static final float LEVEL_MAP_HEIGHT = 420f;

    private static final float[] LEVEL_X = {45f, 310f, 590f, 855f};
    private static final float[] LEVEL_Y = {45f, 185f, 80f, 200f};

    private final List<Chapter> chapters;
    private int selectedIndex;
    private boolean showingLevels;
    private Table questModal;
    private Table questPanel;
    private QuestType selectedQuestType = QuestType.DAILY;

    public AdventureScreen(ScreenNavigator navigator) {
        super(navigator, "Adventure");
        chapters = ChapterCatalog.getChapters();
        selectedIndex = Math.max(0,
                chapters.indexOf(AdventureSession.getInstance()
                        .getSelectedChapter()));
        showingLevels = false;

        ImageButton backButton = assetImageButton(
                BACK_BUTTON_UP, BACK_BUTTON_DOWN);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleAdventureBack();
            }
        });
        // Keep the two edge controls out of the root Table layout. The large
        // Adventure carousel plus an 82px quest button used to make the Table
        // taller than the 720px virtual viewport, pushing the header upward
        // and the quest control downward until both were clipped.
        headerLeading.add().size(68f);
        backButton.setBounds(24f, VIRTUAL_HEIGHT - 24f - 64f, 64f, 64f);
        root.addActor(backButton);

        ImageButton questsButton = createQuestButton();
        questsButton.setBounds(24f, 24f, 82f, 82f);
        root.addActor(questsButton);
        rebuildContent();
    }


    private void handleAdventureBack() {
        if (showingLevels) {
            showingLevels = false;
            rebuildContent();
            return;
        }
        navigator.returnToCurrentMenu();
    }

    private void rebuildContent() {
        content.clearChildren();
        if (showingLevels) {
            buildLevelOverview();
        } else {
            buildChapterCarousel();
        }
    }

    private void buildChapterCarousel() {
        AdventureProgress progress = AdventureSession.getInstance()
                .getProgress();
        Chapter selectedChapter = chapters.get(selectedIndex);
        boolean unlocked = progress.isChapterUnlocked(selectedChapter);

        Table screen = new Table();
        screen.defaults().pad(5f);

        Label heading = new Label("Choose a chapter", skin, "big_outline");
        screen.add(heading).padBottom(4f).row();

        Label progressLabel = new Label(
                buildChapterProgressText(selectedChapter, progress),
                skin, "medium_outline");
        progressLabel.setColor(Color.GOLD);
        screen.add(progressLabel).padBottom(2f).row();

        Table carousel = new Table();
        ImageButton previousButton = createCarouselButton("previous", -1);
        ImageButton nextButton = createCarouselButton("next", 1);
        setButtonEnabled(previousButton, selectedIndex > 0);
        setButtonEnabled(nextButton, selectedIndex + 1 < chapters.size());

        carousel.add(previousButton).size(76f).padRight(14f);
        carousel.add(createChapterSlot(selectedIndex - 1, false))
                .width(230f).height(310f).padRight(8f);
        carousel.add(createChapterSlot(selectedIndex, true))
                .width(370f).height(400f).pad(2f);
        carousel.add(createChapterSlot(selectedIndex + 1, false))
                .width(230f).height(310f).padLeft(8f);
        carousel.add(nextButton).size(76f).padLeft(14f);
        screen.add(carousel).padTop(2f).row();

        TextButton enterChapter = new TextButton("Enter Chapter", skin, "green");
        setButtonEnabled(enterChapter, unlocked);
        enterChapter.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!unlocked) {
                    return;
                }
                AdventureSession.getInstance().selectChapter(selectedChapter);
                showingLevels = true;
                rebuildContent();
            }
        });
        screen.add(enterChapter).width(250f).height(58f).padTop(2f).row();

        if (!unlocked) {
            Label lockedMessage = new Label(
                    "Finish the previous chapter to unlock this world.",
                    skin, "secondary");
            screen.add(lockedMessage).padTop(2f);
        }

        content.add(screen).expand().center();
    }

    private void buildLevelOverview() {
        AdventureSession session = AdventureSession.getInstance();
        Chapter chapter = chapters.get(selectedIndex);
        if (!session.selectChapter(chapter)) {
            showingLevels = false;
            rebuildContent();
            return;
        }
        AdventureProgress progress = session.getProgress();

        Table screen = new Table();
        screen.defaults().pad(4f);

        Table titleRow = new Table();

        Image chapterLogo = createAssetImage(getChapterLogoAsset(chapter));
        chapterLogo.setScaling(Scaling.fit);
        titleRow.add(chapterLogo).width(116f).height(86f).padLeft(14f);

        Table names = new Table();
        names.add(new Label(chapter.getDisplayName(), skin, "big_outline"))
                .left().row();
        names.add(new Label(
                progress.getCompletedLevelCount(chapter) + "/"
                        + chapter.getLevelCount() + " levels completed",
                skin, "secondary")).left();
        titleRow.add(names).expandX().left().padLeft(8f);
        screen.add(titleRow).growX().row();

        Group levelMap = new Group();
        levelMap.setSize(LEVEL_MAP_WIDTH, LEVEL_MAP_HEIGHT);
        List<Level> levels = chapter.getLevels();
        for (int index = 0; index < levels.size(); index++) {
            Level level = levels.get(index);
            Table island = createLevelIsland(chapter, level, progress, index);
            island.setBounds(LEVEL_X[index], LEVEL_Y[index], 220f, 215f);
            levelMap.addActor(island);
        }

        screen.add(levelMap)
                .width(LEVEL_MAP_WIDTH)
                .height(LEVEL_MAP_HEIGHT)
                .padTop(2f)
                .row();

        Label helper = new Label(
                "Unlocked levels are available; later islands stay locked until you progress.",
                skin, "secondary");
        screen.add(helper).padTop(1f);

        content.add(screen).expand().center();
    }

    private ImageButton createQuestButton() {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(requireAssetRegion(
                QUEST_BUTTON_UP));
        style.imageDown = new TextureRegionDrawable(requireAssetRegion(
                QUEST_BUTTON_DOWN));
        style.imageOver = style.imageDown;
        ImageButton button = new ImageButton(style);
        button.getImage().setScaling(Scaling.fit);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showQuestModal();
            }
        });
        return button;
    }

    private void showQuestModal() {
        if (questModal != null) {
            return;
        }
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return;
        }
        user.getQuestProgress().ensureInitialized(user);
        UserManager.saveAllUsers();

        questModal = new Table();
        questModal.setFillParent(true);
        questModal.setTouchable(Touchable.enabled);

        questPanel = new Table();
        questPanel.setBackground(requireAssetDrawable(QUEST_LIST_BACKGROUND));
        questPanel.pad(18f, 22f, 20f, 22f);
        questModal.add(questPanel).width(1010f).height(600f);

        root.setTouchable(Touchable.disabled);
        stage.addActor(questModal);
        rebuildQuestPanel();
    }

    private void rebuildQuestPanel() {
        if (questPanel == null) {
            return;
        }
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            closeQuestModal();
            return;
        }

        questPanel.clearChildren();
        questPanel.top();

        Table header = new Table();
        Image travelLog = createAssetImage(QUEST_TRAVEL_LOG_CORNER);
        travelLog.setScaling(Scaling.fit);
        header.add(travelLog).width(135f).height(90f).left().padRight(8f);

        Table headerText = new Table();
        Label title = new Label("Travel Log", skin, "big_outline");
        Label subtitle = new Label("Quests", skin, "medium_outline");
        subtitle.setColor(Color.GOLD);
        headerText.add(title).left().row();
        headerText.add(subtitle).left().padTop(-4f);
        header.add(headerText).left().expandX();

        ImageButton close = assetImageButton(QUEST_CLOSE_BUTTON,
                QUEST_CLOSE_BUTTON);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeQuestModal();
            }
        });
        header.add(close).size(54f).right().padRight(4f);
        questPanel.add(header).growX().height(94f).row();

        Table tabs = new Table();
        tabs.defaults().width(160f).height(48f).pad(0f, 5f, 4f, 5f);
        tabs.add(createQuestTab("Main", QuestType.MAIN,
                "IMAGE_UI_QUESTS_ACHIEVEMENTS_INACTIVE",
                "IMAGE_UI_QUESTS_ACHIEVEMENTS_ACTIVE"));
        tabs.add(createQuestTab("Epic", QuestType.EPIC,
                "IMAGE_UI_QUESTS_EPIC_INACTIVE",
                "IMAGE_UI_QUESTS_EPIC_ACTIVE"));
        tabs.add(createQuestTab("Daily", QuestType.DAILY,
                "IMAGE_UI_QUESTS_DAILY_INACTIVE",
                "IMAGE_UI_QUESTS_DAILY_ACTIVE"));
        questPanel.add(tabs).center().padTop(-4f).row();

        List<Quest> quests = new ArrayList<>();
        for (Quest quest : user.getQuestProgress().getActiveQuests()) {
            if (quest.getType() == selectedQuestType) {
                quests.add(quest);
            }
        }
        Collections.sort(quests);

        Table questList = new Table();
        questList.top();
        questList.defaults().growX().width(900f).height(104f).pad(5f, 0f, 5f, 0f);
        if (quests.isEmpty()) {
            Label empty = new Label("No active quests on this page.",
                    skin, "medium_outline");
            questList.add(empty).height(90f).center();
        } else {
            for (Quest quest : quests) {
                questList.add(createQuestCard(quest)).row();
            }
        }

        ScrollPane scroll = new ScrollPane(questList, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        questPanel.add(scroll).width(930f).height(390f).center().row();

        Label footer = new Label(
                "Quests are ordered by priority. Progress and rewards are saved automatically.",
                skin, "secondary");
        footer.setColor(new Color(0.92f, 0.82f, 0.58f, 1f));
        questPanel.add(footer).center().padTop(5f);
    }

    private Stack createQuestTab(String text, QuestType type,
            String inactiveAsset, String activeAsset) {
        boolean selected = selectedQuestType == type;
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(requireAssetRegion(
                selected ? activeAsset : inactiveAsset));
        style.imageOver = new TextureRegionDrawable(requireAssetRegion(activeAsset));
        style.imageDown = style.imageOver;
        ImageButton button = new ImageButton(style);
        button.getImage().setScaling(Scaling.stretch);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectedQuestType != type) {
                    selectedQuestType = type;
                    rebuildQuestPanel();
                }
            }
        });

        Label label = new Label(text, skin, "medium_outline");
        label.setAlignment(Align.center);
        label.setTouchable(Touchable.disabled);
        Stack stack = new Stack();
        stack.add(button);
        stack.add(label);
        return stack;
    }

    private Table createQuestCard(Quest quest) {
        Table card = new Table();
        card.setBackground(requireAssetDrawable(questPanelAsset(quest)));
        card.pad(8f, 14f, 8f, 14f);

        Image questIcon = createAssetImage(questIconAsset(quest));
        questIcon.setScaling(Scaling.fit);
        card.add(questIcon).size(76f).padRight(12f);

        Table details = new Table();
        details.left();
        Label name = new Label(quest.getName(), skin, "medium_outline");
        name.setColor(quest.isCompleted() ? Color.GREEN : Color.WHITE);
        details.add(name).left().growX();

        Label priority = new Label(quest.getPriority().name(), skin, "secondary");
        priority.setColor(new Color(0.98f, 0.82f, 0.34f, 1f));
        details.add(priority).right().row();

        Label instructions = new Label(quest.getInstructions(), skin, "secondary");
        instructions.setWrap(true);
        details.add(instructions).colspan(2).left().growX().width(560f)
                .padTop(1f).row();

        Table progressRow = new Table();
        ProgressBar progress = createQuestProgressBar(quest);
        progressRow.add(progress).width(430f).height(15f).left();
        Label progressText = new Label(quest.getProgressText(),
                skin, "secondary");
        progressText.setColor(Color.WHITE);
        progressRow.add(progressText).width(70f).right().padLeft(8f);
        details.add(progressRow).colspan(2).left().padTop(3f);
        card.add(details).growX().left().padRight(10f);

        Table reward = new Table();
        Image rewardIcon = createAssetImage(rewardIconAsset(quest));
        rewardIcon.setScaling(Scaling.fit);
        reward.add(rewardIcon).size(58f).center().row();
        Label rewardText = new Label(quest.getReward().describe(),
                skin, "secondary");
        rewardText.setWrap(true);
        rewardText.setAlignment(Align.center);
        reward.add(rewardText).width(145f).center().padTop(-5f);
        card.add(reward).width(155f).center();
        return card;
    }

    private ProgressBar createQuestProgressBar(Quest quest) {
        ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();
        style.background = requireAssetDrawable(QUEST_PROGRESS_BACKGROUND);
        style.knobBefore = requireAssetDrawable(QUEST_PROGRESS_FILL);
        ProgressBar progress = new ProgressBar(0f,
                Math.max(1f, quest.getTarget()), 1f, false, style);
        progress.setAnimateDuration(0f);
        progress.setValue(Math.min(quest.getProgress(), quest.getTarget()));
        return progress;
    }

    private String questPanelAsset(Quest quest) {
        if (quest.isCompleted()) {
            return quest.getType() == QuestType.EPIC
                    ? QUEST_PANEL_EPIC_COMPLETE : QUEST_PANEL_COMPLETE;
        }
        if (quest.getType() == QuestType.EPIC) {
            return QUEST_PANEL_EPIC;
        }
        if (quest.getType() == QuestType.DAILY) {
            return QUEST_PANEL_DAILY;
        }
        return QUEST_PANEL_DEFAULT;
    }

    private String rewardIconAsset(Quest quest) {
        QuestRewardType type = quest.getReward().getType();
        if (type == QuestRewardType.COINS) {
            return QUEST_REWARD_COINS;
        }
        if (type == QuestRewardType.DIAMONDS) {
            return QUEST_REWARD_GEMS;
        }
        if (type == QuestRewardType.SEED_PACKS) {
            return QUEST_REWARD_PINATA;
        }
        return QUEST_ICON_PLANT;
    }

    private String questIconAsset(Quest quest) {
        QuestCondition condition = quest.getCondition();
        switch (condition) {
        case KILL_ZOMBIES_IN_CHAPTER:
            return chapterQuestIcon(quest.getParameter());
        case KILL_TEN_WITHIN_THIRTY_SECONDS:
        case KILL_IN_FIRST_COLUMN_WITHOUT_MOWER:
            return QUEST_ICON_ZOMBIE;
        case USE_THREE_EXPLOSIVE_PLANTS:
            return QUEST_ICON_POWERUPS;
        case WIN_FIVE_AT_MAXIMUM_DIFFICULTY:
            return QUEST_ICON_LEVELUP;
        case WIN_DAY_LEVEL_WITH_SHROOMS:
            return QUEST_ICON_DARK_AGES;
        default:
            return QUEST_ICON_PLANT;
        }
    }

    private String chapterQuestIcon(String chapterId) {
        if ("ancient-egypt".equals(chapterId)) {
            return "IMAGE_UI_QUESTS_QUESTICONS_EGYPT";
        }
        if ("frostbite-caves".equals(chapterId)) {
            return "IMAGE_UI_QUESTS_QUESTICONS_FROSTBITECAVES";
        }
        if ("big-wave-beach".equals(chapterId)) {
            return "IMAGE_UI_QUESTS_QUESTICONS_BIGWAVEBEACH";
        }
        if ("dark-ages".equals(chapterId)) {
            return "IMAGE_UI_QUESTS_QUESTICONS_DARKAGES";
        }
        return QUEST_ICON_ZOMBIE;
    }

    private ImageButton assetImageButton(String normalAsset,
            String pressedAsset) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(requireAssetRegion(normalAsset));
        style.imageDown = new TextureRegionDrawable(requireAssetRegion(pressedAsset));
        style.imageOver = style.imageDown;
        ImageButton button = new ImageButton(style);
        button.getImage().setScaling(Scaling.fit);
        return button;
    }

    private void closeQuestModal() {
        if (questModal == null) {
            return;
        }
        questModal.remove();
        questModal = null;
        questPanel = null;
        root.setTouchable(Touchable.enabled);
    }

    private ImageButton createCarouselButton(String style, int direction) {
        ImageButton button = new ImageButton(skin, style);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int nextIndex = selectedIndex + direction;
                if (nextIndex < 0 || nextIndex >= chapters.size()) {
                    return;
                }
                selectedIndex = nextIndex;
                rebuildContent();
            }
        });
        return button;
    }

    private Table createChapterSlot(int index, boolean focused) {
        if (index < 0 || index >= chapters.size()) {
            return new Table();
        }

        Chapter chapter = chapters.get(index);
        AdventureProgress progress = AdventureSession.getInstance().getProgress();
        boolean unlocked = progress.isChapterUnlocked(chapter);

        Table slot = new Table();
        float logoWidth = focused ? 325f : 205f;
        float logoHeight = focused ? 285f : 190f;
        float lockSize = focused ? 92f : 66f;

        Stack artwork = new Stack();
        Image logo = createAssetImage(getChapterLogoAsset(chapter));
        logo.setScaling(Scaling.fit);
        if (!unlocked) {
            logo.setColor(0.62f, 0.62f, 0.62f, 1f);
        }
        artwork.add(logo);
        if (!unlocked) {
            artwork.add(centeredLock(lockSize));
        }

        slot.add(artwork).width(logoWidth).height(logoHeight).row();

        Label name = new Label(chapter.getDisplayName(), skin,
                focused ? "medium_outline" : "medium");
        slot.add(name).padTop(focused ? 3f : 0f).row();

        Label completion = new Label(
                progress.getCompletedLevelCount(chapter)
                        + "/" + chapter.getLevelCount(),
                skin, "secondary");
        slot.add(completion).row();

        return slot;
    }

    private Table createLevelIsland(Chapter chapter, Level level,
            AdventureProgress progress, int visualIndex) {
        boolean unlocked = progress.isLevelUnlocked(chapter, level.getNumber());
        boolean completed = progress.isLevelCompleted(chapter, level.getNumber());

        Table island = new Table();

        Stack artwork = new Stack();
        Image islandImage = createAssetImage(LEVEL_ISLAND);
        islandImage.setScaling(Scaling.fit);
        if (!unlocked) {
            islandImage.setColor(0.60f, 0.60f, 0.60f, 1f);
        }
        artwork.add(islandImage);

        Table numberOverlay = new Table();
        numberOverlay.top();
        Label number = new Label(Integer.toString(level.getNumber()),
                skin, "medium_outline");
        number.setColor(completed ? Color.GREEN : Color.WHITE);
        numberOverlay.add(number).padTop(8f);
        artwork.add(numberOverlay);

        if (!unlocked) {
            artwork.add(centeredLock(62f));
        }

        float islandWidth = visualIndex == 1 || visualIndex == 3 ? 190f : 178f;
        float islandHeight = visualIndex == 1 || visualIndex == 3 ? 145f : 135f;
        island.add(artwork).width(islandWidth).height(islandHeight).row();

        Label levelLabel = new Label("Level " + level.getNumber(),
                skin, "medium_outline");
        island.add(levelLabel).padTop(2f).row();

        Label name = new Label(level.getName(), skin, "secondary");
        name.setWrap(true);
        island.add(name).width(205f).padTop(1f).row();

        String status = completed ? "Completed"
                : unlocked ? "Unlocked" : "Locked";
        Label statusLabel = new Label(status, skin, "secondary");
        statusLabel.setColor(completed
                ? Color.GREEN
                : unlocked ? Color.WHITE : Color.LIGHT_GRAY);
        island.add(statusLabel).padTop(1f).row();

        if (unlocked) {
            TextButton play = new TextButton("Play", skin, "green");
            play.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    event.stop();
                    AdventureSession.getInstance().selectChapter(chapter);
                    navigator.showLevelGamePreview(chapter, level);
                }
            });
            island.add(play).width(112f).height(38f).padTop(4f);
        }
        return island;
    }

    private Table centeredLock(float size) {
        Table overlay = new Table();
        overlay.center();
        PamAnimationActor lock = new PamAnimationActor(
                navigator.getPamPlayer(), LOCK_PAM, LOCK_CLIP);
        overlay.add(lock).size(size);
        return overlay;
    }

    private void setButtonEnabled(Button button, boolean enabled) {
        button.setDisabled(!enabled);
        button.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
        button.getColor().a = enabled ? 1f : 0.42f;
    }

    private String buildChapterProgressText(Chapter chapter,
            AdventureProgress progress) {
        return chapter.getDisplayName() + "  -  "
                + progress.getCompletedLevelCount(chapter)
                + " / " + chapter.getLevelCount()
                + " completed";
    }

    private String getChapterLogoAsset(Chapter chapter) {
        if ("ancient-egypt".equals(chapter.getId())) {
            return "IMAGE_WORLDMAP_EGYPT_ISLAND14";
        }
        if ("frostbite-caves".equals(chapter.getId())) {
            return "IMAGE_WORLDMAP_ICEAGE_ISLAND1";
        }
        if ("big-wave-beach".equals(chapter.getId())) {
            return "IMAGE_WORLDMAP_BEACH_ISLAND1";
        }
        if ("dark-ages".equals(chapter.getId())) {
            return "IMAGE_WORLDMAP_DARK_ANIM22_ANIM22_534X1169";
        }
        return "IMAGE_WORLDMAP_EGYPT_ISLAND14";
    }
}
