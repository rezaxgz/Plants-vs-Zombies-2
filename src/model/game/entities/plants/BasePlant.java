package model.game.entities.plants;

import java.util.List;

import model.game.entities.Entity;
import model.game.entities.plants.attacks.PlantAttacks;

public abstract class BasePlant extends Entity {
    private String name;
    private PlantCategory category;
    private List<PlantTag> tags;
    private int level;
    private int cost; // in suns
    private int baseHP;
    private int damage;

    private List<PlantAttacks> plantAttacks;
}
