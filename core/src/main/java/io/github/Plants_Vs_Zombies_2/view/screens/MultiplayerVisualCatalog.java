package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFactory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.IZombieCard;
import io.github.Plants_Vs_Zombies_2.model.game.minigame.IZombieLevel;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionType;
import io.github.Plants_Vs_Zombies_2.view.presentation.Phase3Text;

/**
 * Authoritative mapping from multiplayer wire identifiers to verified PvZ2
 * artwork. The catalog owns no textures; screens borrow regions from the
 * application TextureBank and PAM data from the application PamPlayer.
 */
public final class MultiplayerVisualCatalog {
    public static final String LAWN_ASSET =
            "IMAGE_BACKGROUNDS_EGYPT_TEXTURE";
    public static final String SUN_RESOURCE_ASSET =
            "IMAGE_EFFECTS_SUN_SUN_78X78";
    public static final String BRAIN_PAM =
            "768/FULL/ZOMBIE/POWER_BRAIN_PROJECTILE/"
                    + "POWER_BRAIN_PROJECTILE.PAM";
    public static final String BRAIN_CLIP = "animation";
    public static final String BRAIN_FALLBACK_ASSET =
            "IMAGE_EFFECTS_PRIZE_PINATA_VALENBRAINZ_"
                    + "PRIZE_PINATA_VALENBRAINZ_109X109";
    public static final String MISSING_ASSET = "IMAGE_MISSING_IMAGE";

    public record Visual(String canonicalType, String displayName,
            String packetAsset, String pamPath, String clip,
            String fallbackLabel) {
        public Visual {
            if (!Phase3Text.hasText(canonicalType)
                    || !Phase3Text.hasText(displayName)
                    || !Phase3Text.hasText(fallbackLabel)) {
                throw new IllegalArgumentException(
                        "visual identifiers and fallback must be readable");
            }
        }

        public boolean hasPam() {
            return Phase3Text.hasText(pamPath) && Phase3Text.hasText(clip);
        }

        public boolean hasPacketAsset() {
            return Phase3Text.hasText(packetAsset);
        }
    }

    /** Graphical presentation for the server-owned per-lane brain state. */
    public record BrainVisual(Visual artwork, float red, float green,
            float blue, float alpha) {
    }

    private static final List<String> PLANT_TYPES = List.of(
            "Peashooter", "Sunflower", "Wall-nut", "Potato Mine",
            "Cabbage-pult");
    private static final List<String> ZOMBIE_TYPES = List.of(
            "BASIC", "CONEHEAD", "BUCKETHEAD", "IMP", "NEWSPAPER");
    private static final Map<String, Visual> PLANTS = createPlants();
    private static final Map<String, Visual> ZOMBIES = createZombies();
    private static final Map<String, Visual> PROJECTILES =
            createProjectiles();
    private static final Map<MatchReactionType, String> REACTION_ASSETS =
            createReactionAssets();

    private MultiplayerVisualCatalog() {
    }

    public static List<String> plantTypes() {
        return PLANT_TYPES;
    }

    public static List<String> zombieTypes() {
        return ZOMBIE_TYPES;
    }

    public static Visual plant(String canonicalType) {
        Visual fixed = PLANTS.get(normalize(canonicalType));
        if (fixed != null) return fixed;
        BasePlant plant = PlantFactory.createPlant(canonicalType,
                new EntityPosition(0, 0));
        if (plant == null) return null;
        PlantAnimationCatalog.Preview preview =
                PlantAnimationCatalog.find(plant.getName());
        return new Visual(plant.getName(), plant.getName(),
                PlantPacketCard.packetAssetFor(plant.getName()),
                preview == null ? null : preview.getPath(),
                preview == null ? null : preview.getClip(), plant.getName());
    }

    public static Visual zombie(String canonicalType) {
        return ZOMBIES.get(normalize(canonicalType));
    }

    public static Visual projectile(String canonicalType) {
        Visual fixed = PROJECTILES.get(normalize(canonicalType));
        if (fixed != null) return fixed;
        if (!Phase3Text.hasText(canonicalType)
                || !canonicalType.endsWith("_PROJECTILE")) return null;
        String source = canonicalType.substring(0,
                canonicalType.length() - "_PROJECTILE".length());
        if (PlantFactory.createPlant(source,
                new EntityPosition(0, 0)) == null) return null;
        ProjectileVisualCatalog.Preview preview =
                ProjectileVisualCatalog.findSource(source);
        return new Visual(canonicalType, source + " projectile",
                preview.isStaticImage() ? preview.getImageId()
                        : PlantPacketCard.packetAssetFor(source),
                preview.isStaticImage() ? null : preview.getPath(),
                preview.isStaticImage() ? null : preview.getClip(),
                source + " projectile");
    }

    public static String reactionAsset(MatchReactionType type) {
        return type == null ? null : REACTION_ASSETS.get(type);
    }

    public static BrainVisual brain(boolean available) {
        Visual artwork = new Visual("BRAIN", "Brain", BRAIN_FALLBACK_ASSET,
                BRAIN_PAM, BRAIN_CLIP, "Brain");
        return available
                ? new BrainVisual(artwork, 1f, 1f, 1f, 1f)
                : new BrainVisual(artwork, 0.28f, 0.28f, 0.28f, 0.32f);
    }

    public static String roleIconAsset(MatchRole role) {
        Visual visual = role == MatchRole.PLANTS
                ? plant("Peashooter")
                : role == MatchRole.ZOMBIES ? zombie("BASIC") : null;
        return visual == null ? MISSING_ASSET : visual.packetAsset();
    }

    public static int plantCost(String canonicalType) {
        BasePlant plant = PlantFactory.createPlant(canonicalType,
                new EntityPosition(0, 0));
        return plant == null ? -1 : plant.getCost();
    }

    public static int zombieCost(String canonicalType) {
        IZombieCard card = IZombieLevel.FIRST_BITE.findCard(canonicalType);
        return card == null ? -1 : card.getCost();
    }

    private static Map<String, Visual> createPlants() {
        Map<String, Visual> result = new LinkedHashMap<>();
        for (String type : PLANT_TYPES) {
            PlantAnimationCatalog.Preview preview =
                    PlantAnimationCatalog.find(type);
            result.put(normalize(type), new Visual(type, type,
                    PlantPacketCard.packetAssetFor(type),
                    preview == null ? null : preview.getPath(),
                    preview == null ? null : preview.getClip(), type));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Visual> createZombies() {
        Map<String, Visual> result = new LinkedHashMap<>();
        for (String type : ZOMBIE_TYPES) {
            ZombieVisualCatalog.Visual source = ZombieVisualCatalog.find(
                    ZombieType.valueOf(type));
            String display = Phase3Text.prettyIdentifier(type, "Zombie")
                    + ("BASIC".equals(type) ? " Zombie" : "");
            result.put(normalize(type), new Visual(type, display,
                    source == null ? null : source.getPacketAsset(),
                    source == null ? null : source.getPamPath(),
                    source == null ? null : source.getIdleClip(), display));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Visual> createProjectiles() {
        Map<String, Visual> result = new LinkedHashMap<>();
        addProjectile(result, "Peashooter_PROJECTILE", "Pea",
                "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/"
                        + "T_PEA_PROJECTILE.PAM", "animation",
                "IMAGE_UI_PACKETS_PEASHOOTER");
        addProjectile(result, "Cabbage-pult_PROJECTILE", "Cabbage",
                "768/INITIAL/EFFECTS/T_CABBAGEPULT_PROJECTILE/"
                        + "T_CABBAGEPULT_PROJECTILE.PAM", "animation",
                "IMAGE_UI_PACKETS_CABBAGEPULT");
        addProjectile(result, "Potato Mine_PROJECTILE", "Potato blast",
                "768/INITIAL/EFFECTS/POTATOMINE_EXPLOSION/"
                        + "POTATOMINE_EXPLOSION.PAM", "animation",
                "IMAGE_UI_PACKETS_POTATOMINE");
        addProjectile(result, "Sunflower_PROJECTILE", "Sun pulse", null,
                null, SUN_RESOURCE_ASSET);
        addProjectile(result, "Wall-nut_PROJECTILE", "Wall-nut impact",
                "768/INITIAL/PLANT/WALLNUT/WALLNUT.PAM", "idle",
                "IMAGE_UI_PACKETS_WALLNUT");
        return Collections.unmodifiableMap(result);
    }

    private static void addProjectile(Map<String, Visual> result,
            String type, String display, String path, String clip,
            String fallbackAsset) {
        result.put(normalize(type), new Visual(type, display, fallbackAsset,
                path, clip, display));
    }

    private static Map<MatchReactionType, String> createReactionAssets() {
        Map<MatchReactionType, String> result =
                new EnumMap<>(MatchReactionType.class);
        result.put(MatchReactionType.SMILE,
                "IMAGE_UI_PAUSEMENU_SUNFLOWER_TOPPER");
        result.put(MatchReactionType.LAUGH,
                "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_TUTORIAL_IMP");
        result.put(MatchReactionType.ANGRY,
                "IMAGE_UI_PENNY_PURSUITS_DIFFICULTY_MODIFIER_ICONS_"
                        + "DIFFICULTY_MODIFIER_ZOMBIE_LEVEL_ANGRY");
        return Collections.unmodifiableMap(result);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
