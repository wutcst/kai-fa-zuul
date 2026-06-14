package cn.edu.whut.sept.dungeon.render;

import cn.edu.whut.sept.dungeon.core.GameEngine;
import cn.edu.whut.sept.dungeon.core.GameState;
import org.junit.Test;

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
}
