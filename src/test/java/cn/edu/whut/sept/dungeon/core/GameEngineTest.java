package cn.edu.whut.sept.dungeon.core;

import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.World;
import cn.edu.whut.sept.dungeon.entity.Item;
import cn.edu.whut.sept.dungeon.entity.Npc;
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

    @Test
    public void playerCanPickUpItemAndViewInventory() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        Item item = engine.getState().getItems().get(0);

        moveTo(engine, item.getPosition());
        GameState picked = engine.handleInput(InputCommand.fromKey('e'));
        GameState inventory = engine.handleInput(InputCommand.fromKey('i'));

        assertTrue(picked.getInventory().contains(item.getId()));
        assertEquals("Picked up " + item.getName() + ".", picked.getMessage());
        assertTrue(inventory.getMessage().contains(item.getId()));
    }

    @Test
    public void defenseDoorRejectsMissingMaterials() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));

        moveTo(engine, engine.getState().getWorld().getDefenseHallPosition());
        GameState rejected = engine.handleInput(InputCommand.fromKey('e'));

        assertFalse(rejected.isCompleted());
        assertTrue(rejected.getMessage().contains("Defense hall locked."));
        assertTrue(rejected.getMessage().contains("report"));
    }

    @Test
    public void defenseDoorCompletesGameWithAllMaterials() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));

        completeNpcQuestLine(engine);
        moveTo(engine, engine.getState().getWorld().getDefenseHallPosition());
        GameState completed = engine.handleInput(InputCommand.fromKey('e'));

        assertTrue(completed.isCompleted());
        assertTrue(completed.getMessage().contains("Defense completed"));
        assertTrue(completed.getMessage().contains("software engineering practice"));
    }

    @Test
    public void librarianRequiresStudentCardForReport() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));

        moveTo(engine, findNpc(engine.getState(), "librarian").getPosition());
        GameState withoutCard = engine.handleInput(InputCommand.fromKey('e'));

        assertFalse(withoutCard.getInventory().contains("report"));
        assertTrue(withoutCard.getMessage().contains("student-card"));

        moveTo(engine, findItem(engine.getState(), "student-card").getPosition());
        engine.handleInput(InputCommand.fromKey('e'));
        moveTo(engine, findNpc(engine.getState(), "librarian").getPosition());
        GameState withCard = engine.handleInput(InputCommand.fromKey('e'));

        assertTrue(withCard.getInventory().contains("report"));
        assertTrue(withCard.getQuest().isReportIssued());
    }

    @Test
    public void teacherIssuesPassAfterMaterialsReady() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));

        completeMaterialsBeforeTeacher(engine);
        moveTo(engine, findNpc(engine.getState(), "teacher").getPosition());
        GameState passIssued = engine.handleInput(InputCommand.fromKey('e'));

        assertTrue(passIssued.getInventory().contains("pass"));
        assertTrue(passIssued.getQuest().isPassIssued());
        assertTrue(passIssued.getMessage().contains("defense pass"));
    }

    @Test
    public void npcDialogueChangesWithQuestStateAndPuzzleAnswer() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));

        moveTo(engine, findNpc(engine.getState(), "assistant").getPosition());
        GameState withoutUsb = engine.handleInput(InputCommand.fromKey('e'));
        assertTrue(withoutUsb.getMessage().contains("usb"));

        moveTo(engine, findItem(engine.getState(), "usb").getPosition());
        engine.handleInput(InputCommand.fromKey('e'));
        moveTo(engine, findNpc(engine.getState(), "assistant").getPosition());
        GameState puzzlePrompt = engine.handleInput(InputCommand.fromKey('e'));
        assertTrue(puzzlePrompt.getMessage().contains("Maven"));

        GameState solved = engine.handleInput(InputCommand.answer("pom.xml"));
        assertTrue(solved.getQuest().isMavenPuzzleSolved());
        GameState materials = engine.handleInput(InputCommand.fromKey('e'));

        assertTrue(materials.getInventory().contains("laptop"));
        assertTrue(materials.getInventory().contains("slides"));
        assertTrue(materials.getQuest().isSlidesExported());
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

    private void moveTo(GameEngine engine, Position target) {
        String path = pathTo(engine.getState(), target);
        for (int i = 0; i < path.length(); i++) {
            engine.handleInput(InputCommand.fromKey(path.charAt(i)));
        }
    }

    private void completeNpcQuestLine(GameEngine engine) {
        completeMaterialsBeforeTeacher(engine);
        moveTo(engine, findNpc(engine.getState(), "teacher").getPosition());
        engine.handleInput(InputCommand.fromKey('e'));
    }

    private void completeMaterialsBeforeTeacher(GameEngine engine) {
        moveTo(engine, findItem(engine.getState(), "student-card").getPosition());
        engine.handleInput(InputCommand.fromKey('e'));
        moveTo(engine, findNpc(engine.getState(), "librarian").getPosition());
        engine.handleInput(InputCommand.fromKey('e'));

        moveTo(engine, findItem(engine.getState(), "usb").getPosition());
        engine.handleInput(InputCommand.fromKey('e'));
        moveTo(engine, findNpc(engine.getState(), "assistant").getPosition());
        engine.handleInput(InputCommand.fromKey('e'));
        engine.handleInput(InputCommand.answer("pom.xml"));
        engine.handleInput(InputCommand.fromKey('e'));
    }

    private Item findItem(GameState state, String itemId) {
        for (Item item : state.getItems()) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        throw new AssertionError("Missing item: " + itemId);
    }

    private Npc findNpc(GameState state, String npcId) {
        for (Npc npc : state.getNpcs()) {
            if (npc.getId().equals(npcId)) {
                return npc;
            }
        }
        throw new AssertionError("Missing npc: " + npcId);
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
                        && world.isWalkable(next)) {
                    visited[next.getY()][next.getX()] = true;
                    queue.add(new PathNode(next, current.path + keyFor(direction)));
                }
            }
        }
        throw new AssertionError("Could not find path to " + target);
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
