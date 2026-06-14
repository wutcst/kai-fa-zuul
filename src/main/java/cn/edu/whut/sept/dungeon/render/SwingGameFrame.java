package cn.edu.whut.sept.dungeon.render;

import cn.edu.whut.sept.dungeon.core.GameEngine;
import cn.edu.whut.sept.dungeon.core.InputCommand;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public final class SwingGameFrame extends JFrame {
    private final GameEngine engine;
    private final TilePanel tilePanel;
    private final HudPanel hudPanel;

    public SwingGameFrame(GameEngine engine) {
        super("Campus Dungeon");
        this.engine = engine;
        this.tilePanel = new TilePanel();
        this.hudPanel = new HudPanel();

        setLayout(new BorderLayout());
        add(tilePanel, BorderLayout.CENTER);
        add(hudPanel, BorderLayout.SOUTH);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        addKeyListener(new MovementKeyListener());
        refresh();
        pack();
        setLocationRelativeTo(null);
    }

    public void showWindow() {
        setVisible(true);
        requestFocusInWindow();
    }

    private void refresh() {
        tilePanel.setState(engine.getState());
        hudPanel.setState(engine.getState());
    }

    private final class MovementKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent event) {
            InputCommand command = InputCommand.fromKey(event.getKeyChar());
            engine.handleInput(command);
            refresh();
        }
    }
}
