package cn.edu.whut.sept.dungeon.render;

import cn.edu.whut.sept.dungeon.core.GameState;
import cn.edu.whut.sept.dungeon.world.World;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

public final class TilePanel extends JPanel {
    private final TileRenderer renderer;
    private GameState state;

    public TilePanel() {
        this.renderer = new TileRenderer();
        setPreferredSize(new Dimension(World.DEFAULT_WIDTH * TileRenderer.TILE_SIZE,
                World.DEFAULT_HEIGHT * TileRenderer.TILE_SIZE));
        setFocusable(false);
    }

    public void setState(GameState state) {
        this.state = state;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        renderer.draw(state, (Graphics2D) graphics);
    }
}
