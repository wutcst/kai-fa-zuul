package cn.edu.whut.sept.dungeon.core;

import org.junit.Test;

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
}
