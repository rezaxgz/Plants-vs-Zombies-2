package io.github.Plants_Vs_Zombies_2.view.screens;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class Stage12GraphicalBoundaryTest {
    @Test
    void liveBoardUsesTheExistingGameRendererWithoutStatusOverlays()
            throws IOException {
        String multiplayerSource = source("MultiplayerIZombieGameScreen.java");
        String gameSource = source("GameScreen.java");

        assertTrue(multiplayerSource.contains("extends GameScreen"));
        assertTrue(gameSource.contains("MultiplayerIZombieRenderModel"));
        assertTrue(gameSource.contains("refreshPlantedPlantLayerIfNeeded()"));
        assertTrue(gameSource.contains("refreshZombieRendering()"));
        assertTrue(gameSource.contains("refreshProjectileRendering()"));
        assertTrue(gameSource.contains("PamAnimationActor"));
        assertFalse(multiplayerSource.contains("boardStack"));
        assertFalse(multiplayerSource.contains("Connection:"));
        assertFalse(multiplayerSource.contains("Opponent:"));
        assertFalse(multiplayerSource.contains("getSimulationTick()"));
        assertFalse(multiplayerSource.contains("getRevision()"));
        assertFalse(gameSource.contains("TextField"));
    }

    @Test
    void graphicalScreensDoNotCreateNetworkOrPersistenceServices()
            throws IOException {
        for (String name : new String[] {
                "MultiplayerIZombieMenuScreen.java",
                "MultiplayerPregameScreen.java",
                "MultiplayerIZombieGameScreen.java",
                "LeaderboardScreen.java", "ProfileScreen.java"}) {
            String source = source(name);
            assertFalse(source.contains("new NetworkClient"), name);
            assertFalse(source.contains("UserManager"), name);
        }
    }

    @Test
    void desktopRuntimeResolvesTheSharedBundleFromRepositoryAssets()
            throws IOException {
        Path main = Path.of("src", "main", "java", "io", "github",
                "Plants_Vs_Zombies_2", "Main.java");
        Path desktopBuild = Path.of("..", "lwjgl3", "build.gradle")
                .normalize();
        String mainSource = Files.readString(main);
        String desktopSource = Files.readString(desktopBuild);

        assertTrue(mainSource.contains("\"pvz-assets\""));
        assertTrue(mainSource.contains("Gdx.files.internal(PVZ_ASSETS_ROOT)"));
        assertTrue(mainSource.contains("new TextureBank"));
        assertTrue(desktopSource.contains("rootProject.file('assets')"));
        assertFalse(mainSource.contains("C:\\\\Users\\\\"));
    }

    private static String source(String name) throws IOException {
        return Files.readString(Path.of("src", "main", "java", "io",
                "github", "Plants_Vs_Zombies_2", "view", "screens",
                name));
    }
}
