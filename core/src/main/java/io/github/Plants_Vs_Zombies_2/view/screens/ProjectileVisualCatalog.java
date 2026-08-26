package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.Locale;

import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.LobbedProjectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.Projectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.ProjectileEffect;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.effect.StunEffect;

/** Maps Phase-1 projectile entities to matching PvZ2 projectile PAM artwork. */
final class ProjectileVisualCatalog {
    private static final String PEA_PROJECTILE_PATH =
            "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM";
    private static final String FIRE_PEA_PROJECTILE_PATH =
            "768/INITIAL/EFFECTS/T_FIRE_PEA/T_FIRE_PEA.PAM";

    private ProjectileVisualCatalog() {
    }

    static Preview find(Projectile projectile) {
        if (projectile == null) {
            return null;
        }
        String source = normalize(projectile.getSourcePlantName());

        // Fire Peashooter shots and pea-family projectiles ignited by
        // Torchwood are a complete projectile visual of their own. Do not
        // layer the fire PAM over a normal pea; T_FIRE_PEA already contains
        // the full fire-pea artwork.
        if (projectile.hasFireEffect() && isPeaFamilySource(source)) {
            return preview(FIRE_PEA_PROJECTILE_PATH, "animation", 0.48f);
        }

        if (source.contains("citron")) {
            return preview("768/FULL/EFFECTS/T_CITRON_CITRUS_ORB/"
                    + "T_CITRON_CITRUS_ORB.PAM",
                    "Citron_Citrus_Orb", 0.66f);
        }
        if (source.contains("cabbagepult")) {
            return preview("768/INITIAL/EFFECTS/T_CABBAGEPULT_PROJECTILE/"
                    + "T_CABBAGEPULT_PROJECTILE.PAM", "animation", 0.54f);
        }
        if (source.contains("kernelpult")) {
            if (hasStunEffect(projectile)) {
                // Kernel-pult's butter is a separate atlas image in the PvZ2
                // resources, rather than the normal spinning kernel PAM.
                return imagePreview("IMAGE_EFFECTS_KERNELPULT_PROJECTILE_BUTTER",
                        0.46f);
            }
            return preview("768/INITIAL/EFFECTS/T_KERNALPULT_PROJECTILE/"
                    + "T_KERNALPULT_PROJECTILE.PAM", "animation", 0.46f);
        }
        if (source.contains("wintermelon")) {
            return preview("768/FULL/EFFECTS/T_WINTERMELON_PROJECTILE/"
                    + "T_WINTERMELON_PROJECTILE.PAM", "animation", 0.64f);
        }
        if (source.contains("melonpult")) {
            return preview("768/INITIAL/EFFECTS/T_MELON_PROJECTILE/"
                    + "T_MELON_PROJECTILE.PAM", "animation", 0.64f);
        }
        if (source.contains("pepperpult")) {
            return preview("768/FULL/EFFECTS/T_PEPPERPULT_PROJECTILE/"
                    + "T_PEPPERPULT_PROJECTILE.PAM", "animation", 0.58f);
        }
        if (source.contains("cactus")) {
            return preview("768/INITIAL/EFFECTS/T_CACTUS_PROJECTILE/"
                    + "T_CACTUS_PROJECTILE.PAM", "idle", 0.44f);
        }
        if (source.contains("fumeshroom")) {
            // Fume-shroom's model projectile is piercing. The closest small
            // projectile PAM in the supplied asset set is the puff projectile;
            // its movement still comes entirely from the Phase-1 entity.
            return preview("768/INITIAL/EFFECTS/T_PUFFSHROOM_PROJECTILE/"
                    + "T_PUFFSHROOM_PROJECTILE.PAM", "animation", 0.58f);
        }
        if (source.contains("cattail")) {
            return preview("768/INITIAL/EFFECTS/T_HOMING_THISTLE_PROJECTILE/"
                    + "T_HOMING_THISTLE_PROJECTILE.PAM", "animation", 0.46f);
        }
        if (source.contains("caulipower")) {
            return preview("768/INITIAL/EFFECTS/CAULIPOWER_PROJECTILE/"
                    + "CAULIPOWER_PROJECTILE.PAM", "animation", 0.52f);
        }
        if (source.contains("electricblueberry")) {
            return preview("768/INITIAL/EFFECTS/"
                    + "ELECTRICBLUEBERRY_CLOUD_PROJECTILE/"
                    + "ELECTRICBLUEBERRY_CLOUD_PROJECTILE.PAM",
                    "idle", 0.70f);
        }
        if (source.contains("bowlingbulb")) {
            return preview("768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE1/"
                    + "BOWLINGBULB_PROJECTILE1.PAM", "animation", 0.56f);
        }
        if (source.contains("rotobaga")) {
            return preview("768/FULL/EFFECTS/ROTORUTABAGA_PROJECTILE1/"
                    + "ROTORUTABAGA_PROJECTILE1.PAM", "animation", 0.42f);
        }
        if (source.contains("starfruit")) {
            return preview("768/INITIAL/EFFECTS/T_STARFRUIT_PROJECTILE/"
                    + "T_STARFRUIT_PROJECTILE.PAM", "animation", 0.48f);
        }
        if (source.contains("goopeashooter")) {
            return preview("768/INITIAL/EFFECTS/GOOPEASHOOTER_PROJECTILES/"
                    + "GOOPEASHOOTER_PROJECTILES.PAM", "projectile_t1", 0.45f);
        }
        if (source.contains("snowpea") || projectile.hasChillEffect()) {
            return preview("768/INITIAL/EFFECTS/T_SNOW_PEA/T_SNOW_PEA.PAM",
                    "animation", 0.44f);
        }
        if (source.contains("seashroom") || source.contains("puffshroom")) {
            return preview("768/INITIAL/EFFECTS/T_PUFFSHROOM_PROJECTILE/"
                    + "T_PUFFSHROOM_PROJECTILE.PAM", "animation", 0.46f);
        }

        // SLINGPEA_PROJECTILE belongs to Sling Pea and its very short tier
        // clips can contain transient/empty frames. Using it for ordinary
        // Peashooter shots made the projectile seem to disappear at repeatable
        // points along its path even though the model projectile kept moving.
        // T_PEA_PROJECTILE is the stable normal pea visual.
        if (isPeaFamilySource(source)) {
            return preview(PEA_PROJECTILE_PATH, "animation", 0.46f);
        }

        // Unknown direct projectiles retain a safe generic projectile visual
        // rather than silently disappearing from the board.
        return preview(PEA_PROJECTILE_PATH, "animation",
                projectile instanceof LobbedProjectile ? 0.50f : 0.46f);
    }

    static Preview grape() {
        return preview("768/INITIAL/EFFECTS/GRAPESHOT_PROJECTILE/"
                + "GRAPESHOT_PROJECTILE.PAM", "animation_forward", 0.46f);
    }

    static boolean isPeaFamilyProjectile(Projectile projectile) {
        return projectile != null
                && isPeaFamilySource(normalize(projectile.getSourcePlantName()));
    }

    private static boolean hasStunEffect(Projectile projectile) {
        for (ProjectileEffect effect : projectile.getEffects()) {
            if (effect instanceof StunEffect) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPeaFamilySource(String source) {
        return source.contains("peashooter")
                || source.contains("repeater")
                || source.contains("threepeater")
                || source.contains("peapod")
                || source.contains("splitpea")
                || source.contains("megagatlingpea")
                || source.contains("snowpea");
    }

    private static Preview preview(String path, String clip, float sizeTiles) {
        return new Preview(path, clip, null, sizeTiles);
    }

    private static Preview imagePreview(String imageId, float sizeTiles) {
        return new Preview(null, null, imageId, sizeTiles);
    }

    private static String normalize(String value) {
        return value == null ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    static final class Preview {
        private final String path;
        private final String clip;
        private final String imageId;
        private final float sizeTiles;

        private Preview(String path, String clip, String imageId,
                float sizeTiles) {
            this.path = path;
            this.clip = clip;
            this.imageId = imageId;
            this.sizeTiles = sizeTiles;
        }

        String getPath() {
            return path;
        }

        String getClip() {
            return clip;
        }

        String getImageId() {
            return imageId;
        }

        boolean isStaticImage() {
            return imageId != null;
        }

        float getSizeTiles() {
            return sizeTiles;
        }
    }
}
