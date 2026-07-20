package model.game.structure;

import model.game.entities.EntityPosition;
import model.game.entities.zombies.ZombieType;

/**
 * A breakable Vase Breaker structure with hidden contents.
 */
public final class Vase extends BaseStructure {
    private final VaseType type;
    private final VaseContentType contentType;
    private final String plantType;
    private final ZombieType zombieType;
    private boolean broken;

    private Vase(EntityPosition position, VaseType type,
            VaseContentType contentType, String plantType,
            ZombieType zombieType) {
        super(position);
        if (position == null || type == null || contentType == null) {
            throw new IllegalArgumentException(
                    "vase position, type, and content are required");
        }
        validateContents(type, contentType, plantType, zombieType);
        this.type = type;
        this.contentType = contentType;
        this.plantType = plantType;
        this.zombieType = zombieType;
    }

    public static Vase normalEmpty(EntityPosition position) {
        return new Vase(position, VaseType.NORMAL,
                VaseContentType.EMPTY, null, null);
    }

    public static Vase normalPlant(EntityPosition position,
            String plantType) {
        return new Vase(position, VaseType.NORMAL,
                VaseContentType.SEED_PACKET, plantType, null);
    }

    public static Vase normalZombie(EntityPosition position,
            ZombieType zombieType) {
        return new Vase(position, VaseType.NORMAL,
                VaseContentType.ZOMBIE, null, zombieType);
    }

    public static Vase plantVase(EntityPosition position,
            String plantType) {
        return new Vase(position, VaseType.PLANT,
                VaseContentType.SEED_PACKET, plantType, null);
    }

    public static Vase giantVase(EntityPosition position) {
        return new Vase(position, VaseType.GIANT,
                VaseContentType.ZOMBIE, null, ZombieType.GARGANTUAR);
    }

    private static void validateContents(VaseType type,
            VaseContentType contentType, String plantType,
            ZombieType zombieType) {
        boolean hasPlant = plantType != null && !plantType.isBlank();
        boolean validSeed = contentType == VaseContentType.SEED_PACKET
                && hasPlant && zombieType == null;
        boolean validZombie = contentType == VaseContentType.ZOMBIE
                && zombieType != null && !hasPlant;
        boolean validEmpty = contentType == VaseContentType.EMPTY
                && !hasPlant && zombieType == null;
        if (!validSeed && !validZombie && !validEmpty) {
            throw new IllegalArgumentException("invalid vase contents");
        }
        if (type == VaseType.PLANT && !validSeed) {
            throw new IllegalArgumentException(
                    "plant vases must contain a seed packet");
        }
        if (type == VaseType.GIANT
                && (!validZombie || zombieType != ZombieType.GARGANTUAR)) {
            throw new IllegalArgumentException(
                    "giant vases must contain a Gargantuar");
        }
    }

    public VaseType getType() {
        return type;
    }

    public VaseContentType getContentType() {
        return contentType;
    }

    public String getPlantType() {
        return plantType;
    }

    public ZombieType getZombieType() {
        return zombieType;
    }

    public boolean isBroken() {
        return broken;
    }

    public boolean breakVase() {
        if (broken || isRemoved()) {
            return false;
        }
        broken = true;
        markForRemoval();
        return true;
    }
}
