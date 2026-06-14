package cn.edu.whut.sept.dungeon.core;

public final class GameResult {
    private final GameState state;

    private GameResult(GameState state) {
        this.state = state;
    }

    public static GameResult of(GameState state) {
        return new GameResult(state);
    }

    public GameState getState() {
        return state;
    }
}
