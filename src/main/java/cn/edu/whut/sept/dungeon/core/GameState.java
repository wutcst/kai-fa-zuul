package cn.edu.whut.sept.dungeon.core;

import cn.edu.whut.sept.dungeon.world.Position;
import cn.edu.whut.sept.dungeon.world.World;
import cn.edu.whut.sept.dungeon.world.WorldGenerator;

public final class GameState {
    public static final int VISION_RADIUS = 5;

    private final Long seed;
    private final boolean started;
    private final boolean exited;
    private final boolean saveRequested;
    private final PlayerState player;
    private final World world;
    private final boolean[][] explored;
    private final boolean[][] visible;
    private final String message;

    private GameState(Long seed, boolean started, boolean exited, boolean saveRequested,
                      PlayerState player, World world, boolean[][] explored, boolean[][] visible, String message) {
        this.seed = seed;
        this.started = started;
        this.exited = exited;
        this.saveRequested = saveRequested;
        this.player = player;
        this.world = world;
        this.explored = copyGrid(explored);
        this.visible = copyGrid(visible);
        this.message = message;
    }

    public static GameState initial() {
        return new GameState(null, false, false, false, PlayerState.origin(),
                null, null, null, "Ready.");
    }

    public static GameState newGame(long seed) {
        World world = new WorldGenerator().generate(seed);
        PlayerState player = PlayerState.at(world.getSpawnPosition());
        return new GameState(seed, true, false, false, player, world,
                createExploredFor(world, player), createVisibleFor(world, player),
                "New game started with seed " + seed + ".");
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

    public boolean isExplored(int x, int y) {
        return isMarked(explored, x, y);
    }

    public boolean isVisible(int x, int y) {
        return isMarked(visible, x, y);
    }

    public int getVisibleCount() {
        return countMarked(visible);
    }

    public int getExploredCount() {
        return countMarked(explored);
    }

    public String getMessage() {
        return message;
    }

    public GameState withMessage(String nextMessage) {
        return new GameState(seed, started, exited, saveRequested, player, world,
                explored, visible, nextMessage);
    }

    public GameState markExited() {
        return new GameState(seed, started, true, saveRequested, player, world,
                explored, visible, message);
    }

    public GameState markSaveRequested() {
        return new GameState(seed, started, exited, true, player, world,
                explored, visible, message);
    }

    public GameState movePlayer(Direction direction) {
        if (!started || world == null || direction == null) {
            return withMessage("Start a new game first.");
        }

        PlayerState turnedPlayer = player.withDirection(direction);
        Position target = new Position(player.getX() + direction.getDx(), player.getY() + direction.getDy());
        if (!world.isWalkable(target)) {
            return new GameState(seed, started, exited, saveRequested, turnedPlayer, world,
                    explored, visible, "Blocked by wall.");
        }

        PlayerState movedPlayer = turnedPlayer.moveTo(target);
        return new GameState(seed, started, exited, saveRequested, movedPlayer, world,
                createExploredFor(world, movedPlayer, explored), createVisibleFor(world, movedPlayer),
                "Moved " + direction.name() + ".");
    }

    public static final class PlayerState {
        private final int x;
        private final int y;
        private final Direction direction;
        private final int steps;

        private PlayerState(int x, int y, Direction direction, int steps) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.steps = steps;
        }

        public static PlayerState origin() {
            return new PlayerState(0, 0, Direction.SOUTH, 0);
        }

        public static PlayerState at(Position position) {
            return new PlayerState(position.getX(), position.getY(), Direction.SOUTH, 0);
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

        public int getSteps() {
            return steps;
        }

        public Position getPosition() {
            return new Position(x, y);
        }

        private PlayerState withDirection(Direction nextDirection) {
            return new PlayerState(x, y, nextDirection, steps);
        }

        private PlayerState moveTo(Position position) {
            return new PlayerState(position.getX(), position.getY(), direction, steps + 1);
        }
    }

    private static boolean[][] createVisibleFor(World world, PlayerState player) {
        boolean[][] nextVisible = new boolean[world.getHeight()][world.getWidth()];
        markVision(world, player, nextVisible);
        return nextVisible;
    }

    private static boolean[][] createExploredFor(World world, PlayerState player) {
        boolean[][] nextExplored = new boolean[world.getHeight()][world.getWidth()];
        markVision(world, player, nextExplored);
        return nextExplored;
    }

    private static boolean[][] createExploredFor(World world, PlayerState player, boolean[][] previousExplored) {
        boolean[][] nextExplored = copyGrid(previousExplored);
        if (nextExplored == null) {
            nextExplored = new boolean[world.getHeight()][world.getWidth()];
        }
        markVision(world, player, nextExplored);
        return nextExplored;
    }

    private static void markVision(World world, PlayerState player, boolean[][] target) {
        for (int y = player.getY() - VISION_RADIUS; y <= player.getY() + VISION_RADIUS; y++) {
            for (int x = player.getX() - VISION_RADIUS; x <= player.getX() + VISION_RADIUS; x++) {
                if (world.contains(x, y)) {
                    target[y][x] = true;
                }
            }
        }
    }

    private static boolean isMarked(boolean[][] grid, int x, int y) {
        return grid != null && y >= 0 && y < grid.length && x >= 0 && x < grid[y].length && grid[y][x];
    }

    private static int countMarked(boolean[][] grid) {
        if (grid == null) {
            return 0;
        }
        int count = 0;
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                if (grid[y][x]) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean[][] copyGrid(boolean[][] source) {
        if (source == null) {
            return null;
        }
        boolean[][] copy = new boolean[source.length][];
        for (int y = 0; y < source.length; y++) {
            copy[y] = new boolean[source[y].length];
            System.arraycopy(source[y], 0, copy[y], 0, source[y].length);
        }
        return copy;
    }
}
