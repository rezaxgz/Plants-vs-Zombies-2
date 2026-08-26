package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.model.game.special.ProtectedPlantSpec;
import io.github.Plants_Vs_Zombies_2.model.game.special.TimedWarObjective;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Level;
import io.github.Plants_Vs_Zombies_2.model.roadmap.LevelKind;
import io.github.Plants_Vs_Zombies_2.model.roadmap.SpecialLevelConfig;
import io.github.Plants_Vs_Zombies_2.model.roadmap.SpecialLevelType;

/**
 * Phase-2 start-of-level objective card based on documentation image 15.
 * It blocks the game until CONTINUE is pressed and lists every Phase-1
 * restriction configured on the current adventure level.
 */
final class LevelObjectivesOverlay extends Group {
    private static final float PANEL_WIDTH = 680f;
    private static final float PANEL_HEIGHT = 350f;
    private static final float HEADER_HEIGHT = 68f;
    private static final float CONTINUE_WIDTH = 210f;
    private static final float CONTINUE_HEIGHT = 58f;

    private static final Color BACKDROP = new Color(0f, 0f, 0f, 0.24f);
    private static final Color HEADER = new Color(1f, 0.63f, 0.04f, 1f);
    private static final Color CONTENT = new Color(0.96f, 0.91f, 0.74f, 1f);
    private static final Color OBJECTIVE_TEXT = new Color(0.30f, 0.22f, 0.10f, 1f);
    private static final Color BULLET = new Color(0.83f, 0.91f, 1f, 1f);

    private Texture pixelTexture;
    private final Runnable onContinue;

    LevelObjectivesOverlay(Skin skin, Level level,
            float screenWidth, float screenHeight,
            Runnable onContinue) {
        this(skin, "Level Objectives", buildObjectives(level),
                screenWidth, screenHeight, onContinue);
    }

    LevelObjectivesOverlay(Skin skin, String title,
            List<String> objectives,
            float screenWidth, float screenHeight,
            Runnable onContinue) {
        if (skin == null || title == null || title.isBlank()
                || objectives == null || objectives.isEmpty()
                || onContinue == null) {
            throw new IllegalArgumentException(
                    "skin, title, objectives, and continue action are required");
        }
        this.onContinue = onContinue;
        setBounds(0f, 0f, screenWidth, screenHeight);
        setTouchable(Touchable.enabled);

        createPixelTexture();
        installInputBlocker();
        installCard(skin, title, objectives, screenWidth, screenHeight);
    }

    private void createPixelTexture() {
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        pixelTexture = new Texture(pixel);
        pixel.dispose();
    }

    private void installInputBlocker() {
        Image shade = new Image(solid(BACKDROP));
        shade.setScaling(Scaling.stretch);
        shade.setBounds(0f, 0f, getWidth(), getHeight());
        shade.setTouchable(Touchable.enabled);
        shade.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y,
                    int pointer, int button) {
                return true;
            }
        });
        addActor(shade);
    }

    private void installCard(Skin skin, String heading,
            List<String> objectives,
            float screenWidth, float screenHeight) {
        float panelX = (screenWidth - PANEL_WIDTH) * 0.5f;
        float panelY = (screenHeight - PANEL_HEIGHT) * 0.5f + 16f;

        Table frame = new Table();
        frame.setBackground(skin.getDrawable(
                "image_ui_dialog_asset_inner_bkgd_10"));
        frame.pad(19f);
        frame.setBounds(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        Table header = new Table();
        header.setBackground(solid(HEADER));
        Label title = new Label(heading, skin, "big_outline");
        title.setAlignment(Align.center);
        title.setFontScale(0.82f);
        header.add(title).grow();
        frame.add(header).growX().height(HEADER_HEIGHT).row();

        Table content = new Table();
        content.setBackground(solid(CONTENT));
        content.top().left().pad(24f, 30f, 38f, 30f);
        for (String objective : objectives) {
            addObjectiveRow(content, skin, objective);
        }
        frame.add(content).grow().row();
        addActor(frame);

        TextButton continueButton = new TextButton(
                "CONTINUE", skin, "purple");
        continueButton.setBounds(
                (screenWidth - CONTINUE_WIDTH) * 0.5f,
                panelY - CONTINUE_HEIGHT * 0.42f,
                CONTINUE_WIDTH, CONTINUE_HEIGHT);
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                LevelObjectivesOverlay.this.onContinue.run();
                LevelObjectivesOverlay.this.remove();
            }
        });
        addActor(continueButton);
    }

    private void addObjectiveRow(Table content, Skin skin, String text) {
        Label bullet = new Label("o", skin, "medium_outline");
        bullet.setColor(BULLET);
        bullet.setFontScale(1.12f);
        bullet.setAlignment(Align.center);

        Label objective = new Label(text, skin, "secondary");
        objective.setColor(OBJECTIVE_TEXT);
        objective.setFontScale(0.90f);
        objective.setWrap(true);
        objective.setAlignment(Align.left);

        content.add(bullet).width(28f).height(34f).top().padRight(7f);
        content.add(objective).growX().minHeight(34f).left().row();
    }

    private Drawable solid(Color color) {
        return new TextureRegionDrawable(
                new TextureRegion(pixelTexture)).tint(color);
    }

    private static List<String> buildObjectives(Level level) {
        if (level == null) {
            throw new IllegalArgumentException("level is required");
        }
        List<String> objectives = new ArrayList<>();
        SpecialLevelType type = level.getSpecialLevelType();
        SpecialLevelConfig config = level.getSpecialConfig();

        if (level.getKind() == LevelKind.BOSS) {
            objectives.add("Defeat Zomboss. Its health bar is divided into three phases, and it is stunned briefly when each phase is cleared.");
            objectives.add("Zomboss occupies two rows at once, so plants in either occupied row can damage it.");
            String chapterId = level.getChapterRuleset().name();
            if ("ANCIENT_EGYPT".equals(chapterId)) {
                objectives.add("Watch for missiles that destroy a plant and raise graves, and for the two-row rush attack.");
            } else if ("FROSTBITE_CAVES".equals(chapterId)) {
                objectives.add("The Mammoth stays in one pair of rows and does not summon normal zombies; expect ice missiles, icy wind, and frozen-zombie columns.");
            } else if ("BIG_WAVE_BEACH".equals(chapterId)) {
                objectives.add("Baby sharks eat plants in water, while the turbine pulls plants and zombies in its two rows toward the Shark's mouth.");
            } else if ("DARK_AGES".equals(chapterId)) {
                objectives.add("Dragon fireballs and fire breath destroy plants and leave tiles burning and unplantable for 4 seconds.");
            }
        }

        if (type == SpecialLevelType.DEAD_LINE) {
            objectives.add("Do not let any zombie cross the Dead Line at column "
                    + formatNumber(config.getDeadLineColumn()) + ".");
        } else {
            objectives.add("Do not let any zombie reach your house.");
        }

        switch (type) {
            case CONVEYOR_BELT:
                objectives.add("Use the plants supplied by the conveyor belt.");
                break;
            case LOCKED_PLANTS:
                objectives.add("Defend the lawn using only the provided plants: "
                        + String.join(", ", config.getPlantPool()) + ".");
                break;
            case SAVE_OUR_SEEDS:
                objectives.add(buildSaveOurSeedsObjective(config));
                break;
            case TIMED_WAR:
                objectives.add(buildTimedWarObjective(config));
                if (config.getMinimumCollectedSun() > 0) {
                    objectives.add("Collect at least "
                            + config.getMinimumCollectedSun()
                            + " sun during the level.");
                }
                break;
            case NIGHT_OPS:
                objectives.add("Survive without any sun falling from the sky.");
                break;
            case DEAD_LINE:
                break;
            case LOVE_YOUR_PLANTS:
                objectives.add("Do not lose more than "
                        + config.getMaximumLostPlants()
                        + " plants during the level.");
                break;
            case PLANT_WHAT_YOU_GET:
                objectives.add("Sun-producing plants are locked and no sun "
                        + "falls from the sky; use your starting sun wisely.");
                objectives.add("Plants have no recharge cooldown. Start each "
                        + "zombie wave yourself when you are ready.");
                break;
            case NONE:
                break;
            default:
                break;
        }
        return objectives;
    }

    private static String buildTimedWarObjective(SpecialLevelConfig config) {
        int seconds = Math.max(1,
                (int) Math.round(config.getDurationSeconds()));
        if (config.getTimedObjective() == TimedWarObjective.PRODUCE_SUN) {
            return "Produce at least " + config.getTarget()
                    + " sun within " + seconds + " seconds.";
        }
        return "Defeat at least " + config.getTarget()
                + " zombies within any " + seconds
                + "-second window.";
    }

    private static String buildSaveOurSeedsObjective(
            SpecialLevelConfig config) {
        List<ProtectedPlantSpec> plants = config.getProtectedPlants();
        if (plants.isEmpty()) {
            return "Protect every preset plant until the level is won.";
        }
        List<String> descriptions = new ArrayList<>();
        for (ProtectedPlantSpec plant : plants) {
            descriptions.add(plant.getPlantType() + " at row "
                    + (plant.getPosition().getRow() + 1) + ", column "
                    + (plant.getPosition().getColumn() + 1));
        }
        return "Protect every preset plant: "
                + String.join("; ", descriptions) + ".";
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public boolean remove() {
        boolean removed = super.remove();
        if (removed && pixelTexture != null) {
            pixelTexture.dispose();
            pixelTexture = null;
        }
        return removed;
    }
}
