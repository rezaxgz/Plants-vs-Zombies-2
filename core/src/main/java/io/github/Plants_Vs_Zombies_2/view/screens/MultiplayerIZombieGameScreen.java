package io.github.Plants_Vs_Zombies_2.view.screens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchAssignment;
import io.github.Plants_Vs_Zombies_2.network.matchmaking.MatchRole;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchEntitySnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchProjectileSnapshot;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionEvent;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionKind;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchReactionType;
import io.github.Plants_Vs_Zombies_2.network.multiplayer.MatchStateSnapshot;
import io.github.Plants_Vs_Zombies_2.network.session.ClientSessionState;
import io.github.Plants_Vs_Zombies_2.view.multiplayer.ClientMultiplayerTransport;
import io.github.Plants_Vs_Zombies_2.view.multiplayer.EntityReconciliation;
import io.github.Plants_Vs_Zombies_2.view.multiplayer.LiveMatchController;
import io.github.Plants_Vs_Zombies_2.view.presentation.Phase3Text;

/** Graphical Scene2D projection of server-authoritative I, Zombie snapshots. */
public final class MultiplayerIZombieGameScreen extends AbstractScreen {
    private static final float BOARD_WIDTH = 930f;
    private static final float BOARD_HEIGHT = 430f;

    private final MatchAssignment assignment;
    private final LiveMatchController controller;
    private final Label localPlayerLabel;
    private final Label roleLabel;
    private final Label opponentLabel;
    private final Label plantResourceLabel;
    private final Label zombieResourceLabel;
    private final Label timerLabel;
    private final Label statusLabel;
    private final Label connectionLabel;
    private final Label boardInfoLabel;
    private final TextButton leaveButton;
    private final Table cardTable = new Table();
    private final Table reactionHistoryTable = new Table();
    private final Label reactionStatusLabel;
    private final List<TextButton> reactionButtons = new ArrayList<>();
    private final Map<String, Button> cardButtons = new LinkedHashMap<>();
    private final Stack boardStack = new Stack();
    private final Group cellLayer = new Group();
    private final Group brainLayer = new Group();
    private final Group entityLayer = new Group();
    private final Group projectileLayer = new Group();
    private final Map<String, EntityVisualActor> entityActors = new HashMap<>();
    private final Map<String, Actor> projectileActors = new HashMap<>();
    private final List<Actor> brainActors = new ArrayList<>();
    private final Set<String> missingVisualWarnings = new HashSet<>();
    private final Texture pixelTexture;
    private final BoardGridActor boardGridActor;

    private MatchStateSnapshot pendingSnapshot;
    private MatchStateSnapshot renderedSnapshot;
    private LiveMatchController.State latestState;
    private Button[][] cellButtons = new Button[0][0];
    private int gridRows;
    private int gridColumns;
    private int redLineColumn;
    private String selectedCardType;
    private ClientSessionState renderedConnectionState;
    private boolean finishShown;
    private boolean disposed;

    public MultiplayerIZombieGameScreen(ScreenNavigator navigator,
            MatchAssignment assignment, MatchStateSnapshot initialSnapshot) {
        super(navigator, "Multiplayer I, Zombie - Live");
        this.assignment = assignment;
        if (navigator.getAccountSession().getMultiplayerGameClient() == null) {
            throw new IllegalStateException(
                    "Authoritative multiplayer client is unavailable");
        }

        pixelTexture = createPixelTexture();
        boardGridActor = new BoardGridActor(pixelTexture);
        configureBoardInputLayers(boardGridActor, cellLayer, brainLayer,
                entityLayer, projectileLayer);
        localPlayerLabel = new Label("Player: " + Phase3Text.required(
                assignment.getLocalUsername(), "Player unavailable"), skin,
                "medium_outline");
        roleLabel = new Label("Role: " + Phase3Text.role(assignment.getRole()),
                skin, "medium_outline");
        opponentLabel = new Label("Opponent: "
                + Phase3Text.username(assignment.getOpponentUsername()),
                skin, "medium_outline");
        plantResourceLabel = new Label("0", skin, "medium_outline");
        zombieResourceLabel = new Label("0", skin, "medium_outline");
        timerLabel = new Label("Time: waiting...", skin, "medium_outline");
        statusLabel = new Label("Waiting for authoritative server state...",
                skin, "medium_outline");
        renderedConnectionState = navigator.getAccountSession().getState();
        connectionLabel = new Label("Connection: " + Phase3Text.connection(
                renderedConnectionState), skin,
                "medium_outline");
        boardInfoLabel = new Label("Preparing the lawn...", skin,
                "secondary");
        statusLabel.setWrap(true);
        leaveButton = new TextButton("Leave Match", skin, "brown");
        reactionHistoryTable.top().left();
        reactionStatusLabel = new Label("Choose a predefined reaction.", skin,
                "secondary");
        reactionStatusLabel.setWrap(true);

        buildHud();
        buildBoard();
        buildCards();

        controller = new LiveMatchController(
                new ClientMultiplayerTransport(navigator.getAccountSession()
                        .getMultiplayerGameClient()),
                navigator.getUiDispatcher(), assignment, initialSnapshot,
                this::applyState);

        leaveButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!disposed) controller.leave();
            }
        });
    }

    private void buildHud() {
        Table top = new Table();
        top.defaults().pad(4f);
        top.add(localPlayerLabel).left();
        top.add(opponentLabel).left();
        top.add(roleLabel).left().expandX();
        top.add(resourceCell(MultiplayerVisualCatalog.SUN_RESOURCE_ASSET,
                "Plant energy", plantResourceLabel)).right();
        top.add(resourceCell(MultiplayerVisualCatalog.BRAIN_FALLBACK_ASSET,
                "Zombie energy", zombieResourceLabel)).right();
        top.add(timerLabel).right().row();
        top.add(connectionLabel).left().colspan(2);
        top.add(boardInfoLabel).left().colspan(3).row();
        content.add(top).growX().row();
    }

    private Table resourceCell(String asset, String tooltip, Label value) {
        Table cell = new Table();
        TextureRegion region = borrowRegion(asset, "resource icon", tooltip);
        if (region != null) {
            Image icon = new Image(region);
            icon.setScaling(Scaling.fit);
            cell.add(icon).size(30f).padRight(3f);
        } else {
            Label fallback = new Label(tooltip, skin, "secondary");
            fallback.setFontScale(0.60f);
            cell.add(fallback).padRight(3f);
        }
        cell.add(value);
        return cell;
    }

    private void buildBoard() {
        TextureRegion lawnRegion = borrowRegion(
                MultiplayerVisualCatalog.LAWN_ASSET, "lawn", "Egypt lawn");
        if (lawnRegion != null) {
            Image lawn = new Image(cropEgyptLawn(lawnRegion));
            lawn.setScaling(Scaling.stretch);
            boardStack.add(lawn);
        } else {
            Label fallback = new Label("I, Zombie lawn unavailable", skin,
                    "medium_outline");
            fallback.setAlignment(Align.center);
            boardStack.add(fallback);
        }
        boardStack.add(boardGridActor);
        boardStack.add(cellLayer);
        boardStack.add(brainLayer);
        boardStack.add(entityLayer);
        boardStack.add(projectileLayer);

        Table matchBody = new Table();
        matchBody.add(boardStack).width(BOARD_WIDTH).height(BOARD_HEIGHT)
                .padTop(6f);
        matchBody.add(createReactionPanel()).width(255f).height(BOARD_HEIGHT)
                .padLeft(10f).padTop(6f);
        content.add(matchBody).growX().row();
    }

    static void configureBoardInputLayers(Actor boardGrid, Group cells,
            Group brains, Group entities, Group projectiles) {
        boardGrid.setTouchable(Touchable.disabled);
        cells.setTouchable(Touchable.childrenOnly);
        brains.setTouchable(Touchable.disabled);
        entities.setTouchable(Touchable.childrenOnly);
        projectiles.setTouchable(Touchable.disabled);
    }

    private static TextureRegion cropEgyptLawn(TextureRegion source) {
        int sourceWidth = Math.max(1, source.getRegionWidth());
        int sourceHeight = Math.max(1, source.getRegionHeight());
        int x = Math.round(sourceWidth * 256f / 1024f);
        int y = Math.round(sourceHeight * 200f / 768f);
        int width = Math.max(1, Math.round(sourceWidth * 738f / 1024f));
        int height = Math.max(1, Math.round(sourceHeight * 488f / 768f));
        width = Math.min(width, sourceWidth - x);
        height = Math.min(height, sourceHeight - y);
        return new TextureRegion(source, x, y, width, height);
    }

    private void buildCards() {
        List<String> types = assignment.getRole() == MatchRole.PLANTS
                ? MultiplayerVisualCatalog.plantTypes()
                : MultiplayerVisualCatalog.zombieTypes();
        selectedCardType = types.get(0);
        cardTable.defaults().pad(3f);
        for (String type : types) {
            Button button = createCardButton(type);
            cardButtons.put(type, button);
            cardTable.add(button).width(130f).height(86f);
        }
        updateCardSelection();

        Table controls = new Table();
        controls.defaults().pad(5f);
        controls.add(new Label(assignment.getRole() == MatchRole.PLANTS
                ? "Choose a plant" : "Choose a zombie", skin,
                "medium_outline")).left();
        controls.add(cardTable).expandX().left();
        controls.add(leaveButton).width(170f).height(46f).right().row();
        controls.add(statusLabel).colspan(3).width(1050f).left().row();
        content.add(controls).growX();
    }

    private Button createCardButton(String type) {
        MultiplayerVisualCatalog.Visual visual = assignment.getRole()
                == MatchRole.PLANTS
                        ? MultiplayerVisualCatalog.plant(type)
                        : MultiplayerVisualCatalog.zombie(type);
        TextButtonStyle brown = skin.get("brown", TextButtonStyle.class);
        TextButtonStyle green = skin.get("green", TextButtonStyle.class);
        Button.ButtonStyle style = new Button.ButtonStyle();
        style.up = brown.up;
        style.down = brown.down;
        style.over = brown.over;
        style.checked = green.up;
        style.checkedOver = green.over != null ? green.over : green.up;
        Button button = new Button(style);
        Table contents = new Table();
        TextureRegion art = visual == null ? null : borrowRegion(
                visual.packetAsset(), "packet card", type);
        if (art != null) {
            Image image = new Image(art);
            image.setScaling(Scaling.fit);
            contents.add(image).size(58f, 49f).padRight(3f);
        }
        Table text = new Table();
        String name = visual == null
                ? Phase3Text.prettyIdentifier(type, "Unknown card")
                : visual.displayName();
        Label nameLabel = new Label(name, skin, "secondary");
        nameLabel.setFontScale(0.62f);
        nameLabel.setWrap(true);
        int cost = assignment.getRole() == MatchRole.PLANTS
                ? MultiplayerVisualCatalog.plantCost(type)
                : MultiplayerVisualCatalog.zombieCost(type);
        Label costLabel = new Label(cost < 0 ? "Cost unavailable"
                : "Cost " + cost, skin, "medium_outline");
        costLabel.setFontScale(0.60f);
        text.add(nameLabel).width(62f).left().row();
        text.add(costLabel).left();
        contents.add(text).left();
        button.add(contents).grow();
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!disposed && latestState != null
                        && !latestState.commandInFlight()
                        && latestState.terminalKind()
                                == LiveMatchController.TerminalKind.NONE) {
                    selectedCardType = type;
                    updateCardSelection();
                }
            }
        });
        return button;
    }

    private void updateCardSelection() {
        for (Map.Entry<String, Button> entry : cardButtons.entrySet()) {
            entry.getValue().setChecked(entry.getKey().equals(
                    selectedCardType));
        }
    }

    private void applyState(LiveMatchController.State state) {
        if (disposed) return;
        latestState = state;
        pendingSnapshot = state.snapshot();
        statusLabel.setText(Phase3Text.status(state.status(),
                "Waiting for the server..."));
        boolean terminal = state.terminalKind()
                != LiveMatchController.TerminalKind.NONE;
        leaveButton.setDisabled(state.commandInFlight() || terminal);
        for (Button button : cardButtons.values()) {
            button.setDisabled(state.commandInFlight() || terminal);
        }
        if (pendingSnapshot != null) {
            plantResourceLabel.setText(Integer.toString(
                    pendingSnapshot.getPlantResource()));
            zombieResourceLabel.setText(Integer.toString(
                    pendingSnapshot.getZombieResource()));
            timerLabel.setText(String.format("Time: %.1fs",
                    Math.max(0.0, pendingSnapshot.getRemainingSeconds())));
            String level = Phase3Text.prettyIdentifier(
                    pendingSnapshot.getLevel(), "I, Zombie");
            boardInfoLabel.setText(level + " - synchronized by the server");
        }
        setCellsDisabled(state.commandInFlight() || terminal);
        reactionStatusLabel.setText(Phase3Text.status(state.reactionStatus(),
                "Choose a predefined reaction."));
        renderReactions(state.recentReactions());
        for (TextButton button : reactionButtons) {
            button.setDisabled(state.reactionInFlight() || terminal);
        }
        if (terminal && !finishShown) {
            finishShown = true;
            showTerminalDialog(state);
        }
    }

    private Table createReactionPanel() {
        Table panel = new Table();
        panel.setBackground(skin.get("brown", TextButtonStyle.class).up);
        panel.pad(8f);
        panel.add(new Label("Match Reactions", skin, "medium_outline"))
                .colspan(2).padBottom(5f).row();
        MatchReactionType[] types = MatchReactionType.values();
        for (int index = 0; index < types.length; index++) {
            MatchReactionType type = types[index];
            TextButton button = new TextButton(type.getDisplayText(), skin,
                    type.getKind() == MatchReactionKind.TEXT
                            ? "green" : "brown");
            if (type.getKind() == MatchReactionKind.EMOJI) {
                TextureRegion region = borrowRegion(
                        MultiplayerVisualCatalog.reactionAsset(type),
                        "reaction icon", type.getDisplayText());
                if (region != null) {
                    Image icon = new Image(region);
                    icon.setScaling(Scaling.fit);
                    button.add(icon).size(25f).padRight(4f);
                }
            }
            button.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x,
                        float y) {
                    if (!disposed) controller.sendReaction(type);
                }
            });
            reactionButtons.add(button);
            panel.add(button).width(112f).height(40f).pad(3f);
            if (index % 2 == 1) panel.row();
        }
        panel.add(reactionStatusLabel).colspan(2).width(225f)
                .left().padTop(5f).row();
        panel.add(reactionHistoryTable).colspan(2).width(225f)
                .height(150f).left().top().padTop(5f);
        return panel;
    }

    private void renderReactions(List<MatchReactionEvent> reactions) {
        reactionHistoryTable.clearChildren();
        reactionHistoryTable.defaults().left().padBottom(2f);
        if (reactions == null || reactions.isEmpty()) {
            reactionHistoryTable.add(new Label("No reactions yet", skin,
                    "secondary")).left();
            return;
        }
        String localUsername = navigator.getAccountSession().getProfile()
                == null ? "" : Phase3Text.required(navigator
                        .getAccountSession().getProfile().getUsername(), "");
        for (MatchReactionEvent reaction : reactions) {
            if (reaction == null || reaction.getReactionType() == null) continue;
            boolean local = Phase3Text.hasText(localUsername)
                    && localUsername.equals(reaction.getSenderUsername());
            MatchReactionType type = reaction.getReactionType();
            if (type.getKind() == MatchReactionKind.EMOJI) {
                TextureRegion region = borrowRegion(
                        MultiplayerVisualCatalog.reactionAsset(type),
                        "reaction icon", type.getDisplayText());
                if (region != null) {
                    Image icon = new Image(region);
                    icon.setScaling(Scaling.fit);
                    reactionHistoryTable.add(icon).size(24f).padRight(5f);
                } else {
                    reactionHistoryTable.add().width(29f);
                }
            } else {
                reactionHistoryTable.add().width(29f);
            }
            String sender = local ? "You" : Phase3Text.required(
                    reaction.getSenderUsername(), "Opponent");
            reactionHistoryTable.add(new Label(sender + ": "
                    + type.getDisplayText(), skin, "secondary"))
                    .width(190f).left().row();
        }
    }

    private void rebuildBoardIfNeeded(MatchStateSnapshot snapshot) {
        if (snapshot.getBoardRows() == gridRows
                && snapshot.getBoardColumns() == gridColumns
                && snapshot.getRedLineColumn() == redLineColumn) return;
        gridRows = snapshot.getBoardRows();
        gridColumns = snapshot.getBoardColumns();
        redLineColumn = snapshot.getRedLineColumn();
        boardGridActor.configure(gridRows, gridColumns, redLineColumn,
                assignment.getRole());
        cellLayer.clearChildren();
        cellButtons = new Button[gridRows][gridColumns];
        for (int row = 0; row < gridRows; row++) {
            for (int column = 0; column < gridColumns; column++) {
                final int targetRow = row;
                final int targetColumn = column;
                Button cell = new Button(new Button.ButtonStyle());
                cell.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x,
                            float y) {
                        submitPlacement(targetRow, targetColumn);
                    }
                });
                cellButtons[row][column] = cell;
                cellLayer.addActor(cell);
            }
        }
        rebuildBrains(snapshot);
    }

    private void rebuildBrains(MatchStateSnapshot snapshot) {
        brainLayer.clearChildren();
        brainActors.clear();
        MultiplayerVisualCatalog.Visual brainVisual =
                MultiplayerVisualCatalog.brain(true).artwork();
        for (int row = 0; row < snapshot.getBoardRows(); row++) {
            Actor brain = createPamOrImage(brainVisual, "brain");
            brain.setTouchable(Touchable.disabled);
            brainActors.add(brain);
            brainLayer.addActor(brain);
        }
    }

    private void submitPlacement(int row, int column) {
        if (disposed || latestState == null) return;
        MatchStateSnapshot snapshot = latestState.snapshot();
        if (snapshot == null) return;
        PlacementCommand command = placementCommand(assignment.getRole(),
                selectedCardType, row, column, snapshot.getRedLineColumn(),
                latestState.commandInFlight(), latestState.terminalKind()
                        != LiveMatchController.TerminalKind.NONE);
        if (command == null) return;
        if (command.role() == MatchRole.PLANTS) {
            controller.placePlant(command.canonicalType(), command.row(),
                    command.column());
        } else {
            controller.placeZombie(command.canonicalType(), command.row(),
                    command.column());
        }
    }

    static PlacementCommand placementCommand(MatchRole role,
            String canonicalType, int row, int column, int redLine,
            boolean commandInFlight, boolean terminal) {
        if (role == null || canonicalType == null || canonicalType.isBlank()
                || row < 0 || column < 0 || commandInFlight || terminal) {
            return null;
        }
        boolean allowed = role == MatchRole.PLANTS
                ? column <= redLine : column > redLine;
        return allowed
                ? new PlacementCommand(role, canonicalType, row, column)
                : null;
    }

    record PlacementCommand(MatchRole role, String canonicalType,
            int row, int column) {
    }

    private void renderAuthoritativeSnapshot(MatchStateSnapshot snapshot) {
        rebuildBoardIfNeeded(snapshot);
        renderedSnapshot = snapshot;
        layoutBoardLayers(snapshot);
        updateEntityActors(snapshot);
        updateProjectileActors(snapshot);
        updateBrains(snapshot);
        setCellsDisabled(latestState != null
                && (latestState.commandInFlight()
                        || latestState.terminalKind()
                                != LiveMatchController.TerminalKind.NONE));
    }

    private void layoutBoardLayers(MatchStateSnapshot snapshot) {
        float width = boardStack.getWidth();
        float height = boardStack.getHeight();
        cellLayer.setSize(width, height);
        brainLayer.setSize(width, height);
        entityLayer.setSize(width, height);
        projectileLayer.setSize(width, height);
        boardGridActor.setSize(width, height);
        float cellWidth = width / snapshot.getBoardColumns();
        float cellHeight = height / snapshot.getBoardRows();
        for (int row = 0; row < gridRows; row++) {
            for (int column = 0; column < gridColumns; column++) {
                cellButtons[row][column].setBounds(column * cellWidth,
                        (gridRows - 1 - row) * cellHeight,
                        cellWidth, cellHeight);
            }
        }
    }

    private void updateEntityActors(MatchStateSnapshot snapshot) {
        Map<String, MatchEntitySnapshot> incoming = new LinkedHashMap<>();
        for (MatchEntitySnapshot entity : snapshot.getPlants()) {
            incoming.put(entity.getEntityId(), entity);
        }
        for (MatchEntitySnapshot entity : snapshot.getZombies()) {
            incoming.put(entity.getEntityId(), entity);
        }
        EntityReconciliation.Changes changes = EntityReconciliation.between(
                entityActors.keySet(), incoming.keySet());
        for (String id : changes.removed()) {
            EntityVisualActor actor = entityActors.remove(id);
            if (actor != null) actor.remove();
        }
        for (String id : changes.added()) {
            MatchEntitySnapshot entity = incoming.get(id);
            EntityVisualActor actor = createEntityActor(entity);
            entityActors.put(id, actor);
            entityLayer.addActor(actor);
        }
        for (MatchEntitySnapshot entity : incoming.values()) {
            EntityVisualActor actor = entityActors.get(entity.getEntityId());
            if (actor == null || !actor.matches(entity)) {
                if (actor != null) actor.remove();
                actor = createEntityActor(entity);
                entityActors.put(entity.getEntityId(), actor);
                entityLayer.addActor(actor);
            }
            actor.updateHealth(entity.getHealth(), entity.getMaximumHealth());
            positionEntityActor(actor, entity, snapshot);
        }
    }

    private EntityVisualActor createEntityActor(MatchEntitySnapshot entity) {
        MultiplayerVisualCatalog.Visual visual = entity.getOwnerRole()
                == MatchRole.PLANTS
                        ? MultiplayerVisualCatalog.plant(entity.getEntityType())
                        : MultiplayerVisualCatalog.zombie(entity.getEntityType());
        if (visual == null) {
            String label = Phase3Text.prettyIdentifier(entity.getEntityType(),
                    entity.getOwnerRole() == MatchRole.PLANTS
                            ? "Unknown plant" : "Unknown zombie");
            visual = new MultiplayerVisualCatalog.Visual(
                    entity.getEntityType(), label, null, null, null, label);
            warnMissing("entity:" + entity.getEntityType(),
                    "Missing required " + entity.getOwnerRole()
                            + " mapping for " + entity.getEntityType()
                            + "; using named fallback.");
        }
        Actor artwork = createPamOrImage(visual,
                entity.getOwnerRole() == MatchRole.PLANTS
                        ? "plant" : "zombie");
        EntityVisualActor actor = new EntityVisualActor(pixelTexture, artwork,
                entity.getEntityType(), entity.getOwnerRole());
        actor.setTouchable(Touchable.enabled);
        final String entityId = entity.getEntityId();
        actor.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (!disposed && assignment.getRole() == MatchRole.PLANTS
                        && latestState != null
                        && !latestState.commandInFlight()) {
                    MatchEntitySnapshot current = findPlant(entityId);
                    if (current != null
                            && current.getOwnerRole() == MatchRole.PLANTS) {
                        controller.removePlant(entityId);
                    }
                }
            }
        });
        return actor;
    }

    private Actor createPamOrImage(MultiplayerVisualCatalog.Visual visual,
            String category) {
        if (visual != null && visual.hasPam()) {
            try {
                Rectangle bounds = navigator.getPamPlayer().bounds(
                        visual.pamPath(), visual.clip());
                if (bounds == null || bounds.width <= 0f
                        || bounds.height <= 0f) {
                    throw new IllegalStateException("PAM clip has no bounds");
                }
                PamAnimationActor actor = new PamAnimationActor(
                        navigator.getPamPlayer(), visual.pamPath(),
                        visual.clip());
                actor.setTouchable(Touchable.disabled);
                return actor;
            } catch (RuntimeException failure) {
                warnMissing(category + ":pam:" + visual.canonicalType(),
                        "Could not load " + category + " PAM for "
                                + visual.canonicalType() + " at "
                                + visual.pamPath() + "; using packet fallback.");
            }
        }
        TextureRegion region = visual == null ? null : borrowRegion(
                visual.packetAsset(), category, visual.canonicalType());
        if (region != null) {
            Image image = new Image(region);
            image.setScaling(Scaling.fit);
            image.setTouchable(Touchable.disabled);
            return image;
        }
        String fallback = visual == null ? "Visual unavailable"
                : visual.fallbackLabel();
        Label label = new Label(fallback, skin, "secondary");
        label.setWrap(true);
        label.setAlignment(Align.center);
        label.setTouchable(Touchable.disabled);
        return label;
    }

    private MatchEntitySnapshot findPlant(String entityId) {
        MatchStateSnapshot snapshot = latestState == null
                ? null : latestState.snapshot();
        if (snapshot == null) return null;
        for (MatchEntitySnapshot plant : snapshot.getPlants()) {
            if (entityId.equals(plant.getEntityId())) return plant;
        }
        return null;
    }

    private void updateProjectileActors(MatchStateSnapshot snapshot) {
        Map<String, MatchProjectileSnapshot> incoming = new LinkedHashMap<>();
        for (MatchProjectileSnapshot projectile : snapshot.getProjectiles()) {
            incoming.put(projectile.getProjectileId(), projectile);
        }
        EntityReconciliation.Changes changes = EntityReconciliation.between(
                projectileActors.keySet(), incoming.keySet());
        for (String id : changes.removed()) {
            Actor actor = projectileActors.remove(id);
            if (actor != null) actor.remove();
        }
        for (String id : changes.added()) {
            MatchProjectileSnapshot projectile = incoming.get(id);
            MultiplayerVisualCatalog.Visual visual =
                    MultiplayerVisualCatalog.projectile(
                            projectile.getProjectileType());
            if (visual == null) {
                String label = Phase3Text.prettyIdentifier(
                        projectile.getProjectileType(), "Projectile");
                visual = new MultiplayerVisualCatalog.Visual(
                        projectile.getProjectileType(), label, null,
                        null, null, label);
                warnMissing("projectile:" + projectile.getProjectileType(),
                        "Missing required projectile mapping for "
                                + projectile.getProjectileType()
                                + "; using named fallback.");
            }
            Actor actor = createPamOrImage(visual, "projectile");
            projectileActors.put(id, actor);
            projectileLayer.addActor(actor);
        }
        for (MatchProjectileSnapshot projectile : incoming.values()) {
            Actor actor = projectileActors.get(projectile.getProjectileId());
            float cellWidth = boardStack.getWidth()
                    / snapshot.getBoardColumns();
            float cellHeight = boardStack.getHeight()
                    / snapshot.getBoardRows();
            positionActor(actor, projectile.getLane(),
                    projectile.getColumnPosition(), snapshot,
                    Math.max(20f, cellWidth * 0.34f),
                    Math.max(20f, cellHeight * 0.34f));
        }
    }

    private void updateBrains(MatchStateSnapshot snapshot) {
        float cellWidth = boardStack.getWidth() / snapshot.getBoardColumns();
        float cellHeight = boardStack.getHeight() / snapshot.getBoardRows();
        for (int row = 0; row < brainActors.size(); row++) {
            Actor brain = brainActors.get(row);
            boolean available = Boolean.TRUE.equals(
                    snapshot.getBrainsAvailable().get(row));
            MultiplayerVisualCatalog.BrainVisual visual =
                    MultiplayerVisualCatalog.brain(available);
            brain.setColor(visual.red(), visual.green(), visual.blue(),
                    visual.alpha());
            float size = Math.min(cellWidth, cellHeight) * 0.58f;
            brain.setBounds(3f,
                    (snapshot.getBoardRows() - 1 - row) * cellHeight
                            + (cellHeight - size) * 0.5f,
                    size, size);
        }
    }

    private void positionEntityActor(EntityVisualActor actor,
            MatchEntitySnapshot entity, MatchStateSnapshot snapshot) {
        float cellWidth = boardStack.getWidth() / snapshot.getBoardColumns();
        float cellHeight = boardStack.getHeight() / snapshot.getBoardRows();
        float width = cellWidth * (entity.getOwnerRole() == MatchRole.ZOMBIES
                ? 0.96f : 0.86f);
        float height = cellHeight * (entity.getOwnerRole() == MatchRole.ZOMBIES
                ? 1.30f : 1.05f);
        float x = (float) ((entity.getColumnPosition() + 0.5) * cellWidth
                - width * 0.5f);
        float laneBottom = (snapshot.getBoardRows() - 1 - entity.getRow())
                * cellHeight;
        float y = laneBottom + (cellHeight - height) * 0.30f;
        actor.layoutAt(x, y, width, height);
    }

    private void positionActor(Actor actor, int row, double columnPosition,
            MatchStateSnapshot snapshot, float width, float height) {
        float cellWidth = boardStack.getWidth() / snapshot.getBoardColumns();
        float cellHeight = boardStack.getHeight() / snapshot.getBoardRows();
        float x = (float) ((columnPosition + 0.5) * cellWidth
                - width * 0.5f);
        float y = (snapshot.getBoardRows() - 1 - row) * cellHeight
                + (cellHeight - height) * 0.5f;
        actor.setBounds(x, y, width, height);
    }

    private void setCellsDisabled(boolean disabled) {
        for (int row = 0; row < cellButtons.length; row++) {
            for (int column = 0; column < cellButtons[row].length; column++) {
                boolean roleRestricted = assignment.getRole()
                        == MatchRole.PLANTS
                                ? column > redLineColumn
                                : column <= redLineColumn;
                cellButtons[row][column].setDisabled(
                        disabled || roleRestricted);
            }
        }
    }

    private void showTerminalDialog(LiveMatchController.State state) {
        boolean victory = state.terminalKind()
                == LiveMatchController.TerminalKind.VICTORY;
        MatchStateSnapshot finalSnapshot = state.snapshot();
        String title = victory ? "Match Finished" : "Match Cancelled";
        String message;
        if (victory && finalSnapshot != null
                && finalSnapshot.getWinner() != null) {
            MatchRole winner = finalSnapshot.getWinner();
            boolean localWon = winner == assignment.getRole();
            message = Phase3Text.roleShort(winner) + " won.\n"
                    + (localWon ? "You won!" : "You lost.") + "\n"
                    + Phase3Text.finishReason(
                            finalSnapshot.getFinishReason());
        } else {
            message = "No winner was declared.\n"
                    + Phase3Text.cancellationReason(
                            state.cancellationReason());
        }
        PvzDialog dialog = new PvzDialog(title, skin) {
            @Override protected void result(Object object) {
                if (!disposed) {
                    navigator.showMultiplayerIZombieMenu(victory
                            ? "Previous match finished."
                            : "Previous match was cancelled.");
                }
            }
        };
        dialog.message(message);
        dialog.action("Return to Multiplayer", Boolean.TRUE, "green");
        dialog.setModal(true);
        dialog.show(stage);
    }

    private TextureRegion borrowRegion(String imageId, String category,
            String canonicalType) {
        TextureRegion region = Phase3Text.hasText(imageId)
                ? navigator.getTextureBank().region(imageId) : null;
        if (region != null) return region;
        warnMissing(category + ":asset:" + canonicalType,
                "Missing " + category + " asset for " + canonicalType
                        + " (key " + Phase3Text.required(imageId,
                                "not mapped") + "); using fallback.");
        return null;
    }

    private void warnMissing(String key, String message) {
        if (!missingVisualWarnings.add(key)) return;
        if (Gdx.app != null) {
            Gdx.app.error("MultiplayerVisuals", message);
        } else {
            System.err.println("MultiplayerVisuals: " + message);
        }
    }

    private static Texture createPixelTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override public void render(float delta) {
        ClientSessionState connectionState =
                navigator.getAccountSession().getState();
        if (connectionState != renderedConnectionState) {
            renderedConnectionState = connectionState;
            connectionLabel.setText("Connection: "
                    + Phase3Text.connection(connectionState));
        }
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
        brainActors.clear();
        pixelTexture.dispose();
        super.dispose();
    }

    private static final class EntityVisualActor extends Group {
        private final Actor artwork;
        private final HealthBarActor healthBar;
        private final String canonicalType;
        private final MatchRole role;
        private int previousHealth = -1;

        EntityVisualActor(Texture pixel, Actor artwork, String canonicalType,
                MatchRole role) {
            this.artwork = artwork;
            this.canonicalType = canonicalType;
            this.role = role;
            artwork.setTouchable(Touchable.disabled);
            healthBar = new HealthBarActor(pixel);
            healthBar.setTouchable(Touchable.disabled);
            addActor(artwork);
            addActor(healthBar);
        }

        boolean matches(MatchEntitySnapshot entity) {
            return role == entity.getOwnerRole()
                    && canonicalType.equals(entity.getEntityType());
        }

        void updateHealth(int health, int maximumHealth) {
            if (previousHealth >= 0 && health < previousHealth
                    && artwork instanceof PamAnimationActor pam) {
                pam.flashHurt();
            }
            previousHealth = health;
            healthBar.setRatio(maximumHealth <= 0 ? 0f
                    : health / (float) maximumHealth);
        }

        void layoutAt(float x, float y, float width, float height) {
            setBounds(x, y, width, height);
            artwork.setBounds(0f, 5f, width, Math.max(1f, height - 5f));
            healthBar.setBounds(width * 0.12f, 0f, width * 0.76f, 5f);
        }
    }

    private static final class HealthBarActor extends Actor {
        private final Texture pixel;
        private float ratio = 1f;

        HealthBarActor(Texture pixel) {
            this.pixel = pixel;
        }

        void setRatio(float ratio) {
            this.ratio = Math.max(0f, Math.min(1f, ratio));
        }

        @Override public void draw(Batch batch, float parentAlpha) {
            Color old = batch.getColor();
            float oldR = old.r;
            float oldG = old.g;
            float oldB = old.b;
            float oldA = old.a;
            batch.setColor(0.08f, 0.08f, 0.08f, 0.78f * parentAlpha);
            batch.draw(pixel, getX(), getY(), getWidth(), getHeight());
            batch.setColor(1f - ratio, ratio, 0.12f, parentAlpha);
            batch.draw(pixel, getX(), getY(), getWidth() * ratio,
                    getHeight());
            batch.setColor(oldR, oldG, oldB, oldA);
        }
    }

    private static final class BoardGridActor extends Actor {
        private final Texture pixel;
        private int rows = 1;
        private int columns = 1;
        private int redLine;
        private MatchRole role;

        BoardGridActor(Texture pixel) {
            this.pixel = pixel;
            setTouchable(Touchable.disabled);
        }

        void configure(int rows, int columns, int redLine, MatchRole role) {
            this.rows = Math.max(1, rows);
            this.columns = Math.max(1, columns);
            this.redLine = redLine;
            this.role = role;
        }

        @Override public void draw(Batch batch, float parentAlpha) {
            Color old = batch.getColor();
            float oldR = old.r;
            float oldG = old.g;
            float oldB = old.b;
            float oldA = old.a;
            float cellWidth = getWidth() / columns;
            float cellHeight = getHeight() / rows;
            for (int column = 0; column < columns; column++) {
                boolean allowed = role == MatchRole.PLANTS
                        ? column <= redLine : column > redLine;
                batch.setColor(allowed ? 0.24f : 0.55f,
                        allowed ? 0.78f : 0.28f,
                        allowed ? 0.30f : 0.24f,
                        0.10f * parentAlpha);
                batch.draw(pixel, getX() + column * cellWidth, getY(),
                        cellWidth, getHeight());
            }
            batch.setColor(1f, 1f, 1f, 0.32f * parentAlpha);
            for (int column = 0; column <= columns; column++) {
                batch.draw(pixel, getX() + column * cellWidth - 1f, getY(),
                        2f, getHeight());
            }
            for (int row = 0; row <= rows; row++) {
                batch.draw(pixel, getX(), getY() + row * cellHeight - 1f,
                        getWidth(), 2f);
            }
            batch.setColor(0.96f, 0.05f, 0.05f, 0.92f * parentAlpha);
            batch.draw(pixel, getX() + (redLine + 1) * cellWidth - 3f,
                    getY(), 6f, getHeight());
            batch.setColor(oldR, oldG, oldB, oldA);
        }
    }
}
