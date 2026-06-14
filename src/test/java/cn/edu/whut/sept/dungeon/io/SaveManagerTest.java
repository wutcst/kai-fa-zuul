package cn.edu.whut.sept.dungeon.io;

import cn.edu.whut.sept.dungeon.core.GameEngine;
import cn.edu.whut.sept.dungeon.core.GameState;
import cn.edu.whut.sept.dungeon.core.VisibilityState;
import org.junit.Test;

import java.io.File;

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
}
