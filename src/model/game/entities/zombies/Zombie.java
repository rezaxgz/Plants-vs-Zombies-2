package model.game.entities.zombies;

import java.util.List;

import model.game.entities.Entity;
import model.game.entities.zombies.abilities.ZombieAbility;
import model.game.entities.zombies.armor.Armor;
import model.game.entities.zombies.attack.AttackBehavior;
import model.game.entities.zombies.movement.MovementBehavior;

public class Zombie extends Entity {
    private String name;

    private List<AttackBehavior> attackBehaviors;
    private MovementBehavior movementBehavior;
    private List<Armor> armors;
    private List<ZombieAbility> abilities;

    private int hp;

}
