package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchEntitySnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchProjectileSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.view.multiplayer.ClientMultiplayerTransport;
import io.github.Plants_Vs_Zombies_2.view.multiplayer.LiveMatchController;

/** Scene2D view over Stage 6 authoritative snapshots. No local simulation exists here. */
public final class MultiplayerIZombieGameScreen extends AbstractScreen {
    private static final String[] PLANT_TYPES = {
            "Peashooter", "Sunflower", "Wall-nut", "Potato Mine", "Cabbage-pult"
    };
    private static final String[] ZOMBIE_TYPES = {
            "BASIC", "CONEHEAD", "BUCKETHEAD", "IMP", "NEWSPAPER"
    };

    private final MatchAssignment assignment;
    private final LiveMatchController controller;
    private final Label roleLabel;
    private final Label opponentLabel;
    private final Label resourceLabel;
    private final Label timerLabel;
    private final Label brainLabel;
    private final Label statusLabel;
    private final Label connectionLabel;
    private final Label boardInfoLabel;
    private final SelectBox<String> cardSelect;
    private final TextButton leaveButton;
    private final Table grid = new Table();
    private final Group overlay = new Group();
    private final Stack boardStack = new Stack();
    private final Label redLineActor;
    private final Map<String, Label> entityActors = new HashMap<>();
    private final Map<String, Label> projectileActors = new HashMap<>();
    private MatchStateSnapshot pendingSnapshot;
    private MatchStateSnapshot renderedSnapshot;
    private LiveMatchController.State latestState;
    private TextButton[][] cellButtons = new TextButton[0][0];
    private int gridRows;
    private int gridColumns;
    private boolean finishShown;
    private boolean disposed;

    public MultiplayerIZombieGameScreen(ScreenNavigator navigator,
            MatchAssignment assignment, MatchStateSnapshot initialSnapshot) {
        super(navigator, "Multiplayer I, Zombie - Live");
        this.assignment = assignment;
        if (navigator.getAccountSession().getMultiplayerGameClient() == null) {
            throw new IllegalStateException("Authoritative multiplayer client is unavailable");
        }

        roleLabel = new Label("Role: " + assignment.getRole(), skin, "medium_outline");
        opponentLabel = new Label("Opponent: " + assignment.getOpponentUsername(),
                skin, "medium_outline");
        resourceLabel = new Label("Resources: --", skin, "medium_outline");
        timerLabel = new Label("Time: --", skin, "medium_outline");
        brainLabel = new Label("Brains: --", skin, "medium_outline");
        statusLabel = new Label("", skin, "medium_outline");
        connectionLabel = new Label("Connection: "
                + navigator.getAccountSession().getState(), skin, "medium_outline");
        statusLabel.setWrap(true);
        boardInfoLabel = new Label("Waiting for board dimensions...", skin);
        cardSelect = new SelectBox<>(skin);
        cardSelect.setItems(assignment.getRole() == MatchRole.PLANTS
                ? PLANT_TYPES : ZOMBIE_TYPES);
        leaveButton = new TextButton("Leave Match", skin, "brown");

        Table top = new Table();
        top.defaults().pad(5f);
        top.add(roleLabel).left();
        top.add(opponentLabel).left();
        top.add(resourceLabel).left();
        top.add(timerLabel).left().row();
        top.add(brainLabel).left().colspan(2);
        top.add(connectionLabel).left().colspan(2).row();
        top.add(boardInfoLabel).left().colspan(4).row();
        content.add(top).growX().row();

        redLineActor = new Label("RED LINE", skin, "medium_outline");
        redLineActor.setRotation(90f);
        boardStack.add(grid);
        boardStack.add(overlay);
        overlay.addActor(redLineActor);
        content.add(boardStack).width(930f).height(430f).padTop(8f).row();

        Table controls = new Table();
        controls.defaults().pad(7f);
        controls.add(new Label(assignment.getRole() == MatchRole.PLANTS
                ? "Plant card:" : "Zombie card:", skin));
        controls.add(cardSelect).width(250f).height(48f);
        controls.add(leaveButton).width(190f).height(48f).row();
        controls.add(statusLabel).colspan(3).width(880f).left().row();
        content.add(controls).growX();

        controller = new LiveMatchController(
                new ClientMultiplayerTransport(navigator.getAccountSession().getMultiplayerGameClient()),
                navigator.getUiDispatcher(), assignment, initialSnapshot, this::applyState);

        leaveButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!disposed) controller.leave();
            }
        });
    }

    private void applyState(LiveMatchController.State state) {
        if (disposed) return;
        latestState = state;
        pendingSnapshot = state.snapshot();
        statusLabel.setText(state.status());
        leaveButton.setDisabled(state.commandInFlight()
                || state.terminalKind() != LiveMatchController.TerminalKind.NONE);
        cardSelect.setDisabled(state.commandInFlight()
                || state.terminalKind() != LiveMatchController.TerminalKind.NONE);
        if (pendingSnapshot != null) {
            resourceLabel.setText("Plant: " + pendingSnapshot.getPlantResource()
                    + " | Zombie: " + pendingSnapshot.getZombieResource());
            timerLabel.setText(String.format("Time: %.1fs", pendingSnapshot.getRemainingSeconds()));
            boardInfoLabel.setText("Board " + pendingSnapshot.getBoardRows() + "x"
                    + pendingSnapshot.getBoardColumns() + " | red line column "
                    + pendingSnapshot.getRedLineColumn() + " | tick "
                    + pendingSnapshot.getSimulationTick() + " | rev "
                    + pendingSnapshot.getRevision());
            brainLabel.setText(formatBrains(pendingSnapshot));
        }
        setCellsDisabled(state.commandInFlight()
                || state.terminalKind() != LiveMatchController.TerminalKind.NONE);
        if (state.terminalKind() != LiveMatchController.TerminalKind.NONE && !finishShown) {
            finishShown = true;
            showTerminalDialog(state);
        }
    }

    private String formatBrains(MatchStateSnapshot snapshot) {
        StringBuilder result = new StringBuilder("Brains: ");
        for (Boolean available : snapshot.getBrainsAvailable()) {
            result.append(Boolean.TRUE.equals(available) ? "[B] " : "[eaten] ");
        }
        return result.toString();
    }

    private void rebuildGridIfNeeded(MatchStateSnapshot snapshot) {
        if (snapshot.getBoardRows() == gridRows
                && snapshot.getBoardColumns() == gridColumns) return;
        gridRows = snapshot.getBoardRows();
        gridColumns = snapshot.getBoardColumns();
        grid.clearChildren();
        cellButtons = new TextButton[gridRows][gridColumns];
        float cellWidth = 930f / Math.max(1, gridColumns);
        float cellHeight = 430f / Math.max(1, gridRows);
        for (int row = gridRows - 1; row >= 0; row--) {
            for (int column = 0; column < gridColumns; column++) {
                final int targetRow = row;
                final int targetColumn = column;
                String label = column == snapshot.getRedLineColumn()
                        ? row + "," + column + " |RED|" : row + "," + column;
                TextButton cell = new TextButton(label, skin);
                cell.getLabel().setFontScale(0.7f);
                cell.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        submitPlacement(targetRow, targetColumn);
                    }
                });
                cellButtons[row][column] = cell;
                grid.add(cell).width(cellWidth).height(cellHeight);
            }
            grid.row();
        }
    }

    private void submitPlacement(int row, int column) {
        if (disposed || latestState == null || latestState.commandInFlight()
                || latestState.terminalKind() != LiveMatchController.TerminalKind.NONE) return;
        MatchStateSnapshot snapshot = latestState.snapshot();
        if (snapshot == null) return;
        if (assignment.getRole() == MatchRole.PLANTS) {
            if (column > snapshot.getRedLineColumn()) return;
            controller.placePlant(cardSelect.getSelected(), row, column);
        } else {
            if (column <= snapshot.getRedLineColumn()) return;
            controller.placeZombie(cardSelect.getSelected(), row, column);
        }
    }

    private void renderAuthoritativeSnapshot(MatchStateSnapshot snapshot) {
        rebuildGridIfNeeded(snapshot);
        renderedSnapshot = snapshot;
        overlay.setSize(boardStack.getWidth(), boardStack.getHeight());
        float cellWidth = boardStack.getWidth() / Math.max(1, snapshot.getBoardColumns());
        redLineActor.setBounds((snapshot.getRedLineColumn() + 1) * cellWidth - 18f,
                boardStack.getHeight() * 0.43f, 110f, 28f);
        updateEntityActors(snapshot);
        updateProjectileActors(snapshot);
        setCellsDisabled(latestState != null && latestState.commandInFlight());
    }

    private void updateEntityActors(MatchStateSnapshot snapshot) {
        Set<String> active = new HashSet<>();
        for (MatchEntitySnapshot entity : snapshot.getPlants()) {
            active.add(entity.getEntityId());
            updateEntityActor(entity, snapshot);
        }
        for (MatchEntitySnapshot entity : snapshot.getZombies()) {
            active.add(entity.getEntityId());
            updateEntityActor(entity, snapshot);
        }
        entityActors.entrySet().removeIf(entry -> {
            if (active.contains(entry.getKey())) return false;
            entry.getValue().remove();
            return true;
        });
    }

    private void updateEntityActor(MatchEntitySnapshot entity, MatchStateSnapshot snapshot) {
        Label actor = entityActors.get(entity.getEntityId());
        if (actor == null) {
            actor = new Label("", skin);
            actor.setAlignment(Align.center);
            actor.setTouchable(Touchable.enabled);
            final String entityId = entity.getEntityId();
            actor.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    if (!disposed && assignment.getRole() == MatchRole.PLANTS
                            && !controller.getState().commandInFlight()) {
                        MatchEntitySnapshot current = findPlant(entityId);
                        if (current != null && current.getOwnerRole() == MatchRole.PLANTS) {
                            controller.removePlant(entityId);
                        }
                    }
                }
            });
            entityActors.put(entity.getEntityId(), actor);
            overlay.addActor(actor);
        }
        actor.setText(shortName(entity.getEntityType()) + "\n"
                + entity.getHealth() + "/" + entity.getMaximumHealth());
        positionActor(actor, entity.getRow(), entity.getColumnPosition(), snapshot, 88f, 48f);
    }

    private MatchEntitySnapshot findPlant(String entityId) {
        MatchStateSnapshot snapshot = latestState == null ? null : latestState.snapshot();
        if (snapshot == null) return null;
        for (MatchEntitySnapshot plant : snapshot.getPlants()) {
            if (entityId.equals(plant.getEntityId())) return plant;
        }
        return null;
    }

    private void updateProjectileActors(MatchStateSnapshot snapshot) {
        Set<String> active = new HashSet<>();
        for (MatchProjectileSnapshot projectile : snapshot.getProjectiles()) {
            active.add(projectile.getProjectileId());
            Label actor = projectileActors.computeIfAbsent(projectile.getProjectileId(), id -> {
                Label created = new Label("*", skin);
                overlay.addActor(created);
                return created;
            });
            actor.setText("*");
            positionActor(actor, projectile.getLane(), projectile.getColumnPosition(),
                    snapshot, 24f, 24f);
        }
        projectileActors.entrySet().removeIf(entry -> {
            if (active.contains(entry.getKey())) return false;
            entry.getValue().remove();
            return true;
        });
    }

    private void positionActor(Label actor, int row, double columnPosition,
            MatchStateSnapshot snapshot, float width, float height) {
        float cellWidth = boardStack.getWidth() / Math.max(1, snapshot.getBoardColumns());
        float cellHeight = boardStack.getHeight() / Math.max(1, snapshot.getBoardRows());
        float x = (float) ((columnPosition + 0.5) * cellWidth - width / 2f);
        float y = row * cellHeight + (cellHeight - height) / 2f;
        actor.setBounds(x, y, width, height);
    }

    private static String shortName(String name) {
        if (name == null) return "?";
        return name.length() <= 12 ? name : name.substring(0, 12);
    }

    private void setCellsDisabled(boolean disabled) {
        for (TextButton[] row : cellButtons) {
            for (TextButton button : row) {
                if (button != null) button.setDisabled(disabled);
            }
        }
    }

    private void showTerminalDialog(LiveMatchController.State state) {
        boolean victory = state.terminalKind() == LiveMatchController.TerminalKind.VICTORY;
        MatchStateSnapshot finalSnapshot = state.snapshot();
        String title = victory ? "Match Finished" : "Match Cancelled";
        String message;
        if (victory && finalSnapshot != null) {
            MatchRole winner = finalSnapshot.getWinner();
            boolean localWon = winner == assignment.getRole();
            message = "Winner: " + winner + "\n"
                    + (localWon ? "You won!" : "You lost.") + "\nReason: "
                    + finalSnapshot.getFinishReason();
        } else {
            message = "No winner was declared.\nReason: " + state.cancellationReason();
        }
        Dialog dialog = new Dialog(title, skin) {
            @Override protected void result(Object object) {
                if (!disposed) navigator.showMultiplayerIZombieMenu(
                        victory ? "Previous match finished." : "Previous match was cancelled.");
            }
        };
        dialog.text(message);
        dialog.button("Return to Multiplayer", Boolean.TRUE);
        dialog.setModal(true);
        dialog.show(stage);
    }

    @Override public void render(float delta) {
        connectionLabel.setText("Connection: " + navigator.getAccountSession().getState());
        if (pendingSnapshot != null && boardStack.getWidth() > 0f
                && pendingSnapshot != renderedSnapshot) {
            renderAuthoritativeSnapshot(pendingSnapshot);
        }
        super.render(delta);
    }

    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        controller.close();
        entityActors.clear();
        projectileActors.clear();
        super.dispose();
    }
}
