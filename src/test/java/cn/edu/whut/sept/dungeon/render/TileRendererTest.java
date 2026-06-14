package cn.edu.whut.sept.dungeon.render;

import cn.edu.whut.sept.dungeon.core.GameEngine;
import cn.edu.whut.sept.dungeon.core.GameState;
import cn.edu.whut.sept.dungeon.core.InputCommand;
import cn.edu.whut.sept.dungeon.core.VisibilityState;
import cn.edu.whut.sept.dungeon.core.Direction;
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
                        && world.isWalkable(next)) {
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

    private char keyFor(Direction direction) {
        switch (direction) {
            case NORTH:
                return 'w';
            case SOUTH:
                return 's';
            case WEST:
                return 'a';
            case EAST:
                return 'd';
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
