package cn.edu.whut.sept.dungeon.render;

import cn.edu.whut.sept.dungeon.core.GameEngine;
import cn.edu.whut.sept.dungeon.core.GameState;
import cn.edu.whut.sept.dungeon.core.GameText;
import cn.edu.whut.sept.dungeon.core.InputCommand;
import cn.edu.whut.sept.dungeon.core.VisibilityState;
import cn.edu.whut.sept.dungeon.core.Direction;
import cn.edu.whut.sept.dungeon.entity.Enemy;
import cn.edu.whut.sept.dungeon.entity.Inventory;
import cn.edu.whut.sept.dungeon.entity.Item;
import cn.edu.whut.sept.dungeon.entity.Npc;
import cn.edu.whut.sept.dungeon.entity.Trap;
import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.Room;
import cn.edu.whut.sept.dungeon.world.World;
import org.junit.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TileRendererTest {
    @Test
    public void rendererUsesLargeCameraViewportForActionDungeon() {
        TilePanel tilePanel = new TilePanel();
        StatusPanel statusPanel = new StatusPanel();
        LogPanel logPanel = new LogPanel();

        assertEquals(32, TileRenderer.TILE_SIZE);
        assertEquals(17, TileRenderer.VIEWPORT_WIDTH);
        assertEquals(15, TileRenderer.VIEWPORT_HEIGHT);
        assertEquals(TileRenderer.VIEWPORT_WIDTH * TileRenderer.TILE_SIZE, tilePanel.getPreferredSize().width);
        assertEquals(TileRenderer.VIEWPORT_HEIGHT * TileRenderer.TILE_SIZE, tilePanel.getPreferredSize().height);
        statusPanel.setState(new GameEngine().playWithInputString("n123s").getState());
        logPanel.setState(GameState.initial());
        assertEquals(544, tilePanel.getPreferredSize().width);
        assertEquals(480, tilePanel.getPreferredSize().height);
        assertEquals(200, statusPanel.getPreferredSize().width);
        assertEquals(200, statusPanel.getMaximumSize().width);
        assertEquals(110, logPanel.getPreferredSize().height);
        assertEquals(1, logPanel.getLineCountForTest());
    }

    @Test
    public void logPanelKeepsRecentMessagesWhenManyStatesArrive() {
        LogPanel logPanel = new LogPanel();
        GameState state = new GameEngine().playWithInputString("n123s").getState();

        for (int i = 0; i < 25; i++) {
            logPanel.setState(state.withMessage("日志 " + i));
        }

        assertEquals(20, logPanel.getLineCountForTest());
    }

    @Test
    public void logPanelIgnoresEmptyAndTickMessages() {
        LogPanel logPanel = new LogPanel();
        GameState state = new GameEngine().playWithInputString("n123s").getState();

        logPanel.setState(state.withMessage(""));
        logPanel.setState(state.withMessage(GameText.tick(1)));
        logPanel.setState(state.withMessage("Tick 1."));

        assertEquals(0, logPanel.getLineCountForTest());
    }

    @Test
    public void logPanelKeepsBusinessMessagesAndDoesNotLetTicksPushThemOut() {
        LogPanel logPanel = new LogPanel();
        GameState state = new GameEngine().playWithInputString("n123s").getState();

        logPanel.setState(state.withMessage("NPC：请准备答辩材料。"));
        for (int i = 1; i <= 25; i++) {
            logPanel.setState(state.withMessage(GameText.tick(i)));
        }

        assertEquals(1, logPanel.getLineCountForTest());
    }

    @Test
    public void statusPanelRendersInventoryAsFixedWidthVerticalList() {
        StatusPanel statusPanel = new StatusPanel();
        GameState state = new GameEngine().playWithInputString("n123s").getState();

        statusPanel.setState(withInventory(state, Inventory.empty()));
        assertEquals(1, statusPanel.getInventoryLineCountForTest());

        statusPanel.setState(withInventory(state, Inventory.of(Arrays.asList(
                "report", "coffee", "a-very-long-campus-dungeon-inventory-item-name"))));

        assertEquals(3, statusPanel.getInventoryLineCountForTest());
        assertTrue(statusPanel.getInventoryListWidthForTest() <= statusPanel.getPreferredSize().width);
    }

    @Test
    public void swingFrameDetectsAssistantPuzzlePromptAfterInteraction() {
        GameEngine engine = engineReadyForAssistantPuzzle();
        GameState prompt = engine.handleInput(InputCommand.fromKey('e'));

        assertTrue(SwingGameFrame.shouldPromptForPuzzleAnswer(InputCommand.fromKey('e'), prompt.getMessage()));
    }

    @Test
    public void swingFrameDoesNotPromptForNonInteractionOrNonPuzzleMessages() {
        assertTrue(!SwingGameFrame.shouldPromptForPuzzleAnswer(InputCommand.fromKey('w'),
                GameText.assistantMavenPuzzle()));
        assertTrue(!SwingGameFrame.shouldPromptForPuzzleAnswer(InputCommand.fromKey('e'), GameText.mavenIncorrect()));
        assertTrue(!SwingGameFrame.shouldPromptForPuzzleAnswer(null, GameText.assistantMavenPuzzle()));
    }

    @Test
    public void puzzleAnswerFlowWorksWithoutConstructingGuiFrame() {
        GameEngine engine = engineReadyForAssistantPuzzle();
        GameState prompt = engine.handleInput(InputCommand.fromKey('e'));
        assertEquals(GameText.assistantMavenPuzzle(), prompt.getMessage());

        GameState solved = engine.handleInput(InputCommand.answer("pom.xml"));
        assertTrue(solved.getQuest().isMavenPuzzleSolved());
        assertEquals(GameText.mavenCorrect(), solved.getMessage());
    }

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
    public void rendererDoesNotRevealEnemiesInUnexploredRooms() {
        GameState state = new GameEngine().playWithInputString("n123s").getState();
        TileRenderer renderer = new TileRenderer();
        Enemy hiddenEnemy = firstUnseenEnemy(state);

        assertEquals(TileRenderer.UNEXPLORED_COLOR,
                renderer.colorFor(state, hiddenEnemy.getPosition().getX(), hiddenEnemy.getPosition().getY()));
        assertEquals("", renderer.glyphFor(state, hiddenEnemy.getPosition().getX(), hiddenEnemy.getPosition().getY()));
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
    public void rendererDarkensExploredRoomAfterLeavingIt() {
        GameState start = new GameEngine().playWithInputString("n123s").getState();
        RoomVisit visit = enterDifferentRoom(start);
        TileRenderer renderer = new TileRenderer();

        assertEquals(TileRenderer.SEEN_FLOOR_COLOR,
                renderer.colorFor(visit.state, visit.previousRoom.getX(), visit.previousRoom.getY()));
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
    public void rendererDrawsCombatRoomLockWarning() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        Enemy enemy = engine.getState().getEnemies().get(0);
        GameState state = stateAfterPath(engine.getState(), adjacentWalkableTile(engine.getState(), enemy.getPosition()));

        BufferedImage image = new BufferedImage(TileRenderer.VIEWPORT_WIDTH * TileRenderer.TILE_SIZE,
                TileRenderer.VIEWPORT_HEIGHT * TileRenderer.TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        new TileRenderer().draw(state, graphics);
        graphics.dispose();

        assertEquals(TileRenderer.ROOM_LOCK_COLOR.getRGB(), findFirstPixel(image, TileRenderer.ROOM_LOCK_COLOR));
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
    public void rendererUsesProjectileGlyphAndColorForVisibleProjectile() {
        GameState state = new GameEngine().playWithInputString("n123sdj!tick(1)").getState();
        Position projectilePosition = state.getProjectiles().get(0).getPosition();
        TileRenderer renderer = new TileRenderer();

        assertEquals(TileRenderer.PROJECTILE_COLOR,
                renderer.colorFor(state, projectilePosition.getX(), projectilePosition.getY()));
        assertEquals("*", renderer.glyphFor(state, projectilePosition.getX(), projectilePosition.getY()));
    }

    @Test
    public void rendererProvidesDistinctPixelGlyphsForGameObjects() {
        TileRenderer renderer = new TileRenderer();
        GameState state = new GameEngine().playWithInputString("n123sd").getState();
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

    private int findFirstPixel(BufferedImage image, Color color) {
        int target = color.getRGB();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == target) {
                    return target;
                }
            }
        }
        throw new AssertionError("Missing color: " + color);
    }

    private Enemy firstUnseenEnemy(GameState state) {
        for (Enemy enemy : state.getEnemies()) {
            if (state.getVisibilityState(enemy.getPosition().getX(), enemy.getPosition().getY())
                    == VisibilityState.UNSEEN) {
                return enemy;
            }
        }
        throw new AssertionError("Could not find an unseen enemy.");
    }

    private RoomVisit enterDifferentRoom(GameState state) {
        Room currentRoom = state.getWorld().getRooms().get(state.currentRoomState().getId());
        for (int i = 0; i < state.getWorld().getRooms().size(); i++) {
            Room room = state.getWorld().getRooms().get(i);
            if (!room.equals(currentRoom)
                    && state.getWorld().isReachable(state.getPlayer().getPosition(), room.getCenter())) {
                return new RoomVisit(stateAfterPath(state, room.getCenter()), currentRoom);
            }
        }
        throw new AssertionError("Could not find a different reachable room.");
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
        GameState current = state;
        for (int attempt = 0; attempt < 20 && !current.getPlayer().getPosition().equals(target); attempt++) {
            String path = pathTo(current, target);
            boolean interruptedByCombatRoom = false;
            for (int i = 0; i < path.length(); i++) {
                Position before = current.getPlayer().getPosition();
                current = current.movePlayer(InputCommand.fromKey(path.charAt(i)).getDirection());
                if (current.getPlayer().getPosition().equals(target)) {
                    return current;
                }
                if (before.equals(current.getPlayer().getPosition())
                        && GameText.combatRoomLockedExit().equals(current.getMessage())) {
                    current = clearCurrentCombatRoom(current);
                    interruptedByCombatRoom = true;
                    break;
                }
            }
            if (!interruptedByCombatRoom && !current.getPlayer().getPosition().equals(target)) {
                break;
            }
        }
        if (!current.getPlayer().getPosition().equals(target)) {
            throw new AssertionError("Could not reach " + target + " from " + current.getPlayer().getPosition()
                    + ": " + current.getMessage());
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
        int attempts = 0;
        while (current.getDepth() < targetDepth && attempts < 10) {
            attempts++;
            current = stateAfterPath(current, current.getWorld().getStairsPosition()).interact();
        }
        if (current.getDepth() < targetDepth) {
            throw new AssertionError("Could not descend to depth " + targetDepth + ": " + current.getMessage());
        }
        return current;
    }

    private GameState clearCurrentCombatRoom(GameState state) {
        GameState current = state;
        int roomId = state.currentRoomState() == null ? -1 : state.currentRoomState().getId();
        for (Enemy enemy : state.getEnemies()) {
            if (enemy.isAlive() && current.roomStateAt(enemy.getPosition()) != null
                    && current.roomStateAt(enemy.getPosition()).getId() == roomId) {
                Position adjacent = adjacentWalkableTile(current, enemy.getPosition());
                current = stateAfterPath(current, adjacent);
                Direction direction = directionBetween(adjacent, enemy.getPosition());
                while (findEnemy(current, enemy.getId()).isAlive()) {
                    current = current.movePlayer(direction);
                }
            }
        }
        return current;
    }

    private GameState withInventory(GameState state, Inventory inventory) {
        return GameState.restored(state.getSeed(), state.getDepth(), state.isStarted(), state.isExited(),
                state.isSaveRequested(), state.getStatus(), state.getPlayer(), state.getWorld(), inventory,
                state.getItems(), state.getEnemies(), state.getNpcs(), state.getTraps(), state.getQuest(),
                state.copyExplored(), state.getMessage());
    }

    private GameEngine engineReadyForAssistantPuzzle() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        moveTo(engine, findItem(engine.getState(), "usb").getPosition());
        engine.handleInput(InputCommand.fromKey('e'));
        moveTo(engine, findNpc(engine.getState(), "assistant").getPosition());
        return engine;
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

    private Npc findNpc(GameState state, String npcId) {
        for (Npc npc : state.getNpcs()) {
            if (npc.getId().equals(npcId)) {
                return npc;
            }
        }
        throw new AssertionError("Missing npc: " + npcId);
    }

    private Direction directionBetween(Position from, Position to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
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

    private static final class RoomVisit {
        private final GameState state;
        private final Room previousRoom;

        private RoomVisit(GameState state, Room previousRoom) {
            this.state = state;
            this.previousRoom = previousRoom;
        }
    }
}
