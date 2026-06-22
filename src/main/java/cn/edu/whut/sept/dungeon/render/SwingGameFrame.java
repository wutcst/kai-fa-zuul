package cn.edu.whut.sept.dungeon.render;

import cn.edu.whut.sept.dungeon.core.GameEngine;
import cn.edu.whut.sept.dungeon.core.GameText;
import cn.edu.whut.sept.dungeon.core.InputCommand;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

public class SwingGameFrame extends JFrame {
    private static final int TICK_DELAY_MILLIS = 50;
    private static final int MAX_WINDOW_HEIGHT = 720;

    private final GameEngine engine;
    private final TilePanel tilePanel;
    private final StatusPanel statusPanel;
    private final LogPanel logPanel;
    private final Timer timer;

    public SwingGameFrame(GameEngine engine) {
        super("Campus Dungeon");
        GameFonts.installDefaults();
        this.engine = engine;
        this.tilePanel = new TilePanel();
        this.statusPanel = new StatusPanel();
        this.logPanel = new LogPanel();
        this.timer = new Timer(TICK_DELAY_MILLIS, event -> {
            engine.tick();
            refresh();
        });

        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(12, 14, 18));
        add(createTileFrame(), BorderLayout.CENTER);
        add(statusPanel, BorderLayout.EAST);
        add(logPanel, BorderLayout.SOUTH);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        addKeyListener(new MovementKeyListener());
        refresh();
        pack();
        fitToScreen();
        setLocationRelativeTo(null);
    }

    public void showWindow() {
        setVisible(true);
        timer.start();
        requestFocusInWindow();
    }

    private void refresh() {
        tilePanel.setState(engine.getState());
        statusPanel.setState(engine.getState());
        logPanel.setState(engine.getState());
    }

    private JPanel createTileFrame() {
        JPanel frame = new JPanel(new BorderLayout());
        frame.setBackground(new Color(12, 14, 18));
        frame.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(18, 18, 18, 18),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(93, 82, 64), 2),
                        BorderFactory.createLineBorder(new Color(28, 32, 39), 8))));
        frame.add(tilePanel, BorderLayout.CENTER);
        return frame;
    }

    private void fitToScreen() {
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int safeWidth = Math.min(getWidth(), bounds.width);
        int safeHeight = Math.min(getHeight(), Math.min(bounds.height, MAX_WINDOW_HEIGHT));
        if (safeWidth != getWidth() || safeHeight != getHeight()) {
            setSize(safeWidth, safeHeight);
        }
        setMinimumSize(new Dimension(safeWidth, safeHeight));
        setMaximumSize(new Dimension(safeWidth, safeHeight));
    }

    private void promptForPuzzleAnswer() {
        boolean wasRunning = timer.isRunning();
        if (wasRunning) {
            timer.stop();
        }
        try {
            String answer = requestPuzzleAnswer();
            if (answer == null) {
                return;
            }
            engine.handleInput(InputCommand.answer(answer));
            refresh();
        } finally {
            if (wasRunning && isDisplayable()) {
                timer.start();
                requestFocusInWindow();
            }
        }
    }

    String requestPuzzleAnswer() {
        return JOptionPane.showInputDialog(this, "请输入 Maven 核心配置文件名：", "Maven 谜题",
                JOptionPane.QUESTION_MESSAGE);
    }

    private final class MovementKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent event) {
            InputCommand command = InputCommand.fromKey(event.getKeyChar());
            engine.handleInput(command);
            refresh();
            if (shouldPromptForPuzzleAnswer(command, engine.getState().getMessage())) {
                promptForPuzzleAnswer();
            }
        }
    }

    static boolean shouldPromptForPuzzleAnswer(InputCommand command, String message) {
        return command != null
                && command.getType() == InputCommand.Type.INTERACT
                && GameText.assistantMavenPuzzle().equals(message);
    }
}
