package io.github.Plants_Vs_Zombies_2.view.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionKind;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionType;

class MultiplayerVisualCatalogTest {
    @Test
    void everyCanonicalCardHasArtworkAnimationAndServerCostMapping() {
        assertEquals(5, MultiplayerVisualCatalog.plantTypes().size());
        assertEquals(5, MultiplayerVisualCatalog.zombieTypes().size());

        for (String type : MultiplayerVisualCatalog.plantTypes()) {
            assertComplete(MultiplayerVisualCatalog.plant(type));
            assertTrue(MultiplayerVisualCatalog.plantCost(type) >= 0);
            assertComplete(MultiplayerVisualCatalog.projectile(
                    type + "_PROJECTILE"));
        }
        for (String type : MultiplayerVisualCatalog.zombieTypes()) {
            assertComplete(MultiplayerVisualCatalog.zombie(type));
            assertTrue(MultiplayerVisualCatalog.zombieCost(type) >= 0);
        }
    }

    @Test
    void fixedReactionCatalogHasThreeTextAndThreeGraphicalEmojiChoices() {
        MatchReactionType[] reactions = MatchReactionType.values();
        assertEquals(6, reactions.length);
        assertEquals(3, Arrays.stream(reactions)
                .filter(value -> value.getKind() == MatchReactionKind.TEXT)
                .count());
        assertEquals(3, Arrays.stream(reactions)
                .filter(value -> value.getKind() == MatchReactionKind.EMOJI)
                .count());
        Arrays.stream(reactions).forEach(value ->
                assertTrue(value.getDisplayText() != null
                        && !value.getDisplayText().isBlank()));
        Arrays.stream(reactions)
                .filter(value -> value.getKind() == MatchReactionKind.EMOJI)
                .forEach(value -> assertTrue(
                        MultiplayerVisualCatalog.reactionAsset(value) != null));
    }

    @Test
    void lawnResourcesAndBrainsUseNamedBundleAssets() {
        assertTrue(MultiplayerVisualCatalog.LAWN_ASSET.startsWith("IMAGE_"));
        assertTrue(MultiplayerVisualCatalog.SUN_RESOURCE_ASSET
                .startsWith("IMAGE_"));
        assertTrue(MultiplayerVisualCatalog.BRAIN_FALLBACK_ASSET
                .startsWith("IMAGE_"));
        assertTrue(MultiplayerVisualCatalog.BRAIN_PAM.endsWith(".PAM"));
        MultiplayerVisualCatalog.BrainVisual available =
                MultiplayerVisualCatalog.brain(true);
        MultiplayerVisualCatalog.BrainVisual consumed =
                MultiplayerVisualCatalog.brain(false);
        assertComplete(available.artwork());
        assertEquals(available.artwork(), consumed.artwork());
        assertTrue(consumed.alpha() < available.alpha());
    }

    @Test
    void unlockedLoadoutPlantsReuseTheFullSinglePlayerVisualCatalog() {
        MultiplayerVisualCatalog.Visual repeater =
                MultiplayerVisualCatalog.plant("Repeater");
        MultiplayerVisualCatalog.Visual projectile =
                MultiplayerVisualCatalog.projectile("Repeater_PROJECTILE");

        assertComplete(repeater);
        assertComplete(projectile);
        assertTrue(MultiplayerVisualCatalog.plantCost("Repeater") >= 0);
    }

    private static void assertComplete(MultiplayerVisualCatalog.Visual visual) {
        assertNotNull(visual);
        assertTrue(visual.canonicalType() != null
                && !visual.canonicalType().isBlank());
        assertTrue(visual.fallbackLabel() != null
                && !visual.fallbackLabel().isBlank());
        assertTrue(visual.hasPam() || visual.hasPacketAsset());
    }
}
