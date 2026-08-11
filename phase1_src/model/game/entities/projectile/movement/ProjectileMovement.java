package model.game.entities.projectile.movement;

import model.game.entities.projectile.Projectile;

public abstract class ProjectileMovement {
    public abstract void move(Projectile projectile, float deltaSeconds);
}
