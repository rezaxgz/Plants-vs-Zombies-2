package io.github.Plants_Vs_Zombies_2.model.game.minigame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.github.Plants_Vs_Zombies_2.model.game.Board;
import io.github.Plants_Vs_Zombies_2.model.game.Game;
import io.github.Plants_Vs_Zombies_2.model.game.GameStatus;
import io.github.Plants_Vs_Zombies_2.model.game.entities.EntityPosition;
import io.github.Plants_Vs_Zombies_2.model.game.entities.other.VaseSeedPacket;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.BasePlant;
import io.github.Plants_Vs_Zombies_2.model.game.entities.plants.PlantFactory;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.Zombie;
import io.github.Plants_Vs_Zombies_2.model.game.entities.zombies.ZombieType;
import io.github.Plants_Vs_Zombies_2.model.game.structure.BaseStructure;
import io.github.Plants_Vs_Zombies_2.model.game.structure.Vase;
import io.github.Plants_Vs_Zombies_2.model.game.structure.VaseContentType;
import io.github.Plants_Vs_Zombies_2.model.game.structure.VaseType;
import io.github.Plants_Vs_Zombies_2.view.game.VaseBreakerView;

/**
 * Fully playable Vase Breaker minigame.
 */
public final class VaseBreaker extends Game {
    private static final int FIRST_VASE_COLUMN = 4;

    private final VaseBreakerLevel level;
    private final Random random;
    private final List<VaseSeedPacket> seedPackets = new ArrayList<>();

    public VaseBreaker(VaseBreakerLevel level) {
        this(level, new Random());
    }

    public VaseBreaker(VaseBreakerLevel level, Random random) {
        super(new Board(), null, 0, Collections.emptyList(), false);
        if (level == null || random == null) {
            throw new IllegalArgumentException(
                    "Vase Breaker level and random source are required");
        }
        this.level = level;
        this.random = random;
        disableSkySuns("Vase Breaker has no falling sun");
        placeVases();
        addPendingResult("Vase Breaker level " + level.getNumber()
                + " started: " + level.getName() + ".");
        addPendingResult("Break every vase, plant one-use seed packets before "
                + "they disappear, and defeat every released zombie.");
    }

    @Override
    public void update(float deltaSeconds) {
        super.update(deltaSeconds);
        if (getStatus() != GameStatus.ACTIVE) {
            return;
        }
        if (hasEscapedHostileZombie()) {
            completeGameAsLost(
                    "A zombie released from a vase reached the house; "
                            + "Vase Breaker lost!");
            return;
        }
        reportExpiredSeedPackets();
        checkForVaseBreakerWin();
    }

    @Override
    public void releaseNuke() {
        super.releaseNuke();
        checkForVaseBreakerWin();
    }

    @Override
    public boolean allowsDirectPlanting() {
        return false;
    }

    @Override
    public String getDirectPlantingDisabledMessage() {
        return "ordinary planting is disabled in Vase Breaker; "
                + "use a vase seed packet instead!";
    }

    @Override
    protected boolean shouldProcessZombieDeathDrops() {
        return false;
    }

    @Override
    protected boolean usesLawnMowers() {
        // Vase Breaker has no lawn-mower safety net. A zombie that reaches
        // the house is an immediate minigame loss rather than being silently
        // removed by the normal adventure mower system.
        return false;
    }

    private boolean hasEscapedHostileZombie() {
        for (Zombie zombie : getBoard().getZombies()) {
            if (zombie != null
                    && !zombie.isDead()
                    && !zombie.isRemoved()
                    && !zombie.isHypnotized()
                    && zombie.hasReachedHouse()) {
                return true;
            }
        }
        return false;
    }

    public VaseBreakResult breakVase(EntityPosition position) {
        if (getStatus() != GameStatus.ACTIVE) {
            return VaseBreakResult.GAME_NOT_ACTIVE;
        }
        if (!getBoard().isPositionInsideBoard(position)) {
            return VaseBreakResult.INVALID_POSITION;
        }
        BaseStructure structure = getBoard().getStructureAt(position);
        if (!(structure instanceof Vase)) {
            return VaseBreakResult.NO_VASE;
        }

        Vase vase = (Vase) structure;
        if (!vase.breakVase()) {
            return VaseBreakResult.NO_VASE;
        }
        VaseBreakResult result = releaseVaseContents(vase);
        checkForVaseBreakerWin();
        return result;
    }

    private VaseBreakResult releaseVaseContents(Vase vase) {
        EntityPosition position = vase.getPosition();
        if (vase.getContentType() == VaseContentType.EMPTY) {
            addPendingResult(VaseBreakerView.formatBrokenVase(vase)
                    + " It was empty.");
            return VaseBreakResult.SUCCESS_EMPTY;
        }
        if (vase.getContentType() == VaseContentType.SEED_PACKET) {
            VaseSeedPacket packet = new VaseSeedPacket(position,
                    vase.getPlantType(),
                    level.getSeedPacketLifeSpanSeconds());
            seedPackets.add(packet);
            getBoard().addEntity(packet);
            addPendingResult(VaseBreakerView.formatBrokenVase(vase) + " A "
                    + packet.getPlantType() + " seed packet dropped at "
                    + position + " and will disappear in "
                    + VaseBreakerView.formatSeconds(packet.getLifeSpanSeconds()) + " seconds.");
            return VaseBreakResult.SUCCESS_SEED_PACKET;
        }

        Zombie zombie = createVaseZombie(vase);
        getBoard().addZombie(zombie);
        addPendingResult(VaseBreakerView.formatBrokenVase(vase) + " "
                + zombie.getName() + " emerged at " + position + ".");
        return VaseBreakResult.SUCCESS_ZOMBIE;
    }

    private Zombie createVaseZombie(Vase vase) {
        ZombieType zombieType = vase.getType() == VaseType.GIANT
                ? ZombieType.GARGANTUAR
                : vase.getZombieType();
        EntityPosition position = vase.getPosition();
        return new Zombie(zombieType, 0, position.getRow(),
                position.getColumn(), false);
    }

    public VaseSeedPlantingResult plantFromSeed(
            EntityPosition seedPosition, EntityPosition destination) {
        if (getStatus() != GameStatus.ACTIVE) {
            return VaseSeedPlantingResult.GAME_NOT_ACTIVE;
        }
        if (!getBoard().isPositionInsideBoard(seedPosition)) {
            return VaseSeedPlantingResult.INVALID_SOURCE;
        }
        VaseSeedPacket packet = getSeedPacketAt(seedPosition);
        if (packet == null) {
            return VaseSeedPlantingResult.NO_SEED_PACKET;
        }
        if (!getBoard().isPositionInsideBoard(destination)) {
            return VaseSeedPlantingResult.INVALID_DESTINATION;
        }
        if (getBoard().getStructureAt(destination) != null) {
            return VaseSeedPlantingResult.DESTINATION_BLOCKED;
        }

        BasePlant plant = PlantFactory.createPlant(
                packet.getPlantType(), destination);
        if (plant == null) {
            return VaseSeedPlantingResult.UNKNOWN_PLANT;
        }
        if (!getBoard().addPlant(plant)) {
            return VaseSeedPlantingResult.DESTINATION_BLOCKED;
        }

        packet.collect();
        getBoard().removeEntity(packet);
        addPendingResult("Planted " + plant.getName() + " at "
                + destination + " using the one-use seed packet from "
                + seedPosition + ". No sun was spent.");
        return VaseSeedPlantingResult.SUCCESS;
    }

    public VaseSeedPacket getSeedPacketAt(EntityPosition position) {
        for (VaseSeedPacket packet : seedPackets) {
            if (!packet.isRemoved()
                    && position.equals(packet.getEntityPosition())) {
                return packet;
            }
        }
        return null;
    }

    public List<VaseSeedPacket> getAvailableSeedPackets() {
        List<VaseSeedPacket> available = new ArrayList<>();
        for (VaseSeedPacket packet : seedPackets) {
            if (!packet.isRemoved()) {
                available.add(packet);
            }
        }
        return Collections.unmodifiableList(available);
    }

    public List<Vase> getVases() {
        List<Vase> vases = new ArrayList<>();
        for (BaseStructure structure : getBoard().getStructures()) {
            if (structure instanceof Vase) {
                vases.add((Vase) structure);
            }
        }
        return Collections.unmodifiableList(vases);
    }

    public VaseBreakerLevel getLevel() {
        return level;
    }

    private void reportExpiredSeedPackets() {
        for (VaseSeedPacket packet : seedPackets) {
            if (packet.consumeExpirationEvent()) {
                addPendingResult(packet.getPlantType()
                        + " seed packet at " + packet.getEntityPosition()
                        + " disappeared before it was planted.");
            }
        }
    }

    private void checkForVaseBreakerWin() {
        if (getStatus() != GameStatus.ACTIVE || !getVases().isEmpty()) {
            return;
        }
        for (Zombie zombie : getBoard().getZombies()) {
            if (!zombie.isDead() && !zombie.isHypnotized()) {
                return;
            }
        }
        completeGameAsWon("All vases are broken and every hostile zombie "
                + "has been defeated. Vase Breaker level "
                + level.getNumber() + " complete!");
    }

    private void placeVases() {
        List<EntityPosition> positions = createVasePositions();
        List<VaseTemplate> templates = createVaseTemplates();
        Collections.shuffle(positions, random);
        Collections.shuffle(templates, random);
        for (int index = 0; index < templates.size(); index++) {
            Vase vase = templates.get(index).create(positions.get(index));
            if (!getBoard().addStructure(vase)) {
                throw new IllegalStateException("could not place vase at "
                        + vase.getPosition());
            }
        }
    }

    private List<EntityPosition> createVasePositions() {
        List<EntityPosition> positions = new ArrayList<>();
        int firstColumn = Math.min(FIRST_VASE_COLUMN,
                getBoard().getNumberOfColumns() - 1);
        for (int row = 0; row < getBoard().getNumberOfRows(); row++) {
            for (int column = firstColumn; column < getBoard().getNumberOfColumns(); column++) {
                positions.add(new EntityPosition(row, column));
            }
        }
        if (positions.size() < level.getTotalVaseCount()) {
            throw new IllegalStateException("not enough board cells for vases");
        }
        return positions;
    }

    private List<VaseTemplate> createVaseTemplates() {
        List<VaseTemplate> templates = new ArrayList<>();
        addTemplates(templates, VaseTemplate::normalEmpty,
                level.getNormalEmptyVases());
        addTemplates(templates, this::normalZombieTemplate,
                level.getNormalZombieVases());
        addTemplates(templates, this::normalSeedTemplate,
                level.getNormalSeedVases());
        addTemplates(templates, this::plantVaseTemplate,
                level.getPlantVases());
        addTemplates(templates, VaseTemplate::giant,
                level.getGiantVases());
        return templates;
    }

    private static void addTemplates(List<VaseTemplate> templates,
            TemplateSupplier supplier, int count) {
        for (int index = 0; index < count; index++) {
            templates.add(supplier.create());
        }
    }

    private VaseTemplate normalZombieTemplate() {
        return VaseTemplate.normalZombie(randomZombieType());
    }

    private VaseTemplate normalSeedTemplate() {
        return VaseTemplate.normalSeed(randomPlantType());
    }

    private VaseTemplate plantVaseTemplate() {
        return VaseTemplate.plant(randomPlantType());
    }

    private String randomPlantType() {
        List<String> pool = level.getPlantPool();
        return pool.get(random.nextInt(pool.size()));
    }

    private ZombieType randomZombieType() {
        List<ZombieType> pool = level.getZombiePool();
        return pool.get(random.nextInt(pool.size()));
    }

    @FunctionalInterface
    private interface TemplateSupplier {
        VaseTemplate create();
    }

    private static final class VaseTemplate {
        private final VaseType type;
        private final VaseContentType contentType;
        private final String plantType;
        private final ZombieType zombieType;

        private VaseTemplate(VaseType type, VaseContentType contentType,
                String plantType, ZombieType zombieType) {
            this.type = type;
            this.contentType = contentType;
            this.plantType = plantType;
            this.zombieType = zombieType;
        }

        private static VaseTemplate normalEmpty() {
            return new VaseTemplate(VaseType.NORMAL,
                    VaseContentType.EMPTY, null, null);
        }

        private static VaseTemplate normalSeed(String plantType) {
            return new VaseTemplate(VaseType.NORMAL,
                    VaseContentType.SEED_PACKET, plantType, null);
        }

        private static VaseTemplate normalZombie(ZombieType zombieType) {
            return new VaseTemplate(VaseType.NORMAL,
                    VaseContentType.ZOMBIE, null, zombieType);
        }

        private static VaseTemplate plant(String plantType) {
            return new VaseTemplate(VaseType.PLANT,
                    VaseContentType.SEED_PACKET, plantType, null);
        }

        private static VaseTemplate giant() {
            return new VaseTemplate(VaseType.GIANT,
                    VaseContentType.ZOMBIE, null, ZombieType.GARGANTUAR);
        }

        private Vase create(EntityPosition position) {
            if (type == VaseType.PLANT) {
                return Vase.plantVase(position, plantType);
            }
            if (type == VaseType.GIANT) {
                return Vase.giantVase(position);
            }
            if (contentType == VaseContentType.EMPTY) {
                return Vase.normalEmpty(position);
            }
            if (contentType == VaseContentType.SEED_PACKET) {
                return Vase.normalPlant(position, plantType);
            }
            return Vase.normalZombie(position, zombieType);
        }
    }
}
