package io.github.some_example_name.model.game.entities.projectile;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import io.github.some_example_name.model.game.entities.projectile.effect.ProjectileEffect;
import io.github.some_example_name.model.game.entities.projectile.movement.ProjectileMovement;
import io.github.some_example_name.model.game.entities.zombies.Zombie;

public final class PiercingProjectile extends Projectile {
    private final int maximumTargets;
    private final Set<Zombie> hitTargets;

    public PiercingProjectile(String sourcePlantName, double rowPosition,
            double columnPosition, List<ProjectileEffect> effects,
            ProjectileMovement movement, double maxTravelDistance,
            int maximumTargets) {
        super(sourcePlantName, rowPosition, columnPosition, effects,
                movement, maxTravelDistance);
        if (maximumTargets <= 0) {
            throw new IllegalArgumentException("maximumTargets must be positive");
        }
        this.maximumTargets = maximumTargets;
        this.hitTargets = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    @Override
    public void hit(Zombie zombie) {
        if (!canHit(zombie)) {
            return;
        }
        hitTargets.add(zombie);
        applyEffects(zombie);
        if (hitTargets.size() >= maximumTargets) {
            markForRemoval();
        }
    }

    public boolean canHit(Zombie zombie) {
        return zombie != null && !isRemoved() && !hitTargets.contains(zombie);
    }

    public int getMaximumTargets() {
        return maximumTargets;
    }

    public int getHitCount() {
        return hitTargets.size();
    }
}
