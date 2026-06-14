package cn.edu.whut.sept.dungeon.core;

import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.World;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GameEngineTest {
    @Test
    public void playWithInputStringStartsNewGameWithSeed() {
        GameResult result = new GameEngine().playWithInputString("n20260614s");

        GameState state = result.getState();
        assertTrue(state.isStarted());
        assertEquals(Long.valueOf(20260614L), state.getSeed());
        assertEquals("generated", state.getWorldStatus());
        assertNotNull(state.getWorld());
        assertNotNull(state.getPlayer());
        assertEquals(state.getWorld().getSpawnPosition().getX(), state.getPlayer().getX());
        assertEquals(state.getWorld().getSpawnPosition().getY(), state.getPlayer().getY());
        assertTrue(state.getWorld().isWalkable(state.getWorld().getSpawnPosition()));
    }

    @Test
    public void invalidInputDoesNotCrashAndLeavesMessage() {
        GameResult result = new GameEngine().playWithInputString("?");

        GameState state = result.getState();
        assertFalse(state.isStarted());
        assertEquals("Unknown input: ?", state.getMessage());
    }

    @Test
    public void saveAndQuitMarksStateForSaveAndExit() {
        GameResult result = new GameEngine().playWithInputString("n123s:q");

        GameState state = result.getState();
        assertTrue(state.isStarted());
        assertTrue(state.isSaveRequested());
        assertTrue(state.isExited());
        assertEquals(Long.valueOf(123L), state.getSeed());
        assertEquals("Save requested.", state.getMessage());
    }

    @Test
    public void emptyInputReturnsInitialStateWithMessage() {
        GameResult result = new GameEngine().playWithInputString("");

        GameState state = result.getState();
        assertFalse(state.isStarted());
        assertEquals("No input.", state.getMessage());
    }

    @Test
    public void playerMovesOnFloor() {
        GameState initial = new GameEngine().playWithInputString("n123s").getState();

        GameState moved = new GameEngine().playWithInputString("n123sd").getState();

        assertEquals(initial.getPlayer().getX() + 1, moved.getPlayer().getX());
        assertEquals(initial.getPlayer().getY(), moved.getPlayer().getY());
        assertEquals(Direction.EAST, moved.getPlayer().getDirection());
        assertEquals(1, moved.getPlayer().getSteps());
        assertEquals("Moved EAST.", moved.getMessage());
    }

    @Test
    public void playerCannotMoveThroughWall() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        GameState beforeBlockedMove = moveUntilNextStepIsBlocked(engine, Direction.WEST);

        GameState state = engine.handleInput(InputCommand.fromKey('a'));

        assertTrue(state.isStarted());
        assertEquals(beforeBlockedMove.getPlayer().getX(), state.getPlayer().getX());
        assertEquals(beforeBlockedMove.getPlayer().getY(), state.getPlayer().getY());
        assertTrue(state.getWorld().isWalkable(state.getPlayer().getPosition()));
        assertEquals(Direction.WEST, state.getPlayer().getDirection());
        assertEquals("Blocked by wall.", state.getMessage());
    }

    @Test
    public void validMoveIncreasesStepsButBlockedMoveDoesNot() {
        GameState moved = new GameEngine().playWithInputString("n123sd").getState();
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        GameState beforeBlockedMove = moveUntilNextStepIsBlocked(engine, Direction.WEST);
        GameState blocked = engine.handleInput(InputCommand.fromKey('a'));

        assertEquals(1, moved.getPlayer().getSteps());
        assertEquals(beforeBlockedMove.getPlayer().getSteps(), blocked.getPlayer().getSteps());
    }

    @Test
    public void moveUpdatesVisibleAndExploredTiles() {
        GameState initial = new GameEngine().playWithInputString("n123s").getState();
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        GameState moved = moveUntilSpawnIsSeen(engine);

        assertTrue(initial.isVisible(initial.getPlayer().getX(), initial.getPlayer().getY()));
        assertTrue(moved.isVisible(moved.getPlayer().getX(), moved.getPlayer().getY()));
        assertTrue(moved.isExplored(initial.getPlayer().getX(), initial.getPlayer().getY()));
        assertEquals(VisibilityState.SEEN,
                moved.getVisibilityState(initial.getPlayer().getX(), initial.getPlayer().getY()));
        assertEquals(VisibilityState.VISIBLE,
                moved.getVisibilityState(moved.getPlayer().getX(), moved.getPlayer().getY()));
        assertEquals(VisibilityState.UNSEEN, moved.getVisibilityState(0, 0));
        assertTrue(moved.getExploredCount() >= moved.getVisibleCount());
        assertTrue(moved.getExploredCount() >= initial.getExploredCount());
    }

    private GameState moveUntilNextStepIsBlocked(GameEngine engine, Direction direction) {
        char key = keyFor(direction);
        GameState state = engine.getState();
        for (int i = 0; i < 200; i++) {
            int nextX = state.getPlayer().getX() + direction.getDx();
            int nextY = state.getPlayer().getY() + direction.getDy();
            if (!state.getWorld().isWalkable(new Position(nextX, nextY))) {
                return state;
            }
            state = engine.handleInput(InputCommand.fromKey(key));
        }
        throw new AssertionError("Could not find a blocking wall while moving " + direction);
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

    private static final class PathNode {
        private final Position position;
        private final String path;

        private PathNode(Position position, String path) {
            this.position = position;
            this.path = path;
        }
    }
}
