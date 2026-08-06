package io.github.some_example_name.view.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.github.some_example_name.model.game.Board;
import io.github.some_example_name.model.game.Game;
import io.github.some_example_name.model.game.defense.LawnMower;
import io.github.some_example_name.model.game.entities.EntityPosition;
import io.github.some_example_name.model.game.entities.other.Coin;
import io.github.some_example_name.model.game.entities.other.CollectibleDrop;
import io.github.some_example_name.model.game.entities.other.Diamond;
import io.github.some_example_name.model.game.entities.other.PlantFoodDrop;
import io.github.some_example_name.model.game.entities.other.PotDrop;
import io.github.some_example_name.model.game.entities.other.Sun;
import io.github.some_example_name.model.game.entities.other.VaseSeedPacket;
import io.github.some_example_name.model.game.entities.plants.BasePlant;
import io.github.some_example_name.model.game.entities.plants.PlantFactory;
import io.github.some_example_name.model.game.entities.plants.explosive.ExplosivePlantType;
import io.github.some_example_name.model.game.entities.plants.homing.HomingPlantType;
import io.github.some_example_name.model.game.entities.plants.lobber.LobberPlantType;
import io.github.some_example_name.model.game.entities.plants.melee.MeleePlantType;
import io.github.some_example_name.model.game.entities.plants.modifier.ModifierPlantType;
import io.github.some_example_name.model.game.entities.plants.shooter.ShooterPlantType;
import io.github.some_example_name.model.game.entities.plants.strikeThrough.StrikeThroughPlantType;
import io.github.some_example_name.model.game.entities.plants.sunProducer.SunProducerPlantType;
import io.github.some_example_name.model.game.entities.plants.wallnut.WallnutPlantType;
import io.github.some_example_name.model.game.minigame.BowlingWallnut;
import io.github.some_example_name.model.game.minigame.IZombie;
import io.github.some_example_name.model.game.minigame.VaseBreaker;
import io.github.some_example_name.model.game.minigame.WallnutBowling;
import io.github.some_example_name.model.game.scored.ScoredGame;
import io.github.some_example_name.model.game.structure.BaseStructure;
import io.github.some_example_name.model.game.structure.Grave;
import io.github.some_example_name.model.game.structure.Vase;
import io.github.some_example_name.model.game.tile.Tile;
import io.github.some_example_name.model.game.tile.TileType;

/**
 * Produces the textual views required by the game-menu status commands.
 */
public final class GameStatusView {
    private static final double COOLDOWN_EPSILON = 0.000001;

    private GameStatusView() {
    }

    public static String formatMap(Game game) {
        requireGame(game);
        Board board = game.getBoard();
        StringBuilder output = new StringBuilder();
        appendGameHeader(output, game);
        output.append(System.lineSeparator())
                .append("legend: terrain ")
                .append("N=normal G=empty-grave GS=sun-grave ")
                .append("GP=plant-food-grave NG=necromancy-grave ")
                .append("NGS=necromancy-sun-grave ")
                .append("NGP=necromancy-plant-food-grave ")
                .append("SU=slider-up ")
                .append("SD=slider-down SL=slippery F=frozen ")
                .append("W=water LB=low-beach ")
                .append("LBW=submerged-low-beach ")
                .append("NE=necromancy C=crater; ")
                .append("P=plants Z=zombies S=suns R=structures")
                .append(" V=vase marker (?=normal P=plant G=giant)")
                .append(" D=drops B=rolling-bowling-wallnuts")
                .append(System.lineSeparator());
        for (int row = 0; row < board.getNumberOfRows(); row++) {
            output.append("row ").append(row).append(": ");
            for (int column = 0; column < board.getNumberOfColumns(); column++) {
                EntityPosition position = new EntityPosition(row, column);
                Tile tile = board.getTileAt(position);
                int plants = board.getPlantsAt(position).size();
                int zombies = ZombieView.getZombiesAt(
                        board, row, column).size();
                int suns = board.getSunsAt(position).size();
                int drops = board.getCollectibleDropsAt(position).size();
                int bowlingWallnuts = getBowlingWallnutsAt(
                        game, row, column).size();
                int structures = board.getStructureAt(position) == null
                        ? 0
                        : 1;
                String vase = vaseSymbol(
                        board.getStructureAt(position));
                output.append('[')
                        .append(terrainCode(board, position,
                                tile.getTileType()))
                        .append(" P").append(plants)
                        .append(" Z").append(zombies)
                        .append(" S").append(suns)
                        .append(" R").append(structures)
                        .append(" V").append(vase)
                        .append(" D").append(drops)
                        .append(" B").append(bowlingWallnuts)
                        .append(']');
                if (column < board.getNumberOfColumns() - 1) {
                    if (isMinigameRedLineAfter(game, column)) {
                        output.append(" ||RED|| ");
                    } else {
                        output.append(' ');
                    }
                }
            }
            output.append(System.lineSeparator());
        }
        ZombieView.appendExactPositions(output, board);
        appendExactBowlingWallnutPositions(output, game);
        return trimTrailingLineSeparator(output);
    }

    public static String formatPlantStatuses(Game game) {
        requireGame(game);
        List<BasePlant> plants = game.hasConfiguredPlantLoadout()
                ? new ArrayList<>(game.getPlantLoadoutPrototypes())
                : createPlantCatalog();
        plants.sort(Comparator.comparing(
                BasePlant::getName,
                String.CASE_INSENSITIVE_ORDER));
        StringBuilder output = new StringBuilder();
        output.append("plant statuses")
                .append(System.lineSeparator())
                .append("current sun: ")
                .append(game.getSunCount())
                .append(System.lineSeparator());
        for (BasePlant plant : plants) {
            double cooldown = game.getPlantCooldownRemainingSeconds(
                    plant);
            boolean enoughSun = game.getSunCount() >= plant.getCost();
            boolean cooldownReady = cooldown <= COOLDOWN_EPSILON;
            boolean plantable = enoughSun && cooldownReady;
            output.append("- ")
                    .append(plant.getName())
                    .append(" | cost: ")
                    .append(plant.getCost())
                    .append(" | recharge: ")
                    .append(formatSeconds(
                            plant.getRechargeSeconds()))
                    .append(" | plantable: ")
                    .append(plantable ? "yes" : "no");
            if (!plantable) {
                output.append(" | reason: ");
                if (!enoughSun) {
                    output.append("needs ")
                            .append(plant.getCost()
                                    - game.getSunCount())
                            .append(" more sun");
                }
                if (!enoughSun && !cooldownReady) {
                    output.append("; ");
                }
                if (!cooldownReady) {
                    output.append("cooldown ")
                            .append(formatSeconds(cooldown))
                            .append(" remaining");
                }
            }
            output.append(System.lineSeparator());
        }
        return trimTrailingLineSeparator(output);
    }

    public static String formatTileStatus(
            Game game, EntityPosition position) {
        requireGame(game);
        if (position == null
                || !game.getBoard()
                        .isPositionInsideBoard(position)) {
            throw new IllegalArgumentException(
                    "position must be inside the board");
        }
        Board board = game.getBoard();
        Tile tile = board.getTileAt(position);
        StringBuilder output = new StringBuilder();
        output.append("tile ").append(position)
                .append(System.lineSeparator())
                .append("terrain: ")
                .append(formatTerrain(board, position, tile))
                .append(System.lineSeparator());
        BaseStructure structure = board.getStructureAt(position);
        output.append("structure: ")
                .append(formatStructure(structure))
                .append(System.lineSeparator());
        appendPlantDetails(
                output, board.getPlantsAt(position));
        ZombieView.appendDetails(
                output,
                ZombieView.getZombiesAt(
                        board,
                        position.getRow(),
                        position.getColumn()));
        appendBowlingWallnutDetails(output,
                getBowlingWallnutsAt(game, position.getRow(),
                        position.getColumn()));
        appendSunDetails(
                output, board.getSunsAt(position));
        appendDropDetails(
                output, board.getCollectibleDropsAt(position));
        if (game instanceof IZombie) {
            IZombie iZombie = (IZombie) game;
            output.append("row brain: ")
                    .append(iZombie.isBrainAvailable(
                            position.getRow())
                                    ? "available"
                                    : "eaten");
        } else {
            LawnMower mower = game.getLawnMowerAtRow(
                    position.getRow());
            output.append("row lawn mower: ")
                    .append(mower.isAvailable()
                            ? "ready"
                            : "used");
        }
        return output.toString();
    }

    private static void appendGameHeader(
            StringBuilder output, Game game) {
        int totalWaves = game.getZombieWaves().size();
        output.append("wave: ")
                .append(game.getZombieWaveNumber())
                .append('/').append(totalWaves)
                .append(System.lineSeparator())
                .append("plant food: ")
                .append(game.getPlantFoodCount())
                .append('/')
                .append(game.getMaximumPlantFoodCount())
                .append(System.lineSeparator())
                .append("sun: ")
                .append(game.getSunCount())
                .append(System.lineSeparator())
                .append("elapsed time: ")
                .append(formatSeconds(
                        game.getElapsedSeconds()))
                .append(System.lineSeparator())
                .append("game status: ")
                .append(game.getStatus())
                .append(System.lineSeparator())
                .append("sky suns: ")
                .append(game.areSkySunsDisabled()
                        ? "disabled (" + game.getSkySunDisabledReason() + ")"
                        : "enabled")
                .append(System.lineSeparator());
        appendTideStatus(output, game.getBoard());
        appendVaseBreakerStatus(output, game);
        appendWallnutBowlingStatus(output, game);
        appendIZombieStatus(output, game);
        appendScoredGameStatus(output, game);
        if (game instanceof IZombie) {
            appendBrainSummary(output, (IZombie) game);
        } else {
            appendLawnMowerSummary(output, game);
        }
    }

    private static void appendScoredGameStatus(
            StringBuilder output, Game game) {
        if (!(game instanceof ScoredGame)) {
            return;
        }
        ScoredGame scoredGame = (ScoredGame) game;
        output.append("daily scored challenge: ")
                .append(scoredGame.getChallengeDate())
                .append(" UTC")
                .append(System.lineSeparator())
                .append("MowPoint: ")
                .append(scoredGame.getScore())
                .append(System.lineSeparator());
    }

    private static void appendLawnMowerSummary(
            StringBuilder output, Game game) {
        output.append("lawn mowers: ");
        List<LawnMower> mowers = game.getLawnMowers();
        for (int index = 0; index < mowers.size(); index++) {
            LawnMower mower = mowers.get(index);
            output.append("row ").append(mower.getRow())
                    .append('=').append(mower.isAvailable()
                            ? "ready"
                            : "used");
            if (index < mowers.size() - 1) {
                output.append(", ");
            }
        }
    }

    private static void appendBrainSummary(
            StringBuilder output, IZombie game) {
        output.append("brains: ");
        for (int row = 0; row < game.getBoard().getNumberOfRows(); row++) {
            output.append("row ").append(row).append('=')
                    .append(game.isBrainAvailable(row)
                            ? "available"
                            : "eaten");
            if (row < game.getBoard().getNumberOfRows() - 1) {
                output.append(", ");
            }
        }
    }

    private static String vaseSymbol(BaseStructure structure) {
        if (!(structure instanceof Vase)) {
            return "-";
        }
        return ((Vase) structure).getType().getMapSymbol();
    }

    private static void appendVaseBreakerStatus(
            StringBuilder output, Game game) {
        if (!(game instanceof VaseBreaker)) {
            return;
        }
        VaseBreaker vaseBreaker = (VaseBreaker) game;
        output.append("vase breaker level: ")
                .append(vaseBreaker.getLevel().getNumber())
                .append(" - ")
                .append(vaseBreaker.getLevel().getName())
                .append(System.lineSeparator())
                .append("unbroken vases: ")
                .append(vaseBreaker.getVases().size())
                .append(System.lineSeparator())
                .append("available vase seed packets: ")
                .append(vaseBreaker.getAvailableSeedPackets().size())
                .append(System.lineSeparator());
    }

    private static void appendWallnutBowlingStatus(
            StringBuilder output, Game game) {
        if (!(game instanceof WallnutBowling)) {
            return;
        }
        WallnutBowling bowling = (WallnutBowling) game;
        output.append("Wall-nut Bowling level: ")
                .append(bowling.getLevel().getNumber())
                .append(" - ")
                .append(bowling.getLevel().getName())
                .append(System.lineSeparator())
                .append("red bowling line: launch columns 0 through ")
                .append(bowling.getRedLineColumn())
                .append(System.lineSeparator())
                .append("rolling Wall-nuts: ")
                .append(bowling.getRollingWallnuts().size())
                .append(System.lineSeparator());
    }

    private static void appendIZombieStatus(
            StringBuilder output, Game game) {
        if (!(game instanceof IZombie)) {
            return;
        }
        IZombie iZombie = (IZombie) game;
        output.append("I, Zombie level: ")
                .append(iZombie.getLevel().getNumber())
                .append(" - ").append(iZombie.getLevel().getName())
                .append(System.lineSeparator())
                .append("red line: plants in columns 0 through ")
                .append(iZombie.getRedLineColumn())
                .append("; zombie placement starts at column ")
                .append(iZombie.getRedLineColumn() + 1)
                .append(System.lineSeparator())
                .append("sun producers alive: ")
                .append(iZombie.getLivingSunProducerCount())
                .append('/').append(iZombie.getBoard().getNumberOfRows())
                .append(System.lineSeparator())
                .append("remaining plants: ")
                .append(iZombie.getRemainingPlantCount())
                .append(System.lineSeparator());
    }

    private static void appendExactBowlingWallnutPositions(
            StringBuilder output, Game game) {
        if (!(game instanceof WallnutBowling)) {
            return;
        }
        WallnutBowling bowling = (WallnutBowling) game;
        output.append(System.lineSeparator())
                .append("exact rolling Wall-nut positions:");
        if (bowling.getRollingWallnuts().isEmpty()) {
            output.append(" none");
            return;
        }
        for (BowlingWallnut wallnut : bowling.getRollingWallnuts()) {
            output.append(System.lineSeparator())
                    .append("- ")
                    .append(bowling.describeRollingWallnut(wallnut));
        }
    }

    private static boolean isMinigameRedLineAfter(
            Game game, int column) {
        if (game instanceof WallnutBowling) {
            return ((WallnutBowling) game)
                    .getRedLineColumn() == column;
        }
        return game instanceof IZombie
                && ((IZombie) game).getRedLineColumn() == column;
    }

    private static List<BowlingWallnut> getBowlingWallnutsAt(
            Game game, int row, int column) {
        if (!(game instanceof WallnutBowling)) {
            return List.of();
        }
        return ((WallnutBowling) game).getRollingWallnutsAt(row, column);
    }

    private static void appendBowlingWallnutDetails(
            StringBuilder output, List<BowlingWallnut> wallnuts) {
        output.append("rolling Wall-nuts:");
        if (wallnuts.isEmpty()) {
            output.append(" none").append(System.lineSeparator());
            return;
        }
        output.append(System.lineSeparator());
        for (BowlingWallnut wallnut : wallnuts) {
            output.append("- ")
                    .append(wallnut.getType().getDisplayName())
                    .append(" #").append(wallnut.getId())
                    .append(" | exact position: (")
                    .append(String.format(Locale.ROOT, "%.2f",
                            wallnut.getRowPosition()))
                    .append(", ")
                    .append(String.format(Locale.ROOT, "%.2f",
                            wallnut.getColumnPosition()))
                    .append(") | direction: ")
                    .append(wallnut.getDirectionDescription())
                    .append(System.lineSeparator());
        }
    }

    private static String formatStructure(
            BaseStructure structure) {
        if (structure == null) {
            return "none";
        }
        if (structure instanceof Grave) {
            Grave grave = (Grave) structure;
            return "Grave " + grave.getHitPoints() + "/"
                    + Grave.DEFAULT_HIT_POINTS + " HP | type: "
                    + (grave.isNecromancyGrave()
                            ? "necromancy"
                            : "ordinary")
                    + " | contents: "
                    + grave.getReward().getDescription();
        }
        if (structure instanceof Vase) {
            Vase vase = (Vase) structure;
            return vase.getType().getDisplayName() + " ["
                    + vase.getType().getMapSymbol()
                    + "] | contents: hidden until broken";
        }
        return structure.getClass().getSimpleName();
    }

    private static void appendPlantDetails(
            StringBuilder output,
            List<BasePlant> plants) {
        output.append("plants:");
        if (plants.isEmpty()) {
            output.append(" none")
                    .append(System.lineSeparator());
            return;
        }
        output.append(System.lineSeparator());
        for (BasePlant plant : plants) {
            output.append("- ")
                    .append(plant.getName())
                    .append(" | category: ")
                    .append(plant.getCategory())
                    .append(" | level: ")
                    .append(plant.getLevel())
                    .append(" | hp: ")
                    .append(plant.getCurrentHP())
                    .append('/')
                    .append(plant.getBaseHP())
                    .append(" | damage: ")
                    .append(plant.getDamage())
                    .append(" | cost: ")
                    .append(plant.getCost())
                    .append(" | recharge: ")
                    .append(formatSeconds(
                            plant.getRechargeSeconds()))
                    .append(" | state: ")
                    .append(plantState(plant))
                    .append(System.lineSeparator());
        }
    }

    private static String plantState(BasePlant plant) {
        List<String> states = new ArrayList<>();
        if (plant.isFrozen()) {
            states.add("frozen; ice "
                    + plant.getIceShellHitPoints() + "/"
                    + plant.getIceShellMaximumHitPoints() + " HP");
        } else if (plant.getFreezeLevel() > 0) {
            states.add("freeze level " + plant.getFreezeLevel()
                    + "/" + BasePlant.MAX_FREEZE_LEVEL);
        }
        if (plant.isCoveredByOctopus()) {
            states.add("octopus; hits "
                    + plant.getOctopusHitsRemaining());
        }
        if (plant.isTransformedToSheep()) {
            states.add("transformed to cat");
        }
        if (states.isEmpty()) {
            return "active";
        }
        return String.join(", ", states);
    }

    private static void appendSunDetails(
            StringBuilder output, List<Sun> suns) {
        output.append("suns:");
        if (suns.isEmpty()) {
            output.append(" none")
                    .append(System.lineSeparator());
            return;
        }
        output.append(System.lineSeparator());
        for (Sun sun : suns) {
            output.append("- amount: ")
                    .append(sun.getSunAmount())
                    .append(" | state: ")
                    .append(sun.isDropping()
                            ? "dropping"
                            : sun.isCollectable()
                                    ? "collectable"
                                    : "unavailable")
                    .append(" | persistent: ")
                    .append(sun.isPersistent()
                            ? "yes"
                            : "no")
                    .append(System.lineSeparator());
        }
    }

    private static void appendDropDetails(
            StringBuilder output, List<CollectibleDrop> drops) {
        output.append("drops:");
        if (drops.isEmpty()) {
            output.append(" none")
                    .append(System.lineSeparator());
            return;
        }
        output.append(System.lineSeparator());
        for (CollectibleDrop drop : drops) {
            output.append("- ")
                    .append(dropName(drop))
                    .append(" | despawns in: ")
                    .append(formatSeconds(Math.max(0.0,
                            drop.getLifeSpanSeconds()
                                    - drop.getElapsedSeconds())))
                    .append(System.lineSeparator());
        }
    }

    private static String dropName(CollectibleDrop drop) {
        if (drop instanceof PlantFoodDrop) {
            return "plant food";
        }
        if (drop instanceof Coin) {
            return "50 coins";
        }
        if (drop instanceof Diamond) {
            return "1 diamond";
        }
        if (drop instanceof PotDrop) {
            return "1 pot";
        }
        if (drop instanceof VaseSeedPacket) {
            VaseSeedPacket packet = (VaseSeedPacket) drop;
            return packet.getPlantType() + " one-use vase seed packet";
        }
        return drop.getClass().getSimpleName();
    }

    private static List<BasePlant> createPlantCatalog() {
        List<BasePlant> plants = new ArrayList<>();
        for (SunProducerPlantType type : SunProducerPlantType.values()) {
            addPlant(plants, type.name());
        }
        for (ShooterPlantType type : ShooterPlantType.values()) {
            addPlant(plants, type.name());
        }
        for (HomingPlantType type : HomingPlantType.values()) {
            addPlant(plants, type.name());
        }
        for (LobberPlantType type : LobberPlantType.values()) {
            addPlant(plants, type.name());
        }
        for (StrikeThroughPlantType type : StrikeThroughPlantType.values()) {
            addPlant(plants, type.name());
        }
        for (ExplosivePlantType type : ExplosivePlantType.values()) {
            addPlant(plants, type.name());
        }
        for (MeleePlantType type : MeleePlantType.values()) {
            addPlant(plants, type.name());
        }
        for (ModifierPlantType type : ModifierPlantType.values()) {
            addPlant(plants, type.name());
        }
        for (WallnutPlantType type : WallnutPlantType.values()) {
            addPlant(plants, type.name());
        }
        return plants;
    }

    private static void addPlant(
            List<BasePlant> plants, String typeName) {
        BasePlant plant = PlantFactory.createPlant(
                typeName, new EntityPosition(0, 0));
        if (plant != null) {
            plants.add(plant);
        }
    }

    private static void appendTideStatus(
            StringBuilder output, Board board) {
        if (!board.isBigWaveBeachRulesEnabled()) {
            return;
        }
        output.append("tide: ")
                .append(board.getWaterColumnCount())
                .append('/')
                .append(board.getMaximumWaterColumnCount())
                .append(" rightmost columns; limit begins at column ")
                .append(board.getWaterBoundaryColumn())
                .append(System.lineSeparator());
    }

    private static String formatTerrain(Board board,
            EntityPosition position, Tile tile) {
        if (board.isSubmergedLowBeachTile(position)) {
            return "WATER (submerged LOW_BEACH)";
        }
        BaseStructure structure = board.getStructureAt(position);
        if (structure instanceof Grave) {
            Grave grave = (Grave) structure;
            return "GRAVESTONE ("
                    + (grave.isNecromancyGrave()
                            ? "necromancy"
                            : "ordinary")
                    + "; contents: "
                    + grave.getReward().getDescription() + ")";
        }
        return tile.getTileType().toString();
    }

    private static String terrainCode(Board board,
            EntityPosition position, TileType tileType) {
        if (board.isSubmergedLowBeachTile(position)) {
            return "LBW";
        }
        BaseStructure structure = board.getStructureAt(position);
        if (structure instanceof Grave) {
            return graveTerrainCode((Grave) structure);
        }
        switch (tileType) {
            case NORMAL:
                return "N";
            case GRAVESTONE:
                return "G";
            case SLIDER_UP:
                return "SU";
            case SLIDER_DOWN:
                return "SD";
            case SLIPPERY:
                return "SL";
            case FROZEN:
                return "F";
            case WATER:
                return "W";
            case LOW_BEACH:
                return "LB";
            case NECROMANCY:
                return "NE";
            case CRATER:
                return "C";
            default:
                throw new IllegalStateException(
                        "unknown tile type: " + tileType);
        }
    }

    private static String graveTerrainCode(Grave grave) {
        String prefix = grave.isNecromancyGrave() ? "NG" : "G";
        switch (grave.getReward()) {
            case SUN:
                return prefix + "S";
            case PLANT_FOOD:
                return prefix + "P";
            case NONE:
                return prefix;
            default:
                throw new IllegalStateException(
                        "unknown grave reward");
        }
    }

    private static String formatSeconds(double seconds) {
        return String.format(
                Locale.ROOT, "%.1fs", seconds);
    }

    private static String trimTrailingLineSeparator(
            StringBuilder output) {
        String separator = System.lineSeparator();
        while (output.length() >= separator.length()
                && output.substring(
                        output.length()
                                - separator.length())
                        .equals(separator)) {
            output.setLength(
                    output.length() - separator.length());
        }
        return output.toString();
    }

    private static void requireGame(Game game) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null");
        }
    }
}
