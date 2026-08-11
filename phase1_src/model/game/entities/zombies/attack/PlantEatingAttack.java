package model.game.entities.zombies.attack;

import model.game.entities.plants.BasePlant;
import model.game.entities.zombies.Zombie;

public class PlantEatingAttack extends AttackBehavior {
    private double pendingDamage;

    public PlantEatingAttack(int damagePerSecond) {
        if (damagePerSecond < 0) {
            throw new IllegalArgumentException("damagePerSecond cannot be negative");
        }
    }

    @Override
    public void attack(Zombie zombie, BasePlant plant, float deltaSeconds) {
        if (zombie == null || plant == null) {
            throw new IllegalArgumentException("zombie and plant cannot be null");
        }
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
        }

        int effectiveDamagePerSecond = zombie.getEffectiveEatDPS();
        pendingDamage += effectiveDamagePerSecond * deltaSeconds;
        int damage = (int) pendingDamage;
        if (damage > 0) {
            plant.takeDamage(damage);
            pendingDamage -= damage;
        }
    }
}
