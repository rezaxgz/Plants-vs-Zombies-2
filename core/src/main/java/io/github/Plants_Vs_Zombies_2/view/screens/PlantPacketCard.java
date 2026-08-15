package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollectionItem;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantCategory;

/**
 * Reusable PvZ2-style seed-packet card used by plant collection lists.
 *
 * <p>The phase-two handout uses the same visual language for the collection,
 * plant selection and in-game seed packet lists. Keeping the packet itself in
 * one actor makes it possible to reuse this exact card in those later screens
 * rather than rebuilding the overlays three times.</p>
 */
final class PlantPacketCard extends Table {
    static final float WIDTH = 108f;
    static final float PACKET_HEIGHT = 68f;
    static final float TOTAL_HEIGHT = 94f;

    private static final String EMPTY_PACKET =
            "IMAGE_UI_PACKETS_EMPTY_PACKET";
    private static final String LOCK_SMALL =
            "IMAGE_UI_PACKETS_LOCK_SMALL";
    private static final String PROGRESS_STYLE = "xp_green";
    private static final String FALLBACK_IMAGE = "IMAGE_MISSING_IMAGE";

    private static final Map<String, String> PACKET_OVERRIDES =
            createPacketOverrides();

    private final ScreenNavigator navigator;
    private final Skin skin;
    private final PlantCollectionItem plant;

    PlantPacketCard(ScreenNavigator navigator,
            PlantCollectionItem plant) {
        if (navigator == null || plant == null) {
            throw new IllegalArgumentException(
                    "navigator and plant are required");
        }
        this.navigator = navigator;
        this.skin = navigator.getSkin();
        this.plant = plant;

        setTouchable(Touchable.enabled);
        addListener(new TextTooltip(buildTooltip(), skin));
        build();
    }

    private void build() {
        defaults().pad(0f);

        Stack packet = new Stack();
        packet.add(createPacketBackground());
        packet.add(createPlantArtworkLayer());
        packet.add(createBadgeLayer());

        add(packet).size(WIDTH, PACKET_HEIGHT).row();
        add(createSeedProgress()).width(WIDTH - 8f).height(20f)
                .padTop(1f);
    }

    private Image createPacketBackground() {
        Image background = image(EMPTY_PACKET);
        background.setScaling(Scaling.stretch);
        if (!plant.isUnlocked()) {
            background.setColor(0.48f, 0.48f, 0.48f, 0.72f);
        }
        return background;
    }

    private Table createPlantArtworkLayer() {
        Table artwork = new Table();
        artwork.bottom();

        Image plantImage = image(packetAssetFor(plant.getName()));
        plantImage.setScaling(Scaling.fit);
        if (!plant.isUnlocked()) {
            plantImage.setColor(0.42f, 0.42f, 0.42f, 0.42f);
        }
        artwork.add(plantImage).width(86f).height(57f).padBottom(1f);
        return artwork;
    }

    private Table createBadgeLayer() {
        Table badges = new Table();
        badges.top();

        Image category = image(categoryIconFor(plant.getCategory()));
        category.setScaling(Scaling.fit);
        if (!plant.isUnlocked()) {
            category.setColor(0.55f, 0.55f, 0.55f, 0.65f);
        }
        category.addListener(new TextTooltip(
                prettyCategory(plant.getCategory()), skin));

        badges.add(category).size(21f).left().top().pad(3f)
                .expandX();

        if (plant.isUnlocked()) {
            Label level = new Label(
                    "Lv" + plant.getCurrentLevel(), skin, "medium_outline");
            level.setFontScale(0.68f);
            badges.add(level).right().top().padTop(4f).padRight(4f);
        } else {
            Image lock = image(LOCK_SMALL);
            lock.setScaling(Scaling.fit);
            badges.add(lock).size(21f, 28f).right().top()
                    .padTop(1f).padRight(2f);
        }
        return badges;
    }

    private Stack createSeedProgress() {
        int required = plant.getCardsNeededForNextLevel();
        int collected = plant.getTotalCardsCollected();
        boolean maximum = plant.isAtMaximumLevel();

        float maximumValue = maximum ? 1f : Math.max(1, required);
        float currentValue = maximum
                ? 1f
                : Math.min(collected, maximumValue);

        ProgressBar bar = new ProgressBar(
                0f, maximumValue, 1f, false, skin, PROGRESS_STYLE);
        bar.setValue(currentValue);
        bar.setAnimateDuration(0f);

        Label count = new Label(maximum
                ? collected + "/MAX"
                : collected + "/" + required,
                skin, "medium_outline");
        count.setFontScale(0.54f);

        Table labelLayer = new Table();
        labelLayer.add(count).center();

        Stack stack = new Stack();
        stack.add(bar);
        stack.add(labelLayer);
        return stack;
    }

    private Image image(String imageId) {
        Image image = new Image(region(imageId));
        image.setScaling(Scaling.fit);
        return image;
    }

    private TextureRegion region(String imageId) {
        TextureRegion region = navigator.getTextureBank().region(imageId);
        if (region != null) {
            return region;
        }
        TextureRegion fallback = navigator.getTextureBank().region(
                FALLBACK_IMAGE);
        if (fallback == null) {
            throw new IllegalStateException(
                    "libPVZ could not resolve UI image: " + imageId);
        }
        return fallback;
    }

    private String buildTooltip() {
        String state = plant.isUnlocked()
                ? "Level " + plant.getCurrentLevel()
                : "Locked";
        return plant.getName() + "\n"
                + prettyCategory(plant.getCategory()) + "\n"
                + state;
    }

    private static String packetAssetFor(String plantName) {
        String normalized = normalizeAssetName(plantName);
        String override = PACKET_OVERRIDES.get(normalized);
        return override != null
                ? override
                : "IMAGE_UI_PACKETS_" + normalized;
    }

    private static String categoryIconFor(PlantCategory category) {
        switch (category) {
            case SUN_PRODUCER:
                return "IMAGE_UI_PACKETS_ENLIGHTENMINT";
            case SHOOTER:
                return "IMAGE_UI_PACKETS_APPEASEMINT";
            case HOMING:
                // The project has a custom Cat-tail family with no direct
                // official packet icon, so Contain-mint is the closest
                // visually distinct mint-family badge in the supplied assets.
                return "IMAGE_UI_PACKETS_CONTAINMINT";
            case STRIKE_THROUGH:
                return "IMAGE_UI_PACKETS_SPEARMINT";
            case LOBBER:
                return "IMAGE_UI_PACKETS_ARMAMINT";
            case EXPLOSIVE:
                return "IMAGE_UI_PACKETS_BOMBARDMINT";
            case MELEE:
                return "IMAGE_UI_PACKETS_ENFORCEMINT";
            case WALL_NUT:
                return "IMAGE_UI_PACKETS_REINFORCEMINT";
            case MODIFIER:
                return "IMAGE_UI_PACKETS_ENCHANTMINT";
            default:
                return "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_FAMILY_LARGE";
        }
    }

    private static String prettyCategory(PlantCategory category) {
        String raw = category.name().toLowerCase(Locale.ROOT)
                .replace('_', ' ');
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

    private static String normalizeAssetName(String name) {
        if (name == null) {
            return "";
        }
        return name.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
    }

    private static Map<String, String> createPacketOverrides() {
        Map<String, String> result = new HashMap<>();
        result.put("CHERRYBOMB", "IMAGE_UI_PACKETS_CHERRY_BOMB");
        result.put("ICEBERGLETTUCE", "IMAGE_UI_PACKETS_ICEBURG");
        result.put("ROTOBAGA", "IMAGE_UI_PACKETS_XSHOT");
        result.put("GOOPEASHOOTER", "IMAGE_UI_PACKETS_POISONPEASHOOTER");
        result.put("MEGAGATLINGPEA", "IMAGE_UI_PACKETS_MEGAGATLING");

        // Cat-tail and catTail-mint are project-specific plants and do not
        // have matching IMAGE_UI_PACKETS entries in the supplied resource
        // database. Use visually related official assets until dedicated art
        // is supplied.
        result.put("CATTAIL", "IMAGE_UI_PACKETS_HOMINGTHISTLE");
        result.put("CATTAILMINT", "IMAGE_UI_PACKETS_CONTAINMINT");

        result.put("PIERCEMINT", "IMAGE_UI_PACKETS_SPEARMINT");
        return result;
    }
}
