package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.model.roadmap.AdventureProgress;
import io.github.Plants_Vs_Zombies_2.model.roadmap.AdventureSession;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Chapter;
import io.github.Plants_Vs_Zombies_2.model.roadmap.ChapterCatalog;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Level;

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

    private static final float LEVEL_MAP_WIDTH = 1080f;
    private static final float LEVEL_MAP_HEIGHT = 420f;

    private static final float[] LEVEL_X = {45f, 310f, 590f, 855f};
    private static final float[] LEVEL_Y = {45f, 185f, 80f, 200f};

    private final List<Chapter> chapters;
    private int selectedIndex;
    private boolean showingLevels;

    public AdventureScreen(ScreenNavigator navigator) {
        super(navigator, "Adventure");
        chapters = ChapterCatalog.getChapters();
        selectedIndex = Math.max(0,
                chapters.indexOf(AdventureSession.getInstance()
                        .getSelectedChapter()));
        showingLevels = false;

        addReturnToCurrentMenuButton("Back");
        rebuildContent();
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
        TextButton chapterBack = new TextButton("Back to Chapters", skin, "brown");
        chapterBack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showingLevels = false;
                rebuildContent();
            }
        });
        titleRow.add(chapterBack).width(190f).height(48f).left();

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
        slot.add(completion);
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
        island.add(statusLabel).padTop(1f);
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
