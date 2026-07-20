package model.game.presentation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import model.game.Board;
import model.game.Game;
import model.game.defense.LawnMower;
import model.game.entities.EntityPosition;
import model.game.entities.other.CollectibleDrop;
import model.game.entities.other.Coin;
import model.game.entities.other.Diamond;
import model.game.entities.other.PlantFoodDrop;
import model.game.entities.other.PotDrop;
import model.game.entities.other.Sun;
import model.game.entities.other.VaseSeedPacket;
import model.game.entities.plants.BasePlant;
import model.game.entities.plants.PlantFactory;
import model.game.entities.plants.explosive.ExplosivePlantType;
import model.game.entities.plants.homing.HomingPlantType;
import model.game.entities.plants.lobber.LobberPlantType;
import model.game.entities.plants.melee.MeleePlantType;
import model.game.entities.plants.modifier.ModifierPlantType;
import model.game.entities.plants.shooter.ShooterPlantType;
import model.game.entities.plants.strikeThrough.StrikeThroughPlantType;
import model.game.entities.plants.sunProducer.SunProducerPlantType;
import model.game.entities.plants.wallnut.WallnutPlantType;
import model.game.entities.zombies.Zombie;
import model.game.entities.zombies.armor.Armor;
import model.game.minigame.VaseBreaker;
import model.game.structure.BaseStructure;
import model.game.structure.Grave;
import model.game.structure.Vase;
import model.game.tile.Tile;
import model.game.tile.TileType;
/**
 * Produces the textual views required by the game-menu status commands.
 */
public final class GameStatusFormatter {
    private static final double COOLDOWN_EPSILON = 0.000001;
    private GameStatusFormatter() {
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
                .append(" D=drops")
                .append(System.lineSeparator());
        for (int row = 0;
                row < board.getNumberOfRows(); row++) {
            output.append("row ").append(row).append(": ");
            for (int column = 0;
                    column < board.getNumberOfColumns();
                    column++) {
                EntityPosition position =
                        new EntityPosition(row, column);
                Tile tile = board.getTileAt(position);
                int plants =
                        board.getPlantsAt(position).size();
                int zombies = getZombiesAt(
                        board, row, column).size();
                int suns = board.getSunsAt(position).size();
                int drops = board.getCollectibleDropsAt(position).size();
                int structures =
                        board.getStructureAt(position) == null
                                ? 0 : 1;
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
                        .append(']');
                if (column
                        < board.getNumberOfColumns() - 1) {
                    output.append(' ');
                }
            }
            output.append(System.lineSeparator());
        }
        appendExactZombiePositions(output, board);
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
            double cooldown =
                    game.getPlantCooldownRemainingSeconds(
                            plant);
            boolean enoughSun =
                    game.getSunCount() >= plant.getCost();
            boolean cooldownReady =
                    cooldown <= COOLDOWN_EPSILON;
            boolean plantable =
                    enoughSun && cooldownReady;
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
        BaseStructure structure =
                board.getStructureAt(position);
        output.append("structure: ")
                .append(formatStructure(structure))
                .append(System.lineSeparator());
        appendPlantDetails(
                output, board.getPlantsAt(position));
        appendZombieDetails(
                output,
                getZombiesAt(
                        board,
                        position.getRow(),
                        position.getColumn()));
        appendSunDetails(
                output, board.getSunsAt(position));
        appendDropDetails(
                output, board.getCollectibleDropsAt(position));
        LawnMower mower = game.getLawnMowerAtRow(
                position.getRow());
        output.append("row lawn mower: ")
                .append(mower.isAvailable()
                        ? "ready" : "used");
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
        output.append("lawn mowers: ");
        List<LawnMower> mowers = game.getLawnMowers();
        for (int index = 0;
                index < mowers.size(); index++) {
            LawnMower mower = mowers.get(index);
            output.append("row ")
                    .append(mower.getRow())
                    .append('=')
                    .append(mower.isAvailable()
                            ? "ready" : "used");
            if (index < mowers.size() - 1) {
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

    private static void appendExactZombiePositions(
            StringBuilder output, Board board) {
        List<Zombie> zombies = board.getZombies();
        output.append("exact zombie positions:");
        if (zombies.isEmpty()) {
            output.append(" none");
            return;
        }
        for (Zombie zombie : zombies) {
            output.append(System.lineSeparator())
                    .append("- ")
                    .append(zombie.getName())
                    .append(" at (")
                    .append(zombie.getLane())
                    .append(", ")
                    .append(String.format(
                            Locale.ROOT,
                            "%.2f",
                            zombie.getColumnPosition()))
                    .append(')');
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
                            ? "necromancy" : "ordinary")
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
    private static void appendZombieDetails(
            StringBuilder output,
            List<Zombie> zombies) {
        output.append("zombies:");
        if (zombies.isEmpty()) {
            output.append(" none")
                    .append(System.lineSeparator());
            return;
        }
        output.append(System.lineSeparator());
        for (Zombie zombie : zombies) {
            output.append("- ")
                    .append(zombie.getName())
                    .append(" | type: ")
                    .append(zombie.getType())
                    .append(" | exact position: (")
                    .append(zombie.getLane())
                    .append(", ")
                    .append(String.format(
                            Locale.ROOT,
                            "%.2f",
                            zombie.getColumnPosition()))
                    .append(") | hp: ")
                    .append(zombie.getHitPoints())
                    .append('/')
                    .append(zombie.getMaximumHitPoints())
                    .append(" | wave: ")
                    .append(zombie.getWaveNumber())
                    .append(" | armor: ")
                    .append(armorState(zombie))
                    .append(" | effects: ")
                    .append(zombieEffects(zombie))
                    .append(System.lineSeparator());
        }
    }
    private static String armorState(Zombie zombie) {
        Armor armor = zombie.getArmor();
        if (armor == null || armor.isDestroyed()) {
            return "none";
        }
        return armor.getType().getDisplayName()
                + " " + armor.getCurrentHealth()
                + "/" + armor.getMaximumHealth();
    }
    private static String zombieEffects(Zombie zombie) {
        List<String> effects = new ArrayList<>();
        if (zombie.isEncasedInIce()) {
            effects.add("encased in ice "
                    + zombie.getFrozenShellHitPoints()
                    + "/"
                    + zombie.getFrozenShellMaximumHitPoints()
                    + " HP");
        }
        if (zombie.isFrozen()) {
            effects.add("frozen "
                    + formatSeconds(
                            zombie.getFrozenDuration()));
        }
        if (zombie.isChilled()) {
            effects.add("chilled "
                    + formatSeconds(
                            zombie.getChilledDuration()));
        }
        if (zombie.isStunned()) {
            effects.add("stunned "
                    + formatSeconds(
                            zombie.getStunnedDuration()));
        }
        if (zombie.isHypnotized()) {
            effects.add("hypnotized");
        }
        if (zombie.isGlowing()) {
            effects.add("glowing");
        }
        if (zombie.getPoisonDurationSeconds() > 0.0) {
            effects.add("poisoned "
                    + formatSeconds(
                            zombie
                                    .getPoisonDurationSeconds()));
        }
        return effects.isEmpty()
                ? "none" : String.join(", ", effects);
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
                            ? "yes" : "no")
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
    private static List<Zombie> getZombiesAt(
            Board board, int row, int column) {
        List<Zombie> result = new ArrayList<>();
        for (Zombie zombie : board.getZombies()) {
            if (zombie.getLane() != row) {
                continue;
            }
            int zombieColumn =
                    (int) Math.floor(
                            zombie.getColumnPosition());
            if (zombieColumn == column) {
                result.add(zombie);
            }
        }
        return result;
    }
    private static List<BasePlant> createPlantCatalog() {
        List<BasePlant> plants = new ArrayList<>();
        for (SunProducerPlantType type
                : SunProducerPlantType.values()) {
            addPlant(plants, type.name());
        }
        for (ShooterPlantType type
                : ShooterPlantType.values()) {
            addPlant(plants, type.name());
        }
        for (HomingPlantType type
                : HomingPlantType.values()) {
            addPlant(plants, type.name());
        }
        for (LobberPlantType type
                : LobberPlantType.values()) {
            addPlant(plants, type.name());
        }
        for (StrikeThroughPlantType type
                : StrikeThroughPlantType.values()) {
            addPlant(plants, type.name());
        }
        for (ExplosivePlantType type
                : ExplosivePlantType.values()) {
            addPlant(plants, type.name());
        }
        for (MeleePlantType type
                : MeleePlantType.values()) {
            addPlant(plants, type.name());
        }
        for (ModifierPlantType type
                : ModifierPlantType.values()) {
            addPlant(plants, type.name());
        }
        for (WallnutPlantType type
                : WallnutPlantType.values()) {
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
                            ? "necromancy" : "ordinary")
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
