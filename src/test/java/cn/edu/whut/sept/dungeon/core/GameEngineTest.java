package cn.edu.whut.sept.dungeon.core;

import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.World;
import cn.edu.whut.sept.dungeon.entity.Enemy;
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
        assertEquals(GameStatus.PLAYING, state.getStatus());
        assertEquals(30, state.getPlayer().getHp());
        assertEquals(30, state.getPlayer().getMaxHp());
        assertEquals(5, state.getPlayer().getAtk());
        assertEquals(1, state.getPlayer().getDef());
        assertEquals(1, state.getPlayer().getLevel());
        assertEquals(0, state.getPlayer().getExp());
        assertEquals(state.getWorld().getSpawnPosition().getX(), state.getPlayer().getX());
        assertEquals(state.getWorld().getSpawnPosition().getY(), state.getPlayer().getY());
        assertTrue(state.getWorld().isWalkable(state.getWorld().getSpawnPosition()));
        assertFalse(state.getEnemies().isEmpty());
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

        GameState moved = new GameEngine().playWithInputString("n123sl").getState();

        assertEquals(initial.getPlayer().getX() + 1, moved.getPlayer().getX());
        assertEquals(initial.getPlayer().getY(), moved.getPlayer().getY());
        assertEquals(Direction.EAST, moved.getPlayer().getDirection());
        assertEquals(1, moved.getPlayer().getSteps());
        assertEquals("Moved EAST.", moved.getMessage());
    }

    @Test
    public void vimMovementKeysMoveInExpectedDirections() {
        assertEquals(Direction.WEST, InputCommand.fromKey('h').getDirection());
        assertEquals(Direction.SOUTH, InputCommand.fromKey('j').getDirection());
        assertEquals(Direction.NORTH, InputCommand.fromKey('k').getDirection());
        assertEquals(Direction.EAST, InputCommand.fromKey('l').getDirection());
    }

    @Test
    public void oIsLoadAndLIsEastMovement() {
        assertEquals(InputCommand.Type.LOAD, InputCommand.fromKey('o').getType());
        assertEquals(InputCommand.Type.MOVE, InputCommand.fromKey('l').getType());
        assertEquals(Direction.EAST, InputCommand.fromKey('l').getDirection());
    }

    @Test
    public void playerCannotMoveThroughWall() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        GameState beforeBlockedMove = moveUntilNextStepIsBlocked(engine, Direction.WEST);

        GameState state = engine.handleInput(InputCommand.fromKey('h'));

        assertTrue(state.isStarted());
        assertEquals(beforeBlockedMove.getPlayer().getX(), state.getPlayer().getX());
        assertEquals(beforeBlockedMove.getPlayer().getY(), state.getPlayer().getY());
        assertTrue(state.getWorld().isWalkable(state.getPlayer().getPosition()));
        assertEquals(Direction.WEST, state.getPlayer().getDirection());
        assertEquals("Blocked by wall.", state.getMessage());
    }

    @Test
    public void validMoveIncreasesStepsButBlockedMoveDoesNot() {
        GameState moved = new GameEngine().playWithInputString("n123sl").getState();
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        GameState beforeBlockedMove = moveUntilNextStepIsBlocked(engine, Direction.WEST);
        GameState blocked = engine.handleInput(InputCommand.fromKey('h'));

        assertEquals(1, moved.getPlayer().getSteps());
        assertEquals(beforeBlockedMove.getPlayer().getSteps(), blocked.getPlayer().getSteps());
    }

    @Test
    public void playerDamageCanEndGame() {
        GameState state = new GameEngine().playWithInputString("n123s").getState();

        GameState damaged = state.damagePlayer(7);
        GameState defeated = damaged.damagePlayer(100);

        assertEquals(23, damaged.getPlayer().getHp());
        assertEquals(GameStatus.PLAYING, damaged.getStatus());
        assertEquals(0, defeated.getPlayer().getHp());
        assertEquals(GameStatus.GAME_OVER, defeated.getStatus());
        assertTrue(defeated.isGameOver());
        assertEquals("Game over. HP reached 0.", defeated.getMessage());
    }

    @Test
    public void gameOverStopsNormalMovement() {
        GameState defeated = new GameEngine().playWithInputString("n123s").getState().damagePlayer(100);

        GameState afterMove = defeated.movePlayer(Direction.EAST);
        GameState afterAnswer = defeated.answer("pom.xml");

        assertEquals(GameStatus.GAME_OVER, afterMove.getStatus());
        assertEquals(defeated.getPlayer().getX(), afterMove.getPlayer().getX());
        assertEquals(defeated.getPlayer().getY(), afterMove.getPlayer().getY());
        assertEquals(defeated.getPlayer().getSteps(), afterMove.getPlayer().getSteps());
        assertEquals("Game over. Start a new game to try again.", afterMove.getMessage());
        assertFalse(afterAnswer.getQuest().isMavenPuzzleSolved());
        assertEquals("Game over. Start a new game to try again.", afterAnswer.getMessage());
    }

    @Test
    public void playerAttacksAdjacentEnemyWithoutMovingIntoEnemyTile() {
        GameState state = GameState.newGame(123L);
        Enemy enemy = state.getEnemies().get(0);
        Position adjacent = adjacentWalkableTile(state, enemy.getPosition());
        GameState adjacentState = stateAfterPath(state, adjacent);

        Direction attackDirection = directionBetween(adjacent, enemy.getPosition());
        GameState attacked = adjacentState.movePlayer(attackDirection);
        Enemy damagedEnemy = attacked.enemyAt(enemy.getPosition());

        assertEquals(adjacent, attacked.getPlayer().getPosition());
        assertNotNull(damagedEnemy);
        assertTrue(damagedEnemy.getHp() < enemy.getHp());
        assertEquals("Hit " + enemy.getType() + " for 5 damage.", attacked.getMessage());
    }

    @Test
    public void defeatedEnemyGrantsExpAndNoLongerBlocksMovement() {
        GameState state = GameState.newGame(123L);
        Enemy enemy = state.getEnemies().get(0);
        Position adjacent = adjacentWalkableTile(state, enemy.getPosition());
        GameState adjacentState = stateAfterPath(state, adjacent);
        Direction attackDirection = directionBetween(adjacent, enemy.getPosition());

        GameState firstHit = adjacentState.movePlayer(attackDirection);
        GameState defeated = firstHit.movePlayer(attackDirection);
        GameState movedIntoTile = defeated.movePlayer(attackDirection);

        assertNotNull(firstHit.enemyAt(enemy.getPosition()));
        assertEquals(enemy.getExpReward(), defeated.getPlayer().getExp());
        assertEquals("Defeated " + enemy.getType() + " and gained " + enemy.getExpReward() + " EXP.",
                defeated.getMessage());
        assertEquals(enemy.getPosition(), movedIntoTile.getPlayer().getPosition());
    }

    @Test
    public void enemyMovesAfterValidPlayerMove() {
        GameState state = GameState.newGame(123L);
        Enemy enemy = state.getEnemies().get(0);
        Position playerTarget = nearbyWalkableTile(state, enemy.getPosition(), 6);
        GameState nearEnemy = moveByRulesOnly(state, playerTarget);
        Enemy beforeEnemy = findEnemy(nearEnemy, enemy.getId());

        GameState afterTurn = nearEnemy.advanceEnemyTurn();
        Enemy afterEnemy = findEnemy(afterTurn, enemy.getId());

        assertTrue(afterEnemy.getPosition().manhattanDistanceTo(afterTurn.getPlayer().getPosition())
                < beforeEnemy.getPosition().manhattanDistanceTo(playerTarget));
    }

    @Test
    public void adjacentEnemyAttacksAfterPlayerAction() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        Enemy enemy = engine.getState().getEnemies().get(0);
        Position adjacent = adjacentWalkableTile(engine.getState(), enemy.getPosition());
        GameState adjacentState = stateAfterPath(engine.getState(), adjacent);

        GameState afterTurn = adjacentState.advanceEnemyTurn();

        assertEquals(28, afterTurn.getPlayer().getHp());
        assertTrue(afterTurn.getMessage().contains(enemy.getType() + " hits you for 2 damage"));
    }

    @Test
    public void enemyActsAfterPlayerAttack() {
        GameState state = GameState.newGame(123L);
        Enemy enemy = state.getEnemies().get(0);
        Position adjacent = adjacentWalkableTile(state, enemy.getPosition());
        GameState adjacentState = stateAfterPath(state, adjacent);
        Direction attackDirection = directionBetween(adjacent, enemy.getPosition());

        GameState afterAttack = adjacentState.movePlayer(attackDirection);
        GameState afterEnemyTurn = afterAttack.advanceEnemyTurn();

        assertEquals(30, afterAttack.getPlayer().getHp());
        assertEquals(28, afterEnemyTurn.getPlayer().getHp());
        assertTrue(afterEnemyTurn.getMessage().contains(enemy.getType() + " hits you for 2 damage"));
    }

    @Test
    public void invalidInputAndBlockedMoveDoNotAdvanceEnemyTurn() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        Enemy enemy = engine.getState().getEnemies().get(0);
        Position beforeEnemyPosition = enemy.getPosition();

        GameState unknown = engine.handleInput(InputCommand.fromKey('?'));
        GameState beforeBlockedMove = moveUntilNextStepIsBlocked(engine, Direction.WEST);
        GameState blocked = engine.handleInput(InputCommand.fromKey('h'));

        assertEquals(beforeEnemyPosition, findEnemy(unknown, enemy.getId()).getPosition());
        assertEquals(findEnemy(beforeBlockedMove, enemy.getId()).getPosition(),
                findEnemy(blocked, enemy.getId()).getPosition());
        assertEquals("Blocked by wall.", blocked.getMessage());
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
    public void pickingUpEquipmentUpdatesCombatStats() {
        GameState state = GameState.newGame(123L);
        Item weapon = findItem(state, "steel-keyboard");
        GameState weaponState = stateAfterPath(state, weapon.getPosition()).interact();
        Item armor = findItem(weaponState, "review-robe");

        GameState equipped = stateAfterPath(weaponState, armor.getPosition()).interact();

        assertEquals("steel-keyboard", weaponState.getPlayer().getWeapon());
        assertEquals(9, weaponState.getPlayer().getAtk());
        assertEquals("review-robe", equipped.getPlayer().getArmor());
        assertEquals(4, equipped.getPlayer().getDef());
    }

    @Test
    public void potionRestoresHpAndIsConsumed() {
        GameState state = GameState.newGame(123L);
        Item potion = findItem(state, "small-potion");
        GameState withPotion = stateAfterPath(state, potion.getPosition()).interact().damagePlayer(12);

        GameState healed = withPotion.describeInventory();

        assertEquals(26, healed.getPlayer().getHp());
        assertFalse(healed.getInventory().contains("small-potion"));
        assertTrue(healed.getMessage().contains("restored 8 HP"));
    }

    @Test
    public void coffeeBoostsNextAttackAndThenExpires() {
        GameState state = GameState.newGame(123L);
        Item coffee = findItem(state, "coffee");
        GameState withCoffee = stateAfterPath(state, coffee.getPosition()).interact();
        GameState boosted = withCoffee.describeInventory();
        Enemy enemy = boosted.getEnemies().get(0);
        Position adjacent = adjacentWalkableTile(boosted, enemy.getPosition());
        GameState adjacentState = stateAfterPath(boosted, adjacent);

        GameState attacked = adjacentState.movePlayer(directionBetween(adjacent, enemy.getPosition()));

        assertEquals(3, boosted.getPlayer().getCoffeeBoost());
        assertFalse(boosted.getInventory().contains("coffee"));
        assertFalse(findEnemy(attacked, enemy.getId()).isAlive());
        assertEquals(enemy.getExpReward(), attacked.getPlayer().getExp());
        assertEquals(0, attacked.getPlayer().getCoffeeBoost());
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

    private Position adjacentWalkableTile(GameState state, Position target) {
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
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
                        && state.enemyAt(next) == null) {
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

    private GameState stateAfterPath(GameState state, Position target) {
        return moveByRulesOnly(state, target);
    }

    private GameState moveByRulesOnly(GameState state, Position target) {
        String path = pathTo(state, target);
        GameState current = state;
        for (int i = 0; i < path.length(); i++) {
            current = current.movePlayer(InputCommand.fromKey(path.charAt(i)).getDirection());
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

    private Position nearbyWalkableTile(GameState state, Position target, int maxDistance) {
        World world = state.getWorld();
        Position start = state.getPlayer().getPosition();
        boolean[][] visited = new boolean[world.getHeight()][world.getWidth()];
        Queue<PathNode> queue = new ArrayDeque<PathNode>();
        queue.add(new PathNode(start, ""));
        visited[start.getY()][start.getX()] = true;

        while (!queue.isEmpty()) {
            PathNode current = queue.remove();
            if (current.position.manhattanDistanceTo(target) <= maxDistance
                    && current.position.manhattanDistanceTo(target) > 1
                    && !current.position.equals(start)) {
                return current.position;
            }
            Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
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
        throw new AssertionError("Could not find nearby walkable tile for " + target);
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
                        && world.isWalkable(next)
                        && state.enemyAt(next) == null) {
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
