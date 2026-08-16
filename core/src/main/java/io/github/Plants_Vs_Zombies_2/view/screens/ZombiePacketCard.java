package io.github.Plants_Vs_Zombies_2.view.screens;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.model.collections.zombies.ZombieCollectionItem;

/** PvZ2 Almanac-style zombie discovery card. */
final class ZombiePacketCard extends Table {
    static final float WIDTH = 92f;
    static final float HEIGHT = 132f;

    private static final String READY_BACKGROUND =
            "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_READY";
    private static final String FALLBACK_IMAGE = "IMAGE_MISSING_IMAGE";

    private final ScreenNavigator navigator;
    private final ZombieCollectionItem zombie;

    ZombiePacketCard(ScreenNavigator navigator,
            ZombieCollectionItem zombie) {
        if (navigator == null || zombie == null) {
            throw new IllegalArgumentException(
                    "navigator and zombie are required");
        }
        this.navigator = navigator;
        this.zombie = zombie;

        setTouchable(zombie.isUnlocked()
                ? Touchable.enabled : Touchable.disabled);
        addListener(new TextTooltip(zombie.isUnlocked()
                ? prettyZombieName(zombie)
                : "Undiscovered Zombie", navigator.getSkin()));
        build();
    }

    private void build() {
        Stack packet = new Stack();

        Image background = image(READY_BACKGROUND);
        background.setScaling(Scaling.stretch);
        packet.add(background);

        // The original Almanac leaves undiscovered zombies as empty framed
        // cards. Do not reveal their art until the player has encountered them.
        if (zombie.isUnlocked()) {
            Table artLayer = new Table();
            Image art = image(ZombieVisualCatalog.packetAssetFor(zombie));
            art.setScaling(Scaling.fit);
            artLayer.add(art).width(WIDTH - 10f).height(HEIGHT - 12f);
            packet.add(artLayer);
        }

        add(packet).size(WIDTH, HEIGHT);
    }

    private Image image(String imageId) {
        return new Image(region(imageId));
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

    static String prettyZombieName(ZombieCollectionItem zombie) {
        String type = zombie.getTypeName();
        if ("BASIC".equals(type)) {
            return "Basic Zombie";
        }
        if ("CONEHEAD".equals(type)) {
            return "Conehead Zombie";
        }
        if ("BUCKETHEAD".equals(type)) {
            return "Buckethead Zombie";
        }
        if ("BRICKHEAD".equals(type)) {
            return "Brickhead Zombie";
        }
        if ("FLAG".equals(type)) {
            return "Flag Zombie";
        }
        if ("IMP".equals(type)) {
            return "Imp";
        }
        if ("GARGANTUAR".equals(type)) {
            return "Gargantuar";
        }

        String[] words = type.toLowerCase(java.util.Locale.ROOT)
                .split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }
        if (!result.toString().toLowerCase(java.util.Locale.ROOT)
                .contains("zomboss")
                && !result.toString().toLowerCase(java.util.Locale.ROOT)
                        .contains("gargantuar")
                && !result.toString().toLowerCase(java.util.Locale.ROOT)
                        .contains("imp")
                && !result.toString().toLowerCase(java.util.Locale.ROOT)
                        .contains("weasel")
                && !result.toString().toLowerCase(java.util.Locale.ROOT)
                        .contains("dodo")
                && !result.toString().toLowerCase(java.util.Locale.ROOT)
                        .contains("pet")) {
            result.append(" Zombie");
        }
        return result.toString();
    }
}
