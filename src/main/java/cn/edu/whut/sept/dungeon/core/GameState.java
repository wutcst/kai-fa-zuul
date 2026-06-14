package cn.edu.whut.sept.dungeon.core;

import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.World;
import cn.edu.whut.sept.dungeon.world.WorldGenerator;

public final class GameState {
    private final Long seed;
    private final boolean started;
    private final boolean exited;
    private final boolean saveRequested;
    private final PlayerState player;
    private final World world;
    private final String message;

    private GameState(Long seed, boolean started, boolean exited, boolean saveRequested,
                      PlayerState player, World world, String message) {
        this.seed = seed;
        this.started = started;
        this.exited = exited;
        this.saveRequested = saveRequested;
        this.player = player;
        this.world = world;
        this.message = message;
    }

    public static GameState initial() {
        return new GameState(null, false, false, false, PlayerState.origin(),
                null, "Ready.");
    }

    public static GameState newGame(long seed) {
        World world = new WorldGenerator().generate(seed);
        return new GameState(seed, true, false, false, PlayerState.at(world.getSpawnPosition()),
                world, "New game started with seed " + seed + ".");
    }

    public Long getSeed() {
        return seed;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isExited() {
        return exited;
    }

    public boolean isSaveRequested() {
        return saveRequested;
    }

    public PlayerState getPlayer() {
        return player;
    }

    public String getWorldStatus() {
        return world == null ? "not-created" : "generated";
    }

    public World getWorld() {
        return world;
    }

    public String getMessage() {
        return message;
    }

    public GameState withMessage(String nextMessage) {
        return new GameState(seed, started, exited, saveRequested, player, world, nextMessage);
    }

    public GameState markExited() {
        return new GameState(seed, started, true, saveRequested, player, world, message);
    }

    public GameState markSaveRequested() {
        return new GameState(seed, started, exited, true, player, world, message);
    }

    public static final class PlayerState {
        private final int x;
        private final int y;
        private final Direction direction;

        private PlayerState(int x, int y, Direction direction) {
            this.x = x;
            this.y = y;
            this.direction = direction;
        }

        public static PlayerState origin() {
            return new PlayerState(0, 0, Direction.SOUTH);
        }

        public static PlayerState at(Position position) {
            return new PlayerState(position.getX(), position.getY(), Direction.SOUTH);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Direction getDirection() {
            return direction;
        }
    }
}
