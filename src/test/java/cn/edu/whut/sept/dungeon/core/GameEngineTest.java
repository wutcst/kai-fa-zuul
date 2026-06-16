package cn.edu.whut.sept.dungeon.core;

import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.Room;
import cn.edu.whut.sept.dungeon.world.Tile;
import cn.edu.whut.sept.dungeon.world.World;
import cn.edu.whut.sept.dungeon.entity.Enemy;
import cn.edu.whut.sept.dungeon.entity.Inventory;
import cn.edu.whut.sept.dungeon.entity.Item;
import cn.edu.whut.sept.dungeon.entity.Npc;
import cn.edu.whut.sept.dungeon.entity.Trap;
import cn.edu.whut.sept.dungeon.projectile.Projectile;
import cn.edu.whut.sept.dungeon.quest.QuestState;
import cn.edu.whut.sept.dungeon.room.RoomState;
import cn.edu.whut.sept.dungeon.room.RoomStatus;
import cn.edu.whut.sept.dungeon.room.RoomType;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    public void replayTickCommandAdvancesDeterministicTicks() {
        GameState state = new GameEngine().playWithInputString("n123s!tick(3)").getState();

        assertEquals(3L, state.getTick());
        assertEquals("Tick 3.", state.getMessage());
    }

    @Test
    public void invalidReplayTickCountLeavesStateAndMessage() {
        GameState state = new GameEngine().playWithInputString("n123s!tick(nope)").getState();

        assertEquals(0L, state.getTick());
        assertEquals("Invalid tick count.", state.getMessage());
    }

    @Test
    public void tickDoesNotAdvanceBeforeGameStartsOrAfterGameOver() {
        GameEngine engine = new GameEngine();
        GameState initial = engine.tick();
        engine.handleInput(InputCommand.newGame(123L));
        GameState defeated = engine.getState().damagePlayer(100);

        assertEquals(0L, initial.getTick());
        assertEquals(0L, defeated.tick().getTick());
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
    public void wasdMovementKeysMoveInExpectedDirections() {
        assertEquals(Direction.NORTH, InputCommand.fromKey('w').getDirection());
        assertEquals(Direction.WEST, InputCommand.fromKey('a').getDirection());
        assertEquals(Direction.SOUTH, InputCommand.fromKey('s').getDirection());
        assertEquals(Direction.EAST, InputCommand.fromKey('d').getDirection());
    }

    @Test
    public void oIsLoadAndJOrSpaceIsAttack() {
        assertEquals(InputCommand.Type.LOAD, InputCommand.fromKey('o').getType());
        assertEquals(InputCommand.Type.ATTACK, InputCommand.fromKey('j').getType());
        assertEquals(InputCommand.Type.ATTACK, InputCommand.fromKey(' ').getType());
        assertEquals(InputCommand.Type.SKILL, InputCommand.fromKey('k').getType());
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
        assertEquals(firstHit.getPlayer().getExp() + enemy.getExpReward(), defeated.getPlayer().getExp());
        assertTrue(defeated.getMessage().contains("Defeated " + enemy.getType()));
        assertTrue(defeated.getMessage().contains("Room cleared"));
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
        GameState blocked = engine.handleInput(InputCommand.fromKey('a'));

        assertEquals(beforeEnemyPosition, findEnemy(unknown, enemy.getId()).getPosition());
        assertEquals(findEnemy(beforeBlockedMove, enemy.getId()).getPosition(),
                findEnemy(blocked, enemy.getId()).getPosition());
        assertEquals("Blocked by wall.", blocked.getMessage());
    }

    @Test
    public void attackKeyUsesCurrentDirectionWithoutMovingPlayer() {
        GameState state = GameState.newGame(123L);
        Enemy enemy = state.getEnemies().get(0);
        Position adjacent = adjacentWalkableTile(state, enemy.getPosition());

        Direction attackDirection = directionBetween(adjacent, enemy.getPosition());
        GameState adjacentState = stateAfterPath(state, adjacent);
        GameState turned = facePlayer(adjacentState, attackDirection);
        GameState attacked = turned.attack();

        assertEquals(InputCommand.Type.ATTACK, InputCommand.fromKey('j').getType());
        assertEquals(adjacent, turned.getPlayer().getPosition());
        assertEquals(adjacent, attacked.getPlayer().getPosition());
        assertEquals(1, attacked.getProjectiles().size());
        assertEquals(turned.enemyAt(enemy.getPosition()).getHp(), attacked.enemyAt(enemy.getPosition()).getHp());
        assertTrue(attacked.getMessage().contains("Fired Keyboard Pistol"));
    }

    @Test
    public void attackKeyDoesNotMoveWhenNoEnemyIsInFront() {
        GameState state = new GameEngine().playWithInputString("n123sdj").getState();

        assertEquals(Direction.EAST, state.getPlayer().getDirection());
        assertEquals(1, state.getPlayer().getSteps());
        assertEquals(1, state.getProjectiles().size());
        assertEquals("Fired Keyboard Pistol EAST.", state.getMessage());
    }

    @Test
    public void projectileMovesOnTickAndDamagesEnemyAtRange() {
        GameState state = projectileTestState();

        GameState fired = state.attack();
        GameState firstTick = fired.tick();
        GameState secondTick = firstTick.tick();
        Enemy damagedEnemy = findEnemy(secondTick, "target");

        assertEquals(1, fired.getProjectiles().size());
        assertEquals(new Position(4, 2), firstTick.getProjectiles().get(0).getPosition());
        assertTrue(damagedEnemy.getHp() < findEnemy(state, "target").getHp());
        assertTrue(secondTick.getProjectiles().isEmpty());
        assertEquals("Projectile hit Bug Slime for 4 damage.", secondTick.getMessage());
    }

    @Test
    public void projectileDisappearsWhenItHitsWall() {
        GameState state = facePlayer(projectileWallTestState(), Direction.WEST);

        GameState fired = state.attack();
        GameState afterWall = fired.tick().tick().tick();

        assertEquals(1, fired.getProjectiles().size());
        assertTrue(afterWall.getProjectiles().isEmpty());
        assertEquals("Projectile hit a wall.", afterWall.getMessage());
    }

    @Test
    public void enteringCombatRoomLocksExitsUntilEnemiesAreDefeated() {
        GameState state = combatRoomTestState();
        Enemy enemy = state.getEnemies().get(0);
        Position adjacent = new Position(5, 2);

        GameState activeRoom = stateAfterPath(state, adjacent);
        GameState blockedExit = tryLeaveCurrentRoom(activeRoom);
        GameState clearedRoom = defeatEnemy(activeRoom, enemy.getId());
        GameState afterClearExit = tryLeaveCurrentRoom(clearedRoom);

        assertEquals(RoomStatus.ACTIVE, activeRoom.currentRoomState().getStatus());
        assertEquals(RoomType.COMBAT, activeRoom.currentRoomState().getType());
        assertEquals(activeRoom.getPlayer().getPosition(), blockedExit.getPlayer().getPosition());
        assertEquals("Combat room locked. Defeat all enemies first.", blockedExit.getMessage());
        assertEquals(RoomStatus.CLEARED, clearedRoom.currentRoomState().getStatus());
        assertNotNull(findItem(clearedRoom, "room-reward-" + clearedRoom.getDepth() + "-"
                + clearedRoom.currentRoomState().getId()));
        assertFalse(activeRoom.getPlayer().getPosition().equals(afterClearExit.getPlayer().getPosition()));
    }

    @Test
    public void tickDrivenBugSlimePursuesInsideActiveRoom() {
        GameState state = activeAiRoomState(Enemy.bug("bug-ai", new Position(6, 2)),
                new Position(2, 2));

        GameState ticked = state.tick();

        assertEquals(new Position(5, 2), findEnemy(ticked, "bug-ai").getPosition());
        assertTrue(ticked.getMessage().contains("Tick") || ticked.getMessage().contains("Bug Slime"));
    }

    @Test
    public void deadlineRunnerMovesTwoStepsOnTick() {
        GameState state = activeAiRoomState(Enemy.deadline("runner-ai", new Position(6, 2)),
                new Position(2, 2));

        GameState ticked = state.tick();

        assertEquals(new Position(4, 2), findEnemy(ticked, "runner-ai").getPosition());
    }

    @Test
    public void reviewShooterFiresEnemyProjectileOnCooldown() {
        GameState state = activeAiRoomState(Enemy.reviewShooter("shooter-ai", new Position(6, 2)),
                new Position(2, 2));

        GameState ticked = state.tick().tick().tick();

        assertEquals(1, ticked.getProjectiles().size());
        assertEquals("Review Shooter fires a review note.", ticked.getMessage());
    }

    @Test
    public void enemyProjectileCanDamagePlayerOnTick() {
        GameState state = activeAiRoomState(Enemy.reviewShooter("shooter-hit", new Position(7, 2)),
                new Position(2, 2));

        GameState fired = state.tick().tick().tick();
        GameState hit = fired.tick().tick().tick().tick().tick();

        assertTrue(hit.getPlayer().getHp() < state.getPlayer().getHp());
        assertTrue(hit.getMessage().contains("Enemy projectile hit you"));
    }

    @Test
    public void reviewShooterKeepsDistanceWhenPlayerIsInRange() {
        GameState state = activeAiRoomState(Enemy.reviewShooter("shooter-distance", new Position(5, 2)),
                new Position(2, 2));

        GameState ticked = state.tick();

        assertEquals(new Position(5, 2), findEnemy(ticked, "shooter-distance").getPosition());
        assertTrue(ticked.getProjectiles().isEmpty());
    }

    @Test
    public void tickDrivenEnemyStaysInsideActiveCombatRoom() {
        GameState state = activeAiRoomState(Enemy.deadline("runner-wall", new Position(5, 2)),
                new Position(2, 2));

        GameState ticked = state.tick().tick();

        assertEquals(state.currentRoomState().getId(), ticked.roomStateAt(findEnemy(ticked, "runner-wall").getPosition()).getId());
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
        Enemy enemy = firstAliveEnemy(boosted);
        Position adjacent = adjacentWalkableTile(boosted, enemy.getPosition());
        GameState adjacentState = stateAfterPath(boosted, adjacent);

        GameState attacked = adjacentState.movePlayer(directionBetween(adjacent, enemy.getPosition()));

        assertEquals(3, boosted.getPlayer().getCoffeeBoost());
        assertFalse(boosted.getInventory().contains("coffee"));
        assertTrue(findEnemy(attacked, enemy.getId()).getHp() < enemy.getHp());
        assertEquals(0, attacked.getPlayer().getCoffeeBoost());
    }

    @Test
    public void interactOnStairsDescendsToNextDepth() {
        GameState state = GameState.newGame(123L);
        GameState atStairs = stateAfterPath(state, state.getWorld().getStairsPosition());

        GameState nextDepth = atStairs.interact();

        assertEquals(2, nextDepth.getDepth());
        assertEquals(nextDepth.getWorld().getSpawnPosition(), nextDepth.getPlayer().getPosition());
        assertFalse(state.getWorld().toTileString().equals(nextDepth.getWorld().toTileString()));
        assertEquals("Descended to depth 2.", nextDepth.getMessage());
    }

    @Test
    public void descendingPreservesPlayerProgressAndResetsLevelState() {
        GameState state = GameState.newGame(123L);
        GameState withWeapon = stateAfterPath(state, findItem(state, "steel-keyboard").getPosition()).interact();
        GameState withPotion = stateAfterPath(withWeapon, findItem(withWeapon, "small-potion").getPosition()).interact()
                .damagePlayer(5);
        int exploredBefore = withPotion.getExploredCount();

        GameState nextDepth = stateAfterPath(withPotion, withPotion.getWorld().getStairsPosition()).interact();

        assertEquals(2, nextDepth.getDepth());
        assertEquals(withPotion.getPlayer().getHp(), nextDepth.getPlayer().getHp());
        assertEquals("steel-keyboard", nextDepth.getPlayer().getWeapon());
        assertEquals(withPotion.getPlayer().getAtk(), nextDepth.getPlayer().getAtk());
        assertTrue(nextDepth.getInventory().contains("small-potion"));
        assertTrue(nextDepth.getExploredCount() < exploredBefore);
        assertTrue(nextDepth.getWorld().isReachable(nextDepth.getWorld().getSpawnPosition(),
                nextDepth.getWorld().getStairsPosition()));
    }

    @Test
    public void deeperDungeonEnemiesAreStronger() {
        GameState state = GameState.newGame(123L);
        Enemy firstDepthEnemy = state.getEnemies().get(0);
        GameState secondDepth = stateAfterPath(state, state.getWorld().getStairsPosition()).interact();
        Enemy secondDepthEnemy = secondDepth.getEnemies().get(0);

        assertTrue(secondDepthEnemy.getHp() > firstDepthEnemy.getHp());
        assertTrue(secondDepthEnemy.getAtk() > firstDepthEnemy.getAtk());
    }

    @Test
    public void trapsAreGeneratedDeterministicallyForSameSeed() {
        GameState first = GameState.newGame(123L);
        GameState second = GameState.newGame(123L);

        assertEquals(3, first.getTraps().size());
        assertEquals(first.getTraps().size(), second.getTraps().size());
        for (int i = 0; i < first.getTraps().size(); i++) {
            assertEquals(first.getTraps().get(i).getType(), second.getTraps().get(i).getType());
            assertEquals(first.getTraps().get(i).getPosition(), second.getTraps().get(i).getPosition());
        }
    }

    @Test
    public void damageTrapHurtsPlayerAndMarksTrapTriggered() {
        GameState state = GameState.newGame(123L);
        Trap trap = findTrap(state, Trap.DAMAGE);

        Position adjacent = adjacentWalkableTile(state, trap.getPosition());
        GameState nearTrap = stateAfterPath(state, adjacent);
        GameState triggered = nearTrap.movePlayer(directionBetween(adjacent, trap.getPosition()));

        assertEquals(25, triggered.getPlayer().getHp());
        assertTrue(findTrapById(triggered, trap.getId()).isTriggered());
        assertTrue(triggered.getMessage().contains("Damage trap triggered"));
    }

    @Test
    public void teleportTrapReturnsPlayerToEntrance() {
        GameState state = trapTestState(Trap.teleport("test-teleport", new Position(3, 2)));
        Trap trap = state.getTraps().get(0);

        GameState triggered = state.movePlayer(Direction.EAST);

        assertEquals(triggered.getWorld().getSpawnPosition(), triggered.getPlayer().getPosition());
        assertTrue(findTrapById(triggered, trap.getId()).isTriggered());
        assertTrue(triggered.getMessage().contains("Teleport trap triggered"));
    }

    @Test
    public void weaknessTrapLowersAttack() {
        GameState state = GameState.newGame(123L);
        Trap trap = findTrap(state, Trap.WEAKNESS);

        GameState triggered = stateAfterPath(state, trap.getPosition());

        assertEquals(3, triggered.getPlayer().getAtk());
        assertTrue(findTrapById(triggered, trap.getId()).isTriggered());
        assertTrue(triggered.getMessage().contains("Weakness trap triggered"));
    }

    @Test
    public void newDepthRegeneratesTraps() {
        GameState state = GameState.newGame(123L);
        Trap firstTrap = state.getTraps().get(0);

        GameState nextDepth = stateAfterPath(state, state.getWorld().getStairsPosition()).interact();

        assertEquals(2, nextDepth.getDepth());
        assertFalse(firstTrap.getPosition().equals(nextDepth.getTraps().get(0).getPosition()));
        assertFalse(nextDepth.getTraps().isEmpty());
    }

    @Test
    public void defenseDoorRejectsMissingMaterials() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));
        GameState finalDepth = descendToDepth(engine.getState(), 5);

        GameState withoutBoss = defeatBoss(finalDepth);
        GameState rejected = stateAfterPath(withoutBoss, withoutBoss.getWorld().getDefenseHallPosition()).interact();

        assertFalse(rejected.isCompleted());
        assertTrue(rejected.getMessage().contains("Defense hall locked."));
        assertTrue(rejected.getMessage().contains("report"));
    }

    @Test
    public void defenseDoorRejectsLiveBossEvenWithMaterials() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));

        completeNpcQuestLine(engine);
        GameState finalDepth = descendToDepth(engine.getState(), 5);
        Position adjacent = adjacentWalkableTile(finalDepth, finalDepth.getWorld().getDefenseHallPosition());
        GameState nearBoss = stateAfterPath(finalDepth, adjacent);
        GameState rejected = nearBoss.movePlayer(directionBetween(adjacent, finalDepth.getWorld().getDefenseHallPosition()));

        assertFalse(rejected.isCompleted());
        assertTrue(findEnemy(rejected, "defense-committee").isAlive());
        assertTrue(rejected.getMessage().contains("Defense Committee"));
    }

    @Test
    public void defenseDoorCompletesGameWithAllMaterials() {
        GameEngine engine = new GameEngine();
        engine.handleInput(InputCommand.newGame(123L));

        completeNpcQuestLine(engine);
        GameState armed = stateAfterPath(engine.getState(), findItem(engine.getState(), "refactor-blade").getPosition()).interact();
        GameState finalDepth = descendToDepth(armed, 5);
        GameState withoutBoss = defeatBoss(finalDepth);
        GameState completed = stateAfterPath(withoutBoss, withoutBoss.getWorld().getDefenseHallPosition()).interact();

        assertTrue(completed.isCompleted());
        assertTrue(completed.getMessage().contains("Defense completed"));
        assertTrue(completed.getMessage().contains("software engineering practice"));
    }

    @Test
    public void finalDepthSpawnsDefenseCommitteeBoss() {
        GameState finalDepth = descendToDepth(GameState.newGame(123L), 5);
        Enemy boss = findEnemy(finalDepth, "defense-committee");

        assertEquals("Defense Committee", boss.getType());
        assertTrue(boss.isAlive());
        assertEquals(finalDepth.getWorld().getDefenseHallPosition(), boss.getPosition());
        assertTrue(boss.getHp() > finalDepth.getEnemies().get(0).getHp());
    }

    @Test
    public void defeatingBossUnlocksFinalInteraction() {
        GameState state = GameState.newGame(123L);
        GameState armed = stateAfterPath(state, findItem(state, "refactor-blade").getPosition()).interact();
        GameState finalDepth = descendToDepth(armed, 5);

        GameState withoutBoss = defeatBoss(finalDepth);

        assertFalse(findEnemy(withoutBoss, "defense-committee").isAlive());
        assertTrue(withoutBoss.getMessage().contains("Final defense is ready"));
        assertTrue(withoutBoss.getMessage().contains("Room cleared"));
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
                    && state.enemyAt(candidate) == null
                    && state.trapAt(candidate) == null) {
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
                        && world.isWalkable(next)
                        && state.enemyAt(next) == null
                        && state.trapAt(next) == null) {
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

    private GameState stateAfterPathAllowingTeleport(GameState state, Position target) {
        String path = pathTo(state, target);
        GameState current = state;
        for (int i = 0; i < path.length(); i++) {
            current = current.movePlayer(InputCommand.fromKey(path.charAt(i)).getDirection());
            if (current.getMessage().contains("Teleport trap triggered")) {
                return current;
            }
        }
        return current;
    }

    private GameState moveByRulesOnly(GameState state, Position target) {
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
                        && "Combat room locked. Defeat all enemies first.".equals(current.getMessage())) {
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

    private GameState facePlayer(GameState state, Direction direction) {
        GameState.PlayerState player = state.getPlayer();
        GameState.PlayerState turnedPlayer = GameState.PlayerState.of(player.getX(), player.getY(), direction,
                player.getSteps(), player.getHp(), player.getMaxHp(), player.getAtk(), player.getDef(),
                player.getLevel(), player.getExp(), player.getWeapon(), player.getArmor(), player.getCoffeeBoost());
        return GameState.restored(state.getSeed(), state.getDepth(), state.isStarted(), state.isExited(),
                state.isSaveRequested(), state.getStatus(), turnedPlayer, state.getWorld(), state.getInventory(),
                state.getItems(), state.getEnemies(), state.getNpcs(), state.getTraps(), state.getQuest(),
                state.copyExplored(), state.getMessage());
    }

    private GameState projectileTestState() {
        int width = 8;
        int height = 5;
        Tile[][] tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = x == 0 || y == 0 || x == width - 1 || y == height - 1 ? Tile.WALL : Tile.FLOOR;
            }
        }
        Position playerPosition = new Position(3, 2);
        World world = new World(width, height, tiles,
                Collections.singletonList(new Room(1, 1, 6, 3)),
                Collections.emptyList(), playerPosition, new Position(6, 2), new Position(6, 3));
        GameState.PlayerState player = GameState.PlayerState.of(playerPosition.getX(), playerPosition.getY(),
                Direction.EAST, 0);
        List<Enemy> enemies = new ArrayList<Enemy>();
        enemies.add(Enemy.bug("target", new Position(5, 2)));
        return GameState.restored(123L, 1, true, false, false, GameStatus.PLAYING, player, world,
                Inventory.empty(), Collections.<Item>emptyList(), enemies, Collections.<Npc>emptyList(),
                Collections.<Trap>emptyList(), QuestState.initial(), null, "Projectile test.");
    }

    private GameState projectileWallTestState() {
        int width = 8;
        int height = 5;
        Tile[][] tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = x == 0 || y == 0 || x == width - 1 || y == height - 1 ? Tile.WALL : Tile.FLOOR;
            }
        }
        Position playerPosition = new Position(3, 2);
        World world = new World(width, height, tiles,
                Collections.singletonList(new Room(1, 1, 6, 3)),
                Collections.emptyList(), playerPosition, new Position(6, 2), new Position(6, 3));
        GameState.PlayerState player = GameState.PlayerState.of(playerPosition.getX(), playerPosition.getY(),
                Direction.EAST, 0);
        return GameState.restored(123L, 1, true, false, false, GameStatus.PLAYING, player, world,
                Inventory.empty(), Collections.<Item>emptyList(), Collections.<Enemy>emptyList(),
                Collections.<Npc>emptyList(), Collections.<Trap>emptyList(), QuestState.initial(),
                null, "Projectile wall test.");
    }

    private GameState combatRoomTestState() {
        int width = 9;
        int height = 5;
        Tile[][] tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = Tile.WALL;
            }
        }
        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 3; x++) {
                tiles[y][x] = Tile.FLOOR;
            }
            for (int x = 5; x <= 7; x++) {
                tiles[y][x] = Tile.FLOOR;
            }
        }
        tiles[2][4] = Tile.FLOOR;
        Position spawn = new Position(2, 2);
        World world = new World(width, height, tiles,
                asRooms(new Room(1, 1, 3, 3), new Room(5, 1, 3, 3)),
                Collections.emptyList(), spawn, new Position(7, 2), new Position(7, 2));
        GameState.PlayerState player = GameState.PlayerState.of(spawn.getX(), spawn.getY(), Direction.EAST, 0);
        List<Enemy> enemies = new ArrayList<Enemy>();
        enemies.add(Enemy.bug("room-bug", new Position(6, 2)));
        return GameState.restored(123L, 1, true, false, false, GameStatus.PLAYING, player, world,
                Inventory.empty(), Collections.<Item>emptyList(), enemies, Collections.<Npc>emptyList(),
                Collections.<Trap>emptyList(), QuestState.initial(), null, "Combat room test.");
    }

    private GameState activeAiRoomState(Enemy enemy, Position playerPosition) {
        int width = 10;
        int height = 5;
        Tile[][] tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = x == 0 || y == 0 || x == width - 1 || y == height - 1 ? Tile.WALL : Tile.FLOOR;
            }
        }
        World world = new World(width, height, tiles,
                Collections.singletonList(new Room(1, 1, 8, 3)),
                Collections.emptyList(), new Position(1, 2), new Position(8, 2), new Position(8, 2));
        GameState.PlayerState player = GameState.PlayerState.of(playerPosition.getX(), playerPosition.getY(),
                Direction.EAST, 0);
        List<Enemy> enemies = new ArrayList<Enemy>();
        enemies.add(enemy);
        List<RoomState> rooms = Collections.singletonList(new RoomState(0, RoomType.COMBAT, RoomStatus.ACTIVE, false));
        return GameState.restored(123L, 1, true, false, false, 0L, GameStatus.PLAYING, player, world,
                Inventory.empty(), Collections.<Item>emptyList(), enemies, Collections.<Projectile>emptyList(),
                rooms, Collections.<Npc>emptyList(), Collections.<Trap>emptyList(), QuestState.initial(),
                null, "AI test.");
    }

    private GameState trapTestState(Trap trap) {
        int width = 6;
        int height = 5;
        Tile[][] tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = x == 0 || y == 0 || x == width - 1 || y == height - 1 ? Tile.WALL : Tile.FLOOR;
            }
        }
        Position spawn = new Position(2, 2);
        World world = new World(width, height, tiles,
                Collections.singletonList(new Room(1, 1, 4, 3)),
                Collections.emptyList(), spawn, new Position(4, 2), new Position(4, 2));
        GameState.PlayerState player = GameState.PlayerState.of(spawn.getX(), spawn.getY(), Direction.EAST, 0);
        return GameState.restored(123L, 1, true, false, false, GameStatus.PLAYING, player, world,
                Inventory.empty(), Collections.<Item>emptyList(), Collections.<Enemy>emptyList(),
                Collections.<Npc>emptyList(), Collections.singletonList(trap), QuestState.initial(),
                null, "Trap test.");
    }

    private List<Room> asRooms(Room first, Room second) {
        List<Room> rooms = new ArrayList<Room>();
        rooms.add(first);
        rooms.add(second);
        return rooms;
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
        RoomState roomState = state.currentRoomState();
        if (roomState == null) {
            return state;
        }
        GameState current = state;
        for (Enemy enemy : state.getEnemies()) {
            if (enemy.isAlive() && current.roomStateAt(enemy.getPosition()) != null
                    && current.roomStateAt(enemy.getPosition()).getId() == roomState.getId()) {
                current = defeatEnemy(current, enemy.getId());
            }
        }
        return current;
    }

    private GameState defeatEnemy(GameState state, String enemyId) {
        Enemy enemy = findEnemy(state, enemyId);
        Position adjacent = adjacentWalkableTileInCurrentRoom(state, enemy.getPosition());
        GameState current = state.getPlayer().getPosition().equals(adjacent)
                ? state
                : applyPath(state, pathToInsideCurrentRoom(state, adjacent));
        Direction direction = directionBetween(current.getPlayer().getPosition(), enemy.getPosition());
        while (findEnemy(current, enemyId).isAlive()) {
            current = current.movePlayer(direction);
        }
        return current;
    }

    private GameState defeatBoss(GameState state) {
        Enemy boss = findEnemy(state, "defense-committee");
        Position adjacent = adjacentWalkableTile(state, boss.getPosition());
        GameState current = stateAfterPath(state, adjacent);
        Direction direction = directionBetween(adjacent, boss.getPosition());
        while (findEnemy(current, boss.getId()).isAlive()) {
            current = current.movePlayer(direction);
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

    private Enemy firstAliveEnemy(GameState state) {
        for (Enemy enemy : state.getEnemies()) {
            if (enemy.isAlive()) {
                return enemy;
            }
        }
        throw new AssertionError("Missing alive enemy.");
    }

    private Position adjacentWalkableTileInCurrentRoom(GameState state, Position target) {
        Room room = state.getWorld().getRooms().get(state.currentRoomState().getId());
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (Direction direction : directions) {
            Position candidate = new Position(target.getX() - direction.getDx(), target.getY() - direction.getDy());
            if (room.contains(candidate)
                    && state.getWorld().isWalkable(candidate)
                    && state.enemyAt(candidate) == null
                    && state.trapAt(candidate) == null) {
                return candidate;
            }
        }
        throw new AssertionError("Could not find adjacent room tile for " + target);
    }

    private GameState tryLeaveCurrentRoom(GameState state) {
        RoomState roomState = state.currentRoomState();
        Room room = state.getWorld().getRooms().get(roomState.getId());
        Queue<Position> queue = new ArrayDeque<Position>();
        Queue<String> paths = new ArrayDeque<String>();
        boolean[][] visited = new boolean[state.getWorld().getHeight()][state.getWorld().getWidth()];
        queue.add(state.getPlayer().getPosition());
        paths.add("");
        visited[state.getPlayer().getY()][state.getPlayer().getX()] = true;

        while (!queue.isEmpty()) {
            Position current = queue.remove();
            String path = paths.remove();
            Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
            for (Direction direction : directions) {
                Position next = new Position(current.getX() + direction.getDx(), current.getY() + direction.getDy());
                if (state.getWorld().contains(next.getX(), next.getY())
                        && state.getWorld().isWalkable(next)
                        && !visited[next.getY()][next.getX()]
                        && state.enemyAt(next) == null
                        && state.trapAt(next) == null) {
                    String nextPath = path + keyFor(direction);
                    if (!room.contains(next)) {
                        return applyPath(state, nextPath);
                    }
                    visited[next.getY()][next.getX()] = true;
                    queue.add(next);
                    paths.add(nextPath);
                }
            }
        }
        throw new AssertionError("Could not find room exit path.");
    }

    private String pathToInsideCurrentRoom(GameState state, Position target) {
        Room room = state.getWorld().getRooms().get(state.currentRoomState().getId());
        Queue<Position> queue = new ArrayDeque<Position>();
        Queue<String> paths = new ArrayDeque<String>();
        boolean[][] visited = new boolean[state.getWorld().getHeight()][state.getWorld().getWidth()];
        queue.add(state.getPlayer().getPosition());
        paths.add("");
        visited[state.getPlayer().getY()][state.getPlayer().getX()] = true;

        while (!queue.isEmpty()) {
            Position current = queue.remove();
            String path = paths.remove();
            if (current.equals(target)) {
                return path;
            }
            Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
            for (Direction direction : directions) {
                Position next = new Position(current.getX() + direction.getDx(), current.getY() + direction.getDy());
                if (room.contains(next)
                        && !visited[next.getY()][next.getX()]
                        && state.getWorld().isWalkable(next)
                        && state.enemyAt(next) == null
                        && state.trapAt(next) == null) {
                    visited[next.getY()][next.getX()] = true;
                    queue.add(next);
                    paths.add(path + keyFor(direction));
                }
            }
        }
        throw new AssertionError("Could not find in-room path to " + target);
    }

    private GameState applyPath(GameState state, String path) {
        GameState current = state;
        for (int i = 0; i < path.length(); i++) {
            current = current.movePlayer(InputCommand.fromKey(path.charAt(i)).getDirection());
        }
        return current;
    }

    private Trap findTrap(GameState state, String type) {
        for (Trap trap : state.getTraps()) {
            if (trap.getType().equals(type)) {
                return trap;
            }
        }
        throw new AssertionError("Missing trap type: " + type);
    }

    private Trap findTrapById(GameState state, String trapId) {
        for (Trap trap : state.getTraps()) {
            if (trap.getId().equals(trapId)) {
                return trap;
            }
        }
        throw new AssertionError("Missing trap: " + trapId);
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
                        && state.enemyAt(next) == null
                        && (next.equals(target) || state.trapAt(next) == null)) {
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
                        && state.enemyAt(next) == null
                        && (next.equals(target) || state.trapAt(next) == null)) {
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
