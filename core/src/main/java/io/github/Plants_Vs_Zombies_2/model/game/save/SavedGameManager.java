package io.github.Plants_Vs_Zombies_2.model.game.save;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.github.Plants_Vs_Zombies_2.model.auth.UserManager;
import io.github.Plants_Vs_Zombies_2.model.game.Game;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.projectile.Projectile;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.special.ConveyorPlantPacket;
import io.github.Plants_Vs_Zombies_2.model.game.special.ProtectedPlantStatus;
import io.github.Plants_Vs_Zombies_2.model.game.structure.BaseStructure;
import io.github.Plants_Vs_Zombies_2.model.game.structure.Grave;
import io.github.Plants_Vs_Zombies_2.model.menu.GameMenu;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Chapter;
import io.github.Plants_Vs_Zombies_2.model.roadmap.ChapterCatalog;
import io.github.Plants_Vs_Zombies_2.model.roadmap.Level;
import io.github.Plants_Vs_Zombies_2.model.user.User;

/**
 * Persistence for active adventure games.
 *
 * Saves intentionally live in a separate JSON database from users.json. The
 * JSON contains a readable state summary for debugging/grading and an exact
 * serialized Game payload used for reconstruction. The payload preserves the
 * complete polymorphic object graph: current sun and timers, loadout/boosts,
 * plants and zombies (including HP/positions/statuses), projectiles, structures,
 * waves, random state, lawn mowers, and every special-level subsystem.
 */
public final class SavedGameManager {
    private static final int CURRENT_VERSION = 1;
    private static final String DATABASE_PATH_PROPERTY = "pvz.games.database";
    private static final String DEFAULT_FILE_NAME = "saved_games.json";
    private static final Path databasePath = resolveDatabasePath();

    private SavedGameManager() {
    }

    public static Path getDatabasePath() {
        return databasePath;
    }

    public static synchronized void saveAdventureGame(User user, GameMenu menu) {
        validateAdventureSave(user, menu);

        List<SavedEntry> entries = loadEntries();
        removeMatching(entries, user.getUsername(),
                menu.getChapterId(), menu.getLevelNumber());
        entries.add(new SavedEntry(
                user.getUsername(),
                menu.getChapterId(),
                menu.getLevelNumber(),
                System.currentTimeMillis(),
                serializeGame(menu.getGame()),
                buildSummaryJson(menu.getGame(), menu.getLevel())));
        saveEntries(entries);
    }

    public static synchronized GameMenu loadAdventureGame(
            User user, String chapterId, int levelNumber) {
        if (user == null || chapterId == null || levelNumber <= 0) {
            return null;
        }
        SavedEntry entry = findEntry(loadEntries(), user.getUsername(),
                chapterId, levelNumber);
        if (entry == null) {
            return null;
        }

        Chapter chapter = ChapterCatalog.findById(chapterId);
        Level level = chapter == null ? null : chapter.getLevel(levelNumber);
        if (level == null) {
            deleteAdventureGame(user, chapterId, levelNumber);
            return null;
        }

        try {
            Game game = deserializeGame(entry.payload);
            return new GameMenu(game, chapterId, levelNumber, level);
        } catch (RuntimeException exception) {
            // Incompatible/corrupt saves should never make a level unusable.
            deleteAdventureGame(user, chapterId, levelNumber);
            return null;
        }
    }

    public static synchronized boolean hasAdventureGame(
            User user, String chapterId, int levelNumber) {
        if (user == null || chapterId == null || levelNumber <= 0) {
            return false;
        }
        return findEntry(loadEntries(), user.getUsername(),
                chapterId, levelNumber) != null;
    }

    public static synchronized void deleteAdventureGame(
            User user, String chapterId, int levelNumber) {
        if (user == null || chapterId == null || levelNumber <= 0) {
            return;
        }
        List<SavedEntry> entries = loadEntries();
        if (removeMatching(entries, user.getUsername(), chapterId, levelNumber)) {
            saveEntries(entries);
        }
    }

    private static void validateAdventureSave(User user, GameMenu menu) {
        if (user == null) {
            throw new IllegalArgumentException("login is required to save a game");
        }
        if (menu == null || menu.getChapterId() == null
                || menu.getLevelNumber() <= 0 || menu.getLevel() == null) {
            throw new IllegalArgumentException(
                    "only active adventure levels can be saved");
        }
    }

    private static Path resolveDatabasePath() {
        String configured = System.getProperty(DATABASE_PATH_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        Path usersPath = UserManager.getDatabasePath().toAbsolutePath()
                .normalize();
        Path parent = usersPath.getParent();
        return parent == null
                ? Path.of(DEFAULT_FILE_NAME)
                : parent.resolve(DEFAULT_FILE_NAME);
    }

    private static String serializeGame(Game game) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(game);
            output.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "game state contains data that cannot be saved", exception);
        }
    }

    private static Game deserializeGame(String payload) {
        try {
            byte[] bytes = Base64.getDecoder().decode(payload);
            try (ObjectInputStream input = new ObjectInputStream(
                    new ByteArrayInputStream(bytes))) {
                Object restored = input.readObject();
                if (!(restored instanceof Game)) {
                    throw new IllegalStateException("saved payload is not a Game");
                }
                return (Game) restored;
            }
        } catch (IOException | ClassNotFoundException
                | IllegalArgumentException exception) {
            throw new IllegalStateException("invalid saved game payload", exception);
        }
    }

    private static List<SavedEntry> loadEntries() {
        Path absolute = databasePath.toAbsolutePath().normalize();
        if (!Files.exists(absolute)) {
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(absolute, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return new ArrayList<>();
            }
            JsonValue root = new JsonReader().parse(json);
            int version = root.getInt("version", CURRENT_VERSION);
            if (version != CURRENT_VERSION) {
                throw new IllegalStateException(
                        "unsupported saved game database version: " + version);
            }
            JsonValue saves = root.get("saves");
            List<SavedEntry> entries = new ArrayList<>();
            if (saves == null) {
                return entries;
            }
            for (JsonValue value = saves.child; value != null; value = value.next) {
                String username = value.getString("username", null);
                String chapterId = value.getString("chapterId", null);
                int levelNumber = value.getInt("levelNumber", 0);
                long savedAt = value.getLong("savedAtMillis", 0L);
                String payload = value.getString("payload", null);
                if (username != null && chapterId != null
                        && levelNumber > 0 && payload != null) {
                    entries.add(new SavedEntry(username, chapterId,
                            levelNumber, savedAt, payload, null));
                }
            }
            return entries;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "could not read saved games database: " + absolute,
                    exception);
        }
    }

    private static void saveEntries(List<SavedEntry> entries) {
        Path absolute = databasePath.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        Path temporary = null;
        try {
            if (parent != null) {
                Files.createDirectories(parent);
                temporary = Files.createTempFile(parent,
                        "saved-games-", ".json.tmp");
            } else {
                temporary = Files.createTempFile("saved-games-", ".json.tmp");
            }
            Files.writeString(temporary, toJson(entries), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, absolute,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, absolute,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "could not save game database: " + absolute, exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static String toJson(List<SavedEntry> entries) {
        StringBuilder output = new StringBuilder();
        output.append("{\n  \"version\": ")
                .append(CURRENT_VERSION)
                .append(",\n  \"saves\": [");
        for (int index = 0; index < entries.size(); index++) {
            SavedEntry entry = entries.get(index);
            output.append(index == 0 ? "\n" : ",\n")
                    .append("    {\"username\": \"")
                    .append(escape(entry.username))
                    .append("\", \"chapterId\": \"")
                    .append(escape(entry.chapterId))
                    .append("\", \"levelNumber\": ")
                    .append(entry.levelNumber)
                    .append(", \"savedAtMillis\": ")
                    .append(entry.savedAtMillis);
            String summary = entry.summaryJson == null
                    ? rebuildSummary(entry)
                    : entry.summaryJson;
            if (summary != null) {
                output.append(", \"state\": ").append(summary);
            }
            output.append(", \"payload\": \"")
                    .append(entry.payload)
                    .append("\"}");
        }
        if (!entries.isEmpty()) {
            output.append('\n');
        }
        output.append("  ]\n}\n");
        return output.toString();
    }

    private static String rebuildSummary(SavedEntry entry) {
        try {
            Chapter chapter = ChapterCatalog.findById(entry.chapterId);
            Level level = chapter == null
                    ? null : chapter.getLevel(entry.levelNumber);
            if (level == null) {
                return null;
            }
            return buildSummaryJson(deserializeGame(entry.payload), level);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** Human-readable mirror of the most important state stored in payload. */
    private static String buildSummaryJson(Game game, Level level) {
        StringBuilder out = new StringBuilder();
        out.append('{')
                .append("\"sunAmount\":").append(game.getSunCount())
                .append(",\"plantFood\":").append(game.getPlantFoodCount())
                .append(",\"elapsedSeconds\":").append(game.getElapsedSeconds())
                .append(",\"waveNumber\":").append(game.getZombieWaveNumber())
                .append(",\"status\":\"").append(game.getStatus().name()).append('\"')
                .append(",\"specialLevelType\":\"")
                .append(level.getSpecialLevelType().name()).append('\"');

        out.append(",\"selectedPlants\":[");
        List<BasePlant> loadout = game.getPlantLoadoutPrototypes();
        for (int i = 0; i < loadout.size(); i++) {
            BasePlant plant = loadout.get(i);
            comma(out, i);
            out.append("{\"name\":\"").append(escape(plant.getName()))
                    .append("\",\"level\":").append(plant.getLevel()).append('}');
        }
        out.append(']');
        out.append(",\"boostedPlants\":[");
        appendStringArray(out, game.getBoostedPlantNames());
        out.append(']');
        out.append(",\"greenhouseBoostPlants\":[");
        appendStringArray(out, game.getGreenhouseBoostPlantNames());
        out.append(']');

        out.append(",\"plants\":[");
        List<BasePlant> plants = game.getBoard().getPlants();
        for (int i = 0; i < plants.size(); i++) {
            BasePlant plant = plants.get(i);
            EntityPosition position = plant.getEntityPosition();
            comma(out, i);
            out.append("{\"name\":\"").append(escape(plant.getName()))
                    .append("\",\"row\":").append(position == null ? -1 : position.getRow())
                    .append(",\"column\":").append(position == null ? -1 : position.getColumn())
                    .append(",\"hp\":").append(plant.getCurrentHP())
                    .append(",\"maxHp\":").append(plant.getBaseHP()).append('}');
        }
        out.append(']');

        out.append(",\"zombies\":[");
        List<Zombie> zombies = game.getBoard().getZombies();
        for (int i = 0; i < zombies.size(); i++) {
            Zombie zombie = zombies.get(i);
            comma(out, i);
            out.append("{\"type\":\"").append(zombie.getType().name())
                    .append("\",\"lane\":").append(zombie.getLane())
                    .append(",\"column\":").append(zombie.getColumnPosition())
                    .append(",\"hp\":").append(zombie.getHitPoints())
                    .append(",\"maxHp\":").append(zombie.getMaximumHitPoints())
                    .append('}');
        }
        out.append(']');

        out.append(",\"projectiles\":[");
        List<Projectile> projectiles = game.getBoard().getProjectiles();
        for (int i = 0; i < projectiles.size(); i++) {
            Projectile projectile = projectiles.get(i);
            comma(out, i);
            out.append("{\"type\":\"")
                    .append(escape(projectile.getClass().getSimpleName()))
                    .append("\",\"source\":\"")
                    .append(escape(projectile.getSourcePlantName()))
                    .append("\",\"row\":").append(projectile.getRowPosition())
                    .append(",\"column\":").append(projectile.getColumnPosition())
                    .append('}');
        }
        out.append(']');

        out.append(",\"structures\":[");
        List<BaseStructure> structures = game.getBoard().getStructures();
        for (int i = 0; i < structures.size(); i++) {
            BaseStructure structure = structures.get(i);
            EntityPosition position = structure.getPosition();
            comma(out, i);
            out.append("{\"type\":\"")
                    .append(escape(structure.getClass().getSimpleName()))
                    .append("\",\"row\":").append(position == null ? -1 : position.getRow())
                    .append(",\"column\":").append(position == null ? -1 : position.getColumn());
            if (structure instanceof Grave) {
                Grave grave = (Grave) structure;
                out.append(",\"hp\":").append(grave.getHitPoints())
                        .append(",\"reward\":\"").append(grave.getReward().name()).append('\"');
            }
            out.append('}');
        }
        out.append(']');

        appendSpecialState(out, game);
        out.append('}');
        return out.toString();
    }

    private static void appendSpecialState(StringBuilder out, Game game) {
        out.append(",\"special\":{")
                .append("\"wavesStarted\":").append(game.haveZombieWavesStarted());
        if (game.hasConveyorBelt()) {
            out.append(",\"conveyor\":{\"secondsUntilNextPacket\":")
                    .append(game.getConveyorSecondsUntilNextPacket())
                    .append(",\"packets\":[");
            List<ConveyorPlantPacket> packets = game.getConveyorPackets();
            for (int i = 0; i < packets.size(); i++) {
                ConveyorPlantPacket packet = packets.get(i);
                comma(out, i);
                out.append("{\"sequence\":").append(packet.getSequenceNumber())
                        .append(",\"plant\":\"")
                        .append(escape(packet.getPlantType())).append("\"}");
            }
            out.append("]}");
        }
        if (game.hasLockedPlants()) {
            out.append(",\"lockedPlants\":{\"mode\":\"")
                    .append(game.getLockedPlantsMode().name())
                    .append("\",\"types\":[");
            appendStringArray(out, game.getLockedPlantTypes());
            out.append("]}");
        }
        if (game.hasSaveOurSeeds()) {
            out.append(",\"saveOurSeeds\":[");
            List<ProtectedPlantStatus> statuses = game.getProtectedPlantStatuses();
            for (int i = 0; i < statuses.size(); i++) {
                ProtectedPlantStatus status = statuses.get(i);
                comma(out, i);
                out.append("{\"plant\":\"").append(escape(status.getPlantType()))
                        .append("\",\"hp\":").append(status.getCurrentHitPoints())
                        .append(",\"alive\":").append(status.isAlive()).append('}');
            }
            out.append(']');
        }
        if (game.hasTimedWar()) {
            out.append(",\"timedWar\":{\"objective\":\"")
                    .append(game.getTimedWarObjective().name())
                    .append("\",\"progress\":").append(game.getTimedWarProgress())
                    .append(",\"target\":").append(game.getTimedWarTarget())
                    .append(",\"remainingSeconds\":")
                    .append(game.getTimedWarRemainingSeconds()).append('}');
        }
        if (game.hasDeadLine()) {
            out.append(",\"deadLineColumn\":").append(game.getDeadLineColumn());
        }
        if (game.hasLoveYourPlants()) {
            out.append(",\"loveYourPlants\":{\"lost\":")
                    .append(game.getLostPlantCount())
                    .append(",\"maximumLost\":")
                    .append(game.getMaximumLostPlants()).append('}');
        }
        if (game.hasPlantWhatYouGet()) {
            out.append(",\"plantWhatYouGet\":true");
        }
        if (game.areSkySunsDisabled()) {
            out.append(",\"skySunsDisabledReason\":\"")
                    .append(escape(game.getSkySunDisabledReason())).append('\"');
        }
        out.append('}');
    }

    private static void appendStringArray(StringBuilder out, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            comma(out, i);
            out.append('\"').append(escape(values.get(i))).append('\"');
        }
    }

    private static void comma(StringBuilder out, int index) {
        if (index > 0) {
            out.append(',');
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            switch (ch) {
                case '\\': escaped.append("\\\\"); break;
                case '"': escaped.append("\\\""); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default: escaped.append(ch); break;
            }
        }
        return escaped.toString();
    }

    private static SavedEntry findEntry(List<SavedEntry> entries,
            String username, String chapterId, int levelNumber) {
        for (SavedEntry entry : entries) {
            if (entry.matches(username, chapterId, levelNumber)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean removeMatching(List<SavedEntry> entries,
            String username, String chapterId, int levelNumber) {
        return entries.removeIf(entry ->
                entry.matches(username, chapterId, levelNumber));
    }

    private static final class SavedEntry {
        private final String username;
        private final String chapterId;
        private final int levelNumber;
        private final long savedAtMillis;
        private final String payload;
        private final String summaryJson;

        private SavedEntry(String username, String chapterId,
                int levelNumber, long savedAtMillis, String payload,
                String summaryJson) {
            this.username = username;
            this.chapterId = chapterId;
            this.levelNumber = levelNumber;
            this.savedAtMillis = savedAtMillis;
            this.payload = payload;
            this.summaryJson = summaryJson;
        }

        private boolean matches(String requestedUsername,
                String requestedChapterId, int requestedLevelNumber) {
            return username.equals(requestedUsername)
                    && chapterId.equals(requestedChapterId)
                    && levelNumber == requestedLevelNumber;
        }
    }
}
