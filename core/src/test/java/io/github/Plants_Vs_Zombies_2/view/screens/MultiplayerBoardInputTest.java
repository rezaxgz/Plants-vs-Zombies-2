package io.github.Plants_Vs_Zombies_2.view.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;

class MultiplayerBoardInputTest {
    private static HeadlessApplication application;

    @BeforeAll
    static void startLibGdx() {
        application = new HeadlessApplication(new ApplicationAdapter() {
        }, new HeadlessApplicationConfiguration());
    }

    @AfterAll
    static void stopLibGdx() {
        if (application != null) application.exit();
    }

    @Test
    void emptyCellIsHitThroughDecorativeFullBoardLayers() {
        Layers layers = layers();

        assertSame(layers.cell, layers.stack.hit(50f, 50f, true));
        assertEquals(Touchable.disabled, layers.grid.getTouchable());
        assertEquals(Touchable.childrenOnly,
                layers.cells.getTouchable());
        assertEquals(Touchable.disabled, layers.brains.getTouchable());
        assertEquals(Touchable.childrenOnly,
                layers.entities.getTouchable());
        assertEquals(Touchable.disabled,
                layers.projectiles.getTouchable());
    }

    @Test
    void decorativeChildrenCannotConsumeInputButEntityChildrenCan() {
        Layers layers = layers();
        Actor brain = child(0f, 0f, 100f, 100f, Touchable.enabled);
        Actor projectile = child(0f, 0f, 100f, 100f, Touchable.enabled);
        layers.brains.addActor(brain);
        layers.projectiles.addActor(projectile);

        assertSame(layers.cell, layers.stack.hit(50f, 50f, true));

        Actor ownedPlant = child(20f, 20f, 40f, 40f, Touchable.enabled);
        layers.entities.addActor(ownedPlant);
        assertSame(ownedPlant, layers.stack.hit(30f, 30f, true));
        assertSame(layers.cell, layers.stack.hit(80f, 80f, true));
    }

    @Test
    void placementPolicyPreservesCanonicalTypeCoordinatesAndRoleSides() {
        MultiplayerIZombieGameScreen.PlacementCommand plant =
                MultiplayerIZombieGameScreen.placementCommand(
                        MatchRole.PLANTS, "Cabbage-pult", 3, 2, 3,
                        false, false);
        MultiplayerIZombieGameScreen.PlacementCommand zombie =
                MultiplayerIZombieGameScreen.placementCommand(
                        MatchRole.ZOMBIES, "NEWSPAPER", 4, 7, 3,
                        false, false);

        assertEquals(MatchRole.PLANTS, plant.role());
        assertEquals("Cabbage-pult", plant.canonicalType());
        assertEquals(3, plant.row());
        assertEquals(2, plant.column());
        assertEquals(MatchRole.ZOMBIES, zombie.role());
        assertEquals("NEWSPAPER", zombie.canonicalType());
        assertEquals(4, zombie.row());
        assertEquals(7, zombie.column());

        assertNull(MultiplayerIZombieGameScreen.placementCommand(
                MatchRole.PLANTS, "Peashooter", 0, 4, 3,
                false, false));
        assertNull(MultiplayerIZombieGameScreen.placementCommand(
                MatchRole.ZOMBIES, "BASIC", 0, 3, 3,
                false, false));
        assertNull(MultiplayerIZombieGameScreen.placementCommand(
                MatchRole.PLANTS, "Peashooter", 0, 0, 3,
                true, false));
        assertNull(MultiplayerIZombieGameScreen.placementCommand(
                MatchRole.ZOMBIES, "BASIC", 0, 4, 3,
                false, true));
    }

    private static Layers layers() {
        Stack stack = new Stack();
        stack.setSize(900f, 500f);
        Actor lawn = child(0f, 0f, 900f, 500f, Touchable.disabled);
        Actor grid = child(0f, 0f, 900f, 500f, Touchable.enabled);
        Group cells = group();
        Group brains = group();
        Group entities = group();
        Group projectiles = group();
        Button cell = new Button(new Button.ButtonStyle());
        cell.setBounds(0f, 0f, 100f, 100f);
        cells.addActor(cell);

        MultiplayerIZombieGameScreen.configureBoardInputLayers(
                grid, cells, brains, entities, projectiles);
        stack.add(lawn);
        stack.add(grid);
        stack.add(cells);
        stack.add(brains);
        stack.add(entities);
        stack.add(projectiles);
        stack.validate();
        return new Layers(stack, grid, cells, brains, entities,
                projectiles, cell);
    }

    private static Group group() {
        Group group = new Group();
        group.setSize(900f, 500f);
        return group;
    }

    private static Actor child(float x, float y, float width, float height,
            Touchable touchable) {
        Actor actor = new Actor();
        actor.setBounds(x, y, width, height);
        actor.setTouchable(touchable);
        return actor;
    }

    private record Layers(Stack stack, Actor grid, Group cells,
            Group brains, Group entities, Group projectiles, Button cell) {
    }
}
