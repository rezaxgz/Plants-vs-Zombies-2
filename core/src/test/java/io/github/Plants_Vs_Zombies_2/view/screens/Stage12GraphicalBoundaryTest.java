package io.github.Plants_Vs_Zombies_2.view.screens;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class Stage12GraphicalBoundaryTest {
    @Test
    void liveBoardUsesGraphicalCatalogWithoutTextualBoardPlaceholders()
            throws IOException {
        String source = source("MultiplayerIZombieGameScreen.java");

        assertTrue(source.contains("MultiplayerVisualCatalog"));
        assertTrue(source.contains("PamAnimationActor"));
        assertTrue(source.contains("EntityReconciliation"));
        assertFalse(source.contains("new Label(\"*\""));
        assertFalse(source.contains("[B]"));
        assertFalse(source.contains("[eaten]"));
        assertFalse(source.contains("new Label(\"RED LINE\""));
        assertFalse(source.contains("getSimulationTick()"));
        assertFalse(source.contains("getRevision()"));
        assertFalse(source.contains("TextField"));
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
