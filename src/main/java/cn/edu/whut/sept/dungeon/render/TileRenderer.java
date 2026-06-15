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
import java.awt.Font;
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

    public String glyphFor(GameState state, int x, int y) {
        if (state == null || state.getWorld() == null || state.getVisibilityState(x, y) != VisibilityState.VISIBLE) {
            return "";
        }
        if (isPlayerAt(state, x, y)) {
            switch (state.getPlayer().getDirection()) {
                case NORTH:
                    return "^";
                case SOUTH:
                    return "v";
                case WEST:
                    return "<";
                case EAST:
                    return ">";
                default:
                    return "@";
            }
        }
        Enemy enemy = enemyAt(state, x, y);
        if (enemy != null) {
            return "Defense Committee".equals(enemy.getType()) ? "B" : "!";
        }
        Item item = itemAt(state, x, y);
        if (item != null) {
            return itemGlyph(item);
        }
        if (isNpcAt(state, x, y)) {
            return "?";
        }
        if (isTrapAt(state, x, y)) {
            return "x";
        }
        if (isStairsAt(state, x, y)) {
            return ">";
        }
        if (isDefenseHallAt(state.getWorld(), x, y)) {
            return "#";
        }
        return "";
    }

    public void draw(GameState state, Graphics2D graphics) {
        if (state == null || state.getWorld() == null) {
            return;
        }
        World world = state.getWorld();
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                graphics.setColor(colorFor(state, x, y));
                graphics.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                drawTileTexture(state, graphics, x, y);
                drawGlyph(state, graphics, x, y);
            }
        }
    }

    private void drawTileTexture(GameState state, Graphics2D graphics, int x, int y) {
        if (state.getVisibilityState(x, y) != VisibilityState.VISIBLE) {
            return;
        }
        if (state.getWorld().getTile(x, y) == Tile.WALL) {
            graphics.setColor(new Color(92, 99, 113));
            graphics.drawLine(x * TILE_SIZE + 2, y * TILE_SIZE + 4, x * TILE_SIZE + 13, y * TILE_SIZE + 4);
            graphics.drawLine(x * TILE_SIZE + 1, y * TILE_SIZE + 10, x * TILE_SIZE + 12, y * TILE_SIZE + 10);
            return;
        }
        graphics.setColor(new Color(158, 150, 128));
        graphics.drawRect(x * TILE_SIZE + 3, y * TILE_SIZE + 3, 2, 2);
        graphics.drawRect(x * TILE_SIZE + 10, y * TILE_SIZE + 9, 1, 1);
    }

    private void drawGlyph(GameState state, Graphics2D graphics, int x, int y) {
        String glyph = glyphFor(state, x, y);
        if (glyph.length() == 0) {
            return;
        }
        graphics.setColor(new Color(22, 24, 28));
        graphics.fillRect(x * TILE_SIZE + 3, y * TILE_SIZE + 3, TILE_SIZE - 6, TILE_SIZE - 6);
        graphics.setColor(Color.WHITE);
        graphics.drawString(glyph, x * TILE_SIZE + 5, y * TILE_SIZE + 12);
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
        return itemAt(state, x, y) != null;
    }

    private Item itemAt(GameState state, int x, int y) {
        for (Item item : state.getItems()) {
            if (!item.isCollected() && item.getPosition().getX() == x && item.getPosition().getY() == y) {
                return item;
            }
        }
        return null;
    }

    private boolean isEnemyAt(GameState state, int x, int y) {
        return enemyAt(state, x, y) != null;
    }

    private Enemy enemyAt(GameState state, int x, int y) {
        for (Enemy enemy : state.getEnemies()) {
            if (enemy.isAlive() && enemy.getPosition().getX() == x && enemy.getPosition().getY() == y) {
                return enemy;
            }
        }
        return null;
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

    private String itemGlyph(Item item) {
        String id = item.getId();
        if (id.contains("potion")) {
            return "+";
        }
        if (id.contains("keyboard") || id.contains("blade")) {
            return "/";
        }
        if (id.contains("coat") || id.contains("robe") || id.contains("suit")) {
            return "]";
        }
        return "*";
    }
}
