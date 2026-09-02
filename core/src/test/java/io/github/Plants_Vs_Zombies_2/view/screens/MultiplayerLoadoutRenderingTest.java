package io.github.Plants_Vs_Zombies_2.view.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.model.collections.plants.PlantCollection;

class MultiplayerLoadoutRenderingTest {
    @Test
    void livePlantTrayAcceptsExactlyEightValidDistinctPlants() {
        List<String> eight = new PlantCollection().getAllPlants().stream()
                .limit(8).map(plant -> plant.getName()).toList();

        assertEquals(eight,
                MultiplayerIZombieGameScreen.resolvePlantCardTypes(eight));
        assertEquals(5, MultiplayerIZombieGameScreen.resolvePlantCardTypes(
                List.of(eight.get(0), eight.get(0), eight.get(1), eight.get(2),
                        eight.get(3), eight.get(4), eight.get(5), eight.get(6)))
                .size());
    }

    @Test
    void liveLayoutReusesTheSinglePlayerGameScreenAndItsCardTrays()
            throws IOException {
        String multiplayerSource = Files.readString(Path.of("src", "main", "java",
                "io", "github", "Plants_Vs_Zombies_2", "view", "screens",
                "MultiplayerIZombieGameScreen.java"));
        String gameSource = Files.readString(Path.of("src", "main", "java",
                "io", "github", "Plants_Vs_Zombies_2", "view", "screens",
                "GameScreen.java"));

        assertTrue(multiplayerSource.contains("extends GameScreen"));
        assertTrue(multiplayerSource.contains(
                "super(navigator, assignment, initialSnapshot, plantLoadout)"));
        assertTrue(gameSource.contains("installChapterBoard(null)"));
        assertTrue(gameSource.contains("installGameHud()"));
        assertTrue(gameSource.contains("installSeedTray()"));
        assertTrue(gameSource.contains("installIZombieTray()"));
        assertTrue(gameSource.contains("createGameSeedSlot(plant)"));
        assertTrue(gameSource.contains("new IZombieCardActor(card)"));
        assertTrue(gameSource.contains("installMultiplayerChat()"));
    }
}
