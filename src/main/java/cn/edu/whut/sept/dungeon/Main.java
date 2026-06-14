package cn.edu.whut.sept.dungeon;

import cn.edu.whut.sept.dungeon.core.GameEngine;
import cn.edu.whut.sept.dungeon.core.InputCommand;
import cn.edu.whut.sept.dungeon.render.SwingGameFrame;

import javax.swing.SwingUtilities;

public final class Main {
    private static final long DEFAULT_SEED = 20260614L;

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                GameEngine engine = new GameEngine();
                engine.handleInput(InputCommand.newGame(readSeed(args)));
                SwingGameFrame frame = new SwingGameFrame(engine);
                frame.showWindow();
            }
        });
    }

    private static long readSeed(String[] args) {
        if (args == null || args.length == 0) {
            return DEFAULT_SEED;
        }
        try {
            return Long.parseLong(args[0]);
        } catch (NumberFormatException exception) {
            return DEFAULT_SEED;
        }
    }
}
