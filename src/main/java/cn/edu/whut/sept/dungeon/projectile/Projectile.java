package cn.edu.whut.sept.dungeon.projectile;

import cn.edu.whut.sept.dungeon.core.Direction;
import cn.edu.whut.sept.dungeon.world.Position;

public final class Projectile {
    private final String id;
    private final ProjectileOwner owner;
    private final Position position;
    private final Direction direction;
    private final int speed;
    private final int damage;
    private final int remainingRange;
    private final boolean alive;

    public Projectile(String id, ProjectileOwner owner, Position position, Direction direction,
                      int speed, int damage, int remainingRange, boolean alive) {
        this.id = id;
        this.owner = owner == null ? ProjectileOwner.PLAYER : owner;
        this.position = position;
        this.direction = direction == null ? Direction.SOUTH : direction;
        this.speed = Math.max(1, speed);
        this.damage = Math.max(0, damage);
        this.remainingRange = Math.max(0, remainingRange);
        this.alive = alive && this.remainingRange > 0;
    }

    public static Projectile player(String id, Position position, Direction direction, int damage, int range) {
        return new Projectile(id, ProjectileOwner.PLAYER, position, direction, 1, damage, range, true);
    }

    public static Projectile enemy(String id, Position position, Direction direction, int damage, int range) {
        return new Projectile(id, ProjectileOwner.ENEMY, position, direction, 1, damage, range, true);
    }

    public String getId() {
        return id;
    }

    public ProjectileOwner getOwner() {
        return owner;
    }

    public Position getPosition() {
        return position;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getSpeed() {
        return speed;
    }

    public int getDamage() {
        return damage;
    }

    public int getRemainingRange() {
        return remainingRange;
    }

    public boolean isAlive() {
        return alive;
    }

    public Projectile moveTo(Position nextPosition) {
        return new Projectile(id, owner, nextPosition, direction, speed, damage,
                remainingRange - 1, alive && remainingRange > 1);
    }

    public Projectile kill() {
        return new Projectile(id, owner, position, direction, speed, damage, remainingRange, false);
    }
}
