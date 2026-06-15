package cn.edu.whut.sept.dungeon.io;

import cn.edu.whut.sept.dungeon.core.Direction;
import cn.edu.whut.sept.dungeon.core.GameEngine;
import cn.edu.whut.sept.dungeon.core.GameStatus;
import cn.edu.whut.sept.dungeon.core.GameState;
import cn.edu.whut.sept.dungeon.core.InputCommand;
import cn.edu.whut.sept.dungeon.core.VisibilityState;
import cn.edu.whut.sept.dungeon.entity.Enemy;
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

        GameState saved = new GameEngine(saveManager).playWithInputString("n20260614sll:q").getState();
        GameState loaded = new GameEngine(saveManager).playWithInputString("o").getState();

        assertTrue(saveFile.exists());
        assertEquals(saved.getSeed(), loaded.getSeed());
        assertEquals(saved.getPlayer().getX(), loaded.getPlayer().getX());
        assertEquals(saved.getPlayer().getY(), loaded.getPlayer().getY());
        assertEquals(saved.getPlayer().getDirection(), loaded.getPlayer().getDirection());
        assertEquals(saved.getPlayer().getSteps(), loaded.getPlayer().getSteps());
        assertEquals(saved.getPlayer().getHp(), loaded.getPlayer().getHp());
        assertEquals(saved.getPlayer().getMaxHp(), loaded.getPlayer().getMaxHp());
        assertEquals(saved.getPlayer().getAtk(), loaded.getPlayer().getAtk());
        assertEquals(saved.getPlayer().getDef(), loaded.getPlayer().getDef());
        assertEquals(saved.getPlayer().getLevel(), loaded.getPlayer().getLevel());
        assertEquals(saved.getPlayer().getExp(), loaded.getPlayer().getExp());
    }

    @Test
    public void saveAndLoadRestoresWorldAndExploredState() {
        File saveFile = saveFile("world");
        SaveManager saveManager = new SaveManager(saveFile);

        GameState saved = new GameEngine(saveManager).playWithInputString("n20260614sll:q").getState();
        GameState loaded = new GameEngine(saveManager).playWithInputString("o").getState();

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
        GameState loaded = new GameEngine(saveManager).playWithInputString("o").getState();

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
        GameState loaded = new GameEngine(saveManager).playWithInputString("o").getState();

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

        GameState loaded = new GameEngine(new SaveManager(saveFile)).playWithInputString("o").getState();

        assertFalse(loaded.isStarted());
        assertEquals("No saved game.", loaded.getMessage());
    }

    @Test
    public void saveAndLoadRestoresGameOverStatus() {
        File saveFile = saveFile("game-over");
        SaveManager saveManager = new SaveManager(saveFile);
        GameState defeated = new GameEngine(saveManager).playWithInputString("n123s").getState().damagePlayer(100);

        saveManager.save(defeated);
        GameState loaded = new GameEngine(saveManager).playWithInputString("o").getState();

        assertEquals(GameStatus.GAME_OVER, loaded.getStatus());
        assertEquals(0, loaded.getPlayer().getHp());
        assertTrue(loaded.isGameOver());
    }

    @Test
    public void saveAndLoadRestoresEnemies() {
        File saveFile = saveFile("enemies");
        SaveManager saveManager = new SaveManager(saveFile);
        GameEngine engine = new GameEngine(saveManager);
        engine.handleInput(InputCommand.newGame(123L));
        Enemy enemy = engine.getState().getEnemies().get(0);
        Position adjacent = adjacentWalkableTile(engine.getState(), enemy.getPosition());
        GameState adjacentState = stateAfterPath(engine.getState(), adjacent);
        GameState attacked = adjacentState.movePlayer(directionBetween(adjacent, enemy.getPosition()));

        saveManager.save(attacked);
        GameState loaded = new GameEngine(saveManager).playWithInputString("o").getState();
        Enemy loadedEnemy = findEnemy(loaded, enemy.getId());

        assertFalse(loaded.getEnemies().isEmpty());
        assertEquals(enemy.getId(), loaded.getEnemies().get(0).getId());
        assertEquals(enemy.getType(), loaded.getEnemies().get(0).getType());
        assertTrue(loadedEnemy.getHp() < enemy.getHp());
    }

    @Test
    public void saveAndLoadRestoresEquipmentAndBoost() {
        File saveFile = saveFile("equipment");
        SaveManager saveManager = new SaveManager(saveFile);
        GameState state = GameState.newGame(123L);
        GameState withWeapon = stateAfterPath(state, findItem(state, "steel-keyboard").getPosition()).interact();
        GameState withCoffee = stateAfterPath(withWeapon, findItem(withWeapon, "coffee").getPosition()).interact()
                .describeInventory();

        saveManager.save(withCoffee);
        GameState loaded = new GameEngine(saveManager).playWithInputString("o").getState();

        assertEquals("steel-keyboard", loaded.getPlayer().getWeapon());
        assertEquals(9, loaded.getPlayer().getAtk());
        assertEquals(3, loaded.getPlayer().getCoffeeBoost());
        assertFalse(loaded.getInventory().contains("coffee"));
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

    private Enemy findEnemy(GameState state, String enemyId) {
        for (Enemy enemy : state.getEnemies()) {
            if (enemy.getId().equals(enemyId)) {
                return enemy;
            }
        }
        throw new AssertionError("Missing enemy: " + enemyId);
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
                        && world.isWalkable(next)
                        && state.enemyAt(next) == null) {
                    visited[next.getY()][next.getX()] = true;
                    queue.add(new PathNode(next, current.path + keyFor(direction)));
                }
            }
        }
        throw new AssertionError("Could not find path to " + target);
    }

    private Position adjacentWalkableTile(GameState state, Position target) {
        Direction[] directions = {
                Direction.NORTH,
                Direction.SOUTH,
                Direction.WEST,
                Direction.EAST
        };
        for (Direction direction : directions) {
            Position candidate = new Position(target.getX() - direction.getDx(), target.getY() - direction.getDy());
            if (state.getWorld().contains(candidate.getX(), candidate.getY())
                    && state.getWorld().isWalkable(candidate)
                    && !candidate.equals(state.getPlayer().getPosition())
                    && state.enemyAt(candidate) == null) {
                try {
                    pathTo(state, candidate);
                    return candidate;
                } catch (AssertionError ignored) {
                    // Try the next side of the enemy.
                }
            }
        }
        throw new AssertionError("Could not find adjacent walkable tile for " + target);
    }

    private Direction directionBetween(Position from, Position to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        Direction[] directions = {
                Direction.NORTH,
                Direction.SOUTH,
                Direction.WEST,
                Direction.EAST
        };
        for (Direction direction : directions) {
            if (direction.getDx() == dx && direction.getDy() == dy) {
                return direction;
            }
        }
        throw new AssertionError("Positions are not adjacent: " + from + " -> " + to);
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
