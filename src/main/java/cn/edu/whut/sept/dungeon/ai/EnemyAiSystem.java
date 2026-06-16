package cn.edu.whut.sept.dungeon.ai;

import cn.edu.whut.sept.dungeon.combat.CombatSystem;
import cn.edu.whut.sept.dungeon.core.Direction;
import cn.edu.whut.sept.dungeon.core.GameState;
import cn.edu.whut.sept.dungeon.entity.Enemy;
import cn.edu.whut.sept.dungeon.projectile.Projectile;
import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.Room;
import cn.edu.whut.sept.dungeon.world.World;

import java.util.ArrayList;
import java.util.List;

public final class EnemyAiSystem {
    private static final int ACTIVE_RANGE = 10;
    private static final int SHOOTER_RANGE = 6;
    private static final int SHOOTER_MIN_DISTANCE = 3;
    private static final int SHOOTER_COOLDOWN_TICKS = 3;
    private static final int ENEMY_PROJECTILE_RANGE = 6;

    public EnemyAiResult tick(World world, List<Enemy> enemies, List<Projectile> projectiles,
                              GameState.PlayerState player, long tick, int activeRoomId) {
        List<Enemy> nextEnemies = new ArrayList<Enemy>();
        List<Projectile> nextProjectiles = new ArrayList<Projectile>(projectiles);
        GameState.PlayerState nextPlayer = player;
        String message = null;

        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) {
                nextEnemies.add(enemy);
                continue;
            }
            if (!isEnemyAllowedToAct(world, enemy, activeRoomId)) {
                nextEnemies.add(enemy);
                continue;
            }
            if (enemy.getPosition().manhattanDistanceTo(nextPlayer.getPosition()) == 1) {
                int damage = CombatSystem.damage(enemy.getAtk(), nextPlayer.getDef());
                nextPlayer = nextPlayer.takeDamage(damage);
                nextEnemies.add(enemy);
                message = enemy.getType() + " hits you for " + damage + " damage.";
                continue;
            }
            if (enemy.isReviewShooter()) {
                int distance = enemy.getPosition().manhattanDistanceTo(nextPlayer.getPosition());
                Direction shotDirection = straightShotDirection(enemy.getPosition(), nextPlayer.getPosition());
                if (shotDirection != null && tick % SHOOTER_COOLDOWN_TICKS == 0
                        && distance <= SHOOTER_RANGE) {
                    nextProjectiles.add(Projectile.enemy("enemy-shot-" + tick + "-" + enemy.getId(),
                            enemy.getPosition(), shotDirection, enemy.getAtk(), ENEMY_PROJECTILE_RANGE));
                    nextEnemies.add(enemy);
                    message = enemy.getType() + " fires a review note.";
                    continue;
                }
                if (distance < SHOOTER_MIN_DISTANCE) {
                    nextEnemies.add(moveOneStepAway(world, enemy, nextPlayer.getPosition(), enemies, nextEnemies, activeRoomId));
                } else if (distance > SHOOTER_RANGE) {
                    nextEnemies.add(moveOneStep(world, enemy, nextPlayer.getPosition(), enemies, nextEnemies, activeRoomId));
                } else {
                    nextEnemies.add(enemy);
                }
                continue;
            }
            if (enemy.isDeadlineRunner()) {
                Enemy moved = moveOneStep(world, enemy, nextPlayer.getPosition(), enemies, nextEnemies, activeRoomId);
                if (moved.getPosition().manhattanDistanceTo(nextPlayer.getPosition()) > 1) {
                    moved = moveOneStep(world, moved, nextPlayer.getPosition(), enemies, nextEnemies, activeRoomId);
                }
                nextEnemies.add(moved);
                continue;
            }
            nextEnemies.add(moveOneStep(world, enemy, nextPlayer.getPosition(), enemies, nextEnemies, activeRoomId));
        }

        return new EnemyAiResult(nextPlayer, nextEnemies, nextProjectiles, message);
    }

    private boolean isEnemyAllowedToAct(World world, Enemy enemy, int activeRoomId) {
        if (activeRoomId < 0) {
            return false;
        }
        return roomIdAt(world, enemy.getPosition()) == activeRoomId;
    }

    private Enemy moveOneStep(World world, Enemy enemy, Position player, List<Enemy> allEnemies,
                              List<Enemy> alreadyMoved, int activeRoomId) {
        if (enemy.getPosition().manhattanDistanceTo(player) > ACTIVE_RANGE) {
            return enemy;
        }
        Direction[] directions = preferredDirectionsToward(enemy.getPosition(), player);
        for (Direction direction : directions) {
            Position candidate = new Position(enemy.getPosition().getX() + direction.getDx(),
                    enemy.getPosition().getY() + direction.getDy());
            if (canMoveTo(world, candidate, player, enemy, allEnemies, alreadyMoved, activeRoomId)) {
                return enemy.moveTo(candidate);
            }
        }
        return enemy;
    }

    private Enemy moveOneStepAway(World world, Enemy enemy, Position player, List<Enemy> allEnemies,
                                  List<Enemy> alreadyMoved, int activeRoomId) {
        Direction[] directions = preferredDirectionsAway(enemy.getPosition(), player);
        for (Direction direction : directions) {
            Position candidate = new Position(enemy.getPosition().getX() + direction.getDx(),
                    enemy.getPosition().getY() + direction.getDy());
            if (canMoveTo(world, candidate, player, enemy, allEnemies, alreadyMoved, activeRoomId)
                    && candidate.manhattanDistanceTo(player) > enemy.getPosition().manhattanDistanceTo(player)) {
                return enemy.moveTo(candidate);
            }
        }
        return enemy;
    }

    private boolean canMoveTo(World world, Position candidate, Position player, Enemy movingEnemy,
                              List<Enemy> allEnemies, List<Enemy> alreadyMoved, int activeRoomId) {
        if (!world.contains(candidate.getX(), candidate.getY()) || !world.isWalkable(candidate)) {
            return false;
        }
        if (candidate.equals(player)) {
            return false;
        }
        if (roomIdAt(world, candidate) != activeRoomId) {
            return false;
        }
        for (Enemy enemy : alreadyMoved) {
            if (enemy.isAlive() && enemy.getPosition().equals(candidate)) {
                return false;
            }
        }
        for (Enemy enemy : allEnemies) {
            if (enemy.isAlive()
                    && !enemy.getId().equals(movingEnemy.getId())
                    && enemy.getPosition().equals(candidate)
                    && !alreadyContains(alreadyMoved, enemy.getId())) {
                return false;
            }
        }
        return true;
    }

    private boolean alreadyContains(List<Enemy> alreadyMoved, String enemyId) {
        for (Enemy enemy : alreadyMoved) {
            if (enemy.getId().equals(enemyId)) {
                return true;
            }
        }
        return false;
    }

    private Direction straightShotDirection(Position from, Position target) {
        if (from.getX() == target.getX()) {
            return target.getY() < from.getY() ? Direction.NORTH : Direction.SOUTH;
        }
        if (from.getY() == target.getY()) {
            return target.getX() < from.getX() ? Direction.WEST : Direction.EAST;
        }
        return null;
    }

    private Direction[] preferredDirectionsToward(Position from, Position target) {
        Direction horizontal = target.getX() < from.getX() ? Direction.WEST : Direction.EAST;
        Direction vertical = target.getY() < from.getY() ? Direction.NORTH : Direction.SOUTH;
        if (Math.abs(target.getX() - from.getX()) >= Math.abs(target.getY() - from.getY())) {
            return new Direction[]{horizontal, vertical, opposite(vertical), opposite(horizontal)};
        }
        return new Direction[]{vertical, horizontal, opposite(horizontal), opposite(vertical)};
    }

    private Direction[] preferredDirectionsAway(Position from, Position target) {
        Direction towardHorizontal = target.getX() < from.getX() ? Direction.WEST : Direction.EAST;
        Direction towardVertical = target.getY() < from.getY() ? Direction.NORTH : Direction.SOUTH;
        Direction horizontal = opposite(towardHorizontal);
        Direction vertical = opposite(towardVertical);
        if (Math.abs(target.getX() - from.getX()) >= Math.abs(target.getY() - from.getY())) {
            return new Direction[]{horizontal, vertical, opposite(vertical), opposite(horizontal)};
        }
        return new Direction[]{vertical, horizontal, opposite(horizontal), opposite(vertical)};
    }

    private Direction opposite(Direction direction) {
        switch (direction) {
            case NORTH:
                return Direction.SOUTH;
            case SOUTH:
                return Direction.NORTH;
            case WEST:
                return Direction.EAST;
            case EAST:
                return Direction.WEST;
            default:
                throw new IllegalArgumentException("Unsupported direction: " + direction);
        }
    }

    private int roomIdAt(World world, Position position) {
        for (int i = 0; i < world.getRooms().size(); i++) {
            Room room = world.getRooms().get(i);
            if (room.contains(position)) {
                return i;
            }
        }
        return -1;
    }
}
