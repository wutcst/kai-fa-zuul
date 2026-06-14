package cn.edu.whut.sept.dungeon.io;

import cn.edu.whut.sept.dungeon.core.Direction;
import cn.edu.whut.sept.dungeon.core.GameEngine;
import cn.edu.whut.sept.dungeon.core.GameState;
import cn.edu.whut.sept.dungeon.core.InputCommand;
import cn.edu.whut.sept.dungeon.core.VisibilityState;
import cn.edu.whut.sept.dungeon.entity.Item;
import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.World;
import org.junit.Test;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SaveManagerTest {
    @Test
    public void saveAndLoadRestoresPlayerPosition() {
        File saveFile = saveFile("position");
        SaveManager saveManager = new SaveManager(saveFile);

        GameState saved = new GameEngine(saveManager).playWithInputString("n20260614sdd:q").getState();
        GameState loaded = new GameEngine(saveManager).playWithInputString("l").getState();

        assertTrue(saveFile.exists());
        assertEquals(saved.getSeed(), loaded.getSeed());
        assertEquals(saved.getPlayer().getX(), loaded.getPlayer().getX());
        assertEquals(saved.getPlayer().getY(), loaded.getPlayer().getY());
        assertEquals(saved.getPlayer().getDirection(), loaded.getPlayer().getDirection());
        assertEquals(saved.getPlayer().getSteps(), loaded.getPlayer().getSteps());
    }

    @Test
    public void saveAndLoadRestoresWorldAndExploredState() {
        File saveFile = saveFile("world");
        SaveManager saveManager = new SaveManager(saveFile);

        GameState saved = new GameEngine(saveManager).playWithInputString("n20260614sdd:q").getState();
        GameState loaded = new GameEngine(saveManager).playWithInputString("l").getState();

        assertEquals(saved.getWorld().toTileString(), loaded.getWorld().toTileString());
        assertEquals(saved.getWorld().getDefenseHallPosition(), loaded.getWorld().getDefenseHallPosition());
        assertEquals(saved.getExploredCount(), loaded.getExploredCount());
        assertTrue(loaded.isExplored(saved.getPlayer().getX(), saved.getPlayer().getY()));
        assertEquals(VisibilityState.VISIBLE,
                loaded.getVisibilityState(loaded.getPlayer().getX(), loaded.getPlayer().getY()));
        assertEquals("Loaded saved game.", loaded.getMessage());
    }

    @Test
    public void saveAndLoadRestoresInventoryAndItemState() {
        File saveFile = saveFile("inventory");
        SaveManager saveManager = new SaveManager(saveFile);
        GameEngine engine = new GameEngine(saveManager);
        engine.handleInput(InputCommand.newGame(123L));
        Item item = engine.getState().getItems().get(0);

        moveTo(engine, item.getPosition());
        engine.handleInput(InputCommand.fromKey('e'));
        engine.handleInput(InputCommand.saveAndQuit());
        GameState loaded = new GameEngine(saveManager).playWithInputString("l").getState();

        assertTrue(loaded.getInventory().contains(item.getId()));
        assertTrue(findItem(loaded, item.getId()).isCollected());
    }

    @Test
    public void saveAndLoadRestoresQuestAndNpcState() {
        File saveFile = saveFile("quest");
        SaveManager saveManager = new SaveManager(saveFile);
        GameEngine engine = new GameEngine(saveManager);
        engine.handleInput(InputCommand.newGame(123L));

        engine.handleInput(InputCommand.answer("pom.xml"));
        engine.handleInput(InputCommand.saveAndQuit());
        GameState loaded = new GameEngine(saveManager).playWithInputString("l").getState();

        assertTrue(loaded.getQuest().isMavenPuzzleSolved());
        assertFalse(loaded.getNpcs().isEmpty());
        assertEquals(engine.getState().getNpcs().get(0).getPosition(), loaded.getNpcs().get(0).getPosition());
    }

    @Test
    public void loadWithoutSaveShowsMessage() {
        File saveFile = saveFile("missing");
        if (saveFile.exists()) {
            assertTrue(saveFile.delete());
        }

        GameState loaded = new GameEngine(new SaveManager(saveFile)).playWithInputString("l").getState();

        assertFalse(loaded.isStarted());
        assertEquals("No saved game.", loaded.getMessage());
    }

    private File saveFile(String name) {
        File directory = new File("target/test-saves");
        if (!directory.exists()) {
            assertTrue(directory.mkdirs());
        }
        File file = new File(directory, name + "-save.json");
        if (file.exists()) {
            assertTrue(file.delete());
        }
        return file;
    }

    private Item findItem(GameState state, String itemId) {
        for (Item item : state.getItems()) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        throw new AssertionError("Missing item: " + itemId);
    }

    private void moveTo(GameEngine engine, Position target) {
        String path = pathTo(engine.getState(), target);
        for (int i = 0; i < path.length(); i++) {
            engine.handleInput(InputCommand.fromKey(path.charAt(i)));
        }
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
            Direction[] directions = {
                    Direction.NORTH,
                    Direction.SOUTH,
                    Direction.WEST,
                    Direction.EAST
            };
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
