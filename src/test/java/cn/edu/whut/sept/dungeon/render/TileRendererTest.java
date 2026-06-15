package cn.edu.whut.sept.dungeon.render;

import cn.edu.whut.sept.dungeon.core.GameEngine;
import cn.edu.whut.sept.dungeon.core.GameState;
import cn.edu.whut.sept.dungeon.core.InputCommand;
import cn.edu.whut.sept.dungeon.core.VisibilityState;
import cn.edu.whut.sept.dungeon.core.Direction;
import cn.edu.whut.sept.dungeon.entity.Enemy;
import cn.edu.whut.sept.dungeon.entity.Item;
import cn.edu.whut.sept.dungeon.entity.Npc;
import cn.edu.whut.sept.dungeon.entity.Trap;
import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.World;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.Assert.assertEquals;

public class TileRendererTest {
    @Test
    public void rendererUsesPlayerColorAtPlayerPosition() {
        GameState state = new GameEngine().playWithInputString("n123s").getState();
        TileRenderer renderer = new TileRenderer();

        assertEquals(TileRenderer.PLAYER_COLOR,
                renderer.colorFor(state, state.getPlayer().getX(), state.getPlayer().getY()));
    }

    @Test
    public void rendererHidesUnexploredTiles() {
        GameState state = new GameEngine().playWithInputString("n123s").getState();
        TileRenderer renderer = new TileRenderer();

        assertEquals(TileRenderer.UNEXPLORED_COLOR, renderer.colorFor(state, 0, 0));
    }

    @Test
    public void rendererDarkensSeenTilesOutsideCurrentVision() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        GameState state = moveUntilSpawnIsSeen(engine);
        Position oldPosition = state.getWorld().getSpawnPosition();
        TileRenderer renderer = new TileRenderer();

        assertEquals(TileRenderer.SEEN_FLOOR_COLOR, renderer.colorFor(state, oldPosition.getX(), oldPosition.getY()));
    }

    @Test
    public void rendererUsesEnemyColorForVisibleEnemy() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        Enemy enemy = engine.getState().getEnemies().get(0);
        GameState state = stateAfterPath(engine.getState(), adjacentWalkableTile(engine.getState(), enemy.getPosition()));
        TileRenderer renderer = new TileRenderer();

        assertEquals(TileRenderer.ENEMY_COLOR,
                renderer.colorFor(state, enemy.getPosition().getX(), enemy.getPosition().getY()));
    }

    @Test
    public void rendererUsesStairsColorForVisibleStairs() {
        GameState state = new GameEngine().playWithInputString("n123s").getState();
        Position stairs = state.getWorld().getStairsPosition();
        GameState nearStairs = stateAfterPath(state, adjacentWalkableTile(state, stairs));
        TileRenderer renderer = new TileRenderer();

        assertEquals(TileRenderer.STAIRS_COLOR,
                renderer.colorFor(nearStairs, stairs.getX(), stairs.getY()));
    }

    @Test
    public void rendererUsesTrapColorForVisibleUntriggeredTrap() {
        GameState state = new GameEngine().playWithInputString("n123s").getState();
        Trap trap = state.getTraps().get(0);
        GameState nearTrap = stateAfterPath(state, adjacentWalkableTile(state, trap.getPosition()));
        TileRenderer renderer = new TileRenderer();

        assertEquals(TileRenderer.TRAP_COLOR,
                renderer.colorFor(nearTrap, trap.getPosition().getX(), trap.getPosition().getY()));
    }

    @Test
    public void rendererProvidesDistinctPixelGlyphsForGameObjects() {
        TileRenderer renderer = new TileRenderer();
        GameState state = new GameEngine().playWithInputString("n123sl").getState();
        assertEquals(">", renderer.glyphFor(state, state.getPlayer().getX(), state.getPlayer().getY()));

        Enemy enemy = state.getEnemies().get(0);
        GameState nearEnemy = stateAfterPath(state, adjacentWalkableTile(state, enemy.getPosition()));
        assertEquals("!", renderer.glyphFor(nearEnemy, enemy.getPosition().getX(), enemy.getPosition().getY()));

        Item item = findItem(state, "small-potion");
        GameState nearItem = stateAfterPath(state, adjacentWalkableTile(state, item.getPosition()));
        assertEquals("+", renderer.glyphFor(nearItem, item.getPosition().getX(), item.getPosition().getY()));

        Npc npc = state.getNpcs().get(0);
        GameState nearNpc = stateAfterPath(state, adjacentWalkableTile(state, npc.getPosition()));
        assertEquals("?", renderer.glyphFor(nearNpc, npc.getPosition().getX(), npc.getPosition().getY()));

        Trap trap = state.getTraps().get(0);
        GameState nearTrap = stateAfterPath(state, adjacentWalkableTile(state, trap.getPosition()));
        assertEquals("x", renderer.glyphFor(nearTrap, trap.getPosition().getX(), trap.getPosition().getY()));

        Position stairs = state.getWorld().getStairsPosition();
        GameState nearStairs = stateAfterPath(state, adjacentWalkableTile(state, stairs));
        assertEquals(">", renderer.glyphFor(nearStairs, stairs.getX(), stairs.getY()));

        GameState finalDepth = descendToDepth(state, 5);
        Enemy boss = findEnemy(finalDepth, "defense-committee");
        GameState nearBoss = stateAfterPath(finalDepth, adjacentWalkableTile(finalDepth, boss.getPosition()));
        assertEquals("B", renderer.glyphFor(nearBoss, boss.getPosition().getX(), boss.getPosition().getY()));
    }

    private GameState moveUntilSpawnIsSeen(GameEngine engine) {
        GameState state = engine.getState();
        String path = pathBeyondVision(state);
        for (int i = 0; i < path.length(); i++) {
            state = engine.handleInput(InputCommand.fromKey(path.charAt(i)));
        }
        return state;
    }

    private String pathBeyondVision(GameState state) {
        World world = state.getWorld();
        Position spawn = state.getPlayer().getPosition();
        boolean[][] visited = new boolean[world.getHeight()][world.getWidth()];
        Queue<PathNode> queue = new ArrayDeque<PathNode>();
        queue.add(new PathNode(spawn, ""));
        visited[spawn.getY()][spawn.getX()] = true;

        while (!queue.isEmpty()) {
            PathNode current = queue.remove();
            if (isOutsideSquareVision(spawn, current.position)) {
                return current.path;
            }
            Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
            for (Direction direction : directions) {
                Position next = new Position(current.position.getX() + direction.getDx(),
                        current.position.getY() + direction.getDy());
                if (world.contains(next.getX(), next.getY())
                        && !visited[next.getY()][next.getX()]
                        && world.isWalkable(next)
                        && state.trapAt(next) == null) {
                    visited[next.getY()][next.getX()] = true;
                    queue.add(new PathNode(next, current.path + keyFor(direction)));
                }
            }
        }
        throw new AssertionError("Could not find a walkable path beyond vision radius.");
    }

    private boolean isOutsideSquareVision(Position origin, Position position) {
        return Math.abs(origin.getX() - position.getX()) > GameState.VISION_RADIUS
                || Math.abs(origin.getY() - position.getY()) > GameState.VISION_RADIUS;
    }

    private void moveTo(GameEngine engine, Position target) {
        String path = pathTo(engine.getState(), target);
        for (int i = 0; i < path.length(); i++) {
            engine.handleInput(InputCommand.fromKey(path.charAt(i)));
        }
    }

    private GameState stateAfterPath(GameState state, Position target) {
        String path = pathTo(state, target);
        GameState current = state;
        for (int i = 0; i < path.length(); i++) {
            current = current.movePlayer(InputCommand.fromKey(path.charAt(i)).getDirection());
        }
        return current;
    }

    private String pathTo(GameState state, Position target) {
        World world = state.getWorld();
        Position start = state.getPlayer().getPosition();
        boolean[][] visited = new boolean[world.getHeight()][world.getWidth()];
        Queue<PathNode> queue = new ArrayDeque<PathNode>();
        queue.add(new PathNode(start, ""));
        visited[start.getY()][start.getX()] = true;

        while (!queue.isEmpty()) {
            PathNode current = queue.remove();
            if (current.position.equals(target)) {
                return current.path;
            }
            Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
            for (Direction direction : directions) {
                Position next = new Position(current.position.getX() + direction.getDx(),
                        current.position.getY() + direction.getDy());
                if (world.contains(next.getX(), next.getY())
                        && !visited[next.getY()][next.getX()]
                        && world.isWalkable(next)
                        && state.enemyAt(next) == null
                        && (next.equals(target) || state.trapAt(next) == null)) {
                    visited[next.getY()][next.getX()] = true;
                    queue.add(new PathNode(next, current.path + keyFor(direction)));
                }
            }
        }
        throw new AssertionError("Could not find path to " + target);
    }

    private Position adjacentWalkableTile(GameState state, Position target) {
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (Direction direction : directions) {
            Position candidate = new Position(target.getX() - direction.getDx(), target.getY() - direction.getDy());
            if (state.getWorld().contains(candidate.getX(), candidate.getY())
                    && state.getWorld().isWalkable(candidate)
                    && !candidate.equals(state.getPlayer().getPosition())
                    && state.enemyAt(candidate) == null
                    && state.trapAt(candidate) == null) {
                return candidate;
            }
        }
        throw new AssertionError("Could not find adjacent walkable tile for " + target);
    }

    private GameState descendToDepth(GameState state, int targetDepth) {
        GameState current = state;
        while (current.getDepth() < targetDepth) {
            current = stateAfterPath(current, current.getWorld().getStairsPosition()).interact();
        }
        return current;
    }

    private Enemy findEnemy(GameState state, String enemyId) {
        for (Enemy enemy : state.getEnemies()) {
            if (enemy.getId().equals(enemyId)) {
                return enemy;
            }
        }
        throw new AssertionError("Missing enemy: " + enemyId);
    }

    private Item findItem(GameState state, String itemId) {
        for (Item item : state.getItems()) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        throw new AssertionError("Missing item: " + itemId);
    }

    private char keyFor(Direction direction) {
        switch (direction) {
            case NORTH:
                return 'k';
            case SOUTH:
                return 'j';
            case WEST:
                return 'h';
            case EAST:
                return 'l';
            default:
                throw new IllegalArgumentException("Unsupported direction: " + direction);
        }
    }

    private static final class PathNode {
        private final Position position;
        private final String path;

        private PathNode(Position position, String path) {
            this.position = position;
            this.path = path;
        }
    }
}
