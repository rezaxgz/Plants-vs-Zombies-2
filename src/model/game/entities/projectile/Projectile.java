package model.game.entities.projectile;

import java.util.List;

import model.game.entities.Entity;
import model.game.entities.projectile.effect.ProjectileEffect;
import model.game.entities.projectile.movement.ProjectileMovement;

public class Projectile extends Entity {
    private List<ProjectileEffect> effect;
    private ProjectileMovement movement;

    @Override
    public void tick() {
        super.tick();
        
    }
}
