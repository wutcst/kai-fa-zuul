package cn.edu.whut.sept.dungeon.render;

import cn.edu.whut.sept.dungeon.core.GameState;
import cn.edu.whut.sept.dungeon.core.VisibilityState;
import cn.edu.whut.sept.dungeon.entity.Enemy;
import cn.edu.whut.sept.dungeon.entity.Item;
import cn.edu.whut.sept.dungeon.entity.Npc;
import cn.edu.whut.sept.dungeon.entity.Trap;
import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.Tile;
import cn.edu.whut.sept.dungeon.world.World;

import java.awt.Color;
import java.awt.Graphics2D;

public final class TileRenderer {
    public static final int TILE_SIZE = 16;
    public static final Color UNEXPLORED_COLOR = Color.BLACK;
    public static final Color SEEN_WALL_COLOR = new Color(34, 38, 45);
    public static final Color SEEN_FLOOR_COLOR = new Color(45, 48, 53);
    public static final Color WALL_COLOR = new Color(71, 78, 90);
    public static final Color FLOOR_COLOR = new Color(184, 178, 158);
    public static final Color PLAYER_COLOR = new Color(70, 130, 230);
    public static final Color DEFENSE_HALL_COLOR = new Color(208, 180, 75);
    public static final Color ITEM_COLOR = new Color(96, 170, 105);
    public static final Color NPC_COLOR = new Color(178, 105, 210);
    public static final Color ENEMY_COLOR = new Color(200, 70, 70);
    public static final Color STAIRS_COLOR = new Color(110, 210, 220);
    public static final Color TRAP_COLOR = new Color(230, 120, 45);

    public Color colorFor(GameState state, int x, int y) {
        if (state == null || state.getWorld() == null) {
            return UNEXPLORED_COLOR;
        }
        World world = state.getWorld();
        Tile tile = world.getTile(x, y);
        VisibilityState visibilityState = state.getVisibilityState(x, y);
        switch (visibilityState) {
            case UNSEEN:
                return UNEXPLORED_COLOR;
            case SEEN:
                return tile == Tile.WALL ? SEEN_WALL_COLOR : SEEN_FLOOR_COLOR;
            case VISIBLE:
                if (isPlayerAt(state, x, y)) {
                    return PLAYER_COLOR;
                }
                if (isEnemyAt(state, x, y)) {
                    return ENEMY_COLOR;
                }
                if (isItemAt(state, x, y)) {
                    return ITEM_COLOR;
                }
                if (isNpcAt(state, x, y)) {
                    return NPC_COLOR;
                }
                if (isTrapAt(state, x, y)) {
                    return TRAP_COLOR;
                }
                if (isStairsAt(state, x, y)) {
                    return STAIRS_COLOR;
                }
                if (isDefenseHallAt(world, x, y)) {
                    return DEFENSE_HALL_COLOR;
                }
                return tile == Tile.WALL ? WALL_COLOR : FLOOR_COLOR;
            default:
                return UNEXPLORED_COLOR;
        }
    }

    public void draw(GameState state, Graphics2D graphics) {
        if (state == null || state.getWorld() == null) {
            return;
        }
        World world = state.getWorld();
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                graphics.setColor(colorFor(state, x, y));
                graphics.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    private boolean isPlayerAt(GameState state, int x, int y) {
        return state.getPlayer().getX() == x && state.getPlayer().getY() == y;
    }

    private boolean isDefenseHallAt(World world, int x, int y) {
        Position defenseHall = world.getDefenseHallPosition();
        return defenseHall.getX() == x && defenseHall.getY() == y;
    }

    private boolean isStairsAt(GameState state, int x, int y) {
        Position stairs = state.getWorld().getStairsPosition();
        return state.getDepth() < 5 && stairs.getX() == x && stairs.getY() == y;
    }

    private boolean isItemAt(GameState state, int x, int y) {
        for (Item item : state.getItems()) {
            if (!item.isCollected() && item.getPosition().getX() == x && item.getPosition().getY() == y) {
                return true;
            }
        }
        return false;
    }

    private boolean isEnemyAt(GameState state, int x, int y) {
        for (Enemy enemy : state.getEnemies()) {
            if (enemy.isAlive() && enemy.getPosition().getX() == x && enemy.getPosition().getY() == y) {
                return true;
            }
        }
        return false;
    }

    private boolean isNpcAt(GameState state, int x, int y) {
        for (Npc npc : state.getNpcs()) {
            if (npc.getPosition().getX() == x && npc.getPosition().getY() == y) {
                return true;
            }
        }
        return false;
    }

    private boolean isTrapAt(GameState state, int x, int y) {
        for (Trap trap : state.getTraps()) {
            if (!trap.isTriggered() && trap.getPosition().getX() == x && trap.getPosition().getY() == y) {
                return true;
            }
        }
        return false;
    }
}
